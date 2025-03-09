import React, { useState } from "react";

function SignUpForm() {
    const [formData, setFormData] = useState({ name: "", email: "", password: "" });
    const [message, setMessage] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.name || !formData.email || !formData.password) {
            return setMessage("❌ Veuillez remplir tous les champs.");
        }

        setLoading(true);
        setMessage(null);

        try {
            const response = await fetch("http://localhost:8080/api/add-user", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formData)
            });
            const result = await response.json();

            if (result.success) {
                setMessage("✅ Inscription réussie !");
                setFormData({ name: "", email: "", password: "" });
            } else {
                setMessage(`❌ ${result.message}`);
            }
        } catch {
            setMessage("❌ Erreur de connexion au serveur.");
        }

        setLoading(false);
    };

    return (
        <div>
            <h2>📝 Inscription</h2>
            {message && <p>{message}</p>}

            <form onSubmit={handleSubmit}>
                <input type="text" name="name" placeholder="Nom" value={formData.name} onChange={handleChange} />
                <input type="email" name="email" placeholder="Email" value={formData.email} onChange={handleChange} />
                <input type="password" name="password" placeholder="Mot de passe" value={formData.password} onChange={handleChange} />
                <button type="submit" disabled={loading}>{loading ? "⏳ Inscription en cours..." : "S'inscrire"}</button>
            </form>
        </div>
    );
}

export default SignUpForm;
