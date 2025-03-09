import React, { useState } from "react";

function LoginForm() {
    const [formData, setFormData] = useState({ email: "", password: "" });
    const [message, setMessage] = useState(null);
    const [loading, setLoading] = useState(false);
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user")));

    const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.email || !formData.password) return setMessage("❌ Veuillez remplir tous les champs.");

        setLoading(true);
        try {
            const response = await fetch("http://localhost:8080/api/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formData)
            });
            const result = await response.json();
            if (result.success) {
                setUser(result.user);
                localStorage.setItem("user", JSON.stringify(result.user));
                setMessage("✅ Connexion réussie !");
            } else {
                setMessage(`❌ ${result.message}`);
            }
        } catch {
            setMessage("❌ Erreur de connexion au serveur.");
        }
        setLoading(false);
    };

    const handleLogout = () => {
        localStorage.removeItem("user");
        setUser(null);
    };

    return (
        <div>
            <h2>{user ? `🔑 Bienvenue, ${user.name}` : "🔑 Connexion"}</h2>
            {message && <p>{message}</p>}

            {!user ? (
                <form onSubmit={handleSubmit}>
                    <input type="email" name="email" placeholder="Email" value={formData.email} onChange={handleChange} />
                    <input type="password" name="password" placeholder="Mot de passe" value={formData.password} onChange={handleChange} />
                    <button type="submit" disabled={loading}>{loading ? "⏳ Connexion..." : "Se connecter"}</button>
                </form>
            ) : (
                <div>
                    <p>📧 {user.email}</p>
                    <p>💰 Solde: {user.balance}€</p>
                    <button onClick={handleLogout}>Déconnexion</button>
                </div>
            )}
        </div>
    );
}

export default LoginForm;
