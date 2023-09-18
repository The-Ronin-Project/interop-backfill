# backfill-db
CREATE DATABASE IF NOT EXISTS `backfill-db`;
CREATE USER 'springuser'@'%' IDENTIFIED BY 'ThePassword';
GRANT ALL PRIVILEGES ON `backfill-db`.* TO 'springuser'@'%';
