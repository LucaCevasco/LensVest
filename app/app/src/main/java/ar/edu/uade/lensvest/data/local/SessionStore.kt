package ar.edu.uade.lensvest.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Holds the backend JWT for the current session. */
// ponytail: in memory only (closing the app logs out); persist with Keystore/DataStore for RNF02.
@Singleton
class SessionStore @Inject constructor() {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    fun save(token: String) {
        _token.value = token
    }

    fun clear() {
        _token.value = null
    }
}
