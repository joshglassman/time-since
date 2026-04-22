package com.joshmermelstein.timesince.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Helpers for installing an [UnconfinedTestDispatcher] as the Main dispatcher
 * during ViewModel tests. Call [installMainDispatcher] from an `@BeforeTest`
 * method and [uninstallMainDispatcher] from an `@AfterTest` method.
 *
 * The unconfined dispatcher runs coroutines eagerly, so callers can assert
 * on state immediately after invoking a ViewModel action without manually
 * advancing the test scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun installMainDispatcher() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

@OptIn(ExperimentalCoroutinesApi::class)
fun uninstallMainDispatcher() {
    Dispatchers.resetMain()
}
