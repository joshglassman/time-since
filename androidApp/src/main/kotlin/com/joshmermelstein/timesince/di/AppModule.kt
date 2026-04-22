package com.joshmermelstein.timesince.di

import com.joshmermelstein.timesince.data.local.LocalTaskDataSource
import com.joshmermelstein.timesince.data.local.RoomTaskDataSource
import com.joshmermelstein.timesince.data.local.TaskDatabase
import com.joshmermelstein.timesince.data.local.TaskDatabaseFactory
import com.joshmermelstein.timesince.data.sync.SyncDataSource
import com.joshmermelstein.timesince.notification.NotificationHelper
import com.joshmermelstein.timesince.presentation.settings.SettingsViewModel
import com.joshmermelstein.timesince.presentation.taskedit.TaskEditViewModel
import com.joshmermelstein.timesince.presentation.tasklist.TaskListViewModel
import com.joshmermelstein.timesince.sync.GoogleAuthHelper
import com.joshmermelstein.timesince.sync.GoogleDriveSyncDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule: Module = module {
    single<TaskDatabase> { TaskDatabaseFactory.create(androidContext()) }
    single { get<TaskDatabase>().taskDao() }
    single<LocalTaskDataSource> { RoomTaskDataSource(get()) }
    single { NotificationHelper(androidContext()) }
    single { GoogleAuthHelper(androidContext()) }
    single<SyncDataSource> { GoogleDriveSyncDataSource(tokenProvider = { get<GoogleAuthHelper>().getAccessToken() }) }

    viewModel { TaskListViewModel(get(), get(), get(), get()) }
    viewModel { TaskEditViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
