import play.api.libs.json.{Json, Writes}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}

//  Modèle de Notification
case class Notification(id: Option[Int], userId: Int, message: String, timestamp: Option[java.sql.Timestamp])

// Définition de la conversion en JSON
object Notification {
  implicit val notificationWrites: Writes[Notification] = Json.writes[Notification]
  val tupled = (Notification.apply _).tupled
}


// Mapping Slick pour la table `notifications`
class NotificationsTable(tag: Tag) extends Table[Notification](tag, "notifications") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def message = column[String]("message")
  def timestamp = column[java.sql.Timestamp]("timestamp", O.Default(new java.sql.Timestamp(System.currentTimeMillis())))

  def * = (id.?, userId, message, timestamp.?) <> (Notification.tupled, Notification.unapply)
}

// Gestion des Notifications en Base de Données
class DBNotification(db: Database)(implicit ec: ExecutionContext) {
  val notifications = TableQuery[NotificationsTable]

  // Insérer une notification
  def insertNotification(userId: Int, message: String): Future[Int] = {
    val action = (notifications returning notifications.map(_.id)) += Notification(None, userId, message, None)
    db.run(action)
  }

  // Récupérer toutes les notifications d'un utilisateur
  def getAllNotifications(userId: Int): Future[Seq[Notification]] = {
    db.run(notifications.filter(_.userId === userId).sortBy(_.timestamp.desc).result)
  }

  // Supprimer une notification par ID
  def deleteNotification(notificationId: Int): Future[Int] = {
    db.run(notifications.filter(_.id === notificationId).delete)
  }
}
