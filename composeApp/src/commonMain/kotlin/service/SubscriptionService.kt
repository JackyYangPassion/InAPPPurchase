package service

import model.SubscriptionProduct
import kotlinx.coroutines.flow.Flow
import model.PurchaseState

interface SubscriptionService {
    suspend fun getProducts(): List<SubscriptionProduct>
    suspend fun purchase(productId: String): Flow<PurchaseState>
    suspend fun restorePurchases()
    suspend fun isPurchased(productId: String): Boolean
} 