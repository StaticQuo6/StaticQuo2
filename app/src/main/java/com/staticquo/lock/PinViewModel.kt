package com.staticquo.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PinUiState(
    val screen: PinScreen = PinScreen.LOADING,
    val pinEntry: String = "",
    val error: String? = null,
    val failedAttempts: Int = 0,
    val lockoutRemainingMs: Long = 0,
    val duressTriggered: Boolean = false
)

enum class PinScreen {
    LOADING,
    SETUP,
    UNLOCK,
    MAIN_APP
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val repository: PinRepository,
    private val duressWipe: DuressWipeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isSet = repository.isPinSet()
            _uiState.value = if (isSet) {
                PinUiState(screen = PinScreen.UNLOCK)
            } else {
                PinUiState(screen = PinScreen.SETUP)
            }
            checkLockout()
        }
    }

    fun onPinDigit(digit: Char) {
        val current = _uiState.value.pinEntry
        if (current.length < 8) {
            _uiState.value = _uiState.value.copy(pinEntry = current + digit, error = null)
        }
    }

    fun onDeleteDigit() {
        val current = _uiState.value.pinEntry
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                pinEntry = current.dropLast(1),
                error = null
            )
        }
    }

    // SETUP flow
    private var firstPin = ""

    fun onSetupPin(pin: String) {
        if (firstPin.isEmpty()) {
            if (pin.length < 4) {
                _uiState.value = _uiState.value.copy(error = "PIN must be at least 4 digits")
                return
            }
            firstPin = pin
            _uiState.value = _uiState.value.copy(
                pinEntry = "",
                error = "Confirm your PIN"
            )
        } else {
            if (pin == firstPin) {
                viewModelScope.launch {
                    repository.setPin(pin)
                    _uiState.value = PinUiState(screen = PinScreen.MAIN_APP)
                }
            } else {
                firstPin = ""
                _uiState.value = _uiState.value.copy(
                    pinEntry = "",
                    error = "PINs do not match. Try again."
                )
            }
        }
    }

    // UNLOCK flow
    fun onUnlockPin(pin: String) {
        viewModelScope.launch {
            when (val result = repository.verifyPin(pin)) {
                PinRepository.PinResult.CORRECT -> {
                    _uiState.value = PinUiState(screen = PinScreen.MAIN_APP)
                }
                PinRepository.PinResult.DURESS_CORRECT -> {
                    _uiState.value = PinUiState(
                        screen = PinScreen.MAIN_APP,
                        duressTriggered = true
                    )
                    viewModelScope.launch { duressWipe.performWipe() }
                }
                PinRepository.PinResult.INCORRECT -> {
                    val attempts = repository.getFailedAttempts()
                    _uiState.value = _uiState.value.copy(
                        pinEntry = "",
                        error = if (attempts >= 5) {
                            "Wrong PIN. Lockout will activate after $attempts/5 attempts."
                        } else {
                            "Wrong PIN. $attempts of 5 attempts used."
                        },
                        failedAttempts = attempts
                    )
                }
                PinRepository.PinResult.LOCKED_OUT -> {
                    _uiState.value = _uiState.value.copy(
                        pinEntry = "",
                        error = "Too many attempts. Device locked."
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetPinEntry() {
        _uiState.value = _uiState.value.copy(pinEntry = "", error = null)
    }

    private fun checkLockout() {
        viewModelScope.launch {
            while (_uiState.value.screen == PinScreen.UNLOCK) {
                val remaining = repository.getRemainingLockoutMs()
                _uiState.value = _uiState.value.copy(lockoutRemainingMs = remaining)
                if (remaining > 0) {
                    delay(1000)
                } else {
                    delay(5000)
                }
            }
        }
    }
}
