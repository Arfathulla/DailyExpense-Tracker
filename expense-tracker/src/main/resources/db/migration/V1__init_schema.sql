-- Baseline schema. Written to run unchanged on MySQL 8 and on H2 (MODE=MySQL, used for the zero-setup default).

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id      BIGINT      NOT NULL AUTO_INCREMENT,
    user_id BIGINT      NOT NULL,
    name    VARCHAR(50) NOT NULL,
    type    VARCHAR(10) NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_user_name_type UNIQUE (user_id, name, type),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_categories_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE TABLE transactions (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    user_id     BIGINT         NOT NULL,
    category_id BIGINT         NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    type        VARCHAR(10)    NOT NULL,
    note        VARCHAR(255),
    txn_date    DATE           NOT NULL,
    created_at  DATETIME(6),
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_transactions_type CHECK (type IN ('INCOME', 'EXPENSE'))
);
CREATE INDEX idx_txn_user_date ON transactions (user_id, txn_date);

CREATE TABLE budgets (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    user_id      BIGINT         NOT NULL,
    category_id  BIGINT         NOT NULL,
    budget_month VARCHAR(7)     NOT NULL,
    limit_amount DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT uk_budgets_user_category_month UNIQUE (user_id, category_id, budget_month),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE recurring_transactions (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    type           VARCHAR(10)    NOT NULL,
    note           VARCHAR(255),
    day_of_month   INT            NOT NULL,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    last_run_month VARCHAR(7),
    CONSTRAINT pk_recurring PRIMARY KEY (id),
    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_recurring_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_recurring_day CHECK (day_of_month BETWEEN 1 AND 31)
);
