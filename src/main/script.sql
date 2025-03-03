-- Supprime les tables si elles existent déjà (avec CASCADE)
DROP TABLE IF EXISTS investments CASCADE;
DROP TABLE IF EXISTS users CASCADE;

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
                             FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insertion d'un utilisateur avec un solde initial
INSERT INTO users (name, email, password, balance)
VALUES ('Alice', 'alice@example.com', 'password123', 100.00);

-- Insertion d'un investissement pour Alice
INSERT INTO investments (user_id, company_name, amount_invested)
VALUES ((SELECT id FROM users WHERE email = 'alice@example.com'), 'TechCorp', 50.00);
