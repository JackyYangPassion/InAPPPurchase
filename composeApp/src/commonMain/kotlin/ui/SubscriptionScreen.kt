package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.PurchaseState
import model.SubscriptionProduct
import model.SubscriptionType
import viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCoins: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.purchaseState) {
        if (uiState.purchaseState is PurchaseState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("订阅") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // 使用Material3的返回图标
                        Text("←")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.restorePurchases() }) {
                        Text("恢复购买")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Button(
                            onClick = onNavigateToCoins,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("购买金币")
                        }
                    }

                    items(uiState.products) { product ->
                        SubscriptionCard(
                            product = product,
                            onSubscribe = { viewModel.purchase(product.id) }
                        )
                    }
                }
            }

            if (uiState.purchaseState is PurchaseState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (uiState.purchaseState is PurchaseState.Error) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("错误") },
                    text = { Text((uiState.purchaseState as PurchaseState.Error).message) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Reset purchase state
                            }
                        ) {
                            Text("确定")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    product: SubscriptionProduct,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 添加订阅权益
            when (product.type) {
                SubscriptionType.PLUS -> {
                    BenefitItem("每月2000次查询")
                    BenefitItem("2TB云存储")
                    BenefitItem("每月100张图片创建")
                    BenefitItem("家庭计划（最多5个账户）")
                    BenefitItem("更快的响应速度")
                    BenefitItem("移除水印")
                }
                SubscriptionType.ULTIMATE -> {
                    BenefitItem("无限查询次数")
                    BenefitItem("4TB云存储")
                    BenefitItem("每月400张图片创建")
                    BenefitItem("无限家庭计划")
                    BenefitItem("最快的响应速度")
                    BenefitItem("提前体验新功能")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = product.price,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("订阅")
            }
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
} 