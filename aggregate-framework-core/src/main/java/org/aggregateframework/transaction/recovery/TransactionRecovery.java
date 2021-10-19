package org.aggregateframework.transaction.recovery;


import com.alibaba.fastjson.JSON;
import org.aggregateframework.transaction.Transaction;
import org.aggregateframework.transaction.TransactionOptimisticLockException;
import org.aggregateframework.transaction.TransactionType;
import org.aggregateframework.transaction.repository.LocalStorable;
import org.aggregateframework.transaction.repository.Page;
import org.aggregateframework.transaction.repository.SentinelTransactionRepository;
import org.aggregateframework.transaction.repository.TransactionRepository;
import org.aggregateframework.transaction.support.TransactionConfigurator;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.aggregateframework.transaction.TransactionStatus.CANCELLING;
import static org.aggregateframework.transaction.TransactionStatus.CONFIRMING;

/**
 * Created by changmingxie on 11/10/15.
 */
public class TransactionRecovery {

    static final Logger logger = LoggerFactory.getLogger(TransactionRecovery.class.getSimpleName());


    static volatile ExecutorService recoveryExecutorService = null;

    public static final int CONCURRENT_RECOVERY_TIMEOUT = 10;

    public static final int MAX_ERROR_COUNT_SHREDHOLD = 15;

    private AtomicInteger retryExceedMaxCount = new AtomicInteger();

    private AtomicInteger recoveryErrorCount = new AtomicInteger();

    private volatile int logMaxPrintCount = MAX_ERROR_COUNT_SHREDHOLD;

    private Lock logSync = new ReentrantLock();

    private TransactionConfigurator transactionConfigurator;

    public void setTransactionConfigurator(TransactionConfigurator transactionConfigurator) {
        this.transactionConfigurator = transactionConfigurator;
    }

    public void startRecover() {

        ensureRecoveryExecutorInitialized(transactionConfigurator.getRecoverFrequency().getConcurrentRecoveryThreadCount());

        logMaxPrintCount = transactionConfigurator.getRecoverFrequency().getFetchPageSize() / 2
                > MAX_ERROR_COUNT_SHREDHOLD ?
                MAX_ERROR_COUNT_SHREDHOLD : transactionConfigurator.getRecoverFrequency().getFetchPageSize() / 2;

        TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();

        if (transactionRepository instanceof SentinelTransactionRepository) {

            SentinelTransactionRepository sentinelTransactionRepository = (SentinelTransactionRepository) transactionRepository;

            if (!sentinelTransactionRepository.getSentinelController().degrade()) {
                startRecover(sentinelTransactionRepository.getWorkTransactionRepository());
            }

            startRecover(sentinelTransactionRepository.getDegradedTransactionRepository());

        } else {
            startRecover(transactionRepository);
        }
    }

    public void startRecover(TransactionRepository transactionRepository) {

        Lock recoveryLock = transactionRepository instanceof LocalStorable ? RecoveryLock.DEFAULT_LOCK : transactionConfigurator.getRecoveryLock();

        if (recoveryLock.tryLock()) {
            try {

                String offset = null;

                int totalCount = 0;
                do {

                    Page<Transaction> page = loadErrorTransactionsByPage(transactionRepository, offset);

                    if (page.getData().size() > 0) {
                       //recoverErrorTransactions(transactionRepository, page.getData());
                        concurrentRecoveryErrorTransactions(transactionRepository, page.getData());
                        offset = page.getNextOffset();
                        totalCount += page.getData().size();
                    } else {
                        break;
                    }
                } while (true);

                logger.info(String.format("total recovery count %d from repository:%s", totalCount, transactionRepository.getClass().getName()));
            } catch (Throwable e) {
                logger.error(String.format("recovery failed from repository:%s.", transactionRepository.getClass().getName()), e);
            } finally {
                recoveryLock.unlock();
            }
        }
    }

    private Page<Transaction> loadErrorTransactionsByPage(TransactionRepository transactionRepository, String offset) {

        long currentTimeInMillis = Instant.now().toEpochMilli();

        RecoverFrequency recoverFrequency = transactionConfigurator.getRecoverFrequency();

        return transactionRepository.findAllUnmodifiedSince(new Date(currentTimeInMillis - recoverFrequency.getRecoverDuration() * 1000), offset, recoverFrequency.getFetchPageSize());
    }


