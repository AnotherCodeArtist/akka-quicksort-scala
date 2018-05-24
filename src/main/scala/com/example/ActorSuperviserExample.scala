package com.example

import akka.actor.{Actor, ActorSystem, Props}
import com.example.ActorHierarchyExperiments.system

import scala.io.StdIn


class SupervisingActor extends Actor {
  val child = context.actorOf(Props[SupervisedActor], "supervised-actor")

  override def receive: Receive = {
    case "failChild" ⇒ child ! "fail"
  }
}

class SupervisedActor extends Actor {
  override def preStart(): Unit = println("supervised actor started")
  override def postStop(): Unit = println("supervised actor stopped")

  override def receive: Receive = {
    case "fail" ⇒
      println("supervised actor fails now")
      throw new Exception("I failed!")
  }
}

object ActorSuperviserExample extends App {
  val system = ActorSystem("supervisorTest")
  val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")
  supervisingActor ! "failChild"
  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally system.terminate()
}
