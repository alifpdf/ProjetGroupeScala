-- Supprime les tables si elles existent déjà
DROP TABLE IF EXISTS investments;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;

-- Création de la table users
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL
);

-- Création de la table accounts
CREATE TABLE accounts (
                          id SERIAL PRIMARY KEY,
                          user_id INT UNIQUE NOT NULL,
                          balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Création de la table investments
CREATE TABLE investments (
                             id SERIAL PRIMARY KEY,
                             user_id INT NOT NULL,
                             company_name VARCHAR(255) NOT NULL,
                             amount_invested DECIMAL(15,2) NOT NULL CHECK (amount_invested >= 0),
                             FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insertion d'un utilisateur
INSERT INTO users (name, email, password)
VALUES ('Alice', 'alice@example.com', 'password123');

-- Insertion d'un compte associé à l'utilisateur Alice
INSERT INTO accounts (user_id, balance)
VALUES ((SELECT id FROM users WHERE email = 'alice@example.com'), 100.00);

-- Insertion d'un investissement pour Alice
INSERT INTO investments (user_id, company_name, amount_invested)
VALUES ((SELECT id FROM users WHERE email = 'alice@example.com'), 'TechCorp', 50.00);
