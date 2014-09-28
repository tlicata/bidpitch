CREATE TABLE players (
    id serial PRIMARY KEY,
    username varchar(255) UNIQUE,
    password varchar(255) NOT NULL
);
