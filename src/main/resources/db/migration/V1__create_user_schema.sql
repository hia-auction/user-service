CREATE TABLE users (
    id UUID PRIMARY KEY,

    auth_subject VARCHAR(255) UNIQUE NOT NULL ,
    email VARCHAR(255) NOT NULL UNIQUE ,

    role VARCHAR(20) NOT NULL
        CHECK (role IN ('BUYER', 'SELLER', 'ADMIN')),

    name VARCHAR(100) NOT NULL ,

    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'ACTIVE', 'BANNED', 'WITHDRAWN')),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE seller_profiles (
    id UUID PRIMARY KEY ,

    user_id UUID NOT NULL UNIQUE ,

    artist_name VARCHAR(100) NOT NULL ,
    business_number VARCHAR(20) ,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_seller_profile_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE seller_accounts (
    id UUID PRIMARY KEY,

    seller_id UUID NOT NULL UNIQUE,

    account_number VARCHAR(50) NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    account_holder VARCHAR(50) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_seller_accounts_seller
        FOREIGN KEY (seller_id)
        REFERENCES seller_profiles(id)
);