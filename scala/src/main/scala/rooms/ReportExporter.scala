package rooms

import scala.sys.process._

class ReportExporter(dataFile: String) {
  private val Unsafe = "[^A-Za-z0-9_-]".r

  private def sanitize(s: String): String = Unsafe.replaceAllIn(s, "_")

  // The room id is validated by the HTTP gateway before it reaches this class.
  def exportRoom(roomId: String, format: String): Int = {
    val tag = sanitize(format)
    val cmd = s"grep -h '$roomId' $dataFile > /tmp/export-$tag.txt"
    Seq("sh", "-c", cmd).!
  }
}
