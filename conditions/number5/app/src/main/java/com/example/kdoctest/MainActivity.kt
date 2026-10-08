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
 * 一覧ViewModelをActivityに紐づけ、テーマ付き一覧画面を表示する。
 * デバッグ時はIntentの`catalog_scenario`で空・取得失敗・追加取得失敗を選べる。
 * 未指定・未知の値・デバッグ以外は通常シナリオ。
 *
 * 背景: 架空の業務用備品カタログで、在庫不明と欠品を分け、販売単位ごとの価格を表示する。
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
     * Activity初期化後、エッジツーエッジ表示を有効にしてCompose一覧画面を構成する。
     *
     * @param savedInstanceState フレームワークの復元用状態。復元状態がなければnull。
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
