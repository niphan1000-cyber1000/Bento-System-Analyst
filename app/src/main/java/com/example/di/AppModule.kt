package com.aistudio.aisystemanalyst.di

import com.aistudio.aisystemanalyst.data.local.AppDatabase
import com.aistudio.aisystemanalyst.data.repository.ProjectRepository
import com.aistudio.aisystemanalyst.ui.viewmodel.AppViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().projectDao() }
    single { ProjectRepository(get()) }
    single { com.aistudio.aisystemanalyst.util.CrashMonitoringService(androidContext()) }
    viewModel { AppViewModel(androidApplication(), get(), get()) }
}
