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
    BarElement,
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

    // Définir un mappage fixe des couleurs pour chaque entreprise
    const companyColorMap = {
        'BTC': '#FF6384',
        'ETH': '#36A2EB',
        'DOGE': '#FFCE56',
        // Couleurs supplémentaires pour d'autres entreprises potentielles
        'default1': '#4BC0C0',
        'default2': '#9966FF',
        'default3': '#FF9F40'
    };

    const [investmentData, setInvestmentData] = useState({
        labels: [],
        datasets: [{
            data: [],
            backgroundColor: [],
            hoverBackgroundColor: []
        }]
    });

    // connexion WebSocket et récupération des notifications
    useEffect(() => {
        const fetchLastPrices = async () => {
            const response = await fetch("http://localhost:8080/api/get-last-prices", {
                method: "POST",
                headers: { "Content-Type": "application/json" }
            });
            const data = await response.json();

            if (data.success) {
                const { BTC, ETH, DOGE } = data.prices;
                // dernier prix
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

    // connexion WebSocket et récupération des notifications
    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => console.log("WebSocket connecté");

        ws.onmessage = (event) => {
            const message = JSON.parse(event.data);

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
                if (user) {
                    const filteredInvestments = message.investments.filter(inv => inv.userId === user.id);
                    setInvestments(filteredInvestments);

                    // Mettre à jour les données du graphique en camembert avec des couleurs fixes
                    const investmentLabels = filteredInvestments.map(inv => inv.companyName);
                    const investmentAmounts = filteredInvestments.map(inv => inv.amountInvested);

                    // Utiliser le mappage de couleurs fixe pour chaque entreprise
                    const backgroundColors = investmentLabels.map(label =>
                        companyColorMap[label] || companyColorMap.default1);

                    setInvestmentData({
                        labels: investmentLabels,
                        datasets: [{
                            data: investmentAmounts,
                            backgroundColor: backgroundColors,
                            hoverBackgroundColor: backgroundColors
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
                    // sencé fixé les ratio (rendement) de chaque actions
                    BTC: data.BTC_ratio_sum.toFixed(2),
                    ETH: data.ETH_ratio_sum.toFixed(2),
                    DOGE: data.DOGE_ratio_sum.toFixed(2)
                });
            }
        } catch (err) {
            console.error("ERROR: calculate-sum ", err);
        }
    };

    const investir = async () => {
        if (!user) {
            alert("Vous devez être connecté pour investir");
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

        if (data.success) {
            alert(`Investissement réussi : ${numShares} actions de ${selectedCompany}`);

            fetchUpdatedData();
            fetchBalance();
            fetchCalculatedSum();  // MAJ des ratios automatiquement
        }
    };

    // appelle de l'API (route) recuperer-somme
    const recupererSomme = async (companyName, userId, sommeInvesti) => {
        const response = await fetch("http://localhost:8080/api/recuperer-somme", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ companyName, userId, sommeInvesti })
        });

        const data = await response.json();

        if (data.success) {
            alert("Somme récupérée");

            fetchUpdatedData();
            fetchBalance();
        }
    };

    // appelle de l'API (route) get-investments
    const fetchUpdatedData = async () => {
        const response = await fetch("http://localhost:8080/api/get-investments", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ userId: user.id })
        });

        const message = await response.json();

        if (message.success) {
            setInvestments(Array.isArray(message.investments) ? message.investments : []);

            if (Array.isArray(message.investments)) {
                const investmentLabels = message.investments.map(inv => inv.companyName);
                const investmentAmounts = message.investments.map(inv => inv.amountInvested);

                const backgroundColors = investmentLabels.map(label =>
                    companyColorMap[label] || companyColorMap.default1);

                setInvestmentData({
                    labels: investmentLabels,
                    datasets: [{
                        data: investmentAmounts,
                        backgroundColor: backgroundColors,
                        hoverBackgroundColor: backgroundColors
                    }]
                });
            }

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

    // MAJ de la balance (porte monnaie) appelle de l'api (route) get-balance
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

    /*  --- Le Html --- */
    return (
        <div className="chart-main-container">
            <h2 className="chart-title">Valeurs en temps réel</h2>

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

            <div className="chart-outer-container">
                <h2 className="chart-subtitle">Graphique en Temps Réel</h2>
                <div className="chart-inner-container">
                    <Line
                        data={{
                            labels,
                            datasets: [
                                { label: "BTC", data: dataBTC, borderColor: companyColorMap.BTC, fill: false },
                                { label: "ETH", data: dataETH, borderColor: companyColorMap.ETH, fill: false },
                                { label: "DOGE", data: dataDOGE, borderColor: companyColorMap.DOGE, fill: false }
                            ]
                        }}
                    />
                </div>
            </div>

            <div className="chart-outer-container">
                <h2 className="chart-subtitle">Ratios Calculés (Auto)</h2>
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

                <div className="chart-inner-container">
                    <Bar
                        data={{
                            labels: ["BTC", "ETH", "DOGE"],
                            datasets: [
                                {
                                    label: "Ratios Calculés",
                                    data: [ratios.BTC, ratios.ETH, ratios.DOGE],
                                    backgroundColor: [companyColorMap.BTC, companyColorMap.ETH, companyColorMap.DOGE],
                                    borderRadius: 5,
                                },
                            ],
                        }}
                    />
                </div>
            </div>

            {user ? (
                <div className="user-account-section">
                    <div className="user-balance-display">
                        Solde : {lockedBalance !== null ? `${lockedBalance}€` : "Chargement.."}
                    </div>

                    <h2 className="chart-subtitle">Investir dans une action</h2>
                    <div className="investment-controls">
                        <select
                            className="investment-select"
                            value={selectedCompany}
                            onChange={(e) => setSelectedCompany(e.target.value)}
                        >
                            <option value="BTC">BTC</option>
                            <option value="ETH">ETH</option>
                            <option value="DOGE">DOGE</option>
                        </select>
                        <input
                            className="investment-input"
                            type="number"
                            min="1"
                            value={numShares}
                            onChange={(e) => setNumShares(parseInt(e.target.value) || 1)}
                        />
                        <button className="investment-button" onClick={investir}>Investir</button>
                    </div>

                    <div className="investments-display">
                        <div className="investments-list-container">
                            <h2 className="chart-subtitle">Vos investissements</h2>
                            <ul>
                                {investments.map((inv, index) => (
                                    <li key={inv.id || index} className="investment-item">
                                        <span>{inv.companyName} - {inv.amountInvested}€ - {inv.originalPrice}</span>
                                        <button
                                            className="recover-button"
                                            onClick={() => recupererSomme(inv.companyName, inv.userId, inv.amountInvesti)}
                                        >
                                            Récupérer
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </div>

                        <div className="pie-chart-outer-container">
                            <h2 className="chart-subtitle">Répartition des Investissements</h2>
                            <Pie
                                data={investmentData}
                                options={{
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
                                }}
                            />
                        </div>
                    </div>
                </div>
            ) : (
                <div className="login-prompt">
                    <h3>Veuillez vous connecter pour voir votre solde et vos investissements.</h3>
                </div>
            )}
        </div>
    );
}

export default RealTimeChart;