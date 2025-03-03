  import UsersTable.table
  import slick.jdbc.PostgresProfile.api._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future



  case class User(name: String, email: String, motdepasse:String,balance:BigDecimal)

  class UsersTable(tag: Tag) extends Table[User](tag,"users"){


    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def motdepasse = column[String]("password")
    def balance = column[BigDecimal]("balance")
    def * = (name, email, motdepasse,balance) <> (User.tupled, User.unapply)

  }

  object UsersTable {
    val table = TableQuery[UsersTable]
  }

  class DatabaseService(db:Database) {

    def addUser(user: User): Future[Int] = {
      println(s"📌 Tentative d'insertion de : ${user}")
      db.run(UsersTable.table += user).map { result =>
        println(s"✅ Nombre de lignes insérées : $result")
        result
      }.recover {
        case e: Exception =>
          println(s"❌ Erreur SQL lors de l'ajout d'utilisateur : ${e.getMessage}")
          0
      }
    }

    def getId(email:String):Future[Int] = {
      db.run(UsersTable.table.filter(_.email === email).map(_.id).result.headOption).map(_.getOrElse(0))
    }



    def checkPassword(email: String, password: String): Future[Boolean] = {
      db.run(UsersTable.table.filter(_.email === email).result.headOption).map {
        case Some(user) => user.motdepasse == password // Comparaison directe des mots de passe
        case None => false // Aucun utilisateur trouvé
      }
    }

    def getsomme_restant(email:String):Future[BigDecimal] = {
      db.run(UsersTable.table.filter(_.email===email)
          .map(_.balance).result.headOption).map(_.getOrElse(BigDecimal(0)))
    }

    def updateSommeCompte(somme: BigDecimal, email: String): Future[Int] = {
      db.run(
        UsersTable.table.filter(_.email === email)
          .map(_.balance)
          .update(somme)
      )
    }
  }