package com.example.kdoctest.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kdoctest.domain.repository.CatalogException
import com.example.kdoctest.domain.usecase.LoadCatalogPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 商品ページの取得を起動し、一覧・読み込み状態・失敗メッセージを[uiState]へ反映する。
 * 生成時に[refresh]を呼び、先頭ページの取得を開始する。
 *
 * 要求世代が一致しない成功結果や失敗は状態へ反映しない。
 * 取得コルーチンのキャンセルはエラー表示へ変換せず再送出する。
 *
 * @param useCase 各ページを取得する処理。すべての取得に[PAGE_SIZE]を指定する。
 */
class CatalogViewModel(private val useCase: LoadCatalogPage) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CatalogUiState())
    /**
     * 一覧画面が監視する読み取り専用の状態。各エラー値のnullは、その種別の表示対象エラーがないことを表す。
     * 成功時の`hasMore`は、次カーソルがあり、表示件数が取得した全件数より少ない場合にtrueになる。
     */
    val uiState: StateFlow<CatalogUiState> = mutableUiState.asStateFlow()

    private var nextCursor: String? = null
    private var requestJob: Job? = null
    private var requestGeneration = 0L

    init {
        refresh()
    }

    /**
     * 進行中の取得をキャンセルし、エラー表示を消して先頭ページの非同期取得を開始する。
     *
     * 取得中は既存商品を保持し、商品がなければ初回読み込み、あれば更新中として表示する。
     * 成功時は一覧を置き換える。失敗時は商品とページ情報を保持し、商品がなければ`initialError`、
     * あれば`refreshError`へメッセージを設定する。
     */
    fun refresh() {
        val generation = ++requestGeneration
        requestJob?.cancel()
        val current = mutableUiState.value
        mutableUiState.value = current.copy(
            isInitialLoading = current.items.isEmpty(),
            isRefreshing = current.items.isNotEmpty(),
            isLoadingMore = false,
            initialError = null,
            refreshError = null,
            loadMoreError = null,
        )
        loadPage(cursor = null, replace = true, generation = generation)
    }

    /**
     * 次カーソルで非同期取得を開始し、成功した商品のみを既存一覧の末尾へ追記する。
     *
     * 次カーソルがない場合、いずれかの取得中、または`hasMore`がfalseの場合は何も行わない。
     * 開始時に更新・追加取得のエラーを消し、失敗時は商品とページ情報を保持して`loadMoreError`を設定する。
     */
    fun loadNextPage() {
        val current = mutableUiState.value
        val cursor = nextCursor ?: return
        if (current.isLoading || !current.hasMore) return

        val generation = ++requestGeneration
        mutableUiState.value = current.copy(
            isLoadingMore = true,
            refreshError = null,
            loadMoreError = null,
        )
        loadPage(cursor = cursor, replace = false, generation = generation)
    }

    /**
     * 表示中の失敗に応じ、初回・更新の失敗なら[refresh]、追加取得の失敗なら[loadNextPage]を呼ぶ。
     * 取得中またはエラーがない場合は何も行わない。
     */
    fun retry() {
        val current = mutableUiState.value
        if (current.isLoading) return
        when {
            current.initialError != null || current.refreshError != null -> refresh()
            current.loadMoreError != null -> loadNextPage()
        }
    }

    private fun loadPage(cursor: String?, replace: Boolean, generation: Long) {
        requestJob = viewModelScope.launch {
            try {
                val page = useCase(cursor = cursor, limit = PAGE_SIZE)
                if (generation != requestGeneration) return@launch

                val items = if (replace) page.items else mutableUiState.value.items + page.items
                nextCursor = page.nextCursor
                mutableUiState.value = CatalogUiState(
                    items = items,
                    totalCount = page.totalCount,
                    hasMore = page.nextCursor != null && items.size < page.totalCount,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != requestGeneration) return@launch

                val current = mutableUiState.value
                val message = when (error) {
                    is CatalogException.InvalidResponse -> "商品情報を読み込めませんでした。もう一度お試しください。"
                    else -> "商品を読み込めませんでした。もう一度お試しください。"
                }
                mutableUiState.value = current.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    initialError = message.takeIf { replace && current.items.isEmpty() },
                    refreshError = message.takeIf { replace && current.items.isNotEmpty() },
                    loadMoreError = message.takeUnless { replace },
                )
            }
        }
    }

    companion object {
        /** 初回・更新・追加取得に共通で指定する、1ページの取得件数の上限。 */
        const val PAGE_SIZE = 50
    }
}
