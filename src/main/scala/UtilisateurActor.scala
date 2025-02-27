import akka.actor.{Actor, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Actor {

  case class AddUser(name: String, email: String, password: String)
  case class GetUsers()
  case class GetUserByEmail(email: String)
  case class CheckPassword(email: String, password: String)

  def props(dbService: DatabaseService): Props = Props(new Actor(dbService) {
    override def receive: Receive = ???
  })



}