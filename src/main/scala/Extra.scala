import akka.actor._

object Extra {

  case class GetExchangeRates(currencyPair: (String, String))

  case class Exchange(exchangeName: String, exchangeActor: ActorRef)
  case class ExchangeRatePair(exchangeName: String, currencyPair: (String, String), rate: BigDecimal)
  case class ExchangeRateResults(currencyPair: (String, String), rates: Map[String, BigDecimal])
  case class ExchangeRateRequest(currencyPair: (String, String))

  class ExchangeRatesActor(exchanges: List[Exchange]) extends Actor {

    override def receive = {
      case GetExchangeRates(currencyPair) =>
        context.actorOf(Props(new Actor() {
          val results: scala.collection.mutable.Map[String, Option[BigDecimal]] = scala.collection.mutable.Map() ++ exchanges.map(e => (e.exchangeName, None)).toMap
          val originalSender = sender

          override def receive = {
            case ExchangeRatePair(exchangeName, _, rate) =>
              results(exchangeName) = Some(rate)
              respondIfDone()

          }

          def respondIfDone() = {
            if (!results.values.exists(_.isEmpty)) {
              originalSender ! ExchangeRateResults(currencyPair, results.map(result => (result._1, result._2.get)).toMap)
            }
          }
        }))
    }
  }

  abstract class ExchangeActor(exchangeName: String) extends Actor

  class StableExchangeActor(exchangeName: String) extends ExchangeActor(exchangeName) {
    override def receive = {
      case ExchangeRateRequest(currencyPair) => sender ! ExchangeRatePair(exchangeName, currencyPair, BigDecimal("4.22"))
    }
  }

}
