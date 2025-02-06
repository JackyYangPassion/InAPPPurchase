package model

enum class SubscriptionType {
    PLUS,
    ULTIMATE
}

data class SubscriptionProduct(
    val id: String,
    val type: SubscriptionType,
    val title: String,
    val description: String,
    val price: String,
)

sealed class PurchaseState {
    data object Loading : PurchaseState()
    data object Success : PurchaseState()
    data class Error(val message: String) : PurchaseState()
    data object Initial : PurchaseState()
} 