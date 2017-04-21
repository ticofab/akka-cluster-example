package io.ticofab.akkaclusterexample

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}

/**
  * akka-cluster-example
  * Created by fabiotiriticco on 19/04/2017.
  */
object AkkaClusterExampleApp extends App {
  val as = ActorSystem("words")
  val roles = as.settings.config.getStringList("akka.cluster.roles")
  println("app starting with role " + roles)
  val listener = as.actorOf(Props(new ClusterDomainEventListener()), "listener")
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
