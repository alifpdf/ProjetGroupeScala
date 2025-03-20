import React, { useState, useEffect } from "react";
import "./InvestmentStrategies.css"; // Import CSS file

function InvestmentStrategies() {
    const [investments, setInvestments] = useState([]);
    const [history, setPurchaseHistory] = useState([]);
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user")));
    const [nav, setNav] = useState(0);
    const [sharpeRatio, setSharpeRatio] = useState(0);
    const [volatility, setVolatility] = useState(0);
    const [ws, setWs] = useState(null);
    const [totalInvestments, setTotalInvestments] = useState(0);
    const [balance, setBalance] = useState(0);
    const [btc, setBtc] = useState(0);
    const [eth, setEth] = useState(0);
    const [doge, setDoge] = useState(0);
    const [activeTab, setActiveTab] = useState("portfolio");

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

                    if (data.balance && !isNaN(data.balance)) {
                        setBalance(data.balance);
                    } else {
                        console.error("❌ Balance non valide:", data.balance);
                    }

                    if (data.investments) {
                        setInvestments(data.investments);
                        calculateFinancialIndicators(data.investments);
                        calculateTotalInvestments(data.investments);
                    } else {
                        console.error("❌ Investissements non trouvés:", data.investments);
                    }

                    // Mettre à jour l'historique des achats si présent
                    if (data.history) {
                        setPurchaseHistory(data.history);
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

            return () => {
                if (websocket) {
                    websocket.close();
                }
            };
        }
    }, [user]);

    useEffect(() => {
        if (user) {
            fetchBalance();
            fetchPurchaseHistory();
        }
    }, [user]);

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
                setBalance(data.balance);
                console.log(`📢 Balance mise à jour : ${data.balance}`);
            } else {
                console.error("❌ Erreur lors de la récupération de la balance :", data.message);
            }
        } catch (error) {
            console.error("❌ Erreur lors de la récupération du solde :", error);
        }
    };

    const fetchPurchaseHistory = async () => {
        try {
            const response = await fetch("http://localhost:8080/api/get-products-history", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ userId: user.id })
            });

            const data = await response.json();
            console.log("📢 Réponse de l'API :", data); // Vérifier ce qui est reçu

            if (data.success) {
                if (data.history) {
                    setPurchaseHistory(data.history);
                } else {
                    console.error("❌ Aucune clé 'history' ou 'purchaseHistory' trouvée !");
                }
            } else {
                console.error("❌ Erreur lors de la récupération de l'historique :", data.message);
            }
        } catch (error) {
            console.error("❌ Erreur lors de la récupération de l'historique :", error);
        }
    };

    const calculateAverageReturn = (companyName, history) => {
        const relatedProducts = history.filter(product => product.companyName === companyName);
        const totalRendement = relatedProducts.reduce((sum, product) => sum + product.rendement, 0);

        console.log(`🔹 Rendement total pour ${companyName}: ${totalRendement}`);

        return relatedProducts.length > 0 ? totalRendement / relatedProducts.length : 0;
    };

    const calculateTotalInvestments = (investments) => {
        const total = investments.reduce((sum, investment) => sum + investment.amountInvested, 0);
        setTotalInvestments(total);
    };

    const calculateFinancialIndicators = (investments) => {
        if (investments.length === 0) return;

        const rendements = investments.map(inv => {
            const price = inv.amountInvested;
            const randomRendement = Math.random() * 0.1 - 0.05;
            return price * randomRendement;
        });

        const meanRendement = rendements.reduce((a, b) => a + b, 0) / rendements.length;
        const riskFreeRate = 0.02;
        const variance = rendements.reduce((sum, r) => sum + Math.pow(r - meanRendement, 2), 0) / rendements.length;
        const stdDeviation = Math.sqrt(variance);

        setVolatility(stdDeviation);
        setSharpeRatio(stdDeviation === 0 ? 0 : (meanRendement - riskFreeRate) / stdDeviation);
    };

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

    const calculateNav = () => {
        return balance + totalInvestments;
    };

    const safeToFixed = (value, decimals = 2) => {
        return value !== undefined && value !== null ? value.toFixed(decimals) : "0.00";
    };

    // Calcul du rendement correct
    const calculateReturn = (currentPrice, originalPrice) => {
        if (!currentPrice || !originalPrice) return 0;
        return ((currentPrice - originalPrice) / originalPrice) * 100;
    };

    const notifyStrategy = async (strategy) => {
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

    // Fonction pour formatter la date
    const formatDate = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div className="investment-container">
            <h2 className="investment-header">📊 Stratégies d'Investissement</h2>
            {user ? (
                <div className="investment-dashboard">
                    <div className="dashboard-tabs">
                        <button
                            className={`tab-button ${activeTab === "portfolio" ? "active" : ""}`}
                            onClick={() => setActiveTab("portfolio")}
                        >
                            Portfolio
                        </button>
                        <button
                            className={`tab-button ${activeTab === "history" ? "active" : ""}`}
                            onClick={() => setActiveTab("history")}
                        >
                            Historique des Achats
                        </button>
                    </div>

                    {activeTab === "portfolio" ? (
                        <>
                            <div className="metrics-container">
                                <div className="metric-card">
                                    <h3>💰 Valeur Nette (NAV)</h3>
                                    <p className="metric-value">{safeToFixed(calculateNav())}€</p>
                                </div>
                                <div className="metric-card">
                                    <h3>📈 Ratio de Sharpe</h3>
                                    <p className="metric-value">{safeToFixed(sharpeRatio)}</p>
                                </div>
                                <div className="metric-card">
                                    <h3>📉 Volatilité</h3>
                                    <p className="metric-value">{safeToFixed(volatility * 100)}%</p>
                                </div>
                                <div className="metric-card">
                                    <h3>💸 Total Investi</h3>
                                    <p className="metric-value">{safeToFixed(totalInvestments)}€</p>
                                </div>
                            </div>

                            <div className="strategy-container">
                                <h3>🧠 Stratégie Recommandée :</h3>
                                <p className="strategy-text">
                                    {getInvestmentStrategy()}
                                </p>
                            </div>

                            <div className="investments-list">
                                <h3>📋 Investissements Actuels</h3>
                                <table className="investment-table">
                                    <thead>
                                    <tr>
                                        <th>Actif</th>
                                        <th>Montant Investi</th>
                                        <th>Prix Actuel</th>
                                        <th>Rendement</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {investments.map((inv, index) => {
                                        // Calcul du prix actuel pour l'investissement
                                        const currentPrice = getCurrentPrice(inv.companyName);

                                        // Calcul du rendement moyen de l'investissement (comme dans le code d'origine)
                                        const relatedProducts = history.filter(product => product.companyName === inv.companyName);
                                        const totalRendement = relatedProducts.reduce((sum, product) => sum + product.rendement, 0);
                                        console.log("rendement total: "+totalRendement)
                                        const averageRendement = relatedProducts.length > 0 ? totalRendement / relatedProducts.length : 0;

                                        // Vérifier le type de cryptomonnaie (BTC, ETH, DOGE) et affecter le ratio approprié
                                        let companyRatio = 0;
                                        if (inv.companyName === "BTC") {
                                            companyRatio = inv.BTC_ratio_sum; // Utilise la valeur de BTC_ratio_sum pour BTC
                                        } else if (inv.companyName === "ETH") {
                                            companyRatio = inv.ETH_ratio_sum; // Utilise la valeur de ETH_ratio_sum pour ETH
                                        } else if (inv.companyName === "DOGE") {
                                            companyRatio = inv.DOGE_ratio_sum; // Utilise la valeur de DOGE_ratio_sum pour DOGE
                                        } else {
                                            companyRatio = averageRendement; // Si aucune des cryptos spécifiées, utilise le calcul moyen
                                        }

                                        return (
                                            <tr key={inv.id || index}>
                                                <td>{inv.companyName}</td>
                                                <td>{safeToFixed(inv.amountInvested)}€</td>
                                                <td>{safeToFixed(currentPrice)}€</td>
                                                <td className={companyRatio >= 0 ? "positive-return" : "negative-return"}>
                                                    {safeToFixed(companyRatio, 6)}%
                                                </td>
                                            </tr>
                                        );
                                    })}
                                    </tbody>
                                </table>
                            </div>
                        </>
                    ) : (
                        <div className="history-container">
                            <h3>🕒 Historique des Achats</h3>
                            <table className="history-table">
                                <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Actif</th>
                                    <th>Quantité</th>
                                    <th>Prix</th>
                                    <th>Montant Total</th>
                                </tr>
                                </thead>
                                <tbody>
                                {history.map((purchase, index) => (
                                    <tr key={purchase.id || index}>
                                        <td>{formatDate(purchase.created_at)}</td>
                                        <td>{purchase.companyName}</td>
                                        <td>{purchase.quantity}</td>
                                        <td>{purchase.price/purchase.quantity}€</td>
                                        <td>{safeToFixed(purchase.price)}€</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            ) : (
                <p className="login-message">🔑 Connectez-vous pour voir vos investissements.</p>
            )}
        </div>
    );
}

export default InvestmentStrategies;