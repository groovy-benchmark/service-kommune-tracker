CREATE TABLE `users` (
  `username` VARCHAR(191) NOT NULL,
  `creationDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `bcryptPassword` BINARY(60) NOT NULL,
  PRIMARY KEY (`username`),
  UNIQUE KEY `username` (`username`)
);

CREATE TABLE `permissions` (
  `username` VARCHAR(191) NOT NULL,
  `processId` varchar(191) NOT NULL,
  PRIMARY KEY (`username`, `processId`),
  CONSTRAINT `fk_username` FOREIGN KEY (`username`) REFERENCES `users`(`username`)
);

CREATE TABLE `trackedEvents` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processId` varchar(191) NOT NULL,
  `eventId` varchar(191) NOT NULL,
  `processInstanceId` int(11) NOT NULL,
  `userId` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `trackedEvents_index_processId` (`processId`),
  KEY `trackedEvents_index_eventsAndProcess` (`processId`,`eventId`)
);
