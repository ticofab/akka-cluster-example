package io.ticofab.akkaclusterexample

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.cluster.{Cluster, MemberStatus}
import akka.routing.RoundRobinPool

import scala.sys.process._

/**
  * akka-cluster-example
  * Created by fabiotiriticco on 19/04/2017.
  */
object AkkaClusterExampleApp extends App {
  val as = ActorSystem("words")
  val roles = as.settings.config.getStringList("akka.cluster.roles")
  println("app starting with role " + roles)
  if (roles.contains("seed")) {
    Cluster(as).registerOnMemberUp {
      println("cluster is ready")
      val master = as.actorOf(Props(new Master()), "master")
      val server = as.actorOf(Props(new Server(master)), "server")
      val listener = as.actorOf(Props(new ClusterDomainEventListener(master)), "listener")
    }
  }
}

class ClusterDomainEventListener(master: ActorRef) extends Actor with ActorLogging {
  Cluster(context.system).subscribe(self, classOf[ClusterDomainEvent])

  def receive: Receive = {
    case mu@MemberUp(m) =>
      log.info(s"$m UP.")
      master ! mu
    case MemberExited(m) => log.info(s"$m EXITED.")
    case MemberRemoved(m, previousState) =>
      if (previousState == MemberStatus.Exiting) log.info(s"Member $m gracefully exited, REMOVED.")
      else log.info(s"$m downed after unreachable, REMOVED.")
    case UnreachableMember(m) => log.info(s"$m UNREACHABLE")
    case ReachableMember(m) => log.info(s"$m REACHABLE")
    case s: CurrentClusterState => log.info(s"cluster state: $s")
  }

  override def postStop(): Unit = {
    Cluster(context.system).unsubscribe(self)
    super.postStop()
  }
}

// the master receives work requests and distribute them to the workers
class Master extends Actor with ActorLogging {

  def createWorkerRouter: ActorRef = {
    context.actorOf(
      ClusterRouterPool(RoundRobinPool(4),
        ClusterRouterPoolSettings(
          totalInstances = 1000,
          maxInstancesPerNode = 2,
          allowLocalRoutees = false,
          useRole = None
        )
      ).props(Props[Worker]),
      name = "worker-router")
  }

  // state in which we're waiting for a new worker to join the cluster
  def awaitingNewWorker: Receive = {
    case MemberUp(m) =>
      println(s"Adding ${m.address} to the cluster")
      context become ready(createWorkerRouter)
  }

  def ready(router: ActorRef): Receive = {
    case SentenceToProcess(sentence) => sentence.split(' ').foreach(word => router ! WordToProcess(word))
    case WordProcessed(word) => log.info("word {} processed successfully by {}", word, sender.path.name)
    case RateExceeded =>
      // start a new local process
      Process("sbt -DPORT=2553 -DROLES.1=worker run").run()

      // dismiss the current router
      router ! PoisonPill

      context become awaitingNewWorker
  }

  // initial state
  override def receive: Receive = ready(createWorkerRouter)
}

case object RateExceeded

case class SentenceToProcess(s: String)

case class WordToProcess(s: String)

case class WordProcessed(s: String)

// execute some work on a word
class Worker extends Actor with ActorLogging {
  override def receive = {
    case WordToProcess(word) =>
      log.info("received word to process {}", word)
      Thread.sleep(2000)
      sender ! WordProcessed(word)
  }
}
