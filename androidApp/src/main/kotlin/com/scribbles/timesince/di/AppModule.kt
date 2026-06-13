package com.scribbles.timesince.di

import com.scribbles.timesince.data.local.LocalCategoryDataSource
import com.scribbles.timesince.data.local.LocalTaskDataSource
import com.scribbles.timesince.data.local.RoomCategoryDataSource
import com.scribbles.timesince.data.local.RoomTaskDataSource
import com.scribbles.timesince.data.local.TaskDatabase
import com.scribbles.timesince.data.local.TaskDatabaseFactory
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.data.sync.SyncDataSource
import com.scribbles.timesince.notification.NotificationHelper
import com.scribbles.timesince.presentation.categories.CategoriesViewModel
import com.scribbles.timesince.presentation.settings.SettingsViewModel
import com.scribbles.timesince.presentation.taskedit.TaskEditViewModel
import com.scribbles.timesince.presentation.tasklist.TaskListViewModel
import com.scribbles.timesince.sync.GoogleAuthHelper
import com.scribbles.timesince.sync.GoogleDriveSyncDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule: Module = module {
    single<TaskDatabase> { TaskDatabaseFactory.create(androidContext()) }
    single { get<TaskDatabase>().taskDao() }
    single { get<TaskDatabase>().deletedTaskDao() }
    single { get<TaskDatabase>().categoryDao() }
    single { get<TaskDatabase>().deletedCategoryDao() }
    single<LocalTaskDataSource> { RoomTaskDataSource(get(), get()) }
    single<LocalCategoryDataSource> { RoomCategoryDataSource(get(), get()) }
    single { NotificationHelper(androidContext()) }
    single { GoogleAuthHelper(androidContext()) }
    single<SyncDataSource> {
        GoogleDriveSyncDataSource(tokenProvider = { get<GoogleAuthHelper>().getAccessToken() })
    }
    single {
        SyncCoordinator(
            repository = get(),
            categoryRepository = get(),
            syncDataSource = get(),
            isSignedIn = { get<GoogleAuthHelper>().isSignedIn },
        )
    }

    viewModel { TaskListViewModel(get(), get(), get(), get(), get()) }
    viewModel {
        TaskEditViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { CategoriesViewModel(get(), get(), get(), get(), get()) }
}
