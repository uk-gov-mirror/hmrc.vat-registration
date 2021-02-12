
package itutil

import utils.TimeMachine

import java.time.{LocalDate, LocalDateTime, LocalTime}
import javax.inject.Singleton

@Singleton
class FakeTimeMachine extends TimeMachine with ITFixtures {
  override def today: LocalDate = testDate
  override def timestamp: LocalDateTime = LocalDateTime.of(testDate, LocalTime.of(FakeTimeMachine.hour, 0))
}

object FakeTimeMachine {
  var hour: Int = 9
}
