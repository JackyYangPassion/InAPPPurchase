package compose.project.demo

import ViewModelFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.SubscriptionScreen
import inapppurchase.composeapp.generated.resources.Res
import inapppurchase.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App(viewModelFactory: ViewModelFactory) {
    MaterialTheme {
        var showSubscription by remember { mutableStateOf(false) }
        
        if (showSubscription) {
            SubscriptionScreen(
                viewModel = viewModelFactory.createSubscriptionViewModel(),
                onNavigateBack = { showSubscription = false }
            )
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { showSubscription = true }) {
                    Text("订阅")
                }
                Image(painterResource(Res.drawable.compose_multiplatform), null)
            }
        }
    }
}