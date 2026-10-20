-- This should not make any changes for MariaDB databases, but make h2 respect the upper/lower spelling

ALTER TABLE users CHANGE COLUMN `username` `username` VARCHAR(191);
ALTER TABLE users CHANGE COLUMN `creationDate` `creationDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users CHANGE COLUMN `bcryptPassword` `bcryptPassword` BINARY(60) NOT NULL;
ALTER TABLE users CHANGE COLUMN `isAdmin` `isAdmin` BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE permissions CHANGE COLUMN `username` `username` VARCHAR(191) NOT NULL;
ALTER TABLE permissions CHANGE COLUMN `processId` `processId` VARCHAR(191) NOT NULL;

ALTER TABLE trackedEvents CHANGE COLUMN `id` `id` INT(11) NOT NULL AUTO_INCREMENT;
ALTER TABLE trackedEvents CHANGE COLUMN `timestamp` `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE trackedEvents CHANGE COLUMN `processId` `processId` VARCHAR(191) NOT NULL;
ALTER TABLE trackedEvents CHANGE COLUMN `eventId` `eventId` VARCHAR(191) NOT NULL;
ALTER TABLE trackedEvents CHANGE COLUMN `processInstanceId` `processInstanceId` INT(11) NOT NULL;
ALTER TABLE trackedEvents CHANGE COLUMN `userId` `userId` VARCHAR(255) DEFAULT NULL;
