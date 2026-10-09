package com.example.kdoctest.presentation.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kdoctest.domain.model.AvailabilityStatus
import com.example.kdoctest.domain.model.Product

@Composable
fun CatalogRoute(viewModel: CatalogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadNextPage,
        onRetry = viewModel::retry,
    )
}

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                CatalogHeader(state = state, onRefresh = onRefresh)
                if (state.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().testTag("catalog-refreshing"),
                    )
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                when {
                    state.isInitialLoading -> CatalogLoading()
                    state.initialError != null -> CatalogInitialError(state.initialError, onRetry)
                    state.items.isEmpty() -> CatalogEmpty(onRefresh)
                    else -> CatalogList(state = state, onLoadMore = onLoadMore, onRetry = onRetry)
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(state: CatalogUiState, onRefresh: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "日々の仕事を支える備品",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "備品カタログ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            TextButton(
                onClick = onRefresh,
                enabled = !state.isInitialLoading && !state.isRefreshing,
                modifier = Modifier.testTag("catalog-refresh"),
            ) {
                Text("更新")
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "すべての商品",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${state.items.size} / ${state.totalCount} 件",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("catalog-count"),
            )
        }
    }
}

@Composable
private fun CatalogLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp).testTag("catalog-initial-loading"),
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "商品を読み込んでいます",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CatalogInitialError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "商品を表示できませんでした",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("catalog-initial-error"),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onRetry, modifier = Modifier.testTag("catalog-retry")) {
                Text("もう一度読み込む")
            }
        }
    }
}

@Composable
private fun CatalogEmpty(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp).testTag("catalog-empty"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "登録されている商品はありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "更新すると、最新の商品情報を確認できます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRefresh) { Text("商品情報を更新") }
        }
    }
}

@Composable
private fun CatalogList(state: CatalogUiState, onLoadMore: () -> Unit, onRetry: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("catalog-list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.refreshError?.let { message ->
            item(key = "refresh-error") {
                CatalogErrorNotice(message, onRetry, "catalog-refresh-error")
            }
        }
        items(items = state.items, key = { it.id }, contentType = { "product" }) { product ->
            ProductCard(product)
        }
        item(key = "catalog-footer") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    state.isLoadingMore -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp).testTag("catalog-more-loading"),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "続きを読み込んでいます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.loadMoreError != null -> CatalogErrorNotice(
                        message = state.loadMoreError,
                        onRetry = onRetry,
                        tag = "catalog-more-error",
                    )
                    state.hasMore -> {
                        val remaining = (state.totalCount - state.items.size).coerceAtLeast(1)
                        val nextCount = minOf(CatalogViewModel.PAGE_SIZE, remaining)
                        OutlinedButton(
                            onClick = onLoadMore,
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth().testTag("catalog-load-more"),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("さらに${nextCount}件表示")
                        }
                    }
                    else -> Text(
                        text = "すべての商品を表示しました",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp).testTag("catalog-end"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("product-${product.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = product.category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StockBadge(product)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = ProductFormatter.price(product.price),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${ProductFormatter.taxLabel(product.price)} / ${ProductFormatter.salesUnit(product.salesUnit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (product.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    product.tags.distinct().take(3).forEach { tag ->
                        Surface(
                            modifier = Modifier.weight(1f, fill = false),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockBadge(product: Product) {
    val available = product.availability.status == AvailabilityStatus.AVAILABLE &&
        (product.availability.stockUnits ?: 0) > 0
    val containerColor = if (available) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (available) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(8.dp), color = containerColor) {
        Text(
            text = ProductFormatter.stock(product.availability, product.salesUnit),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Composable
private fun CatalogErrorNotice(message: String, onRetry: () -> Unit, tag: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(tag).semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End).testTag("catalog-retry")) {
                Text("再試行", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
