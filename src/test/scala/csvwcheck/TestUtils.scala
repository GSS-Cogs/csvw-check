package csvwcheck

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import csvwcheck.models.WarningsAndErrors

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object TestUtils {
  implicit final val system: ActorSystem = ActorSystem("actor-system")

  def RunValidationInAkka(validator: Validator): WarningsAndErrors = {
    var warningsAndErrors = WarningsAndErrors()
    val akkaStream =
      validator.validate().map(wAndE => warningsAndErrors = wAndE)
    Await.ready(akkaStream.runWith(Sink.ignore), 10.hours)

    warningsAndErrors
  }
}
