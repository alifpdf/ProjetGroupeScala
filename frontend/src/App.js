import React, { useState } from "react";
import RealTimeChart from "./RealTimeChart";
import SignUpForm from "./SignUpForm";
import LoginForm from "./LoginForm";

function App() {
    const [page, setPage] = useState("home");

    return (
        <div>
            <h1>📊 Application Finance</h1>

            {/* ✅ Navigation */}
            <nav>
                <button onClick={() => setPage("login")}>Se connecter</button>
                <button onClick={() => setPage("signup")}>S'inscrire</button>
                <button onClick={() => setPage("chart")}>Voir le graphique</button>
            </nav>

            {/* ✅ Affichage des pages */}
            {page === "login" && <LoginForm />}
            {page === "signup" && <SignUpForm />}
            {page === "chart" && <RealTimeChart />}
        </div>
    );
}

export default App;
