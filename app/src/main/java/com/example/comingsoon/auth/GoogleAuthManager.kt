package com.example.comingsoon.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

data class GoogleUser(
    val id: String,
    val displayName: String?,
    val email: String?,
    val profilePictureUri: String?,
    val idToken: String
)

sealed interface GoogleSignInResult {
    data class Success(val user: GoogleUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

class GoogleAuthManager(context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(
        activityContext: Context,
        serverClientId: String
    ): GoogleSignInResult {
        if (serverClientId.isBlank() || serverClientId.startsWith("YOUR_")) {
            return GoogleSignInResult.Error(
                "Die Google Web-Client-ID wurde noch nicht konfiguriert."
            )
        }

        val googleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = serverClientId
        ).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            val credential = result.credential

            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(
                    GoogleUser(
                        id = googleCredential.id,
                        displayName = googleCredential.displayName,
                        email = googleCredential.id,
                        profilePictureUri = googleCredential.profilePictureUri?.toString(),
                        idToken = googleCredential.idToken
                    )
                )
            } else {
                GoogleSignInResult.Error("Google hat einen unbekannten Anmeldetyp geliefert.")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: GoogleIdTokenParsingException) {
            GoogleSignInResult.Error("Die Google-Anmeldedaten konnten nicht gelesen werden.")
        } catch (exception: GetCredentialException) {
            GoogleSignInResult.Error(
                exception.localizedMessage ?: "Die Google-Anmeldung ist fehlgeschlagen."
            )
        }
    }
}
