package cz.ackee.ackroutine

import android.content.Context
import android.content.SharedPreferences
import cz.ackee.ackroutine.core.*
import kotlinx.coroutines.*

/**
 * TODO: to be continued...
 */
class CoroutineOAuthManager internal constructor(
    private val oAuthStore: OAuthStore,
    private val refreshTokenAction: (String) -> Deferred<OAuthCredentials>,
    private val onRefreshTokenFailed: (Throwable) -> Unit = {},
    private val errorChecker: ErrorChecker = DefaultErrorChecker()
) {

    constructor(sp: SharedPreferences, refreshTokenAction: (String) -> Deferred<OAuthCredentials>,
        onRefreshTokenFailed: (Throwable) -> Unit = {}, errorChecker: ErrorChecker = DefaultErrorChecker()) :
        this(OAuthStore(sp), refreshTokenAction, onRefreshTokenFailed, errorChecker)

    constructor(context: Context, refreshTokenAction: (String) -> Deferred<OAuthCredentials>,
        onRefreshTokenFailed: (Throwable) -> Unit = {}, errorChecker: ErrorChecker = DefaultErrorChecker()) :
        this(OAuthStore(context), refreshTokenAction, onRefreshTokenFailed, errorChecker)

    val accessToken get() = oAuthStore.accessToken

    val refreshToken get() = oAuthStore.refreshToken

    fun saveCredentials(credentials: OAuthCredentials) {
        oAuthStore.saveCredentials(credentials)
    }

    fun clearCredentials() {
        oAuthStore.clearCredentials()
    }

    fun provideAuthInterceptor() = OAuthInterceptor(oAuthStore)

    fun <T> wrapDeferred(originalCall: Deferred<T>): Deferred<T> {
        return CoroutineScope(Dispatchers.Unconfined).async(start = CoroutineStart.LAZY) {

            if (oAuthStore.tokenExpired()) {
                refreshAccessToken()
                //originalCall.await()
            } else {

            }

            originalCall.await()
        }
    }

    private suspend fun refreshAccessToken() {
        try {
            val credentials = refreshTokenAction(oAuthStore.refreshToken ?: "").await()
            saveCredentials(credentials)

        } catch (e: Exception) {
            if (errorChecker.invalidAccessToken(e)) {
                clearCredentials()
                onRefreshTokenFailed(e)
            }
        }
    }
}

fun <T> Deferred<T>.wrapWithAcessTokenCheck(oAuthManager: CoroutineOAuthManager) = oAuthManager.wrapDeferred(this)