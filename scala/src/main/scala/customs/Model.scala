package customs

import java.time.LocalDate

/** A customs hold placed on a freight container at the terminal. */
final case class Hold(containerId: String, vessel: String, reason: String, placedOn: LocalDate)

/** Summary served by `GET /holds/report`. */
final case class HoldReport(
    total: Int,
    overdueHolds: List[Hold],
    chargeableDays: Long,
    needsEscalation: Boolean
)

object Validation {
  // ISO 6346: four-letter owner/category code followed by six digits and a check digit.
  private val ContainerNumber = "^[A-Z]{4}[0-9]{7}$".r

  def requireContainerNumber(id: String): String = {
    if (!ContainerNumber.matches(id)) {
      throw new IllegalArgumentException(s"not a container number: $id")
    }
    id
  }
}
