package me.padi.qqlite.revived.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import me.padi.qqlite.revived.shared.viewmodel.home.HomeViewModel
import me.padi.qqlite.revived.shared.viewmodel.module.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

object RevivedKoin {
    private val lock = Any()

    fun ensureStarted(context: Context? = null): Koin {
        GlobalContext.getOrNull()?.let { return it }

        return synchronized(lock) {
            GlobalContext.getOrNull() ?: startKoin {
                context?.let { androidContext(it.applicationContext) }
                modules(revivedModules)
            }.koin
        }
    }

    fun createHomeViewModel(initialState: HomeUiState): HomeViewModel {
        return ensureStarted().get { parametersOf(initialState) }
    }

    fun homeViewModelFactory(initialState: HomeUiState): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                return create(modelClass)
            }

            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return createHomeViewModel(initialState) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

private val sharedViewModelModule = module {
    viewModel { MainViewModel() }
    viewModel { params -> HomeViewModel(params.get()) }
    factory { params -> HomeViewModel(params.get()) }
}

private val revivedModules = listOf(
    sharedViewModelModule
)
