package service

import kotlinx.coroutines.flow.Flow
import model.CoinProduct
import model.PurchaseState

interface CoinService {
    suspend fun getProducts(): List<CoinProduct>
    suspend fun purchase(productId: String): Flow<PurchaseState>
} 