create table gameLog
(
    id int unsigned auto_increment,
    startTime DATETIME null,
    endTime DATETIME null,
    gameName varchar(32) null,
    P1 varchar(16) null,
    P2 varchar(16) null,
    P3 varchar(16) null,
    P4 varchar(16) null,
    chipRate double default 0.0,
    firstChips int default 0,
    P1Chips int default 0,
    P2Chips int default 0,
    P3Chips int default 0,
    P4Chips int default 0,

    primary key(id)
);

create table handsLog
(
    id int unsigned auto_increment,
    gameId int unsigned,
    P1card varchar(16) null,
    P2card varchar(16) null,
    P3card varchar(16) null,
    P4card varchar(16) null,
    community varchar(32) null,
    foldP varchar(20) null,

    primary key(id)
);

create table playerData
(
    id int unsigned auto_increment,
    name varchar(16) null,
    uuid varchar(36) unique not null,
    totalWin int default 0,
    win int default 0,

    primary key(id)
);

create index playerData_uuid_index on playerData(uuid);
