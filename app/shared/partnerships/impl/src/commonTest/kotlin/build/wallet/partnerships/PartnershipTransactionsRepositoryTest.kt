package build.wallet.partnerships

import build.wallet.bitkey.f8e.FullAccountIdMock
import build.wallet.coroutines.turbine.turbines
import build.wallet.db.DbTransactionError
import build.wallet.f8e.F8eEnvironment
import build.wallet.f8e.partnerships.FakePartnershipTransfer
import build.wallet.f8e.partnerships.GetPartnershipTransactionF8eClientMock
import build.wallet.ktor.result.HttpError
import build.wallet.ktor.test.HttpResponseMock
import build.wallet.time.ClockFake
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.datetime.Instant

class PartnershipTransactionsRepositoryTest : FunSpec({
  val clock = ClockFake(Instant.fromEpochMilliseconds(123))
  val testId = PartnershipTransactionId("test-transaction-id")

  val daoMock = PartnershipTransactionsDaoMock(
    saveCalls = turbines.create("Save Calls"),
    getTransactionsSubscriptions = turbines.create("Get Transactions Calls"),
    getByIdCalls = turbines.create("Get Most Recent Calls"),
    deleteTransactionCalls = turbines.create("Delete Transaction Calls"),
    clearCalls = turbines.create("Clear Calls")
  )
  val getPartnershipsF8eClient = GetPartnershipTransactionF8eClientMock(
    turbine = turbines::create
  )

  afterTest {
    daoMock.reset()
  }

  test("Create Transaction") {
    val repository = PartnershipTransactionsRepositoryImpl(
      dao = daoMock,
      uuidGenerator = { "test-uuid" },
      clock = clock,
      getPartnershipTransactionF8eClient = getPartnershipsF8eClient
    )

    val result = repository.create(
      partnerInfo = PartnerInfo(
        partnerId = PartnerId("test-partner"),
        name = "test-partner-name",
        logoUrl = "test-partner-logo-url"
      ),
      type = PartnershipTransactionType.PURCHASE
    )

    val savedTransaction = daoMock.saveCalls.awaitItem()
    result.get()
      .shouldBeInstanceOf<PartnershipTransaction>()
      .should { result ->
        savedTransaction.shouldBeSameInstanceAs(result)
        result.id.value.shouldBe("test-uuid")
        result.type.shouldBe(PartnershipTransactionType.PURCHASE)
        result.status.shouldBeNull()
        result.partnerInfo.partnerId.value.shouldBe("test-partner")
        result.partnerInfo.name.shouldBe("test-partner-name")
        result.partnerInfo.logoUrl.shouldBe("test-partner-logo-url")
        result.cryptoAmount.shouldBeNull()
        result.txid.shouldBeNull()
        result.fiatAmount.shouldBeNull()
        result.fiatCurrency.shouldBeNull()
        result.paymentMethod.shouldBeNull()
        result.created.toEpochMilliseconds().shouldBe(123)
        result.updated.toEpochMilliseconds().shouldBe(123)
      }
  }

  test("Sync Transaction") {
    val repository = PartnershipTransactionsRepositoryImpl(
      dao = daoMock,
      uuidGenerator = { TODO() },
      clock = clock,
      getPartnershipTransactionF8eClient = getPartnershipsF8eClient
    )
    daoMock.getByIdResult = Ok(
      FakePartnershipTransaction.copy(
        id = PartnershipTransactionId("test-transaction-id"),
        partnerInfo = PartnerInfo(
          partnerId = PartnerId("test-partner"),
          name = "test-partner-name",
          logoUrl = null
        )
      )
    )
    getPartnershipsF8eClient.response = Ok(
      FakePartnershipTransfer.copy(
        id = PartnershipTransactionId("test-transaction-id"),
        paymentMethod = "test-update"
      )
    )

    val result = repository.syncTransaction(FullAccountIdMock, F8eEnvironment.Local, testId)

    daoMock.getByIdCalls.awaitItem().shouldBe(testId)
    val (fetchedPartnerId, fetchedTransactionId) = getPartnershipsF8eClient.getTransactionCalls.awaitItem()
    val savedTransaction = daoMock.saveCalls.awaitItem()

    fetchedPartnerId.shouldBe(PartnerId("test-partner"))
    fetchedTransactionId.shouldBe(testId)
    result.value.shouldBeInstanceOf<PartnershipTransaction>().should {
      it.id.shouldBe(testId)
      it.paymentMethod.shouldBe("test-update")
    }
  }

  test("Sync Transaction -- DAO Save Failure") {
    val repository = PartnershipTransactionsRepositoryImpl(
      dao = daoMock,
      uuidGenerator = { TODO() },
      clock = clock,
      getPartnershipTransactionF8eClient = getPartnershipsF8eClient
    )
    daoMock.getByIdResult = Ok(
      FakePartnershipTransaction.copy(
        id = testId,
        partnerInfo = PartnerInfo(
          partnerId = PartnerId("test-partner"),
          name = "test-partner-name",
          logoUrl = null
        )
      )
    )
    getPartnershipsF8eClient.response = Ok(
      FakePartnershipTransfer.copy(
        id = testId,
        paymentMethod = "test-update"
      )
    )
    daoMock.saveResult = Err(DbTransactionError(RuntimeException("test save failure")))

    val result = repository.syncTransaction(FullAccountIdMock, F8eEnvironment.Local, testId)

    daoMock.getByIdCalls.awaitItem()
    getPartnershipsF8eClient.getTransactionCalls.awaitItem()
    daoMock.saveCalls.awaitItem()
    result.getError().shouldBeTypeOf<DbTransactionError>().cause.message.shouldBe("test save failure")
  }

  test("Sync Most Recent Transaction -- DAO delete Failure") {
    val repository = PartnershipTransactionsRepositoryImpl(
      dao = daoMock,
      uuidGenerator = { TODO() },
      clock = clock,
      getPartnershipTransactionF8eClient = getPartnershipsF8eClient
    )
    daoMock.getByIdResult = Ok(
      FakePartnershipTransaction.copy(
        id = PartnershipTransactionId("test-transaction-id"),
        partnerInfo = PartnerInfo(
          partnerId = PartnerId("test-partner"),
          name = "test-partner-name",
          logoUrl = null
        )
      )
    )
    getPartnershipsF8eClient.response = Err(HttpError.ClientError(HttpResponseMock(NotFound)))
    daoMock.deleteTransactionResult =
      Err(DbTransactionError(RuntimeException("test delete failure")))

    val result = repository.syncTransaction(FullAccountIdMock, F8eEnvironment.Local, testId)

    daoMock.getByIdCalls.awaitItem()
    getPartnershipsF8eClient.getTransactionCalls.awaitItem()
    daoMock.deleteTransactionCalls.awaitItem()
    result.getError().shouldBeTypeOf<DbTransactionError>().cause.message.shouldBe("test delete failure")
  }

  test("Sync Most Recent Transaction -- Not Found") {
    val repository = PartnershipTransactionsRepositoryImpl(
      dao = daoMock,
      uuidGenerator = { TODO() },
      clock = clock,
      getPartnershipTransactionF8eClient = getPartnershipsF8eClient
    )
    daoMock.getByIdResult = Ok(
      FakePartnershipTransaction.copy(
        id = PartnershipTransactionId("test-transaction-id"),
        partnerInfo = PartnerInfo(
          partnerId = PartnerId("test-partner"),
          name = "test-partner-name",
          logoUrl = null
        )
      )
    )
    getPartnershipsF8eClient.response = Err(HttpError.ClientError(HttpResponseMock(NotFound)))

    val result = repository.syncTransaction(FullAccountIdMock, F8eEnvironment.Local, testId)

    daoMock.getByIdCalls.awaitItem()
    val (fetchedPartnerId, fetchedTransactionId) = getPartnershipsF8eClient.getTransactionCalls.awaitItem()

    fetchedPartnerId.shouldBe(PartnerId("test-partner"))
    fetchedTransactionId.shouldBe(PartnershipTransactionId("test-transaction-id"))
    result.shouldBe(Ok(null))
    daoMock.deleteTransactionCalls.awaitItem()
      .shouldBe(PartnershipTransactionId("test-transaction-id"))
  }

  test("Sync Most Recent Transaction -- Server Error") {
    val repository = PartnershipTransactionsRepositoryImpl(
      dao = daoMock,
      uuidGenerator = { TODO() },
      clock = clock,
      getPartnershipTransactionF8eClient = getPartnershipsF8eClient
    )
    daoMock.getByIdResult = Ok(
      FakePartnershipTransaction.copy(
        id = PartnershipTransactionId("test-transaction-id")
      )
    )
    getPartnershipsF8eClient.response =
      Err(HttpError.ServerError(HttpResponseMock(HttpStatusCode.ServiceUnavailable)))

    val result = repository.syncTransaction(FullAccountIdMock, F8eEnvironment.Local, testId)

    daoMock.getByIdCalls.awaitItem()
    getPartnershipsF8eClient.getTransactionCalls.awaitItem()

    result.getError().shouldBeInstanceOf<HttpError.ServerError>()
  }
})
