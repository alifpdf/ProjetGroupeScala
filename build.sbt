ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "projetScalaGR"
  )
// Définition des versions
val akkaVersion = "2.8.8"
val akkaHttpVersion = "10.5.3"
val slickVersion = "3.5.2"

// Dépendances principales (Akka, HTTP, JSON, Logging)
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "io.spray" %% "spray-json" % "1.3.6",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.16"
)
libraryDependencies += "com.github.jwt-scala" %% "jwt-spray-json" % "9.0.0"


// Dépendances pour PostgreSQL et Slick
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "org.postgresql" % "postgresql" % "42.7.5",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.9.4"
)



libraryDependencies += "ch.megard" %% "akka-http-cors" % "1.2.0"
