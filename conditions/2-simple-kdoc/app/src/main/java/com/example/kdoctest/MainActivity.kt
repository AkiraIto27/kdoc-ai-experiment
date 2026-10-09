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
 * メインActivity。
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
     * 作成時の処理。
     *
     * @param savedInstanceState 保存されたインスタンス状態。
     * @return 戻り値はない。
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
