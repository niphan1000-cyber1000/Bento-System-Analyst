package com.example.di

import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.ui.viewmodel.AppViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().projectDao() }
    single { ProjectRepository(get()) }
    single { com.example.util.CrashMonitoringService(androidContext()) }
    viewModel { AppViewModel(androidApplication(), get(), get()) }
}
