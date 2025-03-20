import React, { useState, useEffect } from "react";
import "./RealTimesChart.css";
import { Line, Pie, Bar } from "react-chartjs-2";
import {
    Chart as ChartJS,
    LineElement,
    PointElement,
    LinearScale,
    CategoryScale,
    ArcElement,
    Tooltip,
    Legend,
    BarElement,   // 👈 Ajouté
} from "chart.js";

ChartJS.register(
    LineElement,
    PointElement,
    LinearScale,
    CategoryScale,
    ArcElement,
    Tooltip,
    Legend,
    BarElement
    // 👈 Enregistrement ajouté
);


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
    const [balance, setBalance] = useState(user ? user.balance : "Chargement...");
    const [lockedBalance, setLockedBalance] = useState(user ? user.balance : null);

    const [selectedCompany, setSelectedCompany] = useState("BTC");
    const [numShares, setNumShares] = useState(1);
    const [ratios, setRatios] = useState({ BTC: 0, ETH: 0, DOGE: 0 });


    const [investmentData, setInvestmentData] = useState({
        labels: [],
        datasets: [{
            data: [],
            backgroundColor: [],
            hoverBackgroundColor: []
        }]
    });

    useEffect(() => {
        // Fetch the latest prices on component mount
        const fetchLastPrices = async () => {

            const response = await fetch("http://localhost:8080/api/get-last-prices", {
                method: "POST",
                headers: { "Content-Type": "application/json" }
            });
            const data = await response.json();

            if (data.success) {
                const { BTC, ETH, DOGE } = data.prices;

                setNumberBTC(BTC[BTC.length - 1]);
                setNumberETH(ETH[ETH.length - 1]);
                setNumberDOGE(DOGE[DOGE.length - 1]);

                setDataBTC(BTC);
                setDataETH(ETH);
                setDataDOGE(DOGE);

                setLabels(BTC.map((_, index) => new Date().toLocaleTimeString()));
            }

        };

        fetchLastPrices();
    }, []);

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => console.log("✅ WebSocket connecté !");

        ws.onmessage = (event) => {

            const message = JSON.parse(event.data);
            console.log("📢 Message WebSocket reçu :", message);

            if (message.type === "random") {
                const newBTC = parseFloat(message.data);
                const newETH = parseFloat(message.data1);
                const newDOGE = parseFloat(message.data2);

                if (!isNaN(newBTC)) {
                    setNumberBTC(newBTC);
                    setDataBTC((prev) => [...prev, newBTC]);
                }

                if (!isNaN(newETH)) {
                    setNumberETH(newETH);
                    setDataETH((prev) => [...prev, newETH]);
                }

                if (!isNaN(newDOGE)) {
                    setNumberDOGE(newDOGE);
                    setDataDOGE((prev) => [...prev, newDOGE]);
                }

                setLabels((prev) => [...prev, new Date().toLocaleTimeString()]);
            } else if (message.type === "update") {
                console.log("📢 Mise à jour reçue :", message);
                if (user) {
                    const filteredInvestments = message.investments.filter(inv => inv.userId === user.id);
                    setInvestments(filteredInvestments);

                    // Mettre à jour les données du graphique en camembert
                    const investmentLabels = filteredInvestments.map(inv => inv.companyName);
                    const investmentAmounts = filteredInvestments.map(inv => inv.amountInvested);
                    const colors = ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40'];

                    setInvestmentData({
                        labels: investmentLabels,
                        datasets: [{
                            data: investmentAmounts,
                            backgroundColor: colors.slice(0, investmentLabels.length),
                            hoverBackgroundColor: colors.slice(0, investmentLabels.length)
                        }]
                    });
                }
            }

        };


        return () => ws.close();
    }, [user]);

    const getCurrentPrice = (company) => {
        switch (company) {
            case "BTC": return numberBTC;
            case "ETH": return numberETH;
            case "DOGE": return numberDOGE;
            default: return 0;
        }
    };
    const fetchCalculatedSum = async () => {
        if (!user) return;
        try {
            const response = await fetch("http://localhost:8080/api/calculate-sum", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    userId: user.id,
                    btcPrice: numberBTC,
                    ethPrice: numberETH,
                    dogePrice: numberDOGE
                })
            });
            const data = await response.json();
            if (data.success) {
                setRatios({
                    BTC: data.BTC_ratio_sum.toFixed(2),
                    ETH: data.ETH_ratio_sum.toFixed(2),
                    DOGE: data.DOGE_ratio_sum.toFixed(2)
                });
            }
        } catch (err) {
            console.error("❌ Erreur calculate-sum :", err);
        }
    };


    const investir = async () => {
        if (!user) {
            alert("❌ Vous devez être connecté pour investir.");
            return;
        }

        const totalInvestment = getCurrentPrice(selectedCompany) * numShares;


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
            fetchBalance();
            fetchCalculatedSum();  // 🔥 Mets à jour les ratios automatiquement

        }

    };

    const recupererSomme = async (companyName, userId, sommeInvesti) => {

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
            fetchBalance();
        }

    };

    const fetchUpdatedData = async () => {

        const response = await fetch("http://localhost:8080/api/get-investments", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ userId: user.id })
        });

        const message = await response.json();

        console.log("📢 Mise à jour des données :", message);
        if (message.success) {
            setInvestments(Array.isArray(message.investments) ? message.investments : []);

            if (message.updatedBalance !== undefined) {
                const updatedUser = { ...user, balance: message.updatedBalance };
                setUser(updatedUser);
                localStorage.setItem("user", JSON.stringify(updatedUser));

                if (lockedBalance !== message.updatedBalance) {
                    setLockedBalance(message.updatedBalance);
                }
            }
        }

    };

    const fetchBalance = async () => {
        if (!user) return;

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

            if (lockedBalance !== data.balance) {
                setLockedBalance(data.balance);
            }
        }

    };

    return (
        <div className="chart-main-container">
            <h2 className="chart-title">📈 Valeurs en temps réel</h2>
            <div className="crypto-values-grid">
                <div className="crypto-value-card">
                    <p className="crypto-value-text btc-text">BTC: {numberBTC}€</p>
                </div>
                <div className="crypto-value-card">
                    <p className="crypto-value-text eth-text">ETH: {numberETH}€</p>
                </div>
                <div className="crypto-value-card">
                    <p className="crypto-value-text doge-text">DOGE: {numberDOGE}€</p>
                </div>
            </div>

            <div className="chart-outer-container chart-width-limiter" style={{ width: "700px", margin: "auto" }}>
                <h2 className="chart-title">📊 Graphique en Temps Réel</h2>
                <div className="chart-inner-container">
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
            </div>

            <div>
                <h2 className="chart-title">📈 Ratios Calculés (Auto)</h2>
                <div className="ratios-grid">
                    <div className="ratio-value-card">
                        <p className="ratio-value-text btc-text">BTC Ratio: {ratios.BTC}</p>
                    </div>
                    <div className="ratio-value-card">
                        <p className="ratio-value-text eth-text">ETH Ratio: {ratios.ETH}</p>
                    </div>
                    <div className="ratio-value-card">
                        <p className="ratio-value-text doge-text">DOGE Ratio: {ratios.DOGE}</p>
                    </div>
                </div>
            </div>

            <div className="chart-outer-container chart-width-limiter" style={{ width: "600px", margin: "20px auto" }}>
                <h2 className="chart-title">📊 Visualisation des Ratios</h2>
                <div className="chart-inner-container">
                    <Bar
                        data={{
                            labels: ["BTC", "ETH", "DOGE"],
                            datasets: [
                                {
                                    label: "Ratios Calculés",
                                    data: [ratios.BTC, ratios.ETH, ratios.DOGE],
                                    backgroundColor: ["#FF6384", "#36A2EB", "#FFCE56"],
                                    borderRadius: 5,
                                },
                            ],
                        }}
                        options={{
                            responsive: true,
                            plugins: {
                                legend: { display: false },
                                tooltip: { enabled: true }
                            },
                            scales: {
                                y: {
                                    beginAtZero: true
                                }
                            }
                        }}
                    />
                </div>
            </div>

            {user ? (
                <div className="user-account-section">
                    <div className="user-balance-display">
                        <h3 className="chart-subtitle">💰 Solde : {lockedBalance !== null ? `${lockedBalance}€` : "Chargement..."}</h3>
                    </div>

                    <h2 className="chart-title">💰 Investir dans une action</h2>
                    <div className="investment-controls">
                        <select className="investment-select" value={selectedCompany} onChange={(e) => setSelectedCompany(e.target.value)}>
                            <option value="BTC">BTC</option>
                            <option value="ETH">ETH</option>
                            <option value="DOGE">DOGE</option>
                        </select>
                        <input className="investment-input" type="number" min="1" value={numShares} onChange={(e) => setNumShares(parseInt(e.target.value) || 1)} />
                        <button className="investment-button" onClick={investir}>Investir</button>
                    </div>

                    <div className="investments-display">
                        <div className="investments-list-container">
                            <ul>
                                {investments.map((inv, index) => (
                                    <li className="investment-item" key={inv.id || index}>
                                        <span>{inv.companyName} - 💰 {inv.amountInvested}€ - {inv.originalPrice}</span>
                                        <button className="recover-button" onClick={() => recupererSomme(inv.companyName, inv.userId, inv.amountInvesti)}>
                                            Récupérer
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </div>
                        <div className="pie-chart-outer-container">
                            <h2 className="chart-title">🍰 Répartition des Investissements</h2>
                            <div className="chart-inner-container">
                                <Pie data={investmentData} options={{
                                    plugins: {
                                        tooltip: {
                                            callbacks: {
                                                label: function (tooltipItem) {
                                                    const label = tooltipItem.label || '';
                                                    const value = tooltipItem.raw || 0;
                                                    const total = tooltipItem.dataset.data.reduce((acc, val) => acc + val, 0);
                                                    const percentage = ((value / total) * 100).toFixed(2);
                                                    return `${label}: ${value}€ (${percentage}%)`;
                                                }
                                            }
                                        },
                                        legend: {
                                            display: true,
                                            position: 'bottom'
                                        }
                                    }
                                }} />
                            </div>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="login-prompt">
                    <h3 className="chart-subtitle">Veuillez vous connecter pour voir votre solde et vos investissements.</h3>
                </div>
            )}
        </div>
    );
}

export default RealTimeChart;