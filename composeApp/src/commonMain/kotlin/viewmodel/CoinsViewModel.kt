package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.CoinProduct
import model.PurchaseState
import service.CoinService

data class CoinsUiState(
    val isLoading: Boolean = true,
    val products: List<CoinProduct> = emptyList(),
    val purchaseState: PurchaseState = PurchaseState.Initial
)

class CoinsViewModel(
    private val coinService: CoinService
) : ViewModel() {
    private val _uiState = MutableStateFlow(CoinsUiState())
    val uiState: StateFlow<CoinsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val products = coinService.getProducts()
            _uiState.update { it.copy(
                isLoading = false,
                products = products
            ) }
        }
    }

    fun purchase(productId: String) {
        viewModelScope.launch {
            coinService.purchase(productId).collect { state ->
                _uiState.update { it.copy(purchaseState = state) }
            }
        }
    }

    fun resetPurchaseState() {
        _uiState.update { it.copy(purchaseState = PurchaseState.Initial) }
    }
} 