package com.bgd.myapplication

import androidx.lifecycle.SavedStateHandle
import com.bgd.myapplication.core.data.AccountRepository
import com.bgd.myapplication.core.data.AccountState
import com.bgd.myapplication.core.data.LoginRequired
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private class FakeAccount : AccountRepository {
        override val account = MutableStateFlow(AccountState())
        val operations = mutableListOf<Pair<Int, Boolean>>()
        var failLogin = false
        var expired = false
        override suspend fun login(username: String, password: String, register: Boolean, confirmation: String) {
            if (failLogin) error("invalid")
            account.value = AccountState(username)
        }
        override suspend fun collect(id: Int, desired: Boolean) {
            if (expired) { expired = false; account.value = AccountState(); throw LoginRequired() }
            operations.add(id to desired)
        }
        override suspend fun logout() { account.value = AccountState() }
    }
    @Test fun loginResumesCollectionOnce() = runTest(dispatcher) {
        val repo = FakeAccount()
        val vm = AuthViewModel(repo, SavedStateHandle())
        vm.collect(42, true)
        assertTrue(vm.loginRequested.value)
        assertTrue(repo.operations.isEmpty())
        vm.submit("user", "password", "", false)
        advanceUntilIdle()
        assertEquals(listOf(42 to true), repo.operations)
        assertFalse(vm.loginRequested.value)
        vm.openLogin(); vm.submit("user", "password", "", false); advanceUntilIdle()
        assertEquals(1, repo.operations.size)
    }
    @Test fun failedLoginKeepsIntentAndCancelClearsIt() = runTest(dispatcher) {
        val repo = FakeAccount().apply { failLogin = true }
        val vm = AuthViewModel(repo, SavedStateHandle())
        vm.collect(42, true); vm.submit("user", "password", "", false); advanceUntilIdle()
        assertTrue(vm.loginRequested.value)
        assertTrue(repo.operations.isEmpty())
        vm.cancelLogin(); repo.failLogin = false
        vm.openLogin(); vm.submit("user", "password", "", false); advanceUntilIdle()
        assertTrue(repo.operations.isEmpty())
    }
    @Test fun directLoginDoesNotCollect() = runTest(dispatcher) {
        val repo = FakeAccount()
        val vm = AuthViewModel(repo, SavedStateHandle())
        vm.openLogin(); vm.submit("user", "password", "", false); advanceUntilIdle()
        assertTrue(repo.operations.isEmpty())
    }
    @Test fun expiredSessionResumesOriginalDesiredState() = runTest(dispatcher) {
        val repo = FakeAccount().apply { account.value = AccountState("user"); expired = true }
        val vm = AuthViewModel(repo, SavedStateHandle())
        vm.collect(7, false); advanceUntilIdle()
        assertTrue(vm.loginRequested.value)
        vm.submit("user", "password", "", false); advanceUntilIdle()
        assertEquals(listOf(7 to false), repo.operations)
    }
    @Test fun repeatedLoginTapDoesNotRepeatCollection() = runTest(dispatcher) {
        val repo = FakeAccount()
        val vm = AuthViewModel(repo, SavedStateHandle())
        vm.collect(8, true)
        vm.submit("user", "password", "", false)
        vm.submit("user", "password", "", false)
        advanceUntilIdle()
        assertEquals(listOf(8 to true), repo.operations)
    }
}
