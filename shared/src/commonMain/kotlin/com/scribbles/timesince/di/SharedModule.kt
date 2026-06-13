package com.scribbles.timesince.di

import com.scribbles.timesince.data.TaskRepositoryImpl
import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.usecase.CompleteTaskUseCase
import com.scribbles.timesince.domain.usecase.CreateTaskUseCase
import com.scribbles.timesince.domain.usecase.DeleteTaskUseCase
import com.scribbles.timesince.domain.usecase.GetOverdueTasksUseCase
import com.scribbles.timesince.domain.usecase.GetSortedTasksUseCase
import com.scribbles.timesince.domain.usecase.UpdateTaskUseCase
import com.scribbles.timesince.domain.time.TimeZoneProvider
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
    single<TaskRepository> { TaskRepositoryImpl(get(), get()) }

    factory { GetSortedTasksUseCase(get(), get(), get()) }
    factory { GetOverdueTasksUseCase(get(), get(), get()) }
    factory { CompleteTaskUseCase(get(), get()) }
    factory { CreateTaskUseCase(get(), get()) }
    factory { UpdateTaskUseCase(get()) }
    factory { DeleteTaskUseCase(get()) }
}
