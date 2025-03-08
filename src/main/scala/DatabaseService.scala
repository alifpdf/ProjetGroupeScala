  import UsersTable.table
  import play.api.libs.json.Json
  import slick.jdbc.PostgresProfile.api._
  import slick.lifted.ProvenShape

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  import play.api.libs.json._
  object User {
    implicit val userFormat: OFormat[User] = Json.format[User]
    val tupled = (User.apply _).tupled // ✅ Ajout de tupled explicitement
  }

  case class User(id: Option[Int] = None, name: String, email: String, motdepasse: String, balance: BigDecimal)

  class UsersTable(tag: Tag) extends Table[User](tag, "users") {

    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column[String]("name")
    def email: Rep[String] = column[String]("email")
    def motdepasse: Rep[String] = column[String]("password")
    def balance: Rep[BigDecimal] = column[BigDecimal]("balance")

    // Projection correcte pour gérer un id optionnel
    def * : ProvenShape[User] = (id.?, name, email, motdepasse, balance) <> (User.tupled, User.unapply)
  }

  object UsersTable {
    val table = TableQuery[UsersTable]
  }

  class DatabaseService(db:Database) {

    // ✅ Récupère tous les utilisateurs
    def getUsers: Future[Seq[User]] = {
      db.run(table.result)
    }
    def getAllUsers: Future[String] = {
      db.run(table.result).map { users =>
        val json = Json.toJson(users) // ✅ Convertit en JSON
        println("📌 JSON des utilisateurs :", json) // Debug
        Json.stringify(json) // ✅ Convertit en String
      }
    }



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

    def getUser(id: Int): Future[Option[User]] = {
      db.run(table.filter(_.id === id).result.headOption)
    }

    def getEmail(id: Int): Future[String] =
    {
      db.run(table.filter(_.id === id).map(_.email).result.headOption).map(_.getOrElse(""))

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
    def getsomme_restant1(id:Int):Future[BigDecimal] = {
      db.run(UsersTable.table.filter(_.id===id)
        .map(_.balance).result.headOption).map(_.getOrElse(BigDecimal(0)))
    }

    import scala.concurrent.Future
    import slick.jdbc.PostgresProfile.api._

    def updateSommeCompte(somme: BigDecimal, id: Int): Future[Int] = {
      println(s"🔄 [DB] Mise à jour du solde de l'utilisateur $id de +$somme")

      val query = UsersTable.table
        .filter(_.id === id)
        .map(_.balance)
        .update(somme)

      db.run(query).map { rowsUpdated =>
        if (rowsUpdated > 0) {
          println(s"✅ [DB] Solde mis à jour pour User ID: $id (Nouveau solde: $somme)")
        } else {
          println(s"⚠️ [DB] Aucun utilisateur trouvé avec ID: $id")
        }
        rowsUpdated
      }.recover { case e =>
        println(s"❌ [DB] Erreur SQL lors de la mise à jour du solde: ${e.getMessage}")
        0
      }
    }

  }