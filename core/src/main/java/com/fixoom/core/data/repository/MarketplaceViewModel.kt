package com.fixoom.core.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

data class MarketplaceState(
    val data: MarketplaceSnapshot = MarketplaceSnapshot(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class MarketplaceViewModel : ViewModel() {
    private val repository = MarketplaceRepository()
    private val mutable = MutableStateFlow(MarketplaceState())
    private val loadingLock = Mutex()
    val state = mutable.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (mutable.value.saving) return
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        loadingLock.withLock {
            try {
                val data = withTimeout(15_000) { repository.snapshot() }
                mutable.value = mutable.value.copy(data = data, loading = false, error = null)
            } catch (cancelled: CancellationException) {
                if (cancelled is kotlinx.coroutines.TimeoutCancellationException)
                    mutable.value =
                        mutable.value.copy(
                            loading = false,
                            error = "Connection timed out. Please refresh.",
                        )
                else throw cancelled
            } catch (_: Exception) {
                mutable.value =
                    mutable.value.copy(
                        loading = false,
                        error =
                            "Unable to load service data. Check your connection and account, then refresh.",
                    )
            }
        }
    }

    fun runAction(action: suspend MarketplaceRepository.() -> Unit) {
        if (mutable.value.saving) return
        mutable.value = mutable.value.copy(saving = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                withTimeout(20_000) { repository.action() }
                mutable.value = mutable.value.copy(notice = "Changes saved.")
                load()
            } catch (cancelled: CancellationException) {
                if (cancelled is kotlinx.coroutines.TimeoutCancellationException)
                    mutable.value =
                        mutable.value.copy(
                            error = "Connection timed out. Refresh before trying again."
                        )
                else throw cancelled
            } catch (_: Exception) {
                mutable.value =
                    mutable.value.copy(
                        error =
                            "The request could not be saved. Check your connection, account access and the current job status."
                    )
            } finally {
                mutable.value = mutable.value.copy(saving = false)
            }
        }
    }

    fun saveProviderPhoto(jpeg: ByteArray?, onSaved: () -> Unit = {}) {
        if (mutable.value.saving) return
        mutable.value = mutable.value.copy(saving = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val path = withTimeout(45_000) { ProviderPhotoRepository().save(jpeg) }
                val data = mutable.value.data
                mutable.value = mutable.value.copy(
                    data = data.copy(
                        profile = data.profile?.copy(avatarUrl = path),
                        ownService = data.ownService?.copy(avatarPath = path),
                    ),
                    notice = if (path == null) "Profile photo removed." else "Profile photo updated.",
                )
                onSaved()
                load()
            } catch (cancelled: CancellationException) {
                if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
                mutable.value = mutable.value.copy(error = "Photo upload timed out. Refresh your profile before retrying.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Your photo could not be saved. Check your connection and try again.")
            } finally {
                mutable.value = mutable.value.copy(saving = false)
            }
        }
    }

    fun authenticate(
        register: Boolean,
        name: String,
        email: String,
        phone: String,
        password: String,
    ) {
        if (mutable.value.saving) return
        mutable.value = mutable.value.copy(saving = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val auth = AuthRepository()
                val result =
                    withTimeout(20_000) {
                        if (register) auth.register(name, email, phone, password, AppRole.CUSTOMER)
                        else auth.signIn(email, password, AppRole.CUSTOMER)
                    }
                when (result) {
                    is AuthResult.Authenticated -> load()
                    AuthResult.EmailVerificationRequired ->
                        mutable.value =
                            mutable.value.copy(
                                notice = "Check your email to verify your account, then sign in."
                            )
                    is AuthResult.WrongApplication ->
                        mutable.value =
                            mutable.value.copy(
                                error =
                                    "Use a customer account here. Provider and administrator accounts have separate apps."
                            )
                    else ->
                        mutable.value =
                            mutable.value.copy(
                                error =
                                    "Unable to sign in or register. Check your details, email verification and account access."
                            )
                }
            } catch (cancelled: CancellationException) {
                if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
                mutable.value =
                    mutable.value.copy(error = "Connection timed out. Please try again.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Unable to connect. Please try again.")
            } finally {
                mutable.value = mutable.value.copy(saving = false)
            }
        }
    }

    fun signOut() = runAction {
        val result = AuthRepository().signOut()
        check(result == AuthResult.SignedOut)
    }

    fun dismissNotice() {
        mutable.value = mutable.value.copy(notice = null)
    }
}
