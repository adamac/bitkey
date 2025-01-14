package build.wallet.statemachine.recovery.socrec

import androidx.compose.runtime.Composable
import build.wallet.bitkey.socrec.TrustedContactAuthenticationState
import build.wallet.bitkey.socrec.TrustedContactAuthenticationState.FAILED
import build.wallet.bitkey.socrec.TrustedContactAuthenticationState.PAKE_DATA_UNAVAILABLE
import build.wallet.statemachine.moneyhome.card.CardModel
import build.wallet.ui.model.button.ButtonModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock

class RecoveryContactCardsUiStateMachineImpl(
  private val clock: Clock,
) : RecoveryContactCardsUiStateMachine {
  @Composable
  override fun model(props: RecoveryContactCardsUiProps): ImmutableList<CardModel> {
    return listOf(
      props.relationships.invitations
        .map {
          RecoveryContactCardModel(
            contact = it,
            buttonText = if (it.isExpired(clock)) {
              "Expired"
            } else {
              "Pending"
            },
            onClick = { props.onClick(it) }
          )
        },
      props.relationships.unendorsedTrustedContacts
        .filter { it.authenticationState in setOf(FAILED, PAKE_DATA_UNAVAILABLE) }
        .map {
          RecoveryContactCardModel(
            contact = it,
            buttonText = "Failed",
            buttonTreatment = ButtonModel.Treatment.Warning,
            onClick = { props.onClick(it) }
          )
        },
      props.relationships.unendorsedTrustedContacts
        .filter { it.authenticationState == TrustedContactAuthenticationState.TAMPERED }
        .map {
          RecoveryContactCardModel(
            contact = it,
            buttonText = "Invalid",
            buttonTreatment = ButtonModel.Treatment.Warning,
            onClick = { props.onClick(it) }
          )
        }
    ).flatten().toImmutableList()
  }
}
