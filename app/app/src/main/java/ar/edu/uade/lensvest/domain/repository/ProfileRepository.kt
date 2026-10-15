package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.InvestorLevel

interface ProfileRepository {
    fun userName(): String?
    fun investorLevel(): InvestorLevel
    fun setInvestorLevel(level: InvestorLevel)
}
