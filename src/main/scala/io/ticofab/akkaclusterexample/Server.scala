package io.ticofab.akkaclusterexample

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * akka-cluster-example
  * Created by fabiotiriticco on 23/0-4/2017.
  */
class Server(master: ActorRef) extends Actor with ActorLogging {

  implicit val am = ActorMaterializer()

  def route: Route = post {
    entity(as[String]) { sentence =>
      println("got request")
      master ! sentence
      complete("thanks")
    }
  } ~ get {
    complete("I'm alive")
  }

  val bindingFuture = Http()(context.system).bindAndHandle(route, "localhost", 8008)
  log.info(s"Server online at http://localhost:8008.")

  override def receive = {
    case _ => log.info("received something unexpected")
  }
}
