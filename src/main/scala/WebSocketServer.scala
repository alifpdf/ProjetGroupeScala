import AkkaStream.websocketFlow
import Main.{notificationActor, productsActor, timeout, utilisateurActor, utilisateurActor2}
import MarketstackDataFetcher.getLastMarketPrices
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// 🔥 Augmente le délai d'attente pour éviter les erreurs

case class InformerVolatile(userId: Int, strategy: String) // Changer 'message' par 'strategy'

object InformerVolatile {
  implicit val format: OFormat[InformerVolatile] = Json.format[InformerVolatile]
}


case class RecupererSommeRequest(companyName: String, userId: Int, sommeInvesti: BigDecimal)

object RecupererSommeRequest {
  implicit val format: Format[RecupererSommeRequest] = Json.format[RecupererSommeRequest]
}

case class Connexion(email: String, password: String)

object Connexion {
  implicit val format: Format[Connexion] = Json.format[Connexion]
}

case class AddUserRequest(name: String, email: String, password: String)

object AddUserRequest {
  implicit val format: Format[AddUserRequest] = Json.format[AddUserRequest]
}

class WebSocketServer(implicit system: ActorSystem, ec: ExecutionContext) {

  val route: Route = cors() {
    concat(
      pathEndOrSingleSlash {
        complete(StatusCodes.OK, "✅ Serveur WebSocket en cours d'exécution. Connectez-vous sur /ws")
      },
      // 📡 Gestion du WebSocket
      path("ws") {
        handleWebSocketMessages(websocketFlow())
      },

      // ✅ Récupérer une somme investie
      path("api" / "recuperer-somme") {
        post {
          entity(as[String]) { body =>
            Json.parse(body).validate[RecupererSommeRequest] match {
              case JsSuccess(request, _) =>
                println(s"✅ Récupération de ${request.sommeInvesti}€ de ${request.companyName} (user: ${request.userId})")

                notificationActor ! SocketActor.SendNotification(request.userId,s"✅ Récupération de ${request.sommeInvesti}€ de ${request.companyName} (user: ${request.userId})")

                val futureRecuperation = (utilisateurActor2 ? InvestmentActor.RecupererlaSomme(request.companyName, request.userId, request.sommeInvesti)).mapTo[String]

                onComplete(futureRecuperation) {
                  case Success(response) =>
                    updateFrontend(request.userId) // 🔥 Met à jour le solde et les investissements
                    complete(Json.obj("success" -> true, "message" -> response).toString())
                }
            }
          }
        }
      },

      // ✅ Ajouter un utilisateur
      path("api" / "add-user") {
        post {
          entity(as[String]) { body =>
            Json.parse(body).validate[AddUserRequest] match {
              case JsSuccess(request, _) =>
                val futureResponse: Future[String] = (utilisateurActor ? UtilisateurActor.AddUtilisateur(request.name, request.email, request.password, 0)).mapTo[String]
                complete(
                  futureResponse.map(response =>
                    Json.obj("success" -> !response.startsWith("❌"), "message" -> response).toString()
                  )
                )
            }
          }
        }
      },

      // ✅ Connexion utilisateur
      path("api" / "login") {
        post {
          entity(as[String]) { body =>
            Json.parse(body).validate[Connexion] match {
              case JsSuccess(request, _) =>
                val futureResponse: Future[String] = (utilisateurActor ? UtilisateurActor.connexion(request.email, request.password)).mapTo[String]
                complete(
                  futureResponse.map(response =>
                    Json.obj("success" -> !response.startsWith("❌"), "user" -> Json.parse(response)).toString()
                  )
                )
            }
          }
        }
      },

      // ✅ Récupération des notifications
      path("api" / "get-notifications") {
        post {
          entity(as[String]) { body =>
            val userId = (Json.parse(body) \ "userId").as[Int]
            val futureNotifications = (notificationActor ? SocketActor.GetNotifications(userId)).mapTo[Seq[Notification]]

            onComplete(futureNotifications) {
              case Success(notifs) =>
                complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                  "success" -> true,
                  "notifications" -> Json.toJson(notifs)
                ).toString()))
            }
          }
        }
      },

      // ✅ Investir
      path("api" / "investir") {
        post {
          entity(as[String]) { body =>
            val json = Json.parse(body)
            val userId = (json \ "userId").as[Int]
            val companyName = (json \ "companyName").as[String]
            val amount = (json \ "amount").as[BigDecimal]
            val numShares = (json \ "numShares").as[Int]

            println(s"📢 Investissement : Utilisateur $userId achète $numShares actions de $companyName pour $amount €")

            notificationActor ! SocketActor.SendNotification(userId, s"📢 Investissement : Utilisateur $userId achète $numShares actions de $companyName pour $amount €")

            val futureInvestment = (utilisateurActor2 ? InvestmentActor.AddInvestment(userId, companyName, amount * numShares, amount)).mapTo[String]

            onComplete(futureInvestment.flatMap { response =>
              println(s"✅ Réponse investissement : $response")

              // 🔎 Extraction de l'ID de l'investissement depuis la réponse
              val investmentIdOpt = "ID: (\\d+)".r.findFirstMatchIn(response).map(_.group(1).toInt)

              investmentIdOpt match {
                case Some(investmentId) =>
                  // ✅ Ajouter le produit
                  (productsActor ? ProductsActor.AddProduct(userId, investmentId, amount, companyName, numShares)).mapTo[String].map { productResponse =>
                    println(s"✅ Produit ajouté : $productResponse")
                    Json.obj("success" -> true, "message" -> response, "product" -> productResponse).toString()
                  }

                case None =>
                  println("❌ Impossible de récupérer l'ID de l'investissement depuis la réponse")
                  Future.successful(Json.obj("success" -> false, "message" -> "Erreur : ID de l'investissement introuvable").toString())
              }
            }) {
              case Success(result) =>
                updateFrontend(userId) // 🔥 Met à jour le solde et les investissements
                complete(HttpEntity(ContentTypes.`application/json`, result))

              case Failure(exception) =>
                println(s"❌ Erreur globale lors de l'investissement : ${exception.getMessage}")
                complete(HttpEntity(ContentTypes.`application/json`, Json.obj("success" -> false, "message" -> "Erreur lors de l'investissement").toString()))
            }
          }
        }
      },

      // ✅ Récupération du solde
      path("api" / "get-balance") {
        post {
          entity(as[String]) { body =>
            val userId = (Json.parse(body) \ "userId").as[Int]
            val futureBalance = (utilisateurActor ? UtilisateurActor.GetBalance1(userId)).mapTo[BigDecimal]

            onComplete(futureBalance) {
              case Success(balance) =>
                complete(Json.obj("success" -> true, "balance" -> balance).toString())
              case Failure(exception) =>
                println(s"❌ Erreur lors de la récupération du solde : ${exception.getMessage}")
                complete(Json.obj("success" -> false, "message" -> "Erreur lors de la récupération du solde").toString())
            }
          }
        }
      },

      // ✅ Notification de stratégie
      path("api" / "notify-strategy") {
        post {
          entity(as[String]) { body =>
            // Parsing du JSON reçu pour obtenir la stratégie et l'ID utilisateur
            Json.parse(body).validate[InformerVolatile] match {
              case JsSuccess(request, _) =>
                // Log de la notification envoyée
                println(s"📢 Notification envoyée : ${request.strategy} à l'utilisateur ${request.userId}")

                // Envoi de la notification via l'acteur notificationActor
                val futureResponse: Future[String] = (notificationActor ? SocketActor.SendNotification(request.userId, request.strategy)).mapTo[String]

                // Envoyer une réponse une fois la notification envoyée
                onComplete(futureResponse) {
                  case Success(response) =>
                    complete(HttpEntity(ContentTypes.`application/json`, Json.obj("success" -> true, "message" -> response).toString()))
                  case Failure(exception) =>
                    // En cas d'erreur dans l'envoi de la notification
                    println(s"❌ Erreur lors de l'envoi de la notification : ${exception.getMessage}")
                    complete(HttpEntity(ContentTypes.`application/json`, Json.obj("success" -> false, "message" -> "Erreur lors de la notification").toString()))
                }

              case JsError(errors) =>
                // Gestion des erreurs lors du parsing
                val errorMessage = s"❌ Erreur lors du parsing de la notification : ${errors.mkString(", ")}"
                println(errorMessage)

                // Réponse en cas d'erreur de parsing
                complete(HttpEntity(ContentTypes.`application/json`,
                  Json.obj("success" -> false, "message" -> "Erreur lors de la notification").toString())
                )
            }
          }
        }
      },

      // ✅ Récupération des investissements
      path("api" / "get-investments") {
        post {
          entity(as[String]) { body =>
            val userId = (Json.parse(body) \ "userId").as[Int] // Récupérer l'ID utilisateur depuis la requête

            // Récupérer les investissements de l'utilisateur via l'acteur utilisateurActor2
            val futureInvestments = (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).mapTo[Seq[Investment]]

            onComplete(futureInvestments) {
              case Success(investments) =>
                val investmentsWithCompanyNames = investments.map { investment =>
                  Json.obj(
                    "investmentId" -> investment.id,
                    "companyName" -> investment.companyName, // Ajout du nom de l'entreprise
                    "amount" -> investment.amountInvested,
                  )
                }

                complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                  "success" -> true,
                  "investments" -> investmentsWithCompanyNames
                ).toString()))

              case Failure(exception) =>
                println(s"❌ Erreur lors de la récupération des investissements : ${exception.getMessage}")
                complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                  "success" -> false,
                  "message" -> "Erreur lors de la récupération des investissements"
                ).toString()))
            }
          }
        }
      }
      ,

      // ✅ Récupération des derniers prix
      path("api" / "get-last-prices") {
        post {
          val futureBtcPrices = getLastMarketPrices("BTC")
          val futureEthPrices = getLastMarketPrices("ETH")
          val futureDogePrices = getLastMarketPrices("DOGE")

          onSuccess(futureBtcPrices.zip(futureEthPrices).zip(futureDogePrices)) {
            case ((btcPrices, ethPrices), dogePrices) =>
              complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                "success" -> true,
                "prices" -> Json.obj(
                  "BTC" -> btcPrices,
                  "ETH" -> ethPrices,
                  "DOGE" -> dogePrices
                )
              ).toString()))
          }
        }
      },

      // ✅ Calcul personnalisé avec récupération des prix actuels
      path("api" / "calculate-sum") {
        post {
          entity(as[String]) { body =>
            val json = Json.parse(body)
            val userId = (json \ "userId").as[Int]
            val btcPrice = (json \ "btcPrice").asOpt[BigDecimal].getOrElse(BigDecimal(1))
            val ethPrice = (json \ "ethPrice").asOpt[BigDecimal].getOrElse(BigDecimal(1))
            val dogePrice = (json \ "dogePrice").asOpt[BigDecimal].getOrElse(BigDecimal(1))

            val futureProducts = (productsActor ? ProductsActor.GetProductsByOwner(userId)).mapTo[Seq[Product]]

            onComplete(futureProducts) {
              case Success(products) =>

                def computeRatio(company: String, currentPrice: BigDecimal): BigDecimal = {
                  if (currentPrice == 0) BigDecimal(0)
                  else {
                    val companyProducts = products.filter(_.entreprise == company)

                    if (companyProducts.isEmpty) BigDecimal(0)
                    else {
                      // Rendement moyen pondéré sur tous les produits de l'entreprise
                      val totalRatio = companyProducts.map { p =>
                        ((currentPrice - p.originalPrice) / p.originalPrice) * 100
                      }.sum

                      // Optionnel : Si tu veux la moyenne du rendement par produit
                      val averageRatio = totalRatio / companyProducts.size

                      averageRatio.setScale(2, BigDecimal.RoundingMode.HALF_UP) // Pour arrondir proprement
                    }
                  }
                }


                val btcTotal = computeRatio("BTC", btcPrice)
                val ethTotal = computeRatio("ETH", ethPrice)
                val dogeTotal = computeRatio("DOGE", dogePrice)

                println(s"✅ Calcul terminé - BTC: $btcTotal, ETH: $ethTotal, DOGE: $dogeTotal")

                complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                  "success" -> true,
                  "userId" -> userId,
                  "BTC_ratio_sum" -> btcTotal,
                  "ETH_ratio_sum" -> ethTotal,
                  "DOGE_ratio_sum" -> dogeTotal
                ).toString()))

            }
          }
        }
      },

      path("api" / "get-products-history") {
        post {
          entity(as[String]) { body =>
            val json = Json.parse(body)
            val userId = (json \ "userId").as[Int]
            val btcPrice = (json \ "btcPrice").asOpt[BigDecimal].getOrElse(BigDecimal(1))
            val ethPrice = (json \ "ethPrice").asOpt[BigDecimal].getOrElse(BigDecimal(1))
            val dogePrice = (json \ "dogePrice").asOpt[BigDecimal].getOrElse(BigDecimal(1))

            val productsFuture = (productsActor ? ProductsActor.GetProductsByOwner(userId)).mapTo[Seq[Product]]

            onComplete(productsFuture) {
              case Success(products) =>
                def computeReturn(company: String, currentPrice: BigDecimal): BigDecimal = {
                  if (currentPrice == 0) BigDecimal(0)
                  else {
                    val companyProducts = products.filter(_.entreprise == company)
                    companyProducts.map(p => ((currentPrice - p.originalPrice) / p.originalPrice) * 100).sum
                  }
                }

                def computeAverageReturn(company: String): BigDecimal = {
                  val companyProducts = products.filter(_.entreprise == company)
                  if (companyProducts.nonEmpty) {
                    val totalReturn = companyProducts.map(p => ((computeReturn(company, getCurrentPrice(company)) / 100) * p.numshare)).sum
                    totalReturn / companyProducts.size
                  } else BigDecimal(0)
                }

                def getCurrentPrice(company: String): BigDecimal = {
                  company match {
                    case "BTC" => btcPrice
                    case "ETH" => ethPrice
                    case "DOGE" => dogePrice
                    case _ => BigDecimal(1)
                  }
                }

                val historyJson = products.map { product =>
                  val currentPrice = getCurrentPrice(product.entreprise)
                  val rendement = computeReturn(product.entreprise, currentPrice)

                  Json.obj(
                    "id" -> product.investmentId,
                    "companyName" -> product.entreprise,
                    "quantity" -> product.numshare,
                    "price" -> product.originalPrice,
                    "created_at" -> product.createdAt.toString,
                    "rendement" -> rendement,
                    "averageReturn" -> computeAverageReturn(product.entreprise)
                  )
                }

                complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                  "success" -> true,
                  "history" -> historyJson
                ).toString()))

              case Failure(exception) =>
                println(s"❌ Erreur lors de la récupération des produits : ${exception.getMessage}")
                complete(HttpEntity(ContentTypes.`application/json`, Json.obj(
                  "success" -> false,
                  "message" -> "Erreur lors de la récupération des produits"
                ).toString()))
            }
          }
        }
      }





    )
  }

  // 🔥 Fonction pour envoyer une mise à jour automatique au frontend
  def updateFrontend(userId: Int): Unit = {
    println(s"🔄 Mise à jour du solde et des investissements pour l'utilisateur $userId")

    // Effectuer les appels pour obtenir les investissements et la balance
    val futureInvestments = (utilisateurActor2 ? InvestmentActor.GetInvestments(userId)).mapTo[Seq[Investment]]
    val futureBalance = (utilisateurActor ? UtilisateurActor.GetBalance1(userId)).mapTo[BigDecimal]
    val futureProducts = (productsActor ? ProductsActor.GetProductsByOwner(userId)).mapTo[Seq[Product]]

    // Une fois tous les résultats obtenus, envoyer la mise à jour au frontend
    for {
      investments <- futureInvestments
      balance <- futureBalance
      products <- futureProducts
    } yield {
      // Créer un message JSON avec les informations des investissements, de la balance et des produits
      val updateMessage = Json.obj(
        "type" -> "update",
        "userId" -> userId,
        "balance" -> balance,
        "investments" -> Json.toJson(investments),
        "products" -> Json.toJson(products)
      ).toString()

      println(s"✅ Envoi de la mise à jour au frontend : $updateMessage")
    }
  }

  def start(): Unit = {
    Http().newServerAt("localhost", 8080).bind(route)
    println("✅ Serveur WebSocket en écoute sur ws://localhost:8080/ws et API REST sur http://localhost:8080/api")
  }
}