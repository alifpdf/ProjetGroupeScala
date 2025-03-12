import React, { useState, useEffect } from "react";
import { Line } from "react-chartjs-2";
import { Chart as ChartJS, LineElement, PointElement, LinearScale, CategoryScale } from "chart.js";

// 📌 Configuration Chart.js
ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale);

function RealTimeChart() {
    const [numberTechCorp, setNumberTechCorp] = useState("En attente...");
    const [numberGoogle, setNumberGoogle] = useState("En attente...");
    const [numberNasdaq, setNumberNasdaq] = useState("En attente...");

    const [dataTechCorp, setDataTechCorp] = useState([]);
    const [dataGoogle, setDataGoogle] = useState([]);
    const [dataNasdaq, setDataNasdaq] = useState([]);
    const [labels, setLabels] = useState([]);

    const [users, setUsers] = useState([]);
    const [investments, setInvestments] = useState([]);

    // 📌 Connexion WebSocket
    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => {
            console.log("✅ WebSocket connecté !");
        };

        ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                console.log("📢 Message WebSocket reçu :", message);

                if (message.type === "random") {
                    // ✅ Récupération des valeurs et conversion en nombre
                    const newTechCorp = parseInt(message.data, 10);
                    const newGoogle = parseInt(message.data1, 10);
                    const newNasdaq = parseInt(message.data2, 10);

                    if (!isNaN(newTechCorp)) {
                        setNumberTechCorp(newTechCorp);
                        setDataTechCorp((prev) => [...prev.slice(-9), newTechCorp]);
                    }

                    if (!isNaN(newGoogle)) {
                        setNumberGoogle(newGoogle);
                        setDataGoogle((prev) => [...prev.slice(-9), newGoogle]);
                    }

                    if (!isNaN(newNasdaq)) {
                        setNumberNasdaq(newNasdaq);
                        setDataNasdaq((prev) => [...prev.slice(-9), newNasdaq]);
                    }

                    setLabels((prev) => [...prev.slice(-9), new Date().toLocaleTimeString()]);
                } else if (message.type === "update") {
                    console.log("📢 Mise à jour reçue :", message);
                    setUsers(message.users);
                    setInvestments(message.investments);
                }
            } catch (error) {
                console.error("❌ Erreur de parsing JSON :", error);
            }
        };

        ws.onerror = (error) => {
            console.error("❌ Erreur WebSocket :", error);
        };

        return () => ws.close();
    }, []);

    // 📌 Fonction pour récupérer la somme d'un investissement
    const recupererSomme = async (companyName, userId, sommeInvesti) => {
        try {
            const response = await fetch("http://localhost:8080/api/recuperer-somme", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ companyName, userId, sommeInvesti })
            });

            const data = await response.json();
            console.log("✅ Réponse du serveur :", data);

            if (data.success) {
                alert("✅ Somme récupérée avec succès !");
                fetchUpdatedData();
            }
        } catch (error) {
            console.error("❌ Erreur lors de la requête :", error);
            alert("❌ Une erreur s'est produite.");
        }
    };

    // 📌 Fonction pour récupérer les données mises à jour après une récupération de somme
    const fetchUpdatedData = async () => {
        try {
            const response = await fetch("http://localhost:8080/api/get-investments"); // ✅ Change l'endpoint
            const message = await response.json();

            console.log("📢 Mise à jour des données :", message);
            setUsers(Array.isArray(message.users) ? message.users : []);
            setInvestments(Array.isArray(message.investments) ? message.investments : []);
        } catch (error) {
            console.error("❌ Erreur lors de la récupération des données mises à jour :", error);
        }
    };

    return (
        <div style={{ textAlign: "center", marginTop: "50px" }}>
            <h2>📈 Valeurs en temps réel</h2>
            <p style={{ fontWeight: "bold", fontSize: "20px", color: "blue" }}>TechCorp: {numberTechCorp}</p>
            <p style={{ fontWeight: "bold", fontSize: "20px", color: "red" }}>Google: {numberGoogle}</p>
            <p style={{ fontWeight: "bold", fontSize: "20px", color: "green" }}>Nasdaq: {numberNasdaq}</p>

            <div style={{ width: "700px", margin: "auto" }}>
                <h2>📊 Graphique en Temps Réel</h2>
                <Line
                    data={{
                        labels,
                        datasets: [
                            {
                                label: "TechCorp",
                                data: dataTechCorp,
                                borderColor: "blue",
                                fill: false
                            },
                            {
                                label: "Google",
                                data: dataGoogle,
                                borderColor: "red",
                                fill: false
                            },
                            {
                                label: "Nasdaq",
                                data: dataNasdaq,
                                borderColor: "green",
                                fill: false
                            }
                        ]
                    }}
                />
            </div>

            <h2>👥 Utilisateurs</h2>
            <ul>
                {users.map((user, index) => (
                    <li key={user.id || index}>
                        {user.name} - 💰 {user.balance}€
                    </li>
                ))}
            </ul>

            <h2>📈 Investissements</h2>
            <ul>
                {investments.map((inv, index) => (
                    <li key={inv.id || index}>
                        {inv.companyName} - 💰 {inv.amountInvested}€
                        <button
                            onClick={() => recupererSomme(inv.companyName, inv.userId, inv.amountInvested)}
                            style={{
                                marginLeft: "10px",
                                padding: "5px",
                                cursor: "pointer",
                                backgroundColor: "green",
                                color: "white",
                                border: "none",
                                borderRadius: "5px"
                            }}
                        >
                            Récupérer
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default RealTimeChart;
