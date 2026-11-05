package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.local.SessionStore
import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.domain.model.UserProfile
import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import ar.edu.uade.lensvest.mapper.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val session: SessionStore,
    private val api: LensVestApi,
) : ProfileRepository {
    // ponytail: preferences kept in memory, like the session; persist them together (DataStore) for RNF02.
    private var level = InvestorLevel.BEGINNER
    private val _sheetMode = MutableStateFlow(SheetMode.SIMPLE)
    private val _plainExplanations = MutableStateFlow(true)

    override fun userName(): String? = session.userName.value

    override suspend fun currentUser(): UserProfile = api.currentUser().toDomain()

    override fun investorLevel(): InvestorLevel = level

    override fun setInvestorLevel(level: InvestorLevel) {
        this.level = level
        // Advanced investors start on the sheet with more numbers.
        _sheetMode.value = if (level == InvestorLevel.ADVANCED) SheetMode.PRO else SheetMode.SIMPLE
    }

    override val sheetMode: StateFlow<SheetMode> = _sheetMode.asStateFlow()

    override fun setSheetMode(mode: SheetMode) {
        _sheetMode.value = mode
    }

    override val plainExplanations: StateFlow<Boolean> = _plainExplanations.asStateFlow()

    override fun setPlainExplanations(enabled: Boolean) {
        _plainExplanations.value = enabled
    }
}
