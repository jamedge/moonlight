CREATE DATABASE moonlight;

CREATE TABLE IF NOT EXISTS `db_versions` (
    `db_version` INT NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`db_version`)
) ENGINE=InnoDB;

INSERT INTO db_versions VALUES(1, NOW());
