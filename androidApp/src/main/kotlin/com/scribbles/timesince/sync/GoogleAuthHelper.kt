package com.scribbles.timesince.sync

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.scribbles.timesince.BuildConfig
import kotlinx.coroutines.tasks.await

/**
 * Drives Google account sign-in via Credential Manager and obtains OAuth access tokens
 * for the Drive `appDataFolder` scope via Identity Services [AuthorizationRequest].
 *
 * Flow:
 *  1. UI calls [signIn] with the hosting [Activity] — the Credential Manager bottom sheet appears.
 *  2. UI calls [requestDriveAuthorization]. If the user has not yet consented to the scope
 *     the result is [AuthorizationOutcome.NeedsUserConsent]; the UI must launch the provided
 *     [PendingIntent] via a `StartIntentSenderForResult` launcher and then pass the result
 *     Intent to [completeAuthorization].
 *  3. Once authorized, [getAccessToken] silently returns a fresh token for each Drive call —
 *     Google Play Services handles refresh internally.
 */
class GoogleAuthHelper(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val credentialManager = CredentialManager.create(appContext)
    private val authorizationClient = Identity.getAuthorizationClient(appContext)

    val signedInEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)

    val isSignedIn: Boolean
        get() = signedInEmail != null && prefs.getBoolean(KEY_DRIVE_AUTHORIZED, false)

    suspend fun signIn(activity: Activity): SignInOutcome {
        if (BuildConfig.WEB_CLIENT_ID.isEmpty()) {
            return SignInOutcome.Error("Web client ID not configured.")
        }
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val tokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                prefs.edit().putString(KEY_EMAIL, tokenCred.id).apply()
                SignInOutcome.Success(tokenCred.id)
            } else {
                SignInOutcome.Error("Unexpected credential type.")
            }
        } catch (e: GetCredentialException) {
            SignInOutcome.Error(e.message ?: "Sign-in failed.")
        }
    }

    suspend fun requestDriveAuthorization(): AuthorizationOutcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        return try {
            val result = authorizationClient.authorize(request).await()
            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                    ?: return AuthorizationOutcome.Error("Authorization resolution missing.")
                AuthorizationOutcome.NeedsUserConsent(pendingIntent)
            } else {
                val token = result.accessToken
                    ?: return AuthorizationOutcome.Error("No access token returned.")
                prefs.edit().putBoolean(KEY_DRIVE_AUTHORIZED, true).apply()
                AuthorizationOutcome.Success(token)
            }
        } catch (e: Exception) {
            AuthorizationOutcome.Error(e.message ?: "Authorization failed.")
        }
    }

    fun completeAuthorization(data: Intent?): AuthorizationOutcome {
        return try {
            val result = authorizationClient.getAuthorizationResultFromIntent(data)
            val token = result.accessToken
                ?: return AuthorizationOutcome.Error("Consent denied.")
            prefs.edit().putBoolean(KEY_DRIVE_AUTHORIZED, true).apply()
            AuthorizationOutcome.Success(token)
        } catch (e: Exception) {
            AuthorizationOutcome.Error(e.message ?: "Authorization failed.")
        }
    }

    suspend fun getAccessToken(): String? {
        if (!isSignedIn) return null
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        return try {
            val result = authorizationClient.authorize(request).await()
            if (result.hasResolution()) null else result.accessToken
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Ignore — sign-out should always succeed from the user's perspective.
        }
        prefs.edit().clear().apply()
    }

    sealed interface SignInOutcome {
        data class Success(val email: String) : SignInOutcome
        data class Error(val message: String) : SignInOutcome
    }

    sealed interface AuthorizationOutcome {
        data class Success(val accessToken: String) : AuthorizationOutcome
        data class NeedsUserConsent(val pendingIntent: PendingIntent) : AuthorizationOutcome
        data class Error(val message: String) : AuthorizationOutcome
    }

    companion object {
        private const val PREFS_NAME = "google_auth"
        private const val KEY_EMAIL = "email"
        private const val KEY_DRIVE_AUTHORIZED = "drive_authorized"
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
