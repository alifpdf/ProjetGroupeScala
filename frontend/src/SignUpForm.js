import React, { useState } from "react";

function SignUpForm() {
    const [formData, setFormData] = useState({
        name: "",
        email: "",
        password: ""
    });
    const [message, setMessage] = useState(null);
    const [loading, setLoading] = useState(false);

    // 📌 Mise à jour des champs du formulaire
    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    // 📌 Vérification avant soumission
    const validateForm = () => {
        if (!formData.name || !formData.email || !formData.password) {
            setMessage({ type: "error", text: "❌ Veuillez remplir tous les champs." });
            return false;
        }

        return true;
    };

    // 📌 Soumission du formulaire
    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) return;

        setLoading(true);
        setMessage(null);


            const response = await fetch("http://localhost:8080/api/add-user", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(formData)
            });

            const result = await response.json();

            if (result.success) {
                setMessage({ type: "success", text: "✅ Inscription réussie !" });
                setFormData({ name: "", email: "", password: ""}); // Réinitialiser le formulaire
            }

        setLoading(false);
    };

    return (
        <div style={styles.container}>
            <h2>📝 Inscription</h2>
            {message && (
                <p style={message.type === "success" ? styles.successMessage : styles.errorMessage}>
                    {message.text}
                </p>
            )}
            <form onSubmit={handleSubmit} style={styles.form}>
                <input
                    type="text"
                    name="name"
                    placeholder="Nom"
                    value={formData.name}
                    onChange={handleChange}
                    style={styles.input}
                />
                <input
                    type="email"
                    name="email"
                    placeholder="Email"
                    value={formData.email}
                    onChange={handleChange}
                    style={styles.input}
                />
                <input
                    type="password"
                    name="password"
                    placeholder="Mot de passe"
                    value={formData.password}
                    onChange={handleChange}
                    style={styles.input}
                />

                <button type="submit" disabled={loading} style={styles.button}>
                    {loading ? "⏳ Inscription en cours..." : "S'inscrire"}
                </button>
            </form>
        </div>
    );
}

// 📌 Styles CSS en JS
const styles = {
    container: {
        width: "300px",
        margin: "auto",
        textAlign: "center",
        padding: "20px",
        border: "1px solid #ccc",
        borderRadius: "8px",
        boxShadow: "0px 0px 10px rgba(0,0,0,0.1)"
    },
    form: {
        display: "flex",
        flexDirection: "column",
        gap: "10px"
    },
    input: {
        padding: "10px",
        fontSize: "16px",
        borderRadius: "5px",
        border: "1px solid #ccc"
    },
    button: {
        padding: "10px",
        fontSize: "16px",
        backgroundColor: "#007BFF",
        color: "white",
        border: "none",
        borderRadius: "5px",
        cursor: "pointer"
    },
    successMessage: {
        color: "green",
        fontWeight: "bold"
    },
    errorMessage: {
        color: "red",
        fontWeight: "bold"
    }
};

export default SignUpForm;
