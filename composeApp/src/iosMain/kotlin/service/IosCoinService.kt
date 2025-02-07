package service

import compose.project.demo.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import model.CoinProduct
import model.PurchaseState
import platform.Foundation.NSError
import platform.StoreKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
class IosCoinService : CoinService {
    private val products = mutableMapOf<String, SKProduct>()
    private val coinProductIds = setOf(
        "coins_3",
        "coins_10",
        "coins_200",
        "coins_500"
    )

    init {
        if (Platform.isDebugBinary) {
            log.v { "启用 StoreKit 本地测试" }
            SKPaymentQueue.defaultQueue().addTransactionObserver(
                object : NSObject(), SKPaymentTransactionObserverProtocol {
                    override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
                        updatedTransactions.forEach { transaction ->
                            (transaction as? SKPaymentTransaction)?.let { handleTransaction(it) }
                        }
                    }
                }
            )
        }
    }

    private fun handleTransaction(transaction: SKPaymentTransaction) {
        when (transaction.transactionState) {
            SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                log.v { "购买成功" }
                SKPaymentQueue.defaultQueue().finishTransaction(transaction)
            }
            SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                log.e { "购买失败: ${transaction.error?.localizedDescription}" }
                SKPaymentQueue.defaultQueue().finishTransaction(transaction)
            }
            else -> {}
        }
    }

    override suspend fun getProducts(): List<CoinProduct> = try {
        log.v { "开始请求金币产品..." }
        
        val request = SKProductsRequest(coinProductIds)
        
        suspendCancellableCoroutine { continuation ->
            val result = mutableListOf<CoinProduct>()
            request.setDelegate(object : NSObject(), SKProductsRequestDelegateProtocol {
                override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
                    log.v { "收到金币产品响应" }
                    log.v { "有效产品数量: ${didReceiveResponse.products.size}" }
                    log.v { "无效的产品ID: ${didReceiveResponse.invalidProductIdentifiers}" }
                    
                    didReceiveResponse.products.forEach { skProduct ->
                        (skProduct as? SKProduct)?.let { product ->
                            log.v { "处理产品: ${product.productIdentifier}" }
                            
                            val coinAmount = when (product.productIdentifier) {
                                "coins_3" -> 3
                                "coins_10" -> 10
                                "coins_200" -> 200
                                "coins_500" -> 500
                                else -> 0
                            }
                            
                            products[product.productIdentifier] = product
                            result.add(
                                CoinProduct(
                                    id = product.productIdentifier,
                                    title = product.localizedTitle,
                                    description = product.localizedDescription,
                                    price = product.price.toString(),
                                    coinAmount = coinAmount
                                )
                            )
                        }
                    }
                    continuation.resume(result)
                }
                
                override fun requestDidFinish(request: SKRequest) {
                    log.v { "金币产品请求完成" }
                }
                
                override fun request(request: SKRequest, didFailWithError: NSError) {
                    log.e { "金币产品请求失败: ${didFailWithError.localizedDescription}" }
                    continuation.resume(emptyList())
                }
            })
            
            log.v { "开始发送金币产品请求..." }
            request.start()
        }
    } catch (e: Exception) {
        log.e { "获取金币产品时发生异常: ${e.message}" }
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
} 