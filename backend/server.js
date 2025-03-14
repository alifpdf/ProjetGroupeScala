const express = require('express');
const cors = require('cors');
const app = express();

// Utiliser CORS
app.use(cors());

// Définir des routes API comme exemple
app.get('/api/investments/:id', (req, res) => {
    res.json({ investments: [] });  // Exemple de réponse
});

app.get('/api/nav/:id', (req, res) => {
    res.json({ nav: 123.45 });  // Exemple de réponse
});

// Lancer le serveur
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log(`Backend is running on http://localhost:${PORT}`);
});
