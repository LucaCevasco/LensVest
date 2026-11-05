package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    fun userName(): String?
    suspend fun currentUser(): UserProfile
    fun investorLevel(): InvestorLevel
    fun setInvestorLevel(level: InvestorLevel)
    val sheetMode: StateFlow<SheetMode>
    fun setSheetMode(mode: SheetMode)
    /** "Explicaciones en criollo" on the asset sheet. */
    val plainExplanations: StateFlow<Boolean>
    fun setPlainExplanations(enabled: Boolean)
}
