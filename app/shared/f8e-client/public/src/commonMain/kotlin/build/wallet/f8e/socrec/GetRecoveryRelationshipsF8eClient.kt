package build.wallet.f8e.socrec

import build.wallet.bitkey.account.Account
import build.wallet.bitkey.f8e.AccountId
import build.wallet.f8e.F8eEnvironment
import build.wallet.ktor.result.NetworkingError
import com.github.michaelbull.result.Result

interface GetRecoveryRelationshipsF8eClient {
  /**
   * Retrieves recovery relationships that the caller is part of: trusted contact invitations
   * they’ve created (the caller is a Full Account), established trusted contacts
   * (if the caller is a Full Account), and established customers they’re protecting
   * (if the caller is either type of account).
   */
  suspend fun getRelationships(
    accountId: AccountId,
    f8eEnvironment: F8eEnvironment,
  ): Result<SocRecRelationships, NetworkingError>
}

suspend fun GetRecoveryRelationshipsF8eClient.getRelationships(
  account: Account,
): Result<SocRecRelationships, NetworkingError> =
  getRelationships(account.accountId, account.config.f8eEnvironment)
