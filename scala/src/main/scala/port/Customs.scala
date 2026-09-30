package port

sealed trait ClearanceError
case object NotFound extends ClearanceError
final case class OnHold(reason: String) extends ClearanceError

trait CustomsGateway {
  /** Left(NotFound) for any container id customs has no record of, Left(OnHold) while a hold is open.
    * Right(()) only when the container is cleared. Never throws.
    */
  def clearance(containerId: String): Either[ClearanceError, Unit]
}

final class ReleaseService(customs: CustomsGateway, tracker: Tracker) {
  def release(containerId: String): Either[String, String] =
    customs.clearance(containerId) match {
      case Left(NotFound)    => Left(s"unknown container $containerId")
      case Left(OnHold(why)) => Left(s"on hold: $why")
      case Right(_)          => Right(s"released $containerId (${tracker.statusOf(containerId).getOrElse("n/a")})")
    }
}
