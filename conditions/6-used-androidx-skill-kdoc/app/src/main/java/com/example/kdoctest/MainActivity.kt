package com.example.kdoctest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.kdoctest.data.remote.CatalogScenario
import com.example.kdoctest.di.CatalogViewModelFactory
import com.example.kdoctest.presentation.catalog.CatalogRoute
import com.example.kdoctest.presentation.catalog.CatalogViewModel
import com.example.kdoctest.ui.theme.KDocTestTheme

/**
 * テーマ付きの商品一覧画面を表示する。
 *
 * ```kotlin
 * val intent = android.content.Intent(context, MainActivity::class.java)
 * intent.putExtra("catalog_scenario", "empty")
 * context.startActivity(intent)
 * ```
 *
 * - 商品一覧の[CatalogViewModel]を[MainActivity]に紐づけ、[KDocTestTheme]内で[CatalogRoute]を表示する
 * - デバッグビルドでは[android.content.Intent]の`catalog_scenario`でシナリオを選ぶ。`empty`は空一覧、`error`は取得失敗、`next-page-error`は追加取得失敗
 * - 未指定・未知の値、およびデバッグ以外のビルドでは通常シナリオを使う
 */
class MainActivity : ComponentActivity() {
    private val catalogViewModel: CatalogViewModel by viewModels {
        val scenario = if (BuildConfig.DEBUG) {
            when (intent.getStringExtra("catalog_scenario")) {
                "empty" -> CatalogScenario.EMPTY
                "error" -> CatalogScenario.ERROR
                "next-page-error" -> CatalogScenario.NEXT_PAGE_ERROR
                else -> CatalogScenario.NORMAL
            }
        } else {
            CatalogScenario.NORMAL
        }
        CatalogViewModelFactory(applicationContext, scenario)
    }

    /**
     * 商品一覧のCompose画面を構成する。
     *
     * ```kotlin
     * // フレームワークが onCreate(savedInstanceState) を呼ぶ
     * // super.onCreate(savedInstanceState) → enableEdgeToEdge()
     * // setContent → KDocTestTheme → CatalogRoute(catalogViewModel)
     * ```
     *
     * - [ComponentActivity.onCreate]による初期化後、[enableEdgeToEdge]でエッジツーエッジ表示を有効にする
     * - [setContent]で[KDocTestTheme]と商品一覧の[CatalogRoute]を構成する
     *
     * @param savedInstanceState フレームワークが渡す復元用の[Bundle]。復元する状態がなければ`null`
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KDocTestTheme {
                CatalogRoute(catalogViewModel)
            }
        }
    }
}
