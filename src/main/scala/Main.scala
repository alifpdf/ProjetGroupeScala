import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import java.sql.DriverManager
import scala.io.Source
import scala.sys.process._

object Main {

  def main(args: Array[String]): Unit = {
    val url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=1234"

    // 📌 Exécuter le script SQL
    val script = Source.fromFile("H:/ING2/scalaProjet/projetScalaGR/src/main/script.sql").mkString
    val conn = DriverManager.getConnection(url)
    val statement = conn.createStatement()

    script.split(";").foreach { query =>
      if (query.trim.nonEmpty) {
        statement.execute(query.trim)
        println(s"✅ Exécuté : $query")
      }
    }

    // 📌 Démarrer Akka System
    implicit val system: ActorSystem = ActorSystem("MainSystem")
    implicit val ec: ExecutionContext = system.dispatcher

    val server = new WebSocketServer()(system, ec)
    server.start()

    // 📌 Lancer `npm start` dans `frontend`
    startFrontend()
  }

  // 📌 Fonction pour démarrer le frontend
  def startFrontend(): Unit = {
    val frontendPath = "H:/ING2/scalaProjet/projetScalaGR/frontend"
    val npmCommand = "cmd /c npm start" // Utilisation de cmd /c pour exécuter npm correctement

    println("🚀 Démarrage du frontend React...")

    val process = Process(npmCommand, new java.io.File(frontendPath)).run()

    process.exitValue() // Attendre que le processus se termine (optionnel)
  }
}
