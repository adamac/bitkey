package build.wallet.statemachine.settings.full.device

import androidx.compose.runtime.*
import build.wallet.analytics.events.screen.context.NfcEventTrackerScreenIdContext.METADATA
import build.wallet.analytics.events.screen.id.EventTrackerScreenId
import build.wallet.analytics.events.screen.id.SettingsEventTrackerScreenId
import build.wallet.availability.AppFunctionalityStatus
import build.wallet.availability.AppFunctionalityStatusProvider
import build.wallet.availability.FunctionalityFeatureStates
import build.wallet.feature.flags.MultipleFingerprintsIsEnabledFeatureFlag
import build.wallet.feature.flags.ResetDeviceIsEnabledFeatureFlag
import build.wallet.firmware.EnrolledFingerprints
import build.wallet.firmware.FirmwareDeviceInfo
import build.wallet.firmware.FirmwareDeviceInfoDao
import build.wallet.statemachine.core.ScreenModel
import build.wallet.statemachine.core.ScreenPresentationStyle.Modal
import build.wallet.statemachine.core.ScreenPresentationStyle.Root
import build.wallet.statemachine.data.firmware.FirmwareData
import build.wallet.statemachine.data.recovery.inprogress.RecoveryInProgressData.CompletingRecoveryData
import build.wallet.statemachine.data.recovery.inprogress.RecoveryInProgressData.WaitingForRecoveryDelayPeriodData
import build.wallet.statemachine.data.recovery.losthardware.LostHardwareRecoveryData.LostHardwareRecoveryInProgressData
import build.wallet.statemachine.fwup.FwupNfcUiProps
import build.wallet.statemachine.fwup.FwupNfcUiStateMachine
import build.wallet.statemachine.nfc.NfcSessionUIStateMachine
import build.wallet.statemachine.nfc.NfcSessionUIStateMachineProps
import build.wallet.statemachine.recovery.losthardware.LostHardwareRecoveryProps
import build.wallet.statemachine.recovery.losthardware.LostHardwareRecoveryUiStateMachine
import build.wallet.statemachine.recovery.losthardware.initiate.InstructionsStyle
import build.wallet.statemachine.settings.full.device.DeviceSettingsUiState.*
import build.wallet.statemachine.settings.full.device.fingerprints.EntryPoint
import build.wallet.statemachine.settings.full.device.fingerprints.ManagingFingerprintsProps
import build.wallet.statemachine.settings.full.device.fingerprints.ManagingFingerprintsUiStateMachine
import build.wallet.statemachine.settings.full.device.fingerprints.PromptingForFingerprintFwUpSheetModel
import build.wallet.statemachine.settings.full.device.resetdevice.ResettingDeviceProps
import build.wallet.statemachine.settings.full.device.resetdevice.ResettingDeviceUiStateMachine
import build.wallet.statemachine.status.AppFunctionalityStatusAlertModel
import build.wallet.time.DateTimeFormatter
import build.wallet.time.DurationFormatter
import build.wallet.time.TimeZoneProvider
import build.wallet.time.nonNegativeDurationBetween
import build.wallet.ui.model.alert.ButtonAlertModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

