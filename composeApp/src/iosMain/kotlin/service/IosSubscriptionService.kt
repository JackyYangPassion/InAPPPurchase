package service

import compose.project.demo.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import model.PurchaseState
import model.SubscriptionProduct
import model.SubscriptionType
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.dataWithContentsOfURL
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKRequest
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionState
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi




@OptIn(ExperimentalNativeApi::class)
class IosSubscriptionService : SubscriptionService {
    private val products = mutableMapOf<String, SKProduct>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    //private val apiService = ApiService() // 假设你有一个API服务类

    init {
        // 启用 StoreKit 本地测试
        @OptIn(ExperimentalNativeApi::class)
        if (Platform.isDebugBinary) {
            log.v { "启用 StoreKit 本地测试" }
            SKPaymentQueue.defaultQueue().addTransactionObserver(
                object : NSObject(), SKPaymentTransactionObserverProtocol {
                    override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
                        updatedTransactions.forEach { transaction ->
                            (transaction as? SKPaymentTransaction)?.let { 
                                scope.launch {
                                    handleTransaction(it)
                                }
                            }
                        }
                    }
                }
            )
        }

        if (SKPaymentQueue.canMakePayments()) {
            log.v { "设备可以进行支付" }
        } else {
            log.v { "设备不能进行支付" }
        }

        log.v { "设备状态: ${SKPaymentQueue.canMakePayments()}" }
        log.v { "沙盒环境: ${Platform.isDebugBinary}" }
    }

    private suspend fun handleTransaction(transaction: SKPaymentTransaction) {
        when (transaction.transactionState) {
            SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                log.v { "购买成功，开始验证收据" }
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

    private suspend fun verifyReceipt() {
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

    override suspend fun getProducts(): List<SubscriptionProduct> = try {
    log.v { "开始请求产品..." }

    val productIdentifiers = setOf(
        "com.ai.razefaces.xyz.plus",
        "com.ai.razefaces.xyz.ulimit",
    )
    log.v { "请求的产品ID: $productIdentifiers" }
    
    val request = SKProductsRequest(productIdentifiers)

    suspendCancellableCoroutine { continuation ->
        val result = mutableListOf<SubscriptionProduct>()
        request.setDelegate(object : NSObject(), SKProductsRequestDelegateProtocol {
            override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
                log.v { "收到产品响应" }
                log.v { "有效产品数量: ${didReceiveResponse.products.size}" }
                log.v { "无效的产品ID: ${didReceiveResponse.invalidProductIdentifiers}" }
                
                didReceiveResponse.products.forEach { skProduct ->
                    (skProduct as? SKProduct)?.let { product ->
                        log.v { "处理产品: ${product.productIdentifier}" }
                        log.v { "产品标题: ${product.localizedTitle}" }
                        log.v { "产品价格: ${product.price}" }
                        
                        products[product.productIdentifier] = product
                        result.add(
                            SubscriptionProduct(
                                id = product.productIdentifier,
                                type = if (product.productIdentifier == "Plus") 
                                    SubscriptionType.PLUS 
                                else 
                                    SubscriptionType.ULTIMATE,
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
                log.v { "产品请求完成" }
            }
            
            override fun request(request: SKRequest, didFailWithError: NSError) {
                log.e { "产品请求失败: ${didFailWithError.localizedDescription}" }
                continuation.resume(emptyList())
            }
        })
        
        log.v { "开始发送产品请求..." }
        request.start()
    }
} catch (e: Exception) {
    log.e { "获取产品时发生异常: ${e.message}" }
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