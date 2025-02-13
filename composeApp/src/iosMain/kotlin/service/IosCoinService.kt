package service

import compose.project.demo.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import model.CoinProduct
import model.PurchaseState
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.dataWithContentsOfURL
import platform.StoreKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
class IosCoinService : CoinService {
    private val products = mutableMapOf<String, SKProduct>()
    private val coinProductIds = setOf(
        "com.ai.razefaces.swiftshopping.coins_3",
        "com.ai.razefaces.swiftshopping.coins_10",
        "com.ai.razefaces.swiftshopping.coins_200",
        "com.ai.razefaces.swiftshopping.coins_500"
    )

    init {
        //if (Platform.isDebugBinary) {
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
        //}
    }

    private fun handleTransaction(transaction: SKPaymentTransaction) {
        when (transaction.transactionState) {
            SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                log.v { "购买成功" }
                verifyReceipt()
                SKPaymentQueue.defaultQueue().finishTransaction(transaction)
            }
            SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                log.e { "购买失败: ${transaction.error?.localizedDescription}" }
                SKPaymentQueue.defaultQueue().finishTransaction(transaction)
            }
            else -> {}
        }
    }

    private fun verifyReceipt() {
        try {
            // 获取应用收据路径
            val receiptUrl = NSBundle.mainBundle.appStoreReceiptURL
            if (receiptUrl != null) {
                // 读取收据数据
                val receiptData = NSData.dataWithContentsOfURL(receiptUrl)
                if (receiptData != null) {
                    // 将收据数据转换为Base64字符串
                    val base64Receipt = receiptData.base64EncodedStringWithOptions(0u)
                    log.v { "收据数据: $base64Receipt" }
//                    // 发送到后端验证
//                    val response = apiService.verifyReceipt(base64Receipt)
//
//                    if (response.isSuccessful) {
//                        log.v { "收据验证成功" }
//                        // 在这里处理验证成功的逻辑
//                    } else {
//                        log.e { "收据验证失败: ${response.message}" }
//                    }
                } else {
                    log.e { "无法读取收据数据" }
                }
            } else {
                log.e { "找不到收据文件" }
            }
        } catch (e: Exception) {
            log.e { "验证收据时发生错误: ${e.message}" }
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
                                "com.ai.razefaces.swiftshopping.coins_3" -> 3
                                "com.ai.razefaces.swiftshopping.coins_10" -> 10
                                "com.ai.razefaces.swiftshopping.coins_200" -> 200
                                "com.ai.razefaces.swiftshopping.coins_500" -> 500
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