class DeviceSettingsUiStateMachineImpl(
  private val lostHardwareRecoveryUiStateMachine: LostHardwareRecoveryUiStateMachine,
  private val nfcSessionUIStateMachine: NfcSessionUIStateMachine,
  private val firmwareDeviceInfoDao: FirmwareDeviceInfoDao,
  private val fwupNfcUiStateMachine: FwupNfcUiStateMachine,
  private val dateTimeFormatter: DateTimeFormatter,
  private val timeZoneProvider: TimeZoneProvider,
  private val durationFormatter: DurationFormatter,
  private val appFunctionalityStatusProvider: AppFunctionalityStatusProvider,
  private val multipleFingerprintsIsEnabledFeatureFlag: MultipleFingerprintsIsEnabledFeatureFlag,
  private val resetDeviceIsEnabledFeatureFlag: ResetDeviceIsEnabledFeatureFlag,
  private val managingFingerprintsUiStateMachine: ManagingFingerprintsUiStateMachine,
  private val resettingDeviceUiStateMachine: ResettingDeviceUiStateMachine,
) : DeviceSettingsUiStateMachine {
  @Composable
  override fun model(props: DeviceSettingsProps): ScreenModel {
    var uiState: DeviceSettingsUiState by remember {
      mutableStateOf(ViewingDeviceDataUiState())
    }

    var alertModel: ButtonAlertModel? by remember { mutableStateOf(null) }

    val appFunctionalityStatus by remember {
      appFunctionalityStatusProvider.appFunctionalityStatus(
        props.accountData.account.config.f8eEnvironment
      )
    }.collectAsState(AppFunctionalityStatus.FullFunctionality)

    val securityAndRecoveryStatus by remember {
      derivedStateOf {
        appFunctionalityStatus.featureStates.securityAndRecovery
      }
    }

    val multipleFingerprintsEnabled = remember {
      multipleFingerprintsIsEnabledFeatureFlag.flagValue().value.value
    }

    val resetDeviceEnabled = remember {
      resetDeviceIsEnabledFeatureFlag.flagValue().value.value
    }

    return when (val state = uiState) {
      is ViewingDeviceDataUiState -> {
        val availability = props.firmwareData.firmwareDeviceInfo?.let { deviceInfo ->
          FirmwareDeviceAvailability.Present(deviceInfo)
        } ?: FirmwareDeviceAvailability.None
        ViewingDeviceScreenModel(
          props = props,
          firmwareDeviceAvailability = availability,
          goToFwup = { uiState = UpdatingFirmwareUiState(it) },
          goToNfcMetadata = { uiState = TappingForFirmwareMetadataUiState },
          goToRecovery = {
            if (securityAndRecoveryStatus == FunctionalityFeatureStates.FeatureState.Available) {
              uiState = InitiatingHardwareRecoveryUiState
            } else {
              alertModel =
                AppFunctionalityStatusAlertModel(
                  status = appFunctionalityStatus as AppFunctionalityStatus.LimitedFunctionality,
                  onDismiss = { alertModel = null }
                )
            }
          },
          onManageReplacement = { uiState = HardwareRecoveryDelayAndNotifyUiState },
          onResetDevice = { uiState = ResettingDeviceUiState },
          dateTimeFormatter = dateTimeFormatter,
          timeZoneProvider = timeZoneProvider,
          durationFormatter = durationFormatter,
          replaceDeviceEnabled = securityAndRecoveryStatus == FunctionalityFeatureStates.FeatureState.Available,
          multipleFingerprintsEnabled = multipleFingerprintsEnabled,
          resetDeviceEnabled = resetDeviceEnabled,
          onManageFingerprints = { uiState = ManagingFingerprintsUiState }
        ).copy(
          alertModel = alertModel,
          bottomSheetModel = PromptingForFingerprintFwUpSheetModel(
            onCancel = { uiState = ViewingDeviceDataUiState() },
            onUpdate = {
              uiState = when (val fwupState = props.firmwareData.firmwareUpdateState) {
                is FirmwareData.FirmwareUpdateState.PendingUpdate -> UpdatingFirmwareUiState(
                  pendingFirmwareUpdate = fwupState
                )
                FirmwareData.FirmwareUpdateState.UpToDate -> {
                  ViewingDeviceDataUiState()
                }
              }
            }
          ).takeIf { state.showingPromptForFingerprintFwUpdate }
        )
      }

      InitiatingHardwareRecoveryUiState ->
        lostHardwareRecoveryUiStateMachine.model(
          props = LostHardwareRecoveryProps(
            account = props.accountData.account,
            lostHardwareRecoveryData = props.accountData.lostHardwareRecoveryData,
            screenPresentationStyle = Modal,
            instructionsStyle = InstructionsStyle.Independent,
            onFoundHardware = {}, // noop
            onExit = { uiState = ViewingDeviceDataUiState() }
          )
        )

      HardwareRecoveryDelayAndNotifyUiState ->
        lostHardwareRecoveryUiStateMachine.model(
          props = LostHardwareRecoveryProps(
            account = props.accountData.account,
            lostHardwareRecoveryData = props.accountData.lostHardwareRecoveryData,
            screenPresentationStyle = Modal,
            instructionsStyle = InstructionsStyle.Independent,
            onFoundHardware = {}, // noop
            onExit = { uiState = ViewingDeviceDataUiState() }
          )
        )

      TappingForFirmwareMetadataUiState ->
        nfcSessionUIStateMachine.model(
          NfcSessionUIStateMachineProps(
            session = { session, commands ->
              firmwareDeviceInfoDao.setDeviceInfo(
                commands.getDeviceInfo(session)
              )
            },
            onSuccess = { uiState = ViewingDeviceDataUiState() },
            onCancel = { uiState = ViewingDeviceDataUiState() },
            isHardwareFake = props.accountData.account.config.isHardwareFake,
            needsAuthentication = false,
            screenPresentationStyle = Modal,
            eventTrackerContext = METADATA
          )
        )

      is UpdatingFirmwareUiState ->
        fwupNfcUiStateMachine.model(
          props =
            FwupNfcUiProps(
              firmwareData = state.pendingFirmwareUpdate,
              isHardwareFake = props.accountData.account.config.isHardwareFake,
              onDone = { uiState = ViewingDeviceDataUiState() }
            )
        )

      is ManagingFingerprintsUiState -> managingFingerprintsUiStateMachine.model(
        props = ManagingFingerprintsProps(
          account = props.accountData.account,
          onBack = { uiState = ViewingDeviceDataUiState() },
          onFwUpRequired = {
            uiState = ViewingDeviceDataUiState(showingPromptForFingerprintFwUpdate = true)
          },
          entryPoint = EntryPoint.DEVICE_SETTINGS
        )
      )

      is ResettingDeviceUiState -> resettingDeviceUiStateMachine.model(
        props = ResettingDeviceProps(
          onBack = { uiState = ViewingDeviceDataUiState() },
          onSuccess = props.onUnwindToMoneyHome,
          spendingWallet = props.accountData.spendingWallet,
          fullAccountConfig = props.accountData.account.config,
          fullAccount = props.accountData.account,
          balance = props.accountData.transactionsData.balance
        )
      )
    }
  }
}

