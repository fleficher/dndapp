package com.flf.dndown.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DnDServiceState {
    val isRunning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setRunning(running: Boolean) {
        isRunning.value = running
    }
}
