import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import java.sql.DriverManager
import scala.concurrent.ExecutionContext
import scala.io.{Source, StdIn}

object Main {

  def main(args: Array[String]): Unit = {
    val url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=1234"

    val script = Source.fromFile("H:/ING2/scalaProjet/projetScalaGR/src/main/script.sql").mkString
    val conn = DriverManager.getConnection(url)
    val statement = conn.createStatement()

    script.split(";").foreach { query =>
      if (query.trim.nonEmpty) {
        statement.execute(query.trim)
        println(s"✅ Exécuté : $query")
      }

    }
    implicit val system: ActorSystem = ActorSystem("MainSystem")
    implicit val ec: ExecutionContext = system.dispatcher // Ajoute ExecutionContext

    val server = new WebSocketServer()(system, ec) // Passe bien system et ec

    server.start()



  }
}