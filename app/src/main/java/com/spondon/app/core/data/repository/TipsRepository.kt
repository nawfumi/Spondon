package com.spondon.app.core.data.repository

import com.spondon.app.core.domain.model.DonationTip
import com.spondon.app.core.domain.model.TipCategory

interface TipsRepository {
    suspend fun getAllTips(): List<DonationTip>
    suspend fun getTipsByCategory(category: TipCategory): List<DonationTip>
    fun getTipOfTheDay(): DonationTip
}
