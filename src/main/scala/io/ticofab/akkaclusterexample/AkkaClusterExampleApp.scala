package io.ticofab.akkaclusterexample

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.cluster.{Cluster, MemberStatus}
import akka.routing.RoundRobinPool

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
      val listener = as.actorOf(Props(new ClusterDomainEventListener()), "listener")
    }
  }
}

class ClusterDomainEventListener extends Actor with ActorLogging {
  Cluster(context.system).subscribe(self, classOf[ClusterDomainEvent])

  def receive = {
    case MemberUp(m) => log.info(s"$m UP.")
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

class Master extends Actor with ActorLogging {

  val router = createWorkerRouter

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

  override def receive = {
    case sentence: String => sentence.split(' ').foreach(word => router ! word)
    case Ok(w) => log.info("word {} processed successfully by {}", w, sender.path.name)
  }
}

case class Ok(s: String)

class Worker extends Actor with ActorLogging {
  override def receive = {
    case s: String =>
      log.info("received string {}", s)
      sender ! Ok(s)
  }
}