@Composable
private fun ViewingDeviceScreenModel(
  props: DeviceSettingsProps,
  firmwareDeviceAvailability: FirmwareDeviceAvailability,
  goToFwup: (FirmwareData.FirmwareUpdateState.PendingUpdate) -> Unit,
  goToNfcMetadata: () -> Unit,
  goToRecovery: () -> Unit,
  onManageReplacement: () -> Unit,
  onResetDevice: () -> Unit,
  dateTimeFormatter: DateTimeFormatter,
  timeZoneProvider: TimeZoneProvider,
  durationFormatter: DurationFormatter,
  replaceDeviceEnabled: Boolean,
  multipleFingerprintsEnabled: Boolean,
  resetDeviceEnabled: Boolean,
  onManageFingerprints: () -> Unit,
): ScreenModel {
  val noInfo = "-"

  data class ModelData(
    val trackerScreenId: EventTrackerScreenId,
    val emptyState: Boolean = true,
    val currentVersion: String = noInfo,
    val updateVersion: String? = null,
    val modelNumber: String = noInfo,
    val serialNumber: String = noInfo,
    val deviceCharge: String = noInfo,
    val lastSyncDate: String = noInfo,
    val modelName: String = noInfo,
    val replacementPending: String? = null,
  )
  return ScreenModel(
    body = run {
      val modelData = when (firmwareDeviceAvailability) {
        FirmwareDeviceAvailability.None -> ModelData(
          trackerScreenId = SettingsEventTrackerScreenId.SETTINGS_DEVICE_INFO_EMPTY
        )
        is FirmwareDeviceAvailability.Present -> {
          val firmwareDeviceInfo = firmwareDeviceAvailability.firmwareDeviceInfo
          ModelData(
            trackerScreenId = SettingsEventTrackerScreenId.SETTINGS_DEVICE_INFO,
            currentVersion = firmwareDeviceInfo.version,
            updateVersion = props.firmwareData.updateVersion,
            modelNumber = firmwareDeviceInfo.hwRevision,
            serialNumber = firmwareDeviceInfo.serial,
            deviceCharge = "${firmwareDeviceInfo.batteryChargeForUninitializedModelGauge()}%",
            lastSyncDate =
              dateTimeFormatter.fullShortDateWithTime(
                localDateTime =
                  Instant.fromEpochSeconds(firmwareDeviceInfo.timeRetrieved)
                    .toLocalDateTime(timeZoneProvider.current())
              ),
            modelName = "Bitkey",
            emptyState = false,
            replacementPending =
              when (val recoveryData = props.accountData.lostHardwareRecoveryData) {
                is LostHardwareRecoveryInProgressData ->
                  when (val recoveryInProgressData = recoveryData.recoveryInProgressData) {
                    is WaitingForRecoveryDelayPeriodData ->
                      durationFormatter.formatWithWords(
                        nonNegativeDurationBetween(
                          startTime = Clock.System.now(),
                          endTime = recoveryInProgressData.delayPeriodEndTime
                        )
                      )
                    is CompletingRecoveryData -> "Awaiting confirmation"
                    else -> null
                  }
                else -> null
              }
          )
        }
      }
      DeviceSettingsFormBodyModel(
        trackerScreenId = modelData.trackerScreenId,
        emptyState = modelData.emptyState,
        modelName = modelData.modelName,
        currentVersion = modelData.currentVersion,
        updateVersion = modelData.updateVersion,
        modelNumber = modelData.modelNumber,
        serialNumber = modelData.serialNumber,
        deviceCharge = modelData.deviceCharge,
        lastSyncDate = modelData.lastSyncDate,
        replaceDeviceEnabled = replaceDeviceEnabled,
        replacementPending = modelData.replacementPending,
        onUpdateVersion =
          when (val firmwareUpdateState = props.firmwareData.firmwareUpdateState) {
            is FirmwareData.FirmwareUpdateState.UpToDate -> null
            is FirmwareData.FirmwareUpdateState.PendingUpdate -> {
              { goToFwup(firmwareUpdateState) }
            }
          },
        onSyncDeviceInfo = { goToNfcMetadata() },
        onReplaceDevice = goToRecovery,
        onManageReplacement = { onManageReplacement() },
        onResetDevice = { onResetDevice() },
        onBack = props.onBack,
        multipleFingerprintsEnabled = multipleFingerprintsEnabled,
        resetDeviceEnabled = resetDeviceEnabled,
        onManageFingerprints = onManageFingerprints
      )
    },
    presentationStyle = Root
  )
}

