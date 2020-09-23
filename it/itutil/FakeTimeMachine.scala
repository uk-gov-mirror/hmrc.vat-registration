package itutil

import java.time.LocalDate

import utils.TimeMachine

class FakeTimeMachine extends TimeMachine with ITFixtures {
  override def today: LocalDate = testDate
}
