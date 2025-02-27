DROP TABLE IF EXISTS users;

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL
);

INSERT INTO users (name, email, password)
VALUES ('Alice', 'alice@example.com', 'password123');

