import React, { useState, useEffect } from "react";
import { Line } from "react-chartjs-2";
import { Chart as ChartJS, LineElement, PointElement, LinearScale, CategoryScale } from "chart.js";

ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale);

function RealTimeChart() {
    const [numberTechCorp, setNumberTechCorp] = useState("En attente...");
    const [numberGoogle, setNumberGoogle] = useState("En attente...");
    const [numberNasdaq, setNumberNasdaq] = useState("En attente...");

    const [dataTechCorp, setDataTechCorp] = useState([]);
    const [dataGoogle, setDataGoogle] = useState([]);
    const [dataNasdaq, setDataNasdaq] = useState([]);
    const [labels, setLabels] = useState([]);

    const [investments, setInvestments] = useState([]);
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user")));

    // 🔽 Menu déroulant pour choisir l'entreprise et le nombre d'actions
    const [selectedCompany, setSelectedCompany] = useState("TechCorp");
    const [numShares, setNumShares] = useState(1);

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => console.log("✅ WebSocket connecté !");

        ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                console.log("📢 Message WebSocket reçu :", message);

                if (message.type === "random") {
                    const newTechCorp = parseFloat(message.data);
                    const newGoogle = parseFloat(message.data1);
                    const newNasdaq = parseFloat(message.data2);

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
                    if (user) {
                        const filteredInvestments = message.investments.filter(inv => inv.userId === user.id);
                        setInvestments(filteredInvestments);
                    }
                }
            } catch (error) {
                console.error("❌ Erreur de parsing JSON :", error);
            }
        };

        ws.onerror = (error) => console.error("❌ Erreur WebSocket :", error);

        return () => ws.close();
    }, [user]);

    // 📌 Fonction pour récupérer le prix actuel de l'action sélectionnée
    const getCurrentPrice = (company) => {
        switch (company) {
            case "TechCorp": return numberTechCorp;
            case "Google": return numberGoogle;
            case "Nasdaq": return numberNasdaq;
            default: return 0;
        }
    };

    // 📌 Fonction pour investir
    const investir = async () => {
        if (!user) {
            alert("❌ Vous devez être connecté pour investir.");
            return;
        }

        const totalInvestment = getCurrentPrice(selectedCompany) * numShares;

        try {
            const response = await fetch("http://localhost:8080/api/investir", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    userId: user.id,
                    companyName: selectedCompany,
                    amount: totalInvestment,
                    numShares
                })
            });

            const data = await response.json();
            console.log("✅ Réponse du serveur :", data);

            if (data.success) {
                alert(`✅ Investissement réussi : ${numShares} actions de ${selectedCompany} !`);
                fetchUpdatedData();
            } else {
                alert("❌ Erreur : " + data.message);
            }
        } catch (error) {
            console.error("❌ Erreur lors de la requête :", error);
        }
    };

    // 📌 Fonction pour récupérer un investissement
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

    // 📌 Fonction pour récupérer les données mises à jour après investissement/récupération

    const fetchUpdatedData = async () => {
        try {
            const response = await fetch("http://localhost:8080/api/get-investments");
            const message = await response.json();

            console.log("📢 Mise à jour des données :", message);
            setInvestments(Array.isArray(message.investments) ? message.investments : []);

            // ✅ Mise à jour du solde utilisateur après investissement/récupération
            if (message.updatedBalance !== undefined) {
                const updatedUser = { ...user, balance: message.updatedBalance };
                setUser(updatedUser);
                localStorage.setItem("user", JSON.stringify(updatedUser));

                // 🔥 Déclencher un événement pour alerter `LoginForm.js`
                window.dispatchEvent(new Event("balanceUpdated"));
            }
        } catch (error) {
            console.error("❌ Erreur lors de la récupération des données mises à jour :", error);
        }
    };


    return (
        <div style={{ textAlign: "center", marginTop: "50px" }}>
            <h2>📈 Valeurs en temps réel</h2>
            <p>TechCorp: {numberTechCorp}€</p>
            <p>Google: {numberGoogle}€</p>
            <p>Nasdaq: {numberNasdaq}€</p>

            <div style={{ width: "700px", margin: "auto" }}>
                <h2>📊 Graphique en Temps Réel</h2>
                <Line
                    data={{
                        labels,
                        datasets: [
                            { label: "TechCorp", data: dataTechCorp, borderColor: "blue", fill: false },
                            { label: "Google", data: dataGoogle, borderColor: "red", fill: false },
                            { label: "Nasdaq", data: dataNasdaq, borderColor: "green", fill: false }
                        ]
                    }}
                />
            </div>

            <h2>💰 Investir dans une action</h2>
            <select value={selectedCompany} onChange={(e) => setSelectedCompany(e.target.value)}>
                <option value="TechCorp">TechCorp</option>
                <option value="Google">Google</option>
                <option value="Nasdaq">Nasdaq</option>
            </select>
            <input type="number" min="1" value={numShares} onChange={(e) => setNumShares(parseInt(e.target.value) || 1)} />
            <button onClick={investir}>Investir</button>

            <h2>📈 Portefeuille Utilisateur</h2>
            <ul>
                {investments.map((inv, index) => (
                    <li key={inv.id || index}>
                        {inv.companyName} - 💰 {inv.amountInvested}€
                        <button onClick={() => recupererSomme(inv.companyName, inv.userId, inv.amountInvested)} style={{ marginLeft: "10px", backgroundColor: "red", color: "white", borderRadius: "5px" }}>
                            Récupérer
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default RealTimeChart;
