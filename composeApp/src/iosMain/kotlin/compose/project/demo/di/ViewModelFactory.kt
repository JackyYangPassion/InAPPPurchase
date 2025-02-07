import service.IosCoinService
import service.IosSubscriptionService
import viewmodel.CoinsViewModel
import viewmodel.SubscriptionViewModel

actual class ViewModelFactory {
    actual fun createSubscriptionViewModel(): SubscriptionViewModel {
        return SubscriptionViewModel(IosSubscriptionService())
    }
    actual fun createCoinsViewModel(): CoinsViewModel {
        return CoinsViewModel(IosCoinService())
    }
} 