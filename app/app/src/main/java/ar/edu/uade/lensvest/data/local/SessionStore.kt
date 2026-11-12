package ar.edu.uade.lensvest.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "session"
private const val TOKEN = "token"
private const val USER_NAME = "user_name"

/**
 * The signed-in user's backend JWT and name. They survive closing the app,
 * encrypted with a Keystore key: never stored as plain text (RNF02).
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cipher: KeystoreCipher,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ponytail: read once at startup on the caller's thread; a couple of small prefs + one AES block.
    private val _token = MutableStateFlow(prefs.getString(TOKEN, null)?.let(cipher::decrypt))
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _userName = MutableStateFlow(_token.value?.let { prefs.getString(USER_NAME, null)?.let(cipher::decrypt) })
    val userName: StateFlow<String?> = _userName.asStateFlow()

    fun save(token: String, userName: String) {
        prefs.edit {
            putString(TOKEN, cipher.encrypt(token))
            putString(USER_NAME, cipher.encrypt(userName))
        }
        _token.value = token
        _userName.value = userName
    }

    fun clear() {
        prefs.edit { clear() }
        _token.value = null
        _userName.value = null
    }
}
