package com.example.iot

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.example.iot.DeviceGroup.{ReplyDeviceList, RequestAllTemperatures, RequestDeviceList}
import com.example.iot.DeviceManager.RequestTrackDevice

object DeviceGroup {
  def props(groupId: String): Props = Props(new DeviceGroup(groupId))
  final case class RequestDeviceList(requestId: Long)
  final case class ReplyDeviceList(requestId: Long, ids: Set[String])
  final case class RequestAllTemperatures(requestId: Long)
  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  sealed trait TemperatureReading
  final case class Temperature(value: Double) extends TemperatureReading
  case object TemperatureNotAvailable extends TemperatureReading
  case object DeviceNotAvailable extends TemperatureReading
  case object DeviceTimedOut extends TemperatureReading
}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {
  var deviceIdToActor = Map.empty[String, ActorRef]

  override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)

  override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)

  override def receive: Receive = {
    case trackMsg @ RequestTrackDevice(`groupId`, _) ⇒
      deviceIdToActor.get(trackMsg.deviceId) match {
        case Some(deviceActor) ⇒
          deviceActor forward trackMsg
        case None ⇒
          log.info("Creating device actor for {}", trackMsg.deviceId)
          val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          context.watch(deviceActor)
          deviceActor forward trackMsg
      }

    case RequestTrackDevice(groupId, deviceId) ⇒
      log.warning(
        "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
        groupId, this.groupId
      )

    case RequestDeviceList(requestId) ⇒
      sender() ! ReplyDeviceList(requestId, deviceIdToActor.keySet)

    case RequestAllTemperatures(requestId) ⇒
      import scala.concurrent.duration._
      context.actorOf(DeviceGroupQuery.props(
        actorToDeviceId = deviceIdToActor.map(_.swap),
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))

    case Terminated(deviceActor) ⇒
      log.info("Device actor {} has been terminated", deviceActor)
      deviceIdToActor = deviceIdToActor.filter(p=>p._2 != deviceActor)
  }
}
