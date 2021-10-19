CREATE TABLE `CQRS_CPL_ORDER` (
  `ORDER_ID` int(11) NOT NULL AUTO_INCREMENT,
  `USER_ID` int(11) DEFAULT NULL,
  `CONTENT` varchar(45) DEFAULT NULL,
  `PAYMENT_ID` int(11) DEFAULT NULL,
  `VERSION` int(11) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `LAST_UPDATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ORDER_ID`)
);

CREATE TABLE `CQRS_CPL_PAYMENT` (
  `PAYMENT_ID` int(11) NOT NULL AUTO_INCREMENT,
  `AMOUNT` decimal(10,0) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `LAST_UPDATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`PAYMENT_ID`)
);

CREATE TABLE `CQRS_CPL_SEAT_AVAILABILITY` (
  `SEAT_AVAILABILITY_ID` int(11) NOT NULL AUTO_INCREMENT,
  `ORDER_ID` int(11) DEFAULT NULL,
  `USER_ID` int(11) DEFAULT NULL,
  `QUANTITY` decimal(10,0) DEFAULT NULL,
  `PAYMENT_ID` int(11) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `LAST_UPDATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`SEAT_AVAILABILITY_ID`)
);


CREATE TABLE `CQRS_HIE_ORDER` (
  `ORDER_ID` int(11) NOT NULL AUTO_INCREMENT,
  `CONTENT` varchar(45) DEFAULT NULL,
  `ORDER_INFO_ID` int(11) DEFAULT NULL,
  `DELIVER` varchar(45) DEFAULT NULL,
  `JOB` varchar(45) DEFAULT NULL,
  `DTYPE` varchar(45) NOT NULL,
  `CREATE_TIME` varchar(45) DEFAULT NULL,
  `LAST_UPDATE_TIME` varchar(45) DEFAULT NULL,
  `VERSION` int(11) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ID`)
);

CREATE TABLE `CQRS_HIE_ORDER_INFO` (
  `ORDER_INFO_ID` int(11) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(45) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `LAST_UPDATE_TIME` datetime DEFAULT NULL,
  `DELIVERY_INFO` varchar(45) DEFAULT NULL,
  `JOB_INFO` varchar(45) DEFAULT NULL,
  `DTYPE` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`ORDER_INFO_ID`)
);

CREATE TABLE `CQRS_QCK_ORDER` (
  `ORDER_ID` bigint(19) NOT NULL AUTO_INCREMENT ,
  `TOTAL_AMOUNT` decimal(10,0) DEFAULT NULL,
  `STATUS_ID` int(11) DEFAULT NULL,
  `MERCHANT_ORDER_NO` varchar(45) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `LAST_UPDATE_TIME` datetime DEFAULT NULL,
  `VERSION` int(11) DEFAULT NULL,
  `PAYMENT_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ID`),
  UNIQUE KEY `MERCHANT_ORDER_NO_UNIQUE` (`MERCHANT_ORDER_NO`)
) ;

CREATE TABLE `CQRS_QCK_ORDER_LINE` (
  `ORDER_LINE_ID` bigint(19) NOT NULL AUTO_INCREMENT,
  `PRODUCT_ID` int(11) DEFAULT NULL,
  `PRICE` decimal(10,0) DEFAULT NULL,
  `ORDER_ID` bigint(19) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `LAST_UPDATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ORDER_LINE_ID`)
);

CREATE TABLE `CQRS_QCK_PAYMENT` (
  `PAYMENT_ID` bigint(19) NOT NULL AUTO_INCREMENT,
  `STATUS_ID` int(11) DEFAULT NULL,
  `TOTAL_AMOUNT` decimal(10,0) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `LAST_UPDATE_TIME` datetime DEFAULT NULL,
  `PAYMENT_NO` varchar(45) DEFAULT NULL,
  `ORDER_ID` bigint(19) DEFAULT NULL,
  `VERSION` int(11) DEFAULT NULL,
  PRIMARY KEY (`PAYMENT_ID`)
);