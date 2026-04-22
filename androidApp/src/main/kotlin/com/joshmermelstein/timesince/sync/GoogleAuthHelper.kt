package com.joshmermelstein.timesince.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages Google Sign-In for Drive appDataFolder scope.
 *
 * Call [getSignInIntent] to launch the sign-in flow, then [handleSignInResult]
 * with the returned Intent. Once signed in, [getAccessToken] returns a fresh
 * token for Drive REST API calls.
 */
@Suppress("DEPRECATION")
class GoogleAuthHelper(private val context: Context) {

    private val client: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun getSignInIntent(): Intent = client.signInIntent

    suspend fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            GoogleSignIn.getSignedInAccountFromIntent(data).await()
        } catch (_: Exception) {
            null
        }
    }

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    val isSignedIn: Boolean
        get() {
            val account = getSignedInAccount() ?: return false
            return account.grantedScopes.contains(Scope(DRIVE_APPDATA_SCOPE))
        }

    val accountEmail: String?
        get() = getSignedInAccount()?.email

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = getSignedInAccount() ?: return@withContext null
        try {
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                context,
                account.account!!,
                "oauth2:$DRIVE_APPDATA_SCOPE",
            )
            token
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signOut() {
        try {
            client.signOut().await()
        } catch (_: Exception) {
            // Ignore sign-out failures.
        }
    }

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
