import React, { useState, useEffect } from "react";
import RealTimeChart from "./RealTimeChart";
import SignUpForm from "./SignUpForm";
import LoginForm from "./LoginForm";
import InvestmentStrategies from "./InvestmentStrategies";

function App() {
    const [page, setPage] = useState("home");
    const [notifications, setNotifications] = useState([]);

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => {
            console.log("✅ WebSocket connecté !");
        };

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log("📩 Message reçu :", data);

                if (data.type === "notification") {
                    setNotifications((prev) => [...prev, data.message]);
                }
            } catch (error) {
                console.error("❌ Erreur lors du parsing JSON :", error);
            }
        };

        ws.onerror = (error) => console.error("❌ Erreur WebSocket :", error);
        ws.onclose = () => console.log("❌ WebSocket déconnecté.");

        return () => ws.close();
    }, []);

    return (
        <div>
            <h1>📊 Application Finance</h1>

            {/* 🔔 Affichage des notifications */}
            {notifications.length > 0 && (
                <div style={{
                    backgroundColor: "#ffeb3b",
                    padding: "10px",
                    marginBottom: "10px",
                    borderRadius: "5px"
                }}>
                    <h3>🔔 Notifications</h3>
                    {notifications.map((notif, index) => (
                        <p key={index}>{notif}</p>
                    ))}
                </div>
            )}

            {/* ✅ Navigation */}
            <nav>
                <button onClick={() => setPage("home")}>Accueil</button>
                <button onClick={() => setPage("login")}>Se connecter</button>
                <button onClick={() => setPage("signup")}>S'inscrire</button>
                <button onClick={() => setPage("chart")}>Voir le graphique</button>
                <button onClick={() => setPage("strategie")}>Strategie</button>
            </nav>

            {/* ✅ Affichage des pages */}
            {page === "home" && <h2>Bienvenue sur l'application de finance !</h2>}
            {page === "login" && <LoginForm />}
            {page === "signup" && <SignUpForm />}
            {page === "chart" && <RealTimeChart />}
            {page === "strategie" && <InvestmentStrategies />}
        </div>
    );
}

export default App;
