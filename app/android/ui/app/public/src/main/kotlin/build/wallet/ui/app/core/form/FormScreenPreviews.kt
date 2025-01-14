package build.wallet.ui.app.core.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import build.wallet.compose.collections.immutableListOf
import build.wallet.money.currency.EUR
import build.wallet.money.currency.GBP
import build.wallet.money.currency.USD
import build.wallet.statemachine.core.Icon
import build.wallet.statemachine.core.form.FormBodyModel
import build.wallet.statemachine.core.form.FormHeaderModel
import build.wallet.statemachine.core.form.FormMainContentModel
import build.wallet.statemachine.core.form.RenderContext
import build.wallet.statemachine.core.list.ListFormBodyModel
import build.wallet.statemachine.money.currency.CurrencyPreferenceFormModel
import build.wallet.statemachine.money.currency.FiatCurrencyListFormModel
import build.wallet.statemachine.transactions.TransactionItemModel
import build.wallet.ui.model.StandardClick
import build.wallet.ui.model.button.ButtonModel
import build.wallet.ui.model.callout.CalloutModel
import build.wallet.ui.model.icon.IconBackgroundType
import build.wallet.ui.model.icon.IconModel
import build.wallet.ui.model.icon.IconSize
import build.wallet.ui.model.icon.IconTint
import build.wallet.ui.model.input.TextFieldModel
import build.wallet.ui.model.list.ListGroupModel
import build.wallet.ui.model.list.ListGroupStyle
import build.wallet.ui.model.list.ListItemPickerMenu
import build.wallet.ui.model.toolbar.ToolbarAccessoryModel
import build.wallet.ui.model.toolbar.ToolbarAccessoryModel.IconAccessory.Companion.BackAccessory
import build.wallet.ui.model.toolbar.ToolbarModel

@Preview
@Composable
internal fun screenWithHeaderAndPrimaryButton() {
  FormScreen(
    model = FormBodyModel(
      id = null,
      onBack = null,
      toolbar = null,
      header = FormHeaderModel(
        icon = Icon.LargeIconCheckFilled,
        headline = "title",
        subline = "message"
      ),
      primaryButton = ButtonModel(
        text = "primaryButtonModel",
        size = ButtonModel.Size.Footer,
        onClick = StandardClick {}
      )
    )
  )
}

@Preview
@Composable
internal fun PreviewMobilePaySheetScreen() {
  FormScreen(
    model = FormBodyModel(
      id = null,
      onBack = { },
      toolbar = null,
      header =
        FormHeaderModel(
          iconModel = IconModel(
            icon = Icon.SmallIconPhone,
            iconSize = IconSize.Large,
            iconTint = IconTint.Primary,
            iconBackgroundType = IconBackgroundType.Circle(
              circleSize = IconSize.Avatar,
              color = IconBackgroundType.Circle.CircleColor.PrimaryBackground20
            ),
            iconTopSpacing = 0
          ),
          headline = "Mobile pay",
          alignment = FormHeaderModel.Alignment.LEADING,
          subline =
            "Leave your device at home, and make small spends with just the key on your phone."
        ),
      primaryButton =
        ButtonModel(
          text = "Enable Mobile Pay",
          size = ButtonModel.Size.Footer,
          onClick = StandardClick {}
        ),
      secondaryButton =
        ButtonModel(
          text = "Set up later",
          size = ButtonModel.Size.Footer,
          treatment = ButtonModel.Treatment.Secondary,
          onClick = StandardClick {}
        ),
      renderContext = RenderContext.Sheet
    )
  )
}

@Preview
@Composable
internal fun PreviewFeeOptionsFormScreen() {
  FormScreen(
    model =
      FormBodyModel(
        onBack = {},
        toolbar =
          ToolbarModel(
            leadingAccessory =
              BackAccessory(onClick = {})
          ),
        header =
          FormHeaderModel(
            icon = Icon.LargeIconSpeedometer,
            headline = "Select a transfer speed",
            alignment = FormHeaderModel.Alignment.CENTER
          ),
        mainContentList =
          immutableListOf(
            FormMainContentModel.FeeOptionList(
              options =
                immutableListOf(
                  FormMainContentModel.FeeOptionList.FeeOption(
                    optionName = "Priority",
                    transactionTime = "~10 mins",
                    transactionFee = "$0.33 (1,086 sats)",
                    selected = false,
                    enabled = true,
                    onClick = {}
                  ),
                  FormMainContentModel.FeeOptionList.FeeOption(
                    optionName = "Standard",
                    transactionTime = "~30 mins",
                    transactionFee = "$0.22 (1,086 sats)",
                    selected = true,
                    enabled = true,
                    onClick = {}
                  ),
                  FormMainContentModel.FeeOptionList.FeeOption(
                    optionName = "Slow",
                    transactionTime = "~60 mins",
                    transactionFee = "$0.11 (1,086 sats)",
                    selected = false,
                    enabled = true,
                    onClick = {}
                  )
                )
            )
          ),
        primaryButton =
          ButtonModel(
            text = "Continue",
            size = ButtonModel.Size.Footer,
            onClick = StandardClick {}
          ),
        id = null
      )
  )
}

