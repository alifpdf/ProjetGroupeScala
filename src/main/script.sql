-- Supprime les tables si elles existent déjà (avec CASCADE)
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS investments CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;

-- Création de la table users avec un solde (`balance`)
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 -- Ajout du solde directement ici
);

-- Création de la table investments
CREATE TABLE investments (
                             id SERIAL PRIMARY KEY,
                             user_id INT NOT NULL,
                             company_name VARCHAR(255) NOT NULL,
                             amount_invested DECIMAL(15,2) NOT NULL CHECK (amount_invested >= 0),
                             original_price DECIMAL(15,2) NOT NULL CHECK (amount_invested >= 0),
                             FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

);

CREATE TABLE notifications (
                               id SERIAL PRIMARY KEY,
                               user_id INT REFERENCES users(id) ON DELETE CASCADE,
                               message TEXT NOT NULL,
                               timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
                          id SERIAL PRIMARY KEY,
                          owner_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          investment_id INT NOT NULL REFERENCES investments(id) ON DELETE CASCADE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          original_price NUMERIC(10,2) NOT NULL
);

-- Insertion d'un utilisateur avec un solde initial
INSERT INTO users (name, email, password, balance)
VALUES ('Alice', 'alice@example.com', 'password123', 100.00);

-- Insertion d'un investissement pour Alice
INSERT INTO investments (user_id, company_name, amount_invested,original_price)
VALUES ((SELECT id FROM users WHERE email = 'alice@example.com'), 'TechCorp', 50.00,8);
-- Insertion d'une notification pour l'utilisateur avec ID 1
INSERT INTO notifications (user_id, message)
VALUES (1, 'Bienvenue sur notre plateforme ! Votre compte a été créé avec succès.');

