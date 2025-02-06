package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import compose.project.demo.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.PurchaseState
import model.SubscriptionProduct
import service.SubscriptionService

data class SubscriptionUiState(
    val products: List<SubscriptionProduct> = emptyList(),
    val purchaseState: PurchaseState = PurchaseState.Initial,
    val isLoading: Boolean = false
)

class SubscriptionViewModel(
    private val subscriptionService: SubscriptionService
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val products = subscriptionService.getProducts()
                log.v { "Products loaded: ${products.size}" }
                _uiState.update { it.copy(
                    products = products,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                log.e { "Failed to load products: $e" }
                //ERROR Exception
//                _uiState.update { it.copy(
//                    isLoading = false,
//                    error = e.message ?: "获取产品信息失败"
//                ) }
                //ERROR Exception
            }
        }
    }

    fun purchase(productId: String) {
        viewModelScope.launch {
            subscriptionService.purchase(productId).collect { state ->
                _uiState.value = _uiState.value.copy(purchaseState = state)
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            subscriptionService.restorePurchases()
        }
    }
} 