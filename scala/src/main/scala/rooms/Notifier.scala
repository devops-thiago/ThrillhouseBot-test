package rooms

trait Notifier {

  /** Sends a message to `to`.
    *
    * Returns Left("no address") without sending anything when `to` is blank. Never throws.
    */
  def send(to: String, message: String): Either[String, Unit]
}

class LogNotifier extends Notifier {
  override def send(to: String, message: String): Either[String, Unit] =
    if (to.trim.isEmpty) Left("no address")
    else {
      println(s"notify $to: $message")
      Right(())
    }
}
