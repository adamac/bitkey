package build.wallet.money.exchange

import build.wallet.bitkey.f8e.AccountId
import build.wallet.f8e.F8eEnvironment
import build.wallet.f8e.client.F8eHttpClient
import build.wallet.ktor.result.NetworkingError
import build.wallet.ktor.result.RedactedRequestBody
import build.wallet.ktor.result.RedactedResponseBody
import build.wallet.ktor.result.bodyResult
import build.wallet.ktor.result.setRedactedBody
import build.wallet.logging.logNetworkFailure
import build.wallet.money.currency.code.IsoCurrencyTextCode
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.zacsweers.redacted.annotations.Unredacted
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ExchangeRateF8eClientImpl(
  private val f8eHttpClient: F8eHttpClient,
) : ExchangeRateF8eClient {
  override suspend fun getExchangeRates(
    f8eEnvironment: F8eEnvironment,
  ): Result<List<ExchangeRate>, NetworkingError> {
    return f8eHttpClient.unauthenticated(f8eEnvironment)
      .bodyResult<ResponseBody> {
        get("/api/exchange-rates")
      }.map { response ->
        response.exchangeRates.map { body ->
          body.exchangeRate()
        }
      }
      .logNetworkFailure { "Failed to get exchange rates" }
  }

  override suspend fun getHistoricalBtcExchangeRates(
    f8eEnvironment: F8eEnvironment,
    accountId: AccountId,
    currencyCode: String,
    timestamps: List<Instant>,
  ): Result<List<ExchangeRate>, NetworkingError> {
    return f8eHttpClient.authenticated(f8eEnvironment, accountId)
      .bodyResult<ResponseBody> {
        post("/api/exchange-rates/historical") {
          setRedactedBody(HistoricalRateRequest(currencyCode, timestamps.map { it.epochSeconds }))
        }
      }
      .map { response ->
        response.exchangeRates.map { body ->
          body.exchangeRate()
        }
      }
      .logNetworkFailure { "Failed to get historical exchange rate for $currencyCode" }
  }

  @Serializable
  data class HistoricalRateRequest(
    @SerialName("currency_code")
    val currencyCode: String,
    val timestamps: List<Long>,
  ) : RedactedRequestBody

  @Serializable
  private data class ResponseBody(
    @Unredacted
    @SerialName("exchange_rates")
    val exchangeRates: List<ExchangeRateDTO>,
  ) : RedactedResponseBody

  @Serializable
  private data class ExchangeRateDTO(
    @SerialName("from_currency")
    val fromCurrency: String,
    @SerialName("to_currency")
    val toCurrency: String,
    @SerialName("time_retrieved")
    val timeRetrieved: Instant,
    val rate: Double,
  ) {
    fun exchangeRate(): ExchangeRate {
      return ExchangeRate(
        fromCurrency = IsoCurrencyTextCode(fromCurrency),
        toCurrency = IsoCurrencyTextCode(toCurrency),
        rate = rate,
        timeRetrieved = timeRetrieved
      )
    }
  }
}