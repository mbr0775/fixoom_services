package com.fixoom.customer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.core.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class CustomerProfileViewModel : ViewModel() {
    private val mutableState = MutableStateFlow<ProfileAccountState>(ProfileAccountState.Loading)
    val state = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        mutableState.value = ProfileAccountState.Loading
        refreshJob = viewModelScope.launch {
            mutableState.value = try {
                withTimeoutOrNull(12_000) {
                    AuthRepository().getCurrentProfile().toCustomerAccountState()
                } ?: ProfileAccountState.Unavailable
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Never put database messages or account identifiers into the UI/logs.
                ProfileAccountState.Unavailable
            }
        }
    }
}