    private void concurrentRecoveryErrorTransactions(TransactionRepository transactionRepository, List<Transaction> transactions) throws InterruptedException, ExecutionException {

        retryExceedMaxCount.set(0);
        recoveryErrorCount.set(0);


        List<RecoverTask> tasks = new ArrayList<>();
        for (Transaction transaction : transactions) {
            tasks.add(new RecoverTask(transactionRepository, transaction));
        }

        List<Future<Void>> futures = recoveryExecutorService.invokeAll(tasks, CONCURRENT_RECOVERY_TIMEOUT, TimeUnit.SECONDS);

        for (Future future : futures) {
            future.get();
        }
    }

    private void recoverErrorTransactions(TransactionRepository transactionRepository, List<Transaction> transactions) {

        retryExceedMaxCount.set(0);
        recoveryErrorCount.set(0);

        for (Transaction transaction : transactions) {
            recoverErrorTransaction(transactionRepository, transaction);
        }
    }

    private void recoverErrorTransaction(TransactionRepository transactionRepository, Transaction transaction) {

        if (transaction.getRetriedCount() > transactionConfigurator.getRecoverFrequency().getMaxRetryCount()) {

            logSync.lock();
            try {
                if (retryExceedMaxCount.get() < logMaxPrintCount) {
                    logger.error(String.format(
                            "recover failed with max retry count,will not try again. txid:%s, status:%s,retried count:%d,transaction content:%s",
                            transaction.getXid(),
                            transaction.getStatus().getId(),
                            transaction.getRetriedCount(),
                            JSON.toJSONString(transaction)));
                    retryExceedMaxCount.incrementAndGet();
                } else if (retryExceedMaxCount.get() == logMaxPrintCount) {
                    logger.error("Too many transaction's retried count max then MaxRetryCount during one page transactions recover process , will not print errors again!");
                }

            } finally {
                logSync.unlock();
            }

            return;
        }

        if (transaction.getTransactionType().equals(TransactionType.BRANCH)
                && (transaction.getCreateTime().getTime() +
                transactionConfigurator.getRecoverFrequency().getMaxRetryCount() *
                        transactionConfigurator.getRecoverFrequency().getRecoverDuration() * 1000
                > System.currentTimeMillis())) {
            return;
        }

        try {
            transaction.setRetriedCount(transaction.getRetriedCount() + 1);

            if (transaction.getStatus().equals(CONFIRMING)) {

                transaction.setStatus(CONFIRMING);
                transactionRepository.update(transaction);
                transaction.commit();
                transactionRepository.delete(transaction);
            } else if (transaction.getStatus().equals(CANCELLING)
                    || transaction.getTransactionType().equals(TransactionType.ROOT)) {

                transaction.setStatus(CANCELLING);
                transactionRepository.update(transaction);
                transaction.rollback();
                transactionRepository.delete(transaction);
            }

        } catch (Throwable throwable) {

            if (throwable instanceof TransactionOptimisticLockException
                    || ExceptionUtils.getRootCause(throwable) instanceof TransactionOptimisticLockException) {

                logger.info(String.format(
                        "optimisticLockException happened while recover. txid:%s, status:%d,retried count:%d",
                        transaction.getXid(),
                        transaction.getStatus().getId(),
                        transaction.getRetriedCount()));
            } else {

                logSync.lock();
                try {
                    if (recoveryErrorCount.get() < logMaxPrintCount) {
                        logger.error(String.format("recover failed, txid:%s, status:%s,retried count:%d,transaction content:%s",
                                transaction.getXid(),
                                transaction.getStatus().getId(),
                                transaction.getRetriedCount(),
                                JSON.toJSONString(transaction)), throwable);
                        recoveryErrorCount.incrementAndGet();
                    } else if (recoveryErrorCount.get() == logMaxPrintCount) {
                        logger.error("Too many transaction's recover error during one page transactions recover process , will not print errors again!");
                    }
                } finally {
                    logSync.unlock();
                }
            }
        }
    }

    private void ensureRecoveryExecutorInitialized(int concurrentRecoveryThreadCount) {
        if (recoveryExecutorService == null) {
            synchronized (TransactionRecovery.class) {
                if (recoveryExecutorService == null) {
                    recoveryExecutorService = Executors.newFixedThreadPool(concurrentRecoveryThreadCount);
                }
            }
        }
    }

    class RecoverTask implements Callable<Void> {

        TransactionRepository transactionRepository;
        Transaction transaction;

        public RecoverTask(TransactionRepository transactionRepository, Transaction transaction) {
            this.transactionRepository = transactionRepository;
            this.transaction = transaction;
        }

        @Override
        public Void call() throws Exception {
            recoverErrorTransaction(transactionRepository, transaction);
            return null;
        }
    }

}

