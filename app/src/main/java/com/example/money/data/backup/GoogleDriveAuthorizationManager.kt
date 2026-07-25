package com.example.money.data.backup

import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface GoogleDriveAuthorizationResult {
    data class Authorized(val accessToken: String) : GoogleDriveAuthorizationResult

    data class ResolutionRequired(
        val pendingIntent: PendingIntent,
    ) : GoogleDriveAuthorizationResult

    data class Failure(val cause: Throwable) : GoogleDriveAuthorizationResult
}

sealed interface GoogleDriveRevocationResult {
    data object Revoked : GoogleDriveRevocationResult

    data class Failure(val cause: Throwable) : GoogleDriveRevocationResult
}

class GoogleDriveAuthorizationManager internal constructor(
    private val gateway: GoogleAuthorizationGateway,
) {
    constructor(client: AuthorizationClient) : this(
        PlayServicesGoogleAuthorizationGateway(client),
    )

    suspend fun authorize(): GoogleDriveAuthorizationResult =
        try {
            mapResult(gateway.authorize(AUTHORIZATION_REQUEST))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            GoogleDriveAuthorizationResult.Failure(error)
        }

    fun completeAuthorization(intent: Intent?): GoogleDriveAuthorizationResult =
        try {
            mapResult(gateway.getAuthorizationResultFromIntent(intent))
        } catch (error: Exception) {
            GoogleDriveAuthorizationResult.Failure(error)
        }

    suspend fun revokeAccess(): GoogleDriveRevocationResult =
        try {
            gateway.revokeAccess(REVOKE_ACCESS_REQUEST)
            GoogleDriveRevocationResult.Revoked
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            GoogleDriveRevocationResult.Failure(error)
        }

    private fun mapResult(
        result: GoogleAuthorizationGatewayResult,
    ): GoogleDriveAuthorizationResult =
        when {
            result.hasResolution -> {
                val pendingIntent = result.pendingIntent
                    ?: return GoogleDriveAuthorizationResult.Failure(
                        IllegalStateException(
                            "Google authorization reported a resolution without a PendingIntent",
                        ),
                    )
                GoogleDriveAuthorizationResult.ResolutionRequired(pendingIntent)
            }
            !result.accessToken.isNullOrBlank() ->
                GoogleDriveAuthorizationResult.Authorized(result.accessToken)
            else -> GoogleDriveAuthorizationResult.Failure(
                IllegalStateException(
                    "Google authorization returned neither an access token nor a resolution",
                ),
            )
        }

    companion object {
        const val DRIVE_APP_DATA_SCOPE =
            "https://www.googleapis.com/auth/drive.appdata"

        private val DRIVE_SCOPES = listOf(Scope(DRIVE_APP_DATA_SCOPE))

        private val AUTHORIZATION_REQUEST = AuthorizationRequest.builder()
            .setRequestedScopes(DRIVE_SCOPES)
            .build()

        private val REVOKE_ACCESS_REQUEST = RevokeAccessRequest.builder()
            .setScopes(DRIVE_SCOPES)
            .build()
    }
}

internal data class GoogleAuthorizationGatewayResult(
    val hasResolution: Boolean = false,
    val pendingIntent: PendingIntent? = null,
    val accessToken: String? = null,
)

internal interface GoogleAuthorizationGateway {
    suspend fun authorize(
        request: AuthorizationRequest,
    ): GoogleAuthorizationGatewayResult

    fun getAuthorizationResultFromIntent(intent: Intent?): GoogleAuthorizationGatewayResult

    suspend fun revokeAccess(request: RevokeAccessRequest)
}

private class PlayServicesGoogleAuthorizationGateway(
    private val client: AuthorizationClient,
) : GoogleAuthorizationGateway {
    override suspend fun authorize(
        request: AuthorizationRequest,
    ): GoogleAuthorizationGatewayResult =
        client.authorize(request).await().toGatewayResult()

    override fun getAuthorizationResultFromIntent(
        intent: Intent?,
    ): GoogleAuthorizationGatewayResult =
        client.getAuthorizationResultFromIntent(intent).toGatewayResult()

    override suspend fun revokeAccess(request: RevokeAccessRequest) {
        client.revokeAccess(request).awaitCompletion()
    }

    private fun AuthorizationResult.toGatewayResult() =
        GoogleAuthorizationGatewayResult(
            hasResolution = hasResolution(),
            pendingIntent = if (hasResolution()) pendingIntent else null,
            accessToken = accessToken,
        )
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Google authorization task failed"),
                )
            }
        }
    }

private suspend fun Task<Void>.awaitCompletion() {
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Google revocation task failed"),
                )
            }
        }
    }
}
