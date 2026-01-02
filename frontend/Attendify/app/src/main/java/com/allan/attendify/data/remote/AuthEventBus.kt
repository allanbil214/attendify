package com.allan.attendify.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthEvent {
    object SessionExpired : AuthEvent()
}

@Singleton
class AuthEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    suspend fun postEvent(event: AuthEvent) {
        _events.emit(event)
    }
}
