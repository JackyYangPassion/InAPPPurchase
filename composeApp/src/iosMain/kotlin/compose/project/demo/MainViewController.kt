package compose.project.demo

import ViewModelFactory
import androidx.compose.ui.window.ComposeUIViewController

//fun MainViewController() = ComposeUIViewController { App() }

fun MainViewController() = ComposeUIViewController {
    val viewModelFactory = ViewModelFactory()
    App(viewModelFactory)
}