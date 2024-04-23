package org.publicvalue.multiplatform.oidc

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import org.publicvalue.multiplatform.oidc.DefaultOpenIdConnectClient.Companion.DefaultHttpClient
import org.publicvalue.multiplatform.oidc.types.AuthRequest
import org.publicvalue.multiplatform.oidc.types.remote.AuthResult
import org.publicvalue.multiplatform.oidc.types.remote.OpenIdConnectConfiguration
import kotlin.coroutines.cancellation.CancellationException
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Builds an [OpenIdConnectClientConfig] using the [block] parameter and returns a
 * [DefaultOpenIdConnectClient].
 *
 * This uses the default ktor HTTP client. You may provide your own client using the
 * [DefaultOpenIdConnectClient] constructor.
 *
 * @param discoveryUri if set, endpoints in the configuration are optional.
 * Setting an endpoint manually will override a discovered endpoint.
 * @param block configuration closure. See [OpenIdConnectClientConfig]
 */
fun OpenIdConnectClient(
    discoveryUri: String? = null,
    block: OpenIdConnectClientConfig.() -> Unit
): OpenIdConnectClient {
    val config = OpenIdConnectClientConfig(discoveryUri).apply(block)
    return DefaultOpenIdConnectClient(config = config)
}

/**
 * Create a [DefaultOpenIdConnectClient].
 */
@Deprecated(
    message = "Use DefaultOpenIdConnectClient constructor instead",
    replaceWith = ReplaceWith("DefaultOpenIdConnectClient(httpClient, config)")
)
fun OpenIdConnectClient(
    httpClient: HttpClient = DefaultHttpClient,
    config: OpenIdConnectClientConfig,
) : OpenIdConnectClient {
    return DefaultOpenIdConnectClient(httpClient = httpClient, config = config)
}

@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "OpenIdConnectClientProtocol", name = "OpenIdConnectClientProtocol", exact = true)
interface OpenIdConnectClient {
    val config: OpenIdConnectClientConfig
    val discoverDocument: OpenIdConnectConfiguration?

    /**
     * Creates an Authorization Code Request which can then be executed by the
     * [CodeAuthFlow][org.publicvalue.multiplatform.oidc.flows.CodeAuthFlow].
     */
    @Throws(OpenIdConnectException::class)
    fun createAuthorizationCodeRequest(configure: (URLBuilder.() -> Unit)? = null): AuthRequest.Code

    /**
     * Discover OpenID Connect Configuration using the discovery endpoint.
     * Updates the configuration, but will keep any existing configuration.
     *
     * See: [OpenID Connect Discovery](https://openid.net/specs/openid-connect-discovery-1_0.html)
     *
     * @param configure configuration closure to configure the http request builder with
     */
    @Throws(OpenIdConnectException::class, CancellationException::class)
    suspend fun discover(configure: (HttpRequestBuilder.() -> Unit)? = null)

    /**
     * RP-initiated logout.
     * Just performs the GET request for logout, we skip the redirect part for convenience.
     *
     * See: [OpenID Spec](https://openid.net/specs/openid-connect-rpinitiated-1_0.html)
     *
     * @param configure configuration closure to configure the http request builder with (will _not_
     * be used for discovery if necessary)
     */
    @Throws(OpenIdConnectException::class, CancellationException::class)
    suspend fun endSession(
        idToken: String,
        configure: (HttpRequestBuilder.() -> Unit)? = null
    ): HttpStatusCode

    /**
     * Create and send an Access Token Request following
     * [RFC6749: OAuth](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3) and
     * [RFC7636: PKCE](https://datatracker.ietf.org/doc/html/rfc7636#section-4.5)
     *
     * @param authCodeRequest the original request for auth code
     * @param code the authcode received via redirect
     * @param configure configuration closure for the HTTP request (will _not_  be used for
     * discovery if necessary)
     *
     * @return [AccessTokenResponse]
     */
    @Throws(OpenIdConnectException::class, CancellationException::class)
    suspend fun exchangeToken(
        authCodeRequest: AuthRequest,
        code: String,
        configure: (HttpRequestBuilder.() -> Unit)? = null
    ): AuthResult.AccessToken

    /**
     * Create and send a Refresh Token Request.
     * [RFC6749](https://datatracker.ietf.org/doc/html/rfc6749#section-6)
     *
     * @param refreshToken the refresh token
     * @param configure configuration closure for the HTTP request (will _not_  be used for
     * discovery if necessary)
     *
     * @return [AccessTokenResponse]
     */
    @Throws(OpenIdConnectException::class, CancellationException::class)
    @Suppress("Unused")
    suspend fun refreshToken(
        refreshToken: String,
        configure: (HttpRequestBuilder.() -> Unit)? = null
    ): AuthResult.AccessToken

    /**
     * Create an Access Token Request.
     * You should use [OpenIdConnectClient.exchangeToken] for creating and executing a request instead.
     *
     * @param authCodeRequest the original request for auth code
     * @param code the authcode received via redirect
     * @param configure configuration closure for the HTTP request
     *
     * @return [TokenRequest]
     */
    @Throws(OpenIdConnectException::class, CancellationException::class)
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun createAccessTokenRequest(
        authCodeRequest: AuthRequest.Code,
        code: String,
        configure: (HttpRequestBuilder.() -> Unit)? = null
    ): AuthRequest.Token

    /**
     * Create a Refresh Token Request.
     * You should use [OpenIdConnectClient.refreshToken] for creating and executing a request instead.
     *
     * @param refreshToken the refresh token
     * @param configure configuration closure for the HTTP request (will _not_  be used for
     * discovery if necessary)
     *
     * @return [TokenRequest]
     */
    @Throws(OpenIdConnectException::class, CancellationException::class)
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun createRefreshTokenRequest(
        refreshToken: String,
        configure: (HttpRequestBuilder.() -> Unit)? = null
    ): AuthRequest.Token

    @Throws(OpenIdConnectException::class, CancellationException::class)
    suspend fun createImplicitAccessTokenRequest(configure: (HttpRequestBuilder.() -> Unit)? = null): AuthRequest.Token
}