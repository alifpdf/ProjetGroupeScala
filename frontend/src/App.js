import React, { useState, useEffect } from "react";
import RealTimeChart from "./RealTimeChart";
import SignUpForm from "./SignUpForm";
import LoginForm from "./LoginForm";
import InvestmentStrategies from "./InvestmentStrategies";
import "./App.css";
import { Bell, Home, LogIn, UserPlus, TrendingUp } from "lucide-react";

function App() {
    const [page, setPage] = useState("home");
    const [notifications, setNotifications] = useState([]);

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8080/ws");

        ws.onopen = () => console.log("WebSocket connecté !");
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data.type === "notification") {
                    setNotifications((prev) => [...prev, data.message]);
                }
            } catch (error) {
                console.error("ERROR: JSON :", error);
            }
        };
        ws.onerror = (error) => console.error("ERROR: WebSocket :", error);
        ws.onclose = () => console.log("WebSocket déconnecté.");
        return () => ws.close();
    }, []);

    return (
        <div className="app-container">
            <header className="header">
                <h1 className="title">CY Tech SmartFinance </h1>
                <nav className="nav">
                    <button className="nav-button" onClick={() => setPage("home")}><Home className="icon" /> Accueil</button>
                    <button className="nav-button" onClick={() => setPage("login")}><LogIn className="icon" /> Connexion</button>
                    <button className="nav-button" onClick={() => setPage("signup")}><UserPlus className="icon" /> Inscription</button>
                    <button className="nav-button" onClick={() => setPage("chart")}><TrendingUp className="icon" /> Graphique</button>
                    <button className="nav-button" onClick={() => setPage("strategie")}><Bell className="icon" /> Stratégie</button>
                </nav>
            </header>

            <main className="main-content">
                {notifications.length > 0 && (
                    <div className="notification-card">
                        <h3>🔔 Notifications</h3>
                        {notifications.map((notif, index) => (
                            <p key={index}>{notif}</p>
                        ))}
                    </div>
                )}

                <div className="content-card">
                    {page === "home" && <h2 className="welcome">Bienvenue sur l'application de finance !</h2>}
                    {page === "login" && <LoginForm />}
                    {page === "signup" && <SignUpForm />}
                    {page === "chart" && <RealTimeChart />}
                    {page === "strategie" && <InvestmentStrategies />}
                </div>
            </main>
        </div>
    );
}

export default App;
