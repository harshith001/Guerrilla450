package com.example.guerrilla450.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionState { Connected, Searching, Offline }

class AppViewModel : ViewModel() {
    private val _conn = MutableStateFlow(ConnectionState.Connected)
    val conn = _conn.asStateFlow()

    fun setConn(state: ConnectionState) { _conn.value = state }
}
