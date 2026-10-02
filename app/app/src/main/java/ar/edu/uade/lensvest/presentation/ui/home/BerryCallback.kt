package ar.edu.uade.lensvest.presentation.ui.home

import android.net.Uri

/** What Berry sent back to lensvest://berry/callback, already parsed for the ViewModel. */
sealed interface BerryCallback {
    data class Authorized(val code: String, val state: String) : BerryCallback
    data class Failed(val message: String) : BerryCallback

    companion object {
        fun from(uri: Uri): BerryCallback {
            uri.getQueryParameter("error")?.let { error ->
                return Failed(uri.getQueryParameter("error_description") ?: error)
            }
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            return if (code != null && state != null) Authorized(code, state)
            else Failed("Berry no devolvió un código de autorización")
        }
    }
}
