ALTER TABLE users
    RENAME COLUMN name TO nick_name;

ALTER TABLE users
ALTER COLUMN nick_name TYPE VARCHAR(20);

ALTER TABLE users
    ADD CONSTRAINT uk_users_nick_name UNIQUE (nick_name);