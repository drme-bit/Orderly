CREATE TABLE users
(
    id           uuid         NOT NULL,
    first_name   varchar(255) NOT NULL,
    middle_name  varchar(255),
    last_name    varchar(255) NOT NULL,
    email        varchar(255) NOT NULL,
    country_code varchar(255) NOT NULL,
    city         varchar(255) NOT NULL,
    status       varchar(255) NOT NULL,
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    version      bigint       NOT NULL,

    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED', 'BLOCKED'))
);
