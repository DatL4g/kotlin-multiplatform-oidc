package org.publicvalue.multiplatform.oauth.clientdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.publicvalue.multiplatform.oauth.compose.circuit.ErrorPresenter
import org.publicvalue.multiplatform.oauth.compose.circuit.catchErrorMessage
import org.publicvalue.multiplatform.oauth.data.daos.ClientDao
import org.publicvalue.multiplatform.oauth.data.db.Client
import org.publicvalue.multiplatform.oauth.data.types.CodeChallengeMethod
import org.publicvalue.multiplatform.oauth.domain.Authorize
import org.publicvalue.multiplatform.oauth.domain.AuthorizeResult
import org.publicvalue.multiplatform.oauth.domain.ExchangeToken
import org.publicvalue.multiplatform.oauth.domain.ExchangeTokenResult
import org.publicvalue.multiplatform.oauth.logging.Logger
import org.publicvalue.multiplatform.oauth.screens.ClientDetailScreen
import org.publicvalue.multiplatform.oidc.types.AuthRequest
import org.publicvalue.multiplatform.oidc.types.remote.AuthResult
import org.publicvalue.multiplatform.oidc.types.remote.ErrorResponse

@Inject
class ClientDetailUiPresenterFactory(
    private val presenterFactory: (Navigator, ClientDetailScreen) -> ClientDetailPresenter,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            is ClientDetailScreen -> presenterFactory(navigator, screen)
            else -> null
        }
    }
}

@Inject
class ClientDetailPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ClientDetailScreen,
    private val logger: Logger,
    private val clientDao: ClientDao,
    private val authorize: Authorize,
    private val exchangeToken: ExchangeToken
) : ErrorPresenter<ClientDetailUiState> {

    override var errorMessage = MutableStateFlow<String?>(null)

    @Composable
    override fun present(): ClientDetailUiState {
        val scope = rememberCoroutineScope()

        val client by clientDao.getClient(screen.clientId).collectAsState(null)
        val errorMessage by this.errorMessage.collectAsRetainedState()

        var authcodeRequestUrl: String? by rememberRetained { mutableStateOf(null) }
        var authcodeResponseQueryString: String? by rememberRetained { mutableStateOf(null) }
        var authcode: String? by rememberRetained { mutableStateOf(null) }

        var tokenRequestParameters: Parameters? by rememberRetained { mutableStateOf(null) }
        var tokenResponse: AuthResult.AccessToken? by rememberRetained { mutableStateOf(null) }
        var errorTokenResponse: ErrorResponse? by rememberRetained { mutableStateOf(null) }
        var tokenResponseStatusCode: HttpStatusCode? by rememberRetained { mutableStateOf(null) }

        fun eventSink(event: ClientDetailUiEvent) {
            when (event) {
                ClientDetailUiEvent.NavigateUp -> {
                    navigator.pop()
                }

                is ClientDetailUiEvent.ChangeClientProperty<*> -> {
                    client?.let { client ->
                        scope.launch {
                            when (event.prop) {
                                Client::name -> clientDao.update(client.copy(name = event.value as String))
                                Client::client_id -> clientDao.update(client.copy(client_id = event.value as String))
                                Client::client_secret -> clientDao.update(client.copy(client_secret = event.value as String))
                                Client::scope -> clientDao.update(client.copy(scope = event.value as String))
                                Client::code_challenge_method -> clientDao.update(client.copy(code_challenge_method = event.value as CodeChallengeMethod))
                            }
                        }
                    }
                }
                ClientDetailUiEvent.Login -> {
                    client?.let { client ->
                        scope.launch {
                            catchErrorMessage {
                                var authCodeRequest: AuthRequest.Code? = null
                                authorize(client)
                                    .collect { it ->
                                    when (it) {
                                        is AuthorizeResult.Request -> {
                                            authcodeRequestUrl = it.authCodeRequestUrl
                                            authCodeRequest = it.authCodeRequest
                                        }
                                        is AuthorizeResult.Response -> {
                                            authcode = it.authCode
                                            authcodeResponseQueryString = it.authCodeResponseQueryString
                                        }
                                    }
                                }

                                if (authCodeRequest != null && authcode != null) {
                                    exchangeToken(client, authCodeRequest!!, authcode!!).collect {
                                        when (it) {
                                            is ExchangeTokenResult.Request -> {
                                                tokenRequestParameters = it.parameters
                                            }
                                            is ExchangeTokenResult.Response -> {
                                                tokenResponse = it.accessTokenResponse
                                                tokenResponseStatusCode = it.httpStatusCode
                                                errorTokenResponse = it.errorResponse
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ClientDetailUiEvent.ResetErrorMessage -> resetErrorMessage()
            }
        }

        return ClientDetailUiState(
            errorMessage = errorMessage,
            isLoading = false,
            eventSink = ::eventSink,
            client = client,
            authcodeRequestUrl = authcodeRequestUrl,
            authcodeResponseQueryString = authcodeResponseQueryString,
            authcode = authcode,
            tokenRequestParameters = tokenRequestParameters,
            tokenResponse = tokenResponse,
            errorTokenResponse = errorTokenResponse,
            tokenResponseStatusCode = tokenResponseStatusCode
        )
    }
}
