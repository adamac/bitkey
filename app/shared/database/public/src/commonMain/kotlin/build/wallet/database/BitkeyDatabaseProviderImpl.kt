package build.wallet.database

import app.cash.sqldelight.EnumColumnAdapter
import build.wallet.analytics.v1.Event
import build.wallet.database.adapters.*
import build.wallet.database.adapters.bitkey.*
import build.wallet.database.adapters.DurationColumnAdapter
import build.wallet.database.adapters.EmailColumnAdapter
import build.wallet.database.adapters.FullAccountColumnAdapter
import build.wallet.database.adapters.InactiveKeysetIdsColumnAdapter
import build.wallet.database.adapters.IsoCurrencyTextCodeColumnAdapter
import build.wallet.database.adapters.MobilePaySnapValueColumnAdapter
import build.wallet.database.adapters.NetworkConnectionColumnAdapter
import build.wallet.database.adapters.ProtectedCustomerAliasColumnAdapter
import build.wallet.database.adapters.PublicKeyColumnAdapter
import build.wallet.database.adapters.TimeZoneColumnAdapter
import build.wallet.database.adapters.TrustedContactAliasColumnAdapter
import build.wallet.database.adapters.bitkey.AppGlobalAuthKeyHwSignatureColumnAdapter
import build.wallet.database.adapters.bitkey.AppSpendingPublicKeyColumnAdapter
import build.wallet.database.adapters.bitkey.F8eEnvironmentColumnAdapter
import build.wallet.database.adapters.bitkey.F8eSpendingPublicKeyColumnAdapter
import build.wallet.database.adapters.bitkey.FullAccountIdColumnAdapter
import build.wallet.database.adapters.bitkey.HwAuthPublicKeyColumnAdapter
import build.wallet.database.adapters.bitkey.HwSpendingPublicKeyColumnAdapter
import build.wallet.database.adapters.bitkey.LiteAccountIdColumnAdapter
import build.wallet.database.adapters.bitkey.SoftwareAccountIdColumnAdapter
import build.wallet.database.sqldelight.*
import build.wallet.money.currency.code.IsoCurrencyTextCode
import build.wallet.partnerships.PartnershipTransactionId
import build.wallet.sqldelight.SqlDriverFactory
import build.wallet.sqldelight.adapter.ByteStringColumnAdapter
import build.wallet.sqldelight.adapter.InstantColumnAdapter
import build.wallet.sqldelight.adapter.WireColumnAdapter
import build.wallet.sqldelight.withLogging

class BitkeyDatabaseProviderImpl(sqlDriverFactory: SqlDriverFactory) : BitkeyDatabaseProvider {
  private val sqlDriver by lazy {
    sqlDriverFactory
      .createDriver(
        dataBaseName = "bitkey.db",
        dataBaseSchema = BitkeyDatabase.Schema
      )
      .withLogging(tag = "BitkeyDatabase")
  }

  private val debugSqlDriver by lazy {
    sqlDriverFactory
      .createDriver(
        dataBaseName = "bitkeyDebug.db",
        dataBaseSchema = BitkeyDebugDatabase.Schema
      )
      .withLogging(tag = "BitkeyDebugDb")
  }

