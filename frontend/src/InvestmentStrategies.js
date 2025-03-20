import React, { useState, useEffect } from "react";
import "./InvestmentStrategies.css";

function InvestmentStrategies() {
    const [investments, setInvestments] = useState([]); // liste des investissement
    const [history, setPurchaseHistory] = useState([]); // listes des produits (acheter)
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user"))); // le user
    const [nav, setNav] = useState(0); // Nav (Net Asset Value)
    const [sharpeRatio, setSharpeRatio] = useState(0); // ratio sharp
    const [volatility, setVolatility] = useState(0); // volatilité
    const [ws, setWs] = useState(null);
    const [totalInvestments, setTotalInvestments] = useState(0); // sommes des produits d'un investissement
    const [balance, setBalance] = useState(0); // porte monnai de l'utilisateur
    const [btc, setBtc] = useState(0); // bitcoin
    const [eth, setEth] = useState(0); // ether
    const [doge, setDoge] = useState(0); // doge coin
    const [activeTab, setActiveTab] = useState("portfolio");

    // connexion WebSocket et récupération des notifications
    useEffect(() => {
        if (user) {
            const websocket = new WebSocket("ws://localhost:8080/ws");
            setWs(websocket);

            websocket.onopen = () => {
                console.log("Connexion WebSocket établie");
            };

            websocket.onmessage = (event) => {
                const data = JSON.parse(event.data);
                //console.log("données reçues du WebSocket pour RealTimesChart.js");

                if (data.type === "update") {
                    console.log("MAJ des investissements reçu");

                    if (data.balance && !isNaN(data.balance)) {
                        setBalance(data.balance);

                    } else {console.error("ERROR: balance invalide");}

                    if (data.investments) {
                        setInvestments(data.investments);
                        calculateFinancialIndicators(data.investments);
                        calculateTotalInvestments(data.investments);

                    } else {console.error("ERROR: Investissements non trouvés");}

                    // MAJ de l'historique si besoin
                    if (data.history) {setPurchaseHistory(data.history);}
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

            websocket.onerror = (error) => {console.error("ERROR: WebSocket:", error);};

            websocket.onclose = () => {console.log("ERROR: Connexion fermée");};

            return () => {
                if (websocket) { websocket.close(); }
            };
        }
    }, [user]);

    useEffect(() => {
        if (user) {
            fetchBalance();
            fetchPurchaseHistory();

        }
    }, [user]);

    // met à jour la balance (porte monnaie)
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
                //console.log(`balance MAJ : ${data.balance}`);

            } else {console.error("ERROR: récupération de la balance :", data.message);}

        } catch (error) {console.error("ERROR: récupération du solde :", error);}
    };

    // cherche l'historique des achats d'un utilisateur
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
            //console.log("réponse de l'API :", data); // Verif reception

            if (data.success) {
                if (data.history) {
                    setPurchaseHistory(data.history);
                } else {
                    console.error("ERROR_0: 'history' ou 'purchaseHistory' introuvable");
                }
            } else {
                console.error("ERROR_1: récupération de l'historique");
            }
        } catch (error) {
            console.error("ERROR_2: récupération de l'historique :", error);
        }
    };

    // moyenne des rendement (ne marche pas)
    // PLUS UTILISER !
    const calculateAverageReturn = (companyName, history) => {
        const relatedProducts = history.filter(product => product.companyName === companyName);
        const totalRendement = relatedProducts.reduce((sum, product) => sum + product.rendement, 0);

        console.log(`Rendement total pour ${companyName}: ${totalRendement}`);

        return relatedProducts.length > 0 ? totalRendement / relatedProducts.length : 0;
    };

    // retourne l'investissement total
    const calculateTotalInvestments = (investments) => {
        const total = investments.reduce((sum, investment) => sum + investment.amountInvested, 0);
        setTotalInvestments(total);
    };

    // calcule les differents indicateur comme le ratio de sharp et la volatilité
    const calculateFinancialIndicators = (investments) => {
        if (investments.length === 0) return;

        const rendements = investments.map(inv => {
            const price = inv.amountInvested;
            const randomRendement = Math.random() * 0.1 - 0.05;
            return price * randomRendement;
        });
        // utilise des variables aléatoires
        const meanRendement = rendements.reduce((a, b) => a + b, 0) / rendements.length;
        const riskFreeRate = 0.02;
        const variance = rendements.reduce((sum, r) => sum + Math.pow(r - meanRendement, 2), 0) / rendements.length;
        const stdDeviation = Math.sqrt(variance);

        setVolatility(stdDeviation);
        setSharpeRatio(stdDeviation === 0 ? 0 : (meanRendement - riskFreeRate) / stdDeviation);
    };

    // variation du message de la strategies à adopter
    const getInvestmentStrategy = () => {
        if (sharpeRatio > 1 && volatility < 0.2) {
            // couleur bleu
            return <span style={{ color: "var(--secondary-color)" }}>Stratégie Défensive: Investissez dans des actifs sûrs.</span>;
        } else if (sharpeRatio > 1.5) {
            notifyStrategy("Stratégie Équilibrée: Mélangez actions, ETF et crypto pour diversifier.");
            // couleur verte
            return <span style={{ color: "var(--positive-color)" }}>Stratégie Équilibrée: Mélangez les crypto pour diversifier.</span>;
        } else if (sharpeRatio < 1 && volatility > 0.3) {
            // couleur rouge
            return <span style={{ color: "var(--negative-color)" }}>Stratégie Agressive: Trop de risques, Diversifiez vos placements.</span>;
        } else {
            // couleur normal
            return <span style={{ color: "var(--text-color)" }}>Stratégie Neutre: Restez attentif et surveiller vos investissements.</span>;
        }
    };

    // récupère les prix actuel
    const getCurrentPrice = (company) => {
        switch (company) {
            case "BTC": return btc;
            case "ETH": return eth;
            case "DOGE": return doge;
            default: return 0;
        }
    };

    // calcul le NAV (porte monnaie + actif)
    const calculateNav = () => {
        return balance + totalInvestments;
    };

    // affiche un nombre (peut changer le nombre de chiffres après la virgule)
    const safeToFixed = (value, decimals = 2) => {
        return value !== undefined && value !== null ? value.toFixed(decimals) : "0.00";
    };

    // Calcul du rendement correct
    const calculateReturn = (currentPrice, originalPrice) => {
        if (!currentPrice || !originalPrice) return 0;
        return ((currentPrice - originalPrice) / originalPrice) * 100;
    };

    // récupération du message strategique dynamique
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
            console.error("ERROR: notification de la stratégie", error);
        }
    };

    // formate la date à une norme
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

    /*  --- Le Html --- */
    return (
        <div className="investment-container">
            <h2 className="investment-header">Stratégies d'Investissement</h2>
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
                                    <h3>Valeur Nette (NAV)</h3>
                                    <p className="metric-value">{safeToFixed(calculateNav())}€</p>
                                </div>
                                <div className="metric-card">
                                    <h3>Ratio de Sharpe</h3>
                                    <p className="metric-value">{safeToFixed(sharpeRatio)}</p>
                                </div>
                                <div className="metric-card">
                                    <h3>Volatilité</h3>
                                    <p className="metric-value">{safeToFixed(volatility)}%</p>
                                </div>
                                <div className="metric-card">
                                    <h3>Total Investi</h3>
                                    <p className="metric-value">{safeToFixed(totalInvestments)}€</p>
                                </div>
                            </div>

                            <div className="strategy-container">
                                <h3>Stratégie Recommandée</h3>
                                <p className="strategy-text">
                                    {getInvestmentStrategy()}
                                </p>
                            </div>

                            <div className="investments-list">
                                <h3>Investissements Actuels</h3>
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
                                        // calcul prix actuel pour investissement
                                        const currentPrice = getCurrentPrice(inv.companyName);

                                        // Calcul rendement moyen de l'investissement
                                        const relatedProducts = history.filter(product => product.companyName === inv.companyName);
                                        const totalRendement = relatedProducts.reduce((sum,product) => sum+product.rendement, 0);
                                        //console.log("rendement total: "+totalRendement)
                                        const averageRendement = relatedProducts.length>0 ? totalRendement / relatedProducts.length: 0;

                                        // affecter le ratio approprié
                                        // NE MARCHE PAS :(
                                        let companyRatio = 0;
                                        if (inv.companyName === "BTC") {
                                            companyRatio = inv.BTC_ratio_sum; // BTC
                                        } else if (inv.companyName === "ETH") {
                                            companyRatio = inv.ETH_ratio_sum; // ETH
                                        } else if (inv.companyName === "DOGE") {
                                            companyRatio = inv.DOGE_ratio_sum; // DOGE
                                        } else {
                                            companyRatio = averageRendement; // calcul moyen
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
                                    <th>Prix Unitaire</th>
                                    <th>Montant Total</th>
                                </tr>
                                </thead>
                                <tbody>
                                {[...history] // crée une copie de history pour la triée
                                    .sort((a, b) => new Date(b.created_at) - new Date(a.created_at)) // plus récent au plus ancien
                                    .map((purchase, index) => (
                                        <tr key={purchase.id || index}>
                                            <td>{formatDate(purchase.created_at)}</td>
                                            <td>{purchase.companyName}</td>
                                            <td>{purchase.quantity}</td>
                                            <td>{safeToFixed(purchase.price / purchase.quantity)}€</td>
                                            <td>{safeToFixed(purchase.price)}€</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                    )}
                </div>
            ) : (
                <p className="login-message">Connectez-vous pour voir vos investissements</p>
            )}
        </div>
    );
}

export default InvestmentStrategies;