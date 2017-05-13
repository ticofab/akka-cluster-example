package io.ticofab.akkaclusterexample

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * akka-cluster-example
  * Created by fabiotiriticco on 23/04/2017.
  */
class Server(master: ActorRef) extends Actor with ActorLogging {

  implicit val am = ActorMaterializer()
  private var requestTimestamps = List[Long]()

  def route: Route = post {
    entity(as[String]) { sentence =>
      val arrivalMoment = DateTime.now
      requestTimestamps = arrivalMoment.getMillis :: requestTimestamps.filter(_ > arrivalMoment.minus(1000).getMillis)
      val requestsPerSecond = requestTimestamps.size
      println(s"got request, current rate is $requestsPerSecond per second")

      if (requestsPerSecond > 2) {
        println("rate exceeded!")
        master ! RateExceeded
      }

      master ! SentenceToProcess(sentence)
      complete("thanks\n")
    }
  } ~ get {
    complete("I'm alive\n")
  }

  Http()(context.system).bindAndHandle(route, "localhost", 8008)
  log.info(s"Server online at http://localhost:8008.")

  override def receive: Receive = {
    case _ => log.info("received something unexpected")
  }
}
