package com.joshmermelstein.timesince.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val useCase = DeleteTaskUseCase(repository)

    @Test
    fun deletesExistingTask() = runTest {
        repository.create(taskWith(id = "1"))
        repository.create(taskWith(id = "2"))

        useCase("1")

        val remaining = repository.observeAll().first()
        assertEquals(1, remaining.size)
        assertEquals("2", remaining[0].id)
    }

    @Test
    fun deletingNonexistentTaskDoesNothing() = runTest {
        repository.create(taskWith(id = "1"))

        useCase("nonexistent")

        val remaining = repository.observeAll().first()
        assertEquals(1, remaining.size)
    }
}
