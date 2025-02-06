import service.IosSubscriptionService
import viewmodel.SubscriptionViewModel

actual class ViewModelFactory {
    actual fun createSubscriptionViewModel(): SubscriptionViewModel {
        return SubscriptionViewModel(IosSubscriptionService())
    }
} 