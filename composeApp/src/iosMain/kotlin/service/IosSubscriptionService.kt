package service

import compose.project.demo.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import model.PurchaseState
import model.SubscriptionProduct
import model.SubscriptionType
import platform.Foundation.NSError
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKRequest

import platform.darwin.NSObject
import kotlin.coroutines.resume



class IosSubscriptionService : SubscriptionService {
    private val products = mutableMapOf<String, SKProduct>()

override suspend fun getProducts(): List<SubscriptionProduct> = try {
    // 使用本地测试的产品标识符
    val productIdentifiers = setOf(
        "Plus",
        "Ultimate"
    )
    val request = SKProductsRequest(productIdentifiers)
    
    suspendCancellableCoroutine { continuation ->
        val result = mutableListOf<SubscriptionProduct>()
        request.setDelegate(object : NSObject(), SKProductsRequestDelegateProtocol {
            override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
                log.v { "收到产品响应"}
                log.v {"无效的产品ID: ${didReceiveResponse.invalidProductIdentifiers}"}
                
                didReceiveResponse.products.forEach { skProduct ->
                    (skProduct as? SKProduct)?.let { product ->
                        log.v { "找到产品: ${product.productIdentifier}"}
                        products[product.productIdentifier] = product
                        result.add(
                            SubscriptionProduct(
                                id = product.productIdentifier,
                                type = if (product.productIdentifier == "Plus") SubscriptionType.PLUS else SubscriptionType.ULTIMATE,
                                title = product.localizedTitle,
                                description = product.localizedDescription,
                                price = product.price.toString()
                            )
                        )
                    }
                }
                continuation.resume(result)
            }
            
            override fun requestDidFinish(request: SKRequest) {
                log.v { "产品请求完成"}
            }
            
            override fun request(request: SKRequest, didFailWithError: NSError) {
                log.e { "产品请求失败: ${didFailWithError.localizedDescription}"}
                continuation.resume(emptyList())
            }
        })
        request.start()
    }
} catch (e: Exception) {
    log.e { "获取产品时发生异常: ${e.message}"}
    emptyList()
}

    override suspend fun purchase(productId: String): Flow<PurchaseState> = flow {
        emit(PurchaseState.Loading)
        try {
            products[productId]?.let { product ->
                val payment = SKPayment.paymentWithProduct(product)
                SKPaymentQueue.defaultQueue().addPayment(payment)
                emit(PurchaseState.Success)
            } ?: emit(PurchaseState.Error("Product not found"))
        } catch (e: Exception) {
            emit(PurchaseState.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun restorePurchases() {
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }

    override suspend fun isPurchased(productId: String): Boolean {
        // 实际项目中需要实现本地存储或远程验证
        return false
    }
} 