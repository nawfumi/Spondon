package com.spondon.app.core.data.repository

import android.content.Context
import com.spondon.app.core.domain.model.DonationTip
import com.spondon.app.core.domain.model.TipCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TipsRepository {

    private var cachedTips: List<DonationTip>? = null

    private suspend fun ensureLoaded(): List<DonationTip> {
        cachedTips?.let { return it }
        val json = context.assets.open("tips.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(json)
        val tips = mutableListOf<DonationTip>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            tips.add(
                DonationTip(
                    id = obj.getString("id"),
                    category = TipCategory.valueOf(obj.getString("category")),
                    title = obj.getString("title"),
                    titleBn = obj.getString("titleBn"),
                    body = obj.getString("body"),
                    bodyBn = obj.getString("bodyBn"),
                    iconName = obj.getString("iconName"),
                ),
            )
        }
        cachedTips = tips
        return tips
    }

    override suspend fun getAllTips(): List<DonationTip> {
        return ensureLoaded()
    }

    override suspend fun getTipsByCategory(category: TipCategory): List<DonationTip> {
        return ensureLoaded().filter { it.category == category }
    }

    override fun getTipOfTheDay(): DonationTip {
        val tips = cachedTips ?: run {
            val json = context.assets.open("tips.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)
            val loaded = mutableListOf<DonationTip>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                loaded.add(
                    DonationTip(
                        id = obj.getString("id"),
                        category = TipCategory.valueOf(obj.getString("category")),
                        title = obj.getString("title"),
                        titleBn = obj.getString("titleBn"),
                        body = obj.getString("body"),
                        bodyBn = obj.getString("bodyBn"),
                        iconName = obj.getString("iconName"),
                    ),
                )
            }
            cachedTips = loaded
            loaded
        }
        return tips.random()
    }
}
