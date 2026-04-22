package com.joshmermelstein.timesince.di

import com.joshmermelstein.timesince.data.TaskRepositoryImpl
import com.joshmermelstein.timesince.domain.repository.TaskRepository
import com.joshmermelstein.timesince.domain.usecase.CompleteTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.CreateTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.DeleteTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.GetOverdueTasksUseCase
import com.joshmermelstein.timesince.domain.usecase.GetSortedTasksUseCase
import com.joshmermelstein.timesince.domain.usecase.UpdateTaskUseCase
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
    single<TaskRepository> { TaskRepositoryImpl(get()) }

    factory { GetSortedTasksUseCase(get(), get()) }
    factory { GetOverdueTasksUseCase(get(), get()) }
    factory { CompleteTaskUseCase(get(), get()) }
    factory { CreateTaskUseCase(get(), get()) }
    factory { UpdateTaskUseCase(get()) }
    factory { DeleteTaskUseCase(get()) }
}
