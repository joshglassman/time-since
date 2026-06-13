package com.scribbles.timesince.di

import com.scribbles.timesince.data.CategoryRepositoryImpl
import com.scribbles.timesince.data.TaskRepositoryImpl
import com.scribbles.timesince.domain.repository.CategoryRepository
import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.usecase.CompleteTaskUseCase
import com.scribbles.timesince.domain.usecase.CreateCategoryUseCase
import com.scribbles.timesince.domain.usecase.CreateTaskUseCase
import com.scribbles.timesince.domain.usecase.DeleteCategoryUseCase
import com.scribbles.timesince.domain.usecase.DeleteTaskUseCase
import com.scribbles.timesince.domain.usecase.GetCategoriesUseCase
import com.scribbles.timesince.domain.usecase.GetOverdueTasksUseCase
import com.scribbles.timesince.domain.usecase.GetSortedTasksUseCase
import com.scribbles.timesince.domain.usecase.SetArchivedUseCase
import com.scribbles.timesince.domain.usecase.SetPausedUseCase
import com.scribbles.timesince.domain.usecase.SnoozeTaskUseCase
import com.scribbles.timesince.domain.usecase.UndoTaskUseCase
import com.scribbles.timesince.domain.usecase.UpdateCategoryUseCase
import com.scribbles.timesince.domain.usecase.UpdateTaskUseCase
import com.scribbles.timesince.domain.time.TimeZoneProvider
import com.scribbles.timesince.domain.undo.UndoStore
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * Koin module shared across all platforms. Provides the repository,
 * use cases, and a default [Clock]. Platform modules must provide a
 * `LocalTaskDataSource` binding (see Android `appModule`).
 */
val sharedModule: Module = module {
    single<Clock> { Clock.System }
    single<TimeZoneProvider> { TimeZoneProvider.System }
    single { UndoStore() }
    single<TaskRepository> { TaskRepositoryImpl(get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get()) }

    factory { GetCategoriesUseCase(get()) }
    factory { CreateCategoryUseCase(get(), get()) }
    factory { UpdateCategoryUseCase(get()) }
    factory { DeleteCategoryUseCase(get(), get()) }

    factory { GetSortedTasksUseCase(get(), get(), get()) }
    factory { GetOverdueTasksUseCase(get(), get(), get()) }
    factory { CompleteTaskUseCase(get(), get(), get()) }
    factory { SnoozeTaskUseCase(get(), get(), get(), get()) }
    factory { UndoTaskUseCase(get(), get()) }
    factory { SetPausedUseCase(get(), get()) }
    factory { SetArchivedUseCase(get()) }
    factory { CreateTaskUseCase(get(), get()) }
    factory { UpdateTaskUseCase(get()) }
    factory { DeleteTaskUseCase(get()) }
}
