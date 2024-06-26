import androidx.compose.ui.window.ComposeUIViewController
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import org.publicvalue.multiplatform.oidc.appsupport.IosAuthFlowFactory
import org.publicvalue.multiplatform.oidc.sample.screens.HomeScreen
import org.publicvalue.multiplatform.oidc.settings.IosSettingsStore

fun MainViewController() = ComposeUIViewController {

    val factory = IosAuthFlowFactory()

    val backstack = rememberSaveableBackStack {
        push(HomeScreen)
    }
    val navigator = rememberCircuitNavigator(backstack) {

    }

    val settingsStore = IosSettingsStore()

    App(
        backstack = backstack,
        navigator = navigator,
        settingsStore = settingsStore,
        authFlowFactory = factory
    )
}