private sealed interface FirmwareDeviceAvailability {
  /**
   * When [FirmwareDeviceInfo] is available
   */
  data class Present(val firmwareDeviceInfo: FirmwareDeviceInfo) : FirmwareDeviceAvailability

  /**
   * When FirmwareDeviceInfo is not available. Can happen in cases when the app doesn't have
   * a device paired
   */
  data object None : FirmwareDeviceAvailability
}

sealed interface DeviceSettingsUiState {
  /**
   * Viewing the metadata screen
   */
  data class ViewingDeviceDataUiState(
    val showingPromptForFingerprintFwUpdate: Boolean = false,
  ) : DeviceSettingsUiState

  /**
   * Initiating hardware recovery once replace device is invoked
   */
  data object InitiatingHardwareRecoveryUiState : DeviceSettingsUiState

  /**
   * Checking in on a pending delay and notify period for lost hardware
   */
  data object HardwareRecoveryDelayAndNotifyUiState : DeviceSettingsUiState

  /**
   * Initiating a hardware sync via nfc tap
   */
  data object TappingForFirmwareMetadataUiState : DeviceSettingsUiState

  /**
   * Initiating a FWUP if an update is available
   */
  data class UpdatingFirmwareUiState(
    val pendingFirmwareUpdate: FirmwareData.FirmwareUpdateState.PendingUpdate,
  ) : DeviceSettingsUiState

  /**
   * Managing (i.e. adding/editing/deleting) enrolled fingerprints
   */
  data object ManagingFingerprintsUiState : DeviceSettingsUiState

  /**
   * Resetting the device
   */
  data object ResettingDeviceUiState : DeviceSettingsUiState
}

sealed interface EnrolledFingerprintResult {
  /** A firmware update is required to support multiple fingerprints. */
  data object FwUpRequired : EnrolledFingerprintResult

  data class Success(
    val enrolledFingerprints: EnrolledFingerprints,
  ) : EnrolledFingerprintResult
}
