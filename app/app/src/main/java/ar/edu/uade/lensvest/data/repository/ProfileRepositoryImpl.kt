package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.local.SessionStore
import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val session: SessionStore,
) : ProfileRepository {
    // ponytail: kept in memory; persist with the rest of the profile when it reaches the backend.
    private var level = InvestorLevel.BEGINNER

    override fun userName(): String? = session.userName.value

    override fun investorLevel(): InvestorLevel = level

    override fun setInvestorLevel(level: InvestorLevel) {
        this.level = level
    }
}
