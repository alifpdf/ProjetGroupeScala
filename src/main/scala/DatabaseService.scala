  import UsersTable.table
  import slick.jdbc.PostgresProfile.api._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future



  case class User(name: String, email: String, motdepasse:String)

  class UsersTable(tag: Tag) extends Table[User](tag,"Compte"){

    def name = column[String]("name")
    def email = column[String]("email")
    def motdepasse = column[String]("motdepasse")
    def * = (name, email, motdepasse) <> (User.tupled, User.unapply)

  }

  object UsersTable {
    val table = TableQuery[UsersTable]
  }

  class DatabaseService(db:Database) {

    def addUser(user: User): Future[Int] = {
      db.run(table += user)
    }
    def getUsers: Future[Seq[User]] = {
      db.run(UsersTable.table.result)
    }

    def getEmail(email:String): Future[Seq[User]] = {
      db.run(UsersTable.table.filter(_.email===email).result)
    }

    def checkPassword(email: String, password: String): Future[Boolean] = {
      db.run(UsersTable.table.filter(_.email === email).result.headOption).map {
        case Some(user) => user.motdepasse == password // Comparaison directe des mots de passe
        case None => false // Aucun utilisateur trouvé
      }
    }
  }