### Building
-sudo sbt compile
-sudo sbt
-reload
-sudo docker build -t app .
-sudo docker images
-sudo docker run --net=host -p 8080:8080 -t <NOM_DE_L'IMAGE>
