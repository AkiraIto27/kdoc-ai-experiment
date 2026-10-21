package com.example.kdoctest.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kdoctest.data.remote.CatalogScenario
import com.example.kdoctest.data.remote.createCatalogHttpClient
import com.example.kdoctest.data.repository.HttpProductRepository
import com.example.kdoctest.domain.usecase.LoadCatalogPage
import com.example.kdoctest.presentation.catalog.CatalogViewModel
import java.io.Closeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogViewModelFactory(
    context: Context,
    private val scenario: CatalogScenario = CatalogScenario.NORMAL,
) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CatalogViewModel::class.java))
        val client = createCatalogHttpClient(
            readFixture = {
                withContext(Dispatchers.IO) {
                    applicationContext.assets.open("catalog/products.json")
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            },
            scenario = scenario,
        )
        val viewModel = CatalogViewModel(LoadCatalogPage(HttpProductRepository(client)))
        viewModel.addCloseable(Closeable { client.close() })
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}