@Preview
@Composable
internal fun SetCustomElectrumFormScreenPreview() {
  FormScreen(
    model =
      FormBodyModel(
        id = null,
        onBack = {},
        header =
          FormHeaderModel(
            headline = "Change Electrum Server",
            subline = "Provide details for a custom Electrum Server: "
          ),
        mainContentList =
          immutableListOf(
            FormMainContentModel.TextInput(
              title = "Server:",
              fieldModel =
                TextFieldModel(
                  value = "",
                  placeholderText = "example.com",
                  onValueChange = { _, _ -> },
                  keyboardType = TextFieldModel.KeyboardType.Uri
                )
            ),
            FormMainContentModel.TextInput(
              title = "Port:",
              fieldModel =
                TextFieldModel(
                  value = "",
                  placeholderText = "50002",
                  onValueChange = { _, _ -> },
                  keyboardType = TextFieldModel.KeyboardType.Decimal
                )
            )
          ),
        toolbar =
          ToolbarModel(
            leadingAccessory = ToolbarAccessoryModel.IconAccessory.CloseAccessory {}
          ),
        primaryButton =
          ButtonModel(
            text = "Save",
            isEnabled = true,
            onClick = StandardClick {},
            size = ButtonModel.Size.Footer
          )
      )
  )
}

@Preview
@Composable
internal fun CurrencyListPreview() {
  FormScreen(
    model =
      FiatCurrencyListFormModel(
        onClose = {},
        selectedCurrency = USD,
        currencyList = listOf(USD, GBP, EUR),
        onCurrencySelection = {}
      )
  )
}

@Preview
@Composable
fun CurrencyPreferencePreview() {
  FormScreen(
    model =
      CurrencyPreferenceFormModel(
        onBack = {},
        moneyHomeHero = FormMainContentModel.MoneyHomeHero("$0", "0 sats"),
        fiatCurrencyPreferenceString = "USD",
        onFiatCurrencyPreferenceClick = {},
        bitcoinDisplayPreferenceString = "Sats",
        bitcoinDisplayPreferencePickerModel = CurrencyPreferenceListItemPickerMenu,
        onBitcoinDisplayPreferenceClick = {},
        onEnableHideBalanceChanged = {}
      )
  )
}

val CurrencyPreferenceListItemPickerMenu =
  ListItemPickerMenu(
    isShowing = false,
    selectedOption = "Sats",
    options = listOf("Sats", "Bitcoin"),
    onOptionSelected = {},
    onDismiss = {}
  )

@Preview
@Composable
fun AllTransactionsPreview() {
  FormScreen(
    model =
      ListFormBodyModel(
        toolbarTitle = "Activity",
        listGroups = immutableListOf(
          ListGroupModel(
            header = null,
            style = ListGroupStyle.NONE,
            items =
              immutableListOf(
                TransactionItemModel(
                  truncatedRecipientAddress = "1AH7...CkGJ",
                  date = "Apr 6 at 12:20 pm",
                  amount = "+ $11.36",
                  amountEquivalent = "0.000105 BTC",
                  incoming = true,
                  isPending = true,
                  onClick = {}
                ),
                TransactionItemModel(
                  truncatedRecipientAddress = "2AH7...CkGJ",
                  date = "Apr 6 at 12:20 pm",
                  amount = "$21.36",
                  amountEquivalent = "0.000205 BTC",
                  incoming = false,
                  isPending = true,
                  onClick = {}
                )
              )
          ),
          ListGroupModel(
            header = null,
            style = ListGroupStyle.NONE,
            items =
              immutableListOf(
                TransactionItemModel(
                  truncatedRecipientAddress = "3AH7...CkGJ",
                  date = "Pending",
                  amount = "+ $11.36",
                  amountEquivalent = "0.000105 BTC",
                  incoming = true,
                  isPending = false,
                  onClick = {}
                ),
                TransactionItemModel(
                  truncatedRecipientAddress = "4AH7...CkGJ",
                  date = "Pending",
                  amount = "$21.36",
                  amountEquivalent = "0.000205 BTC",
                  incoming = false,
                  isPending = false,
                  onClick = {}
                )
              )
          )
        ),
        onBack = {},
        id = null
      )
  )
}

@Preview
@Composable
fun CalloutPreview() {
  FormScreen(
    model = FormBodyModel(
      id = null,
      onBack = {},
      toolbar = null,
      header = null,
      mainContentList =
        immutableListOf(
          FormMainContentModel.Callout(
            item = CalloutModel(
              title = "At least one fingerprint is required",
              subtitle = "Add another fingerprint to delete",
              leadingIcon = Icon.SmallIconInformationFilled,
              treatment = CalloutModel.Treatment.Information
            )
          )
        ),
      primaryButton = null
    )
  )
}