  private val database by lazy {
    BitkeyDatabase(
      driver = sqlDriver,
      liteAccountEntityAdapter =
        LiteAccountEntity.Adapter(
          accountIdAdapter = LiteAccountIdColumnAdapter,
          appRecoveryAuthKeyAdapter = PublicKeyColumnAdapter(),
          bitcoinNetworkTypeAdapter = EnumColumnAdapter(),
          f8eEnvironmentAdapter = F8eEnvironmentColumnAdapter
        ),
      activeLiteAccountEntityAdapter =
        ActiveLiteAccountEntity.Adapter(
          accountIdAdapter = LiteAccountIdColumnAdapter
        ),
      onboardingLiteAccountEntityAdapter =
        OnboardingLiteAccountEntity.Adapter(
          accountIdAdapter = LiteAccountIdColumnAdapter
        ),
      fullAccountEntityAdapter =
        FullAccountEntity.Adapter(
          accountIdAdapter = FullAccountIdColumnAdapter
        ),
      activeFullAccountEntityAdapter =
        ActiveFullAccountEntity.Adapter(
          accountIdAdapter = FullAccountIdColumnAdapter
        ),
      onboardingFullAccountEntityAdapter =
        OnboardingFullAccountEntity.Adapter(
          accountIdAdapter = FullAccountIdColumnAdapter
        ),
      softwareAccountEntityAdapter =
        SoftwareAccountEntity.Adapter(
          accountIdAdapter = SoftwareAccountIdColumnAdapter,
          bitcoinNetworkTypeAdapter = EnumColumnAdapter(),
          f8eEnvironmentAdapter = F8eEnvironmentColumnAdapter
        ),
      activeSoftwareAccountEntityAdapter =
        ActiveSoftwareAccountEntity.Adapter(
          accountIdAdapter = SoftwareAccountIdColumnAdapter
        ),
      onboardingSoftwareAccountEntityAdapter =
        OnboardingSoftwareAccountEntity.Adapter(
          accountIdAdapter = SoftwareAccountIdColumnAdapter,
          appGlobalAuthKeyAdapter = PublicKeyColumnAdapter(),
          appRecoveryAuthKeyAdapter = PublicKeyColumnAdapter()
        ),
      keyboxEntityAdapter =
        KeyboxEntity.Adapter(
          accountAdapter = FullAccountColumnAdapter,
          inactiveKeysetIdsAdapter = InactiveKeysetIdsColumnAdapter,
          networkTypeAdapter = EnumColumnAdapter(),
          f8eEnvironmentAdapter = F8eEnvironmentColumnAdapter,
          delayNotifyDurationAdapter = DurationColumnAdapter,
          appGlobalAuthKeyHwSignatureAdapter = AppGlobalAuthKeyHwSignatureColumnAdapter
        ),
      transactionDetailEntityAdapter =
        TransactionDetailEntity.Adapter(
          broadcastTimeInstantAdapter = InstantColumnAdapter,
          estimatedConfirmationTimeInstantAdapter = InstantColumnAdapter
        ),
      eventEntityAdapter =
        EventEntity.Adapter(
          eventAdapter = WireColumnAdapter(Event.ADAPTER),
          f8eEnvironmentAdapter = F8eEnvironmentColumnAdapter
        ),
      gettingStartedTaskEntityAdapter =
        GettingStartedTaskEntity.Adapter(
          taskIdAdapter = EnumColumnAdapter(),
          taskStateAdapter = EnumColumnAdapter()
        ),
      exchangeRateEntityAdapter =
        ExchangeRateEntity.Adapter(
          fromCurrencyAdapter = IsoCurrencyTextCodeColumnAdapter,
          toCurrencyAdapter = IsoCurrencyTextCodeColumnAdapter,
          timeRetrievedAdapter = InstantColumnAdapter
        ),
      historicalExchangeRateEntityAdapter =
        HistoricalExchangeRateEntity.Adapter(
          timeAdapter = InstantColumnAdapter,
          fromCurrencyAdapter = IsoCurrencyTextCodeColumnAdapter,
          toCurrencyAdapter = IsoCurrencyTextCodeColumnAdapter
        ),
      onboardingStepStateEntityAdapter =
        OnboardingStepStateEntity.Adapter(
          stepIdAdapter = EnumColumnAdapter(),
          stateAdapter = EnumColumnAdapter()
        ),
      appKeyBundleEntityAdapter =
        AppKeyBundleEntity.Adapter(
          globalAuthKeyAdapter = PublicKeyColumnAdapter(),
          recoveryAuthKeyAdapter = PublicKeyColumnAdapter(),
          spendingKeyAdapter = AppSpendingPublicKeyColumnAdapter
        ),
      hwKeyBundleEntityAdapter =
        HwKeyBundleEntity.Adapter(
          spendingKeyAdapter = HwSpendingPublicKeyColumnAdapter,
          authKeyAdapter = HwAuthPublicKeyColumnAdapter
        ),
      registerWatchAddressEntityAdapter =
        RegisterWatchAddressEntity.Adapter(
          addressAdapter = BitcoinAddressColumnAdapter,
          f8eEnvironmentAdapter = F8eEnvironmentColumnAdapter
        ),
      spendingKeysetEntityAdapter =
        SpendingKeysetEntity.Adapter(
          appKeyAdapter = AppSpendingPublicKeyColumnAdapter,
          hardwareKeyAdapter = HwSpendingPublicKeyColumnAdapter,
          serverKeyAdapter = F8eSpendingPublicKeyColumnAdapter
        ),
      activeServerRecoveryEntityAdapter =
        ActiveServerRecoveryEntity.Adapter(
          accountAdapter = FullAccountColumnAdapter,
          startTimeAdapter = InstantColumnAdapter,
          endTimeAdapter = InstantColumnAdapter,
          lostFactorAdapter = EnumColumnAdapter(),
          destinationAppGlobalAuthKeyAdapter = PublicKeyColumnAdapter(),
          destinationAppRecoveryAuthKeyAdapter = PublicKeyColumnAdapter(),
          destinationHardwareAuthKeyAdapter = HwAuthPublicKeyColumnAdapter
        ),
      localRecoveryAttemptEntityAdapter =
        LocalRecoveryAttemptEntity.Adapter(
          accountAdapter = FullAccountColumnAdapter,
          destinationAppGlobalAuthKeyAdapter = PublicKeyColumnAdapter(),
          destinationAppRecoveryAuthKeyAdapter = PublicKeyColumnAdapter(),
          destinationHardwareAuthKeyAdapter = HwAuthPublicKeyColumnAdapter,
          destinationAppSpendingKeyAdapter = AppSpendingPublicKeyColumnAdapter,
          destinationHardwareSpendingKeyAdapter = HwSpendingPublicKeyColumnAdapter,
          appGlobalAuthKeyHwSignatureAdapter = AppGlobalAuthKeyHwSignatureColumnAdapter,
          serverSpendingKeyAdapter = F8eSpendingPublicKeyColumnAdapter,
          lostFactorAdapter = EnumColumnAdapter(),
          sealedCsekAdapter = ByteStringColumnAdapter
        ),
      emailTouchpointEntityAdapter =
        EmailTouchpointEntity.Adapter(
          emailAdapter = EmailColumnAdapter
        ),
      spendingLimitEntityAdapter =
        SpendingLimitEntity.Adapter(
          limitAmountCurrencyAlphaCodeAdapter = IsoCurrencyTextCodeColumnAdapter,
          limitTimeZoneZoneIdAdapter = TimeZoneColumnAdapter
        ),
      fiatCurrencyPreferenceEntityAdapter =
        FiatCurrencyPreferenceEntity.Adapter(
          currencyAdapter = IsoCurrencyTextCodeColumnAdapter
        ),
      firmwareDeviceInfoEntityAdapter =
        FirmwareDeviceInfoEntity.Adapter(
          activeSlotAdapter = EnumColumnAdapter(),
          secureBootConfigAdapter = EnumColumnAdapter()
        ),
      fwupDataEntityAdapter =
        FwupDataEntity.Adapter(
          fwupModeAdapter = EnumColumnAdapter()
        ),
      templateFullAccountConfigEntityAdapter =
        TemplateFullAccountConfigEntity.Adapter(
          bitcoinNetworkTypeAdapter = EnumColumnAdapter(),
          f8eEnvironmentAdapter = F8eEnvironmentColumnAdapter,
          delayNotifyDurationAdapter = DurationColumnAdapter
        ),
      priorityPreferenceEntityAdapter =
        PriorityPreferenceEntity.Adapter(
          priorityAdapter = EnumColumnAdapter()
        ),
      homeUiBottomSheetEntityAdapter =
        HomeUiBottomSheetEntity.Adapter(
          sheetIdAdapter = EnumColumnAdapter()
        ),
      bitcoinDisplayPreferenceEntityAdapter =
        BitcoinDisplayPreferenceEntity.Adapter(
          displayUnitAdapter = EnumColumnAdapter()
        ),
      fiatCurrencyEntityAdapter =
        FiatCurrencyEntity.Adapter(
          textCodeAdapter = IsoCurrencyTextCodeColumnAdapter
        ),
      fiatCurrencyMobilePayConfigurationEntityAdapter =
        FiatCurrencyMobilePayConfigurationEntity.Adapter(
          textCodeAdapter = IsoCurrencyTextCodeColumnAdapter,
          snapValuesAdapter = MobilePaySnapValueColumnAdapter
        ),
      socRecProtectedCustomerEntityAdapter =
        SocRecProtectedCustomerEntity.Adapter(
          aliasAdapter = ProtectedCustomerAliasColumnAdapter
        ),
      socRecTrustedContactInvitationEntityAdapter =
        SocRecTrustedContactInvitationEntity.Adapter(
          trustedContactAliasAdapter = TrustedContactAliasColumnAdapter,
          expiresAtAdapter = InstantColumnAdapter
        ),
      socRecTrustedContactEntityAdapter =
        SocRecTrustedContactEntity.Adapter(
          trustedContactAliasAdapter = TrustedContactAliasColumnAdapter,
          authenticationStateAdapter = EnumColumnAdapter()
        ),
      socRecEnrollmentAuthenticationAdapter =
        SocRecEnrollmentAuthentication.Adapter(
          protectedCustomerEnrollmentPakeKeyAdapter =
            PublicKeyColumnAdapter(),
          pakeCodeAdapter = ByteStringColumnAdapter
        ),
      socRecStartedChallengeAuthenticationAdapter =
        SocRecStartedChallengeAuthentication.Adapter(
          protectedCustomerRecoveryPakeKeyAdapter =
            PublicKeyColumnAdapter(),
          pakeCodeAdapter = ByteStringColumnAdapter
        ),
      socRecUnendorsedTrustedContactEntityAdapter =
        SocRecUnendorsedTrustedContactEntity.Adapter(
          trustedContactAliasAdapter = TrustedContactAliasColumnAdapter,
          enrollmentPakeKeyAdapter = PublicKeyColumnAdapter(),
          enrollmentKeyConfirmationAdapter = ByteStringColumnAdapter,
          authenticationStateAdapter = EnumColumnAdapter()
        ),
      socRecKeysAdapter =
        SocRecKeys.Adapter(
          purposeAdapter = EnumColumnAdapter(),
          keyAdapter = PublicKeyColumnAdapter()
        ),
      networkReachabilityEventEntityAdapter =
        NetworkReachabilityEventEntity.Adapter(
          connectionAdapter = NetworkConnectionColumnAdapter,
          reachabilityAdapter = EnumColumnAdapter(),
          timestampAdapter = InstantColumnAdapter
        ),
      onboardingKeyboxHwAuthPublicKeyAdapter =
        OnboardingKeyboxHwAuthPublicKey.Adapter(
          hwAuthPublicKeyAdapter = HwAuthPublicKeyColumnAdapter,
          appGlobalAuthKeyHwSignatureAdapter = AppGlobalAuthKeyHwSignatureColumnAdapter
        ),
      authKeyRotationAttemptEntityAdapter =
        AuthKeyRotationAttemptEntity.Adapter(
          destinationAppGlobalAuthKeyAdapter = PublicKeyColumnAdapter(),
          destinationAppRecoveryAuthKeyAdapter = PublicKeyColumnAdapter(),
          destinationAppGlobalAuthKeyHwSignatureAdapter = AppGlobalAuthKeyHwSignatureColumnAdapter
        ),
      partnershipTransactionEntityAdapter = PartnershipTransactionEntity.Adapter(
        transactionIdAdapter = DelegatedColumnAdapter(::PartnershipTransactionId, PartnershipTransactionId::value),
        fiatCurrencyAdapter = DelegatedColumnAdapter(::IsoCurrencyTextCode, IsoCurrencyTextCode::code),
        typeAdapter = EnumColumnAdapter(),
        statusAdapter = EnumColumnAdapter(),
        createdAdapter = InstantColumnAdapter,
        updatedAdapter = InstantColumnAdapter
      ),
      coachmarkEntityAdapter = CoachmarkEntity.Adapter(
        expirationAdapter = InstantColumnAdapter
      )
    )
  }

  private val debugDatabase by lazy {
    BitkeyDebugDatabase(
      driver = debugSqlDriver,
      onboardingStepSkipConfigEntityAdapter =
        OnboardingStepSkipConfigEntity.Adapter(
          onboardingStepAdapter = EnumColumnAdapter()
        )
    )
  }

  override fun database(): BitkeyDatabase = database

  override fun debugDatabase(): BitkeyDebugDatabase = debugDatabase
}
