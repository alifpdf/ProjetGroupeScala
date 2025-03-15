import React, { useState, useEffect } from "react";
import { Line } from "react-chartjs-2";
import { Chart as ChartJS, LineElement, PointElement, LinearScale, CategoryScale } from "chart.js";

ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale);

function RealTimeChart() {
    const [numberBTC, setNumberBTC] = useState("En attente...");
    const [numberETH, setNumberETH] = useState("En attente...");
    const [numberDOGE, setNumberDOGE] = useState("En attente...");

    const [dataBTC, setDataBTC] = useState([]);
    const [dataETH, setDataETH] = useState([]);
    const [dataDOGE, setDataDOGE] = useState([]);
    const [labels, setLabels] = useState([]);

    const [investments, setInvestments] = useState([]);
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user")));
    const [balance, setBalance] = useState(user ? user.balance : "Chargement..."); // Solde en attente initial
    const [lockedBalance, setLockedBalance] = useState(user ? user.balance : null); // Balance verrouillée

    // 🔽 Menu déroulant pour choisir l'entreprise et le nombre d'actions
    const [selectedCompany, setSelectedCompany] = useState("BTC");
    const [numShares, setNumShares] = useState(1);

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => console.log("✅ WebSocket connecté !");

        ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                console.log("📢 Message WebSocket reçu :", message);

                if (message.type === "random") {
                    const newBTC = parseFloat(message.data);
                    const newETH = parseFloat(message.data1);
                    const newDOGE = parseFloat(message.data2);

                    if (!isNaN(newBTC)) {
                        setNumberBTC(newBTC);
                        setDataBTC((prev) => [...prev.slice(-9), newBTC]);
                    }

                    if (!isNaN(newETH)) {
                        setNumberETH(newETH);
                        setDataETH((prev) => [...prev.slice(-9), newETH]);
                    }

                    if (!isNaN(newDOGE)) {
                        setNumberDOGE(newDOGE);
                        setDataDOGE((prev) => [...prev.slice(-9), newDOGE]);
                    }

                    setLabels((prev) => [...prev.slice(-9), new Date().toLocaleTimeString()]);
                } else if (message.type === "update") {
                    console.log("📢 Mise à jour reçue :", message);
                    if (user) {
                        const filteredInvestments = message.investments.filter(inv => inv.userId === user.id);
                        setInvestments(filteredInvestments);

                        // Nous ne mettons pas à jour la balance directement ici, elle sera verrouillée
                        // à moins qu'une action valide ne modifie le solde.
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
            case "BTC": return numberBTC;
            case "ETH": return numberETH;
            case "DOGE": return numberDOGE;
            default: return 0;
        }
    };

    // 📌 Fonction pour investir
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

                // Mettre à jour les investissements après avoir investi
                fetchUpdatedData();  // Met à jour les investissements
                fetchBalance();  // Met à jour le solde après investissement
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
                fetchUpdatedData(); // Récupérer les données mises à jour après récupération
                fetchBalance(); // Mettre à jour le solde après récupération
            }
        } catch (error) {
            console.error("❌ Erreur lors de la requête :", error);
            alert("❌ Une erreur s'est produite.");
        }
    };

    // 📌 Fonction pour récupérer les données mises à jour après investissement/récupération
    // 📌 Fonction pour récupérer les données mises à jour après investissement/récupération
    const fetchUpdatedData = async () => {
        try {
            const response = await fetch("http://localhost:8080/api/get-investments", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ userId: user.id })  // Ajouter l'ID de l'utilisateur dans la requête
            });

            const message = await response.json();

            console.log("📢 Mise à jour des données :", message);
            if (message.success) {
                setInvestments(Array.isArray(message.investments) ? message.investments : []);

                // ✅ Mise à jour du solde utilisateur après investissement/récupération
                if (message.updatedBalance !== undefined) {
                    const updatedUser = { ...user, balance: message.updatedBalance };
                    setUser(updatedUser);
                    localStorage.setItem("user", JSON.stringify(updatedUser));

                    // Mettre à jour la balance verrouillée seulement si la valeur a changé
                    if (lockedBalance !== message.updatedBalance) {
                        setLockedBalance(message.updatedBalance);
                    }
                }
            } else {
                console.error("❌ Erreur lors de la récupération des investissements :", message.message);
            }
        } catch (error) {
            console.error("❌ Erreur lors de la récupération des données mises à jour :", error);
        }
    };


    // 📌 Fonction pour récupérer le solde de l'utilisateur
    const fetchBalance = async () => {
        if (!user) return;
        try {
            const response = await fetch("http://localhost:8080/api/get-balance", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ userId: user.id })
            });

            const data = await response.json();
            if (data.success) {
                setBalance(data.balance);
                const updatedUser = { ...user, balance: data.balance };
                setUser(updatedUser);
                localStorage.setItem("user", JSON.stringify(updatedUser));

                // Verrouiller la balance après la récupération
                if (lockedBalance !== data.balance) {
                    setLockedBalance(data.balance);
                }
            }
        } catch (error) {
            console.error("❌ Erreur lors de la récupération du solde :", error);
        }
    };

    return (
        <div style={{ textAlign: "center", marginTop: "50px" }}>

            {/* Affichage du solde utilisateur verrouillé */}
            <h3>💰 Solde : {lockedBalance !== null ? `${lockedBalance}€` : "Chargement..."}</h3>

            <h2>📈 Valeurs en temps réel</h2>
            <p>BTC: {numberBTC}€</p>
            <p>ETH: {numberETH}€</p>
            <p>DOGE: {numberDOGE}€</p>

            <div style={{ width: "700px", margin: "auto" }}>
                <h2>📊 Graphique en Temps Réel</h2>
                <Line
                    data={{
                        labels,
                        datasets: [
                            { label: "BTC", data: dataBTC, borderColor: "blue", fill: false },
                            { label: "ETH", data: dataETH, borderColor: "red", fill: false },
                            { label: "DOGE", data: dataDOGE, borderColor: "green", fill: false }
                        ]
                    }}
                />
            </div>

            <h2>💰 Investir dans une action</h2>
            <select value={selectedCompany} onChange={(e) => setSelectedCompany(e.target.value)}>
                <option value="BTC">BTC</option>
                <option value="ETH">ETH</option>
                <option value="DOGE">DOGE</option>
            </select>
            <input type="number" min="1" value={numShares} onChange={(e) => setNumShares(parseInt(e.target.value) || 1)} />
            <button onClick={investir}>Investir</button>

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
