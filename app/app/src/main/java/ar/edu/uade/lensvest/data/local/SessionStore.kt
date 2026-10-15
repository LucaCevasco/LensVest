package ar.edu.uade.lensvest.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The signed-in user's backend JWT and name. */
// ponytail: in memory only (closing the app logs out); persist with Keystore/DataStore for RNF02.
@Singleton
class SessionStore @Inject constructor() {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    fun save(token: String, userName: String) {
        _token.value = token
        _userName.value = userName
    }

    fun clear() {
        _token.value = null
        _userName.value = null
    }
}
