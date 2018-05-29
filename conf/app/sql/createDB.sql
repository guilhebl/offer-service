create database offerweb;

use offerweb;

CREATE TABLE scim_user (
    id varchar(128) PRIMARY KEY NOT NULL,
    userName varchar(128)
);

CREATE TABLE scim_group (
    id varchar(128) PRIMARY KEY NOT NULL,
    userName varchar(128)
);