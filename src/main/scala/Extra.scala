import akka.actor._

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

class RandomExchangeActor(exchangeName: String) extends ExchangeActor(exchangeName) {
  override def receive = {
    case ExchangeRateRequest(currencyPair) =>
      val r = scala.util.Random
      val variablePart = r.nextDouble / 2.0
      val rate = 4 + variablePart
      sender ! ExchangeRatePair(exchangeName, currencyPair, BigDecimal(rate))
  }
}

class DelayingExchangeActor(exchangeName: String) extends ExchangeActor(exchangeName) {
  override def receive = {
    case ExchangeRateRequest(currencyPair) =>
      Thread.sleep(100)
      sender ! ExchangeRatePair(exchangeName, currencyPair, BigDecimal("4.22"))
  }
}

class TimingOutExchangeActor(exchangeName: String) extends ExchangeActor(exchangeName) {
  override def receive = {
    case ExchangeRateRequest(currencyPair) => // it just never responds
  }
}

