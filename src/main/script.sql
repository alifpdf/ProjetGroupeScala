CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     name VARCHAR(100),
    email VARCHAR(150) UNIQUE
    );

INSERT INTO users (name, email) VALUES ('Alice', 'alice@example.com');
