package itutil

import java.time.LocalDate

import utils.TimeMachine

class FakeTimeMachine extends TimeMachine {
  override def today: LocalDate = LocalDate.parse("2020-01-01")
}
