package com.example

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import com.example.ActorHierarchyExperiments.system
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.StdIn

/**
  * Created by salho on 20.05.18.
  */
object QuicksortAkka extends App {

  trait SortMessage
  case class Sort(list: List[Int]) extends SortMessage
  case class Result(sorted: List[Int]) extends SortMessage

  class QuickSorter extends Actor with ActorLogging {


    var pivots: List[Int] = Nil

    override def receive: Receive = LoggingReceive {
      case Sort(list) => list match {
        case l if l.length <= 1 =>
          sender() ! Result(l)
          context.stop(self)
        case l => {
          val leftSorter = context.actorOf(Props[QuickSorter],"left")
          val rightSorter = context.actorOf(Props[QuickSorter],"right")
          val pivot = list(scala.util.Random.nextInt(list.length))
          leftSorter ! Sort(l.filter(_ < pivot))
          rightSorter ! Sort(l.filter(_ > pivot))
          this.pivots = l.filter(_ == pivot)
          context.become(waitForLeftOrRightResult(sender(),leftSorter))
        }
      }
    }

    def waitForLeftOrRightResult(recipient: ActorRef, left: ActorRef): Receive = LoggingReceive {
      case Result(list) => if (sender() == left)
        context.become(waitForRightResult(recipient,list)) else
        context.become(waitForLeftResult(recipient,list))
    }

    def waitForLeftResult(recipient: ActorRef, rightResult: List[Int]): Receive = LoggingReceive {
      case Result(list) => {
        recipient ! Result(list ++ this.pivots ++ rightResult)
        context.stop(self)
      }
    }

    def waitForRightResult(recipient: ActorRef, leftResult: List[Int]): Receive = LoggingReceive {
      case Result(list) => {
        recipient ! Result(leftResult ++ this.pivots ++ list)
        context.stop(self)
      }
    }

  }

  class QuickSortRunner extends Actor with ActorLogging {

    override def receive = start

    def start: Receive = LoggingReceive {
      case Sort(list) => {
        val sorter = context.actorOf(Props[QuickSorter],"quicksorter")
        sorter ! Sort(list)
        context.become(result)
      }
    }

    def result: Receive = LoggingReceive {
      case Result(list) =>
        println(s"Result: $list")
        context.become(start)
    }

  }

  val system: ActorSystem = ActorSystem("quicksortAkka")
  val sorter = system.actorOf(Props[QuickSortRunner],"runner")
  sorter ! Sort(List(4, 2, 8, 7, 21, 1, 1, 0))
  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally system.terminate()

}
