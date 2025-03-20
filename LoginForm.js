import React, { useState, useEffect } from "react";

function LoginForm() {
    const [formData, setFormData] = useState({ email: "", password: "" });
    const [message, setMessage] = useState(null);
    const [loading, setLoading] = useState(false);
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user")));
    const [socket, setSocket] = useState(null);
    const [notifications, setNotifications] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const itemsPerPage = 10;
    const [showNotifications, setShowNotifications] = useState(false);

    // gestion du changement de champs
    const handleChange = (e) => {setFormData({ ...formData, [e.target.name]: e.target.value });};

    // gestion envoie au formulaire
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.email || !formData.password) {
            setMessage("Veuillez remplir tous les champs.");
            return;
        }

        setLoading(true);
        try {
            const response = await fetch("http://localhost:8080/api/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formData),
            });
            const result = await response.json();

            if (result.success) {
                setUser(result.user);
                localStorage.setItem("user", JSON.stringify(result.user));
                setMessage("Connexion réussie");

            } else {
                setMessage(`${result.message}`);
            }
        } catch {setMessage("Erreur de connexion au serveur.");}
        setLoading(false);
    };

    // déconnexion
    const handleLogout = () => {
        localStorage.removeItem("user");
        setUser(null);
    };

    // connexion WebSocket et récupération des notifications
    useEffect(() => {
        if (user) {
            const ws = new WebSocket("ws://localhost:8080/ws");

            ws.onopen = () => {
                // console.log("WebSocket connecté");
                ws.send(JSON.stringify({ type: "connect", userId: user.id }));
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);

                    if (data.type === "notification" || data.type === "broadcast") {
                        setNotifications((prev) => [{ id: Date.now(), message: data.message }, ...prev]);
                    } else if (data.type === "delete-notification") {
                        setNotifications((prev) => prev.filter((notif) => notif.id !== data.notificationId));
                    }
                } catch (error) {
                    console.error("ERROR: no parsing JSON :", error);
                }
            };

            ws.onerror = (error) => console.error("ERROR: WebSocket ", error);
            ws.onclose = () => console.log("WebSocket déconnecté");

            setSocket(ws);

            // charger les notifications depuis l'API
            fetch("http://localhost:8080/api/get-notifications", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ userId: user.id })
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        setNotifications(data.notifications);
                    }
                })
                .catch(err => console.error("ERROR: récupération notifications:", err));

            return () => ws.close();
        }
    }, [user]);

    // supprimer une notification
    const handleDeleteNotification = async (notificationId) => {
        try {
            const response = await fetch("http://localhost:8080/api/delete-notification", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ notificationId, userId: user.id })
            });

            const result = await response.json();
            if (result.success) {
                setNotifications(notifications.filter(notif => notif.id !== notificationId));
            }
        } catch (error) {
            console.error("ERROR: suppression notification ", error);
        }
    };

    // gestion de la pagination
    const indexOfLastItem = (currentPage + 1) * itemsPerPage;
    const indexOfFirstItem = currentPage * itemsPerPage;
    const currentNotifications = notifications.slice(indexOfFirstItem, indexOfLastItem);

    return (
        <div style={{ position: "relative", padding: "20px" }}>
            <h2>{user ? `Bienvenue, ${user.name}` : "Connexion"}</h2>
            {message && <p>{message}</p>}

            {/* 🔔 Icône de cloche avec badge */}
            {user && ( // si connecter
                <div style={{ position: "absolute", top: 10, right: 10 }}>
                    <button
                        onClick={() => setShowNotifications(!showNotifications)}
                        style={{
                            fontSize: "24px",
                            cursor: "pointer",
                            background: "none",
                            border: "none",
                            position: "relative",
                        }}
                    >
                        🔔
                        {notifications.length > 0 && (
                            <span
                                style={{
                                    position: "absolute",
                                    top: "-5px",
                                    right: "-5px",
                                    background: "red",
                                    color: "white",
                                    borderRadius: "50%",
                                    padding: "5px 8px",
                                    fontSize: "12px",
                                    fontWeight: "bold"
                                }}
                            >
                                {notifications.length}
                            </span>
                        )}
                    </button>

                    {/* 📜 Affichage du menu des notifications */}
                    {showNotifications && (
                        <div style={{
                            position: "absolute",
                            right: 0,
                            top: "30px",
                            width: "300px",
                            background: "#fff",
                            boxShadow: "0px 4px 6px rgba(0,0,0,0.1)",
                            borderRadius: "5px",
                            padding: "10px",
                            zIndex: 1000
                        }}>
                            <h3 style={{ margin: "0 0 10px 0" }}>🔔 Notifications</h3>
                            {currentNotifications.length > 0 ? (
                                currentNotifications.map((notif) => (
                                    <div key={notif.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid #ddd", padding: "5px" }}>
                                        <p>{notif.message}</p>
                                        <button onClick={() => handleDeleteNotification(notif.id)} style={{ background: "none", border: "none", cursor: "pointer", color: "red" }}>🗑️</button>
                                    </div>
                                ))
                            ) : (
                                <p>Aucune notification.</p>
                            )}

                            {/* Pagination */}
                            <div style={{ display: "flex", justifyContent: "space-between", marginTop: "10px" }}>
                                <button onClick={() => setCurrentPage(prev => Math.max(prev - 1, 0))} disabled={currentPage === 0}>⬅ Précédent</button>
                                <button onClick={() => setCurrentPage(prev => (indexOfLastItem < notifications.length ? prev + 1 : prev))} disabled={indexOfLastItem >= notifications.length}>Suivant</button>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {!user ? (
                <form onSubmit={handleSubmit}>
                    <input type="email" name="email" placeholder="Email" value={formData.email} onChange={handleChange} />
                    <input type="password" name="password" placeholder="Mot de passe" value={formData.password} onChange={handleChange} />
                    <button type="submit" disabled={loading}>{loading ? "Connexion.." : "Se connecter"}</button>
                </form>
            ) : (
                <div>
                    <p>{user.email}</p>
                    <p>Solde: {user.balance}€</p>
                    <button onClick={handleLogout}>Déconnexion</button>
                </div>
            )}
        </div>
    );
}

export default LoginForm;
