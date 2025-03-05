// components/RealTimeChart.js
import React, { useState, useEffect } from "react";
import { Line } from "react-chartjs-2";
import { Chart as ChartJS, LineElement, PointElement, LinearScale, CategoryScale } from "chart.js";

// **📌 Configuration Chart.js**
ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale);

function RealTimeChart() {
    const [number, setNumber] = useState("En attente...");
    const [dataPoints, setDataPoints] = useState([]); // Stocke les valeurs
    const [labels, setLabels] = useState([]); // Stocke les timestamps

    // 📌 **Fonction pour démarrer WebSocket**
    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => {
            console.log("✅ WebSocket connecté !");
        };

        ws.onmessage = (event) => {
            const newNumber = parseInt(event.data.trim(), 10); // Convertir en nombre

            if (!isNaN(newNumber)) {
                setNumber(newNumber);

                // ✅ Mettre à jour le graphique
                setDataPoints((prev) => [...prev.slice(-9), newNumber]); // Garde les 10 derniers points
                setLabels((prev) => [...prev.slice(-9), new Date().toLocaleTimeString()]); // Ajoute l'heure actuelle
            }
        };

        ws.onerror = (error) => {
            console.error("❌ Erreur WebSocket :", error);
        };

        return () => ws.close(); // 🔴 Ferme la connexion WebSocket quand le composant est démonté
    }, []);

    // 📌 **Configuration des données du graphique**
    const chartData = {
        labels: labels,
        datasets: [
            {
                label: "Valeur en temps réel",
                data: dataPoints,
                borderColor: "blue",
                backgroundColor: "rgba(0, 0, 255, 0.2)",
                tension: 0.2,
            },
        ],
    };

    // 📌 **Options du graphique**
    const options = {
        responsive: true,
        scales: {
            x: { title: { display: true, text: "Temps" } },
            y: { min: 0, max: 100, title: { display: true, text: "Valeur" } },
        },
    };

    return (
        <div style={{ textAlign: "center", marginTop: "50px" }}>
            <h2>🔢 Nombre en temps réel</h2>
            <p style={{ fontWeight: "bold", fontSize: "32px", color: "blue" }}>{number}</p>
            <div style={{ width: "600px", margin: "auto", textAlign: "center" }}>
                <h2>📊 Graphique en Temps Réel</h2>
                <Line data={chartData} options={options} />
            </div>
        </div>
    );
}

export default RealTimeChart;
