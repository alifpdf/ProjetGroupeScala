import React, { useState, useEffect, useRef } from "react";

function InvestmentStrategies() {
    const [investments, setInvestments] = useState([]);
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user")));

    const [nav, setNav] = useState(0);
    const [sharpeRatio, setSharpeRatio] = useState(0);
    const [volatility, setVolatility] = useState(0);

    const wsRef = useRef(null);

    useEffect(() => {
        if (user && !wsRef.current) {
            connectWebSocket();
        }

        return () => {
            if (wsRef.current) {
                wsRef.current.close();
                wsRef.current = null;
            }
        };
    }, [user]);

    const connectWebSocket = () => {
        const ws = new WebSocket("ws://localhost:8080/ws");
        wsRef.current = ws;

        ws.onopen = () => {
            console.log("✅ WebSocket connecté !");
            ws.send(JSON.stringify({ type: "fetchInvestments", userId: user.id }));
            ws.send(JSON.stringify({ type: "fetchNAV", userId: user.id }));
        };

        ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            console.log("📢 Message WebSocket reçu :", message);

            if (message.type === "investments") {
                setInvestments(message.data);
                calculateFinancialIndicators(message.data);
            } else if (message.type === "nav") {
                setNav(message.data.nav);
            }
        };

        ws.onerror = (error) => {
            console.error("❌ Erreur WebSocket :", error);
        };

        ws.onclose = () => {
            console.warn("⚠️ WebSocket fermé. Tentative de reconnexion...");
            wsRef.current = null;
            setTimeout(() => {
                if (user) {
                    connectWebSocket();
                }
            }, 5000);
        };
    };

    const calculateFinancialIndicators = (investments) => {
        if (investments.length === 0) return;

        const rendements = investments.map(inv => inv.amountInvested * (Math.random() * 0.1 - 0.05));
        const meanRendement = rendements.reduce((a, b) => a + b, 0) / rendements.length;
        const riskFreeRate = 0.02;
        const variance = rendements.reduce((sum, r) => sum + Math.pow(r - meanRendement, 2), 0) / rendements.length;
        const stdDeviation = Math.sqrt(variance);

        setVolatility(stdDeviation);
        setSharpeRatio(stdDeviation === 0 ? 0 : (meanRendement - riskFreeRate) / stdDeviation);
    };

    // 📌 Définir une stratégie en fonction des indicateurs
    const getInvestmentStrategy = () => {
        if (sharpeRatio > 1 && volatility < 0.2) {
            return "🔵 Stratégie Défensive : Investissez dans des actifs sûrs (obligations, blue chips).";
        } else if (sharpeRatio > 1.5) {
            return "🟢 Stratégie Équilibrée : Mélangez actions, ETF et crypto pour diversifier.";
        } else if (sharpeRatio < 1 && volatility > 0.3) {
            return "🔴 Stratégie Agressive : Vous prenez trop de risques ! Diversifiez vos placements.";
        } else {
            return "⚪ Stratégie Neutre : Continuez à surveiller vos investissements.";
        }
    };


    return (
        <div style={{ textAlign: "center", marginTop: "50px" }}>
            <h2>📊 Stratégies d'Investissement</h2>
            {user ? (
                <>
                    <h3>💰 Valeur Nette du Portefeuille (NAV) : {nav.toFixed(2)}€</h3>
                    <h3>📈 Ratio de Sharpe : {sharpeRatio.toFixed(2)}</h3>
                    <h3>📉 Volatilité : {(volatility * 100).toFixed(2)}%</h3>

                    <h3>🧠 Stratégie Recommandée :</h3>
                    <p style={{ fontSize: "18px", fontWeight: "bold", color: "#007bff" }}>
                        {getInvestmentStrategy()}
                    </p>

                    <h2>📋 Investissements Actuels</h2>
                    <ul>
                        {investments.length > 0 ? (
                            investments.map((inv, index) => (
                                <li key={index}>
                                    {inv.companyName} - 💰 {inv.amountInvested}€
                                </li>
                            ))
                        ) : (
                            <p>📭 Aucun investissement trouvé.</p>
                        )}
                    </ul>
                </>
            ) : (
                <p>🔑 Connectez-vous pour voir vos investissements.</p>
            )}
        </div>
    );
}

export default InvestmentStrategies;
