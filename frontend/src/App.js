import React, { useState } from "react";
import RealTimeChart from "./RealTimeChart";
import SignUpForm from "./SignUpForm";

function App() {
    const [page, setPage] = useState("home"); // 🔥 Gère la page affichée

    return (
        <div style={styles.container}>
            <h1>📊 Application Finance</h1>

            {/* ✅ Boutons de navigation */}
            <div style={styles.navbar}>
                <button onClick={() => setPage("signup")} style={styles.button}>S'inscrire</button>
                <button onClick={() => setPage("chart")} style={styles.button}>Voir le graphique</button>
            </div>

            {/* ✅ Affichage conditionnel des pages */}
            {page === "signup" ? <SignUpForm /> : <RealTimeChart />}
        </div>
    );
}

// 📌 Styles CSS en JS
const styles = {
    container: {
        textAlign: "center",
        padding: "20px"
    },
    navbar: {
        marginBottom: "20px",
        display: "flex",
        justifyContent: "center",
        gap: "10px"
    },
    button: {
        padding: "10px 15px",
        fontSize: "16px",
        backgroundColor: "#007BFF",
        color: "white",
        border: "none",
        borderRadius: "5px",
        cursor: "pointer"
    }
};

export default App;
