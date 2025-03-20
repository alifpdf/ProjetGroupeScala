import React, { useState, useEffect } from "react";
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
        <div style={{ textAlign: "center", marginTop: "50px" }}>
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

            <div style={{ marginTop: "20px" }}>
                <h2>📈 Ratios Calculés (Auto)</h2>
                <p>BTC Ratio: {ratios.BTC}</p>
                <p>ETH Ratio: {ratios.ETH}</p>
                <p>DOGE Ratio: {ratios.DOGE}</p>
            </div>
            <div style={{ width: "600px", margin: "20px auto" }}>
                <h2>📊 Visualisation des Ratios</h2>
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



            {user ? (
                <>
                    <h3>💰 Solde : {lockedBalance !== null ? `${lockedBalance}€` : "Chargement..."}</h3>

                    <h2>💰 Investir dans une action</h2>
                    <select value={selectedCompany} onChange={(e) => setSelectedCompany(e.target.value)}>
                        <option value="BTC">BTC</option>
                        <option value="ETH">ETH</option>
                        <option value="DOGE">DOGE</option>
                    </select>
                    <input type="number" min="1" value={numShares} onChange={(e) => setNumShares(parseInt(e.target.value) || 1)} />
                    <button onClick={investir}>Investir</button>

                    <div style={{ display: "flex", justifyContent: "space-around", marginTop: "20px" }}>
                        <ul style={{ listStyleType: "none", padding: 0 }}>
                            {investments.map((inv, index) => (
                                <li key={inv.id || index} style={{ marginBottom: "10px" }}>
                                    {inv.companyName} - 💰 {inv.amountInvested}€ - {inv.originalPrice}
                                    <button onClick={() => recupererSomme(inv.companyName, inv.userId, inv.amountInvested)} style={{ marginLeft: "10px", backgroundColor: "red", color: "white", borderRadius: "5px" }}>
                                        Récupérer
                                    </button>
                                </li>
                            ))}
                        </ul>
                        <div style={{ width: "300px", height: "300px" }}>
                            <h2>🍰 Répartition des Investissements</h2>
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
                </>
            ) : (
                <div>
                    <h3>Veuillez vous connecter pour voir votre solde et vos investissements.</h3>
                </div>
            )}
        </div>
    );
}

export default RealTimeChart;