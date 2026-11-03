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
 * 商品一覧のViewModelをActivityに紐づけ、テーマ付きの一覧画面を表示する。
 *
 * デバッグビルドではIntentの`catalog_scenario`で空一覧・取得失敗・追加取得失敗を選べる。
 * 未指定または未知の値、およびデバッグ以外のビルドでは通常シナリオを使用する。
 *
 * デバッグ用の値は`empty`を空一覧、`error`を取得失敗、`next-page-error`を追加取得失敗へ対応させる。
 * デバッグ以外ではextraの値をシナリオ選択に使用しない。
 * 選んだシナリオをFactoryへ渡し、Activityに紐づくViewModelを一覧画面から利用する。
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
     * Activityの初期化後、エッジツーエッジ表示を有効にして商品一覧のCompose画面を構成する。
     *
     * [savedInstanceState]をそのまま親の初期化処理へ渡し、その後に表示を構成する。
     * このメソッドではBundleから商品一覧やページ情報を独自に復元しない。
     *
     * @param savedInstanceState フレームワークが渡す復元用状態。復元する状態がなければnull。
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
