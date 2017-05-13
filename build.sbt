name := "akka-cluster-example"
version := "1.0.0"
scalaVersion := "2.12.2"

libraryDependencies ++= {
  val akkaVersion = "2.5.1"
  val akkaHttpVersion = "10.0.6"
  Seq(
    // akka stuff
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1",

    // joda datetime for scala
    "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  )
}
