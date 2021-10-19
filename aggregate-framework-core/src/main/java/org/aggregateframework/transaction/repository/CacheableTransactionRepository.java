package org.aggregateframework.transaction.repository;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.aggregateframework.transaction.Transaction;
import org.aggregateframework.transaction.TransactionOptimisticLockException;
import org.aggregateframework.transaction.TransactionXid;

import javax.transaction.xa.Xid;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by changmingxie on 10/30/15.
 */
public abstract class CacheableTransactionRepository implements TransactionRepository, AutoCloseable {

    private int expireDuration = 5;

    private int maxCacheSize = 20000;

    private Cache<Xid, Transaction> transactionXidCompensableTransactionCache;

    @Override
    public int create(Transaction transaction) {
        int result = doCreate(transaction);
        if (result > 0) {
            putToCache(transaction);
        }
        return result;
    }

    @Override
    public int update(Transaction transaction) {
        int result = 0;

        try {
            result = doUpdate(transaction);
            if (result > 0) {
                putToCache(transaction);
            } else {
                throw new TransactionOptimisticLockException();
            }
        } finally {
            if (result <= 0) {
                removeFromCache(transaction);
            }
        }

        return result;
    }

    @Override
    public int delete(Transaction transaction) {
        int result = 0;

        try {
            result = doDelete(transaction);

        } finally {
            removeFromCache(transaction);
        }
        return result;
    }

    @Override
    public Transaction findByXid(TransactionXid transactionXid) {
        Transaction transaction = findFromCache(transactionXid);

        if (transaction == null) {
            transaction = doFindOne(transactionXid);

            if (transaction != null) {
                putToCache(transaction);
            }
        }

        return transaction;
    }

    @Override
    public Page<Transaction> findAllUnmodifiedSince(Date date, String offset, int pageSize) {

        Page<Transaction> page = doFindAllUnmodifiedSince(date, offset, pageSize);

        for (Transaction transaction : page.getData()) {
            putToCache(transaction);
        }

        return page;
    }

    public CacheableTransactionRepository() {
        transactionXidCompensableTransactionCache = CacheBuilder.newBuilder()
                .expireAfterAccess(expireDuration, TimeUnit.SECONDS)
                .maximumSize(maxCacheSize)
                .build();
    }

    protected void putToCache(Transaction transaction) {
        transactionXidCompensableTransactionCache.put(transaction.getXid(), transaction);
    }

    protected void removeFromCache(Transaction transaction) {
        transactionXidCompensableTransactionCache.invalidate(transaction.getXid());
    }

    protected Transaction findFromCache(TransactionXid transactionXid) {
        return transactionXidCompensableTransactionCache.getIfPresent(transactionXid);
    }

    public void setExpireDuration(int durationInSeconds) {
        this.expireDuration = durationInSeconds;
    }

    protected abstract int doCreate(Transaction transaction);

    protected abstract int doUpdate(Transaction transaction);

    protected abstract int doDelete(Transaction transaction);

    protected abstract Transaction doFindOne(Xid xid);

    protected abstract Page<Transaction> doFindAllUnmodifiedSince(Date date, String offset, int pageSize);

    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
}
