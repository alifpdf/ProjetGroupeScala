import React, { useState, useEffect } from "react";

function InvestmentStrategies() {
    const [investments, setInvestments] = useState([]);
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user")));
    const [nav, setNav] = useState(0); // Valeur nette du portefeuille (NAV)
    const [sharpeRatio, setSharpeRatio] = useState(0);
    const [volatility, setVolatility] = useState(0);
    const [ws, setWs] = useState(null); // WebSocket connection
    const [totalInvestments, setTotalInvestments] = useState(0); // Somme des investissements
    const [balance, setBalance] = useState(0); // Balance de l'utilisateur
    const [btc, setBtc] = useState(0);
    const [eth, setEth] = useState(0);
    const [doge, setDoge] = useState(0);

    // Connexion au WebSocket dès que l'utilisateur est connecté
    useEffect(() => {
        if (user) {
            const websocket = new WebSocket("ws://localhost:8080/ws");
            setWs(websocket);

            websocket.onopen = () => {
                console.log("✅ Connexion WebSocket établie");
            };

            websocket.onmessage = (event) => {
                const data = JSON.parse(event.data);
                console.log("Données reçues du WebSocket:", data);

                if (data.type === "update") {
                    console.log("📢 Mise à jour des investissements reçue:", data);

                    // Vérifier que la balance reçue est valide avant de la mettre à jour
                    if (data.balance && !isNaN(data.balance)) {
                        setBalance(data.balance); // Mise à jour de la balance
                    } else {
                        console.error("❌ Balance non valide:", data.balance);
                    }

                    if (data.investments) {
                        setInvestments(data.investments); // Mise à jour des investissements
                        calculateFinancialIndicators(data.investments); // Calcul des indicateurs financiers
                        calculateTotalInvestments(data.investments); // Calcul de la somme des investissements
                    } else {
                        console.error("❌ Investissements non trouvés:", data.investments);
                    }

                }
                if (data.type === "random") {
                    const newBTC = parseFloat(data.data);
                    const newETH = parseFloat(data.data1);
                    const newDOGE = parseFloat(data.data2);
                    setBtc(newBTC);
                    setEth(newETH);
                    setDoge(newDOGE);
                }
            };

            websocket.onerror = (error) => {
                console.error("❌ Erreur WebSocket:", error);
            };

            websocket.onclose = () => {
                console.log("❌ Connexion WebSocket fermée");
            };

            // Fermer la connexion WebSocket lors de la déconnexion de l'utilisateur
            return () => {
                if (websocket) {
                    websocket.close();
                }
            };
        }
    }, [user]);

    // Récupérer la balance via l'API REST
    useEffect(() => {
        if (user) {
            fetchBalance();
        }
    }, [user]);

    // Fonction pour récupérer la balance de l'utilisateur
    const fetchBalance = async () => {
        try {
            const response = await fetch("http://localhost:8080/api/get-balance", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ userId: user.id })
            });

            const data = await response.json();
            if (data.success) {
                setBalance(data.balance); // Mise à jour de la balance
                console.log(`📢 Balance mise à jour : ${data.balance}`);
            } else {
                console.error("❌ Erreur lors de la récupération de la balance :", data.message);
            }
        } catch (error) {
            console.error("❌ Erreur lors de la récupération du solde :", error);
        }
    };

    // Fonction pour calculer la somme des investissements
    const calculateTotalInvestments = (investments) => {
        const total = investments.reduce((sum, investment) => sum + investment.amountInvested, 0);
        setTotalInvestments(total); // Mise à jour du total des investissements
    };

    // Calcul des indicateurs financiers
    const calculateFinancialIndicators = (investments) => {
        if (investments.length === 0) return;

        const rendements = investments.map(inv => {
            const price = inv.amountInvested; // Pour l'exemple, on utilise l'amountInvested
            const randomRendement = Math.random() * 0.1 - 0.05; // Le rendement aléatoire entre -5% et +5%
            return price * randomRendement; // Calcul du rendement
        });

        const meanRendement = rendements.reduce((a, b) => a + b, 0) / rendements.length;
        const riskFreeRate = 0.02; // Taux sans risque à 2%
        const variance = rendements.reduce((sum, r) => sum + Math.pow(r - meanRendement, 2), 0) / rendements.length;
        const stdDeviation = Math.sqrt(variance); // Calcul de la déviation standard (volatilité)

        setVolatility(stdDeviation);
        setSharpeRatio(stdDeviation === 0 ? 0 : (meanRendement - riskFreeRate) / stdDeviation);
    };

    // Définir une stratégie en fonction des indicateurs financiers
    const getInvestmentStrategy = () => {
        if (sharpeRatio > 1 && volatility < 0.2) {
            return "🔵 Stratégie Défensive : Investissez dans des actifs sûrs (obligations, blue chips).";
        } else if (sharpeRatio > 1.5) {
            notifyStrategy("🟢 Stratégie Équilibrée : Mélangez actions, ETF et crypto pour diversifier.");
            return "🟢 Stratégie Équilibrée : Mélangez actions, ETF et crypto pour diversifier.";
        } else if (sharpeRatio < 1 && volatility > 0.3) {
            return "🔴 Stratégie Agressive : Vous prenez trop de risques ! Diversifiez vos placements.";
        } else {
            return "⚪ Stratégie Neutre : Continuez à surveiller vos investissements.";
        }
    };

    const getCurrentPrice = (company) => {
        switch (company) {
            case "BTC": return btc;
            case "ETH": return eth;
            case "DOGE": return doge;
            default: return 0;
        }
    };

    // Calculer la NAV : balance + total des investissements
    const calculateNav = () => {
        return balance + totalInvestments;
    };

    // Vérifier si les données sont valides avant d'appeler `toFixed`
    const safeToFixed = (value, decimals = 2) => {
        return value !== undefined && value !== null ? value.toFixed(decimals) : "0.00";
    };

    // Fonction pour notifier la stratégie au backend
    const notifyStrategy = async (strategy) => {
        // Utilisateur connecté
        const strategyMessage = {
            strategy: strategy,
            userId: user.id
        };

        try {
            const response = await fetch("http://localhost:8080/api/notify-strategy", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(strategyMessage)
            });

            await response.json();
        } catch (error) {
            console.error("❌ Erreur lors de la notification de la stratégie", error);
            alert(`❌ Erreur lors de l'envoi de la notification : ${error.message}`);
        }
    };

    return (
        <div style={{ textAlign: "center", marginTop: "50px" }}>
            <h2>📊 Stratégies d'Investissement</h2>
            {user ? (
                <>
                    <h3>💰 Valeur Nette du Portefeuille (NAV) : {safeToFixed(calculateNav())}</h3> {/* Calculer la NAV */}
                    <h3>📈 Ratio de Sharpe : {safeToFixed(sharpeRatio)}</h3>
                    <h3>📉 Volatilité : {safeToFixed(volatility * 100)}%</h3>

                    <h3>🧠 Stratégie Recommandée :</h3>
                    <p style={{ fontSize: "18px", fontWeight: "bold", color: "#007bff" }} >
                        {getInvestmentStrategy()}
                    </p>

                    <h3>📋 Total des Investissements : {safeToFixed(totalInvestments)}</h3> {/* Affichage de la somme totale des investissements */}

                    <h2>📋 Investissements Actuels</h2>
                    <ul>
                        {investments.map((inv, index) => {
                            const currentPrice = getCurrentPrice(inv.companyName);
                            const percentageChange = currentPrice ? ((currentPrice - inv.originalPrice) / currentPrice) * 100 : 0;
                            return (
                                <li key={inv.id || index}>
                                    {inv.companyName} - 💰 {inv.amountInvested}€ - {percentageChange.toFixed(2)}%
                                </li>
                            );
                        })}
                    </ul>
                </>
            ) : (
                <p>🔑 Connectez-vous pour voir vos investissements.</p>
            )}
        </div>
    );
}

export default InvestmentStrategies;
