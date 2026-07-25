package com.example.money.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GoogleDriveAuthorizationManagerTest {

    @Test
    fun authorize_returnsAccessTokenWithoutPersistingIt() = runBlocking {
        val gateway = FakeGoogleAuthorizationGateway(
            authorizationResult = GoogleAuthorizationGatewayResult(
                accessToken = "temporary-token",
            ),
        )
        val manager = GoogleDriveAuthorizationManager(gateway)

        val result = manager.authorize()

        assertEquals(
            GoogleDriveAuthorizationResult.Authorized("temporary-token"),
            result,
        )
        assertEquals(1, gateway.authorizeCalls)
        assertEquals(
            listOf(GoogleDriveAuthorizationManager.DRIVE_APP_DATA_SCOPE),
            gateway.authorizationRequest!!.requestedScopes.map { it.scopeUri },
        )
    }

    @Test
    fun authorize_returnsResolutionPendingIntent() = runBlocking {
        val pendingIntent = pendingIntent()
        val manager = GoogleDriveAuthorizationManager(
            FakeGoogleAuthorizationGateway(
                authorizationResult = GoogleAuthorizationGatewayResult(
                    hasResolution = true,
                    pendingIntent = pendingIntent,
                ),
            ),
        )

        val result = manager.authorize()

        assertTrue(result is GoogleDriveAuthorizationResult.ResolutionRequired)
        assertSame(
            pendingIntent,
            (result as GoogleDriveAuthorizationResult.ResolutionRequired).pendingIntent,
        )
    }

    @Test
    fun completeAuthorization_parsesForegroundIntentSynchronously() {
        val foregroundIntent = Intent("authorization-result")
        val gateway = FakeGoogleAuthorizationGateway(
            foregroundResult = GoogleAuthorizationGatewayResult(
                accessToken = "foreground-token",
            ),
        )
        val manager = GoogleDriveAuthorizationManager(gateway)

        val result = manager.completeAuthorization(foregroundIntent)

        assertEquals(
            GoogleDriveAuthorizationResult.Authorized("foreground-token"),
            result,
        )
        assertSame(foregroundIntent, gateway.completedIntent)
    }

    @Test
    fun failuresAreReturnedAsTypedAuthorizationState() = runBlocking {
        val expected = IllegalStateException("authorization unavailable")
        val manager = GoogleDriveAuthorizationManager(
            FakeGoogleAuthorizationGateway(authorizeFailure = expected),
        )

        val result = manager.authorize()

        assertTrue(result is GoogleDriveAuthorizationResult.Failure)
        assertSame(expected, (result as GoogleDriveAuthorizationResult.Failure).cause)
    }

    @Test
    fun revokeAccess_delegatesAndReturnsRevoked() = runBlocking {
        val gateway = FakeGoogleAuthorizationGateway()
        val manager = GoogleDriveAuthorizationManager(gateway)

        val result = manager.revokeAccess()

        assertEquals(GoogleDriveRevocationResult.Revoked, result)
        assertEquals(1, gateway.revokeCalls)
    }

    private fun pendingIntent(): PendingIntent {
        val context: Context = RuntimeEnvironment.getApplication()
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, javaClass),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private class FakeGoogleAuthorizationGateway(
        private val authorizationResult: GoogleAuthorizationGatewayResult =
            GoogleAuthorizationGatewayResult(accessToken = "default-token"),
        private val foregroundResult: GoogleAuthorizationGatewayResult = authorizationResult,
        private val authorizeFailure: Exception? = null,
    ) : GoogleAuthorizationGateway {
        var authorizeCalls = 0
        var revokeCalls = 0
        var completedIntent: Intent? = null
        var authorizationRequest: AuthorizationRequest? = null

        override suspend fun authorize(
            request: AuthorizationRequest,
        ): GoogleAuthorizationGatewayResult {
            authorizeCalls += 1
            authorizationRequest = request
            authorizeFailure?.let { throw it }
            return authorizationResult
        }

        override fun getAuthorizationResultFromIntent(
            intent: Intent?,
        ): GoogleAuthorizationGatewayResult {
            completedIntent = intent
            return foregroundResult
        }

        override suspend fun revokeAccess(request: RevokeAccessRequest) {
            revokeCalls += 1
        }
    }
}
