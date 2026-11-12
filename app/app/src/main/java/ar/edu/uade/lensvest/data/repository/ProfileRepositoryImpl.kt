package ar.edu.uade.lensvest.data.repository

import android.content.Context
import androidx.core.content.edit
import ar.edu.uade.lensvest.data.local.SessionStore
import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.domain.model.UserProfile
import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import ar.edu.uade.lensvest.mapper.toDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val session: SessionStore,
    private val api: LensVestApi,
) : ProfileRepository {
    // Display preferences, nothing secret: plain prefs on this device.
    // ponytail: per device, not per account; move them to the backend if accounts get shared.
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private var level = prefs.getString(LEVEL, null)?.let { runCatching { InvestorLevel.valueOf(it) }.getOrNull() } ?: InvestorLevel.BEGINNER
    private val _sheetMode = MutableStateFlow(
        prefs.getString(SHEET_MODE, null)?.let { runCatching { SheetMode.valueOf(it) }.getOrNull() } ?: SheetMode.SIMPLE,
    )
    private val _plainExplanations = MutableStateFlow(prefs.getBoolean(PLAIN, true))

    override fun userName(): String? = session.userName.value

    override suspend fun currentUser(): UserProfile = api.currentUser().toDomain()

    override fun investorLevel(): InvestorLevel = level

    override fun setInvestorLevel(level: InvestorLevel) {
        this.level = level
        prefs.edit { putString(LEVEL, level.name) }
        // Advanced investors start on the sheet with more numbers.
        setSheetMode(if (level == InvestorLevel.ADVANCED) SheetMode.PRO else SheetMode.SIMPLE)
    }

    override val sheetMode: StateFlow<SheetMode> = _sheetMode.asStateFlow()

    override fun setSheetMode(mode: SheetMode) {
        _sheetMode.value = mode
        prefs.edit { putString(SHEET_MODE, mode.name) }
    }

    override val plainExplanations: StateFlow<Boolean> = _plainExplanations.asStateFlow()

    override fun setPlainExplanations(enabled: Boolean) {
        _plainExplanations.value = enabled
        prefs.edit { putBoolean(PLAIN, enabled) }
    }
}

private const val LEVEL = "level"
private const val SHEET_MODE = "sheet_mode"
private const val PLAIN = "plain_explanations"
