import viewmodel.CoinsViewModel
import viewmodel.SubscriptionViewModel

expect class ViewModelFactory {
    fun createSubscriptionViewModel(): SubscriptionViewModel
    fun createCoinsViewModel(): CoinsViewModel
} 