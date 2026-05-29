package com.spondon.app.core.data.repository

import android.content.Context
import com.spondon.app.core.data.local.PreferencesManager
import com.spondon.app.core.domain.model.CriteriaGroup
import com.spondon.app.core.domain.model.DeferralType
import com.spondon.app.core.domain.model.EligibilityCriteria
import com.spondon.app.core.domain.model.UserEligibilityProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EligibilityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
) : EligibilityRepository {

    private var cachedCriteria: List<EligibilityCriteria>? = null

    private fun loadCriteriaSync(): List<EligibilityCriteria> {
        cachedCriteria?.let { return it }
        val json = context.assets.open("criteria.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(json)
        val criteria = mutableListOf<EligibilityCriteria>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            criteria.add(
                EligibilityCriteria(
                    id = obj.getString("id"),
                    group = CriteriaGroup.valueOf(obj.getString("group")),
                    label = obj.getString("label"),
                    labelBn = obj.getString("labelBn"),
                    description = obj.getString("description"),
                    descriptionBn = obj.getString("descriptionBn"),
                    deferralType = DeferralType.valueOf(obj.getString("deferralType")),
                    deferralDays = if (obj.isNull("deferralDays")) null else obj.getInt("deferralDays"),
                ),
            )
        }
        cachedCriteria = criteria
        return criteria
    }

    override suspend fun getCriteria(): List<EligibilityCriteria> {
        return loadCriteriaSync()
    }

    override suspend fun evaluateQuiz(answers: Map<String, String>): UserEligibilityProfile {
        val now = System.currentTimeMillis()
        var worstType = DeferralType.ELIGIBLE
        var maxDeferralDays = 0
        var reason: String? = null
        var reasonBn: String? = null

        // Age check
        val age = answers["age"] ?: ""
        if (age == "under_17") {
            worstType = DeferralType.PERMANENT
            reason = "Donors must be at least 17 years old."
            reasonBn = "রক্তদাতাদের বয়স কমপক্ষে ১৭ বছর হতে হবে।"
        }
        if (age == "65_plus") {
            worstType = DeferralType.PERMANENT
            reason = "Donors must be under 65 years old."
            reasonBn = "রক্তদাতাদের বয়স ৬৫ বছরের নিচে হতে হবে।"
        }

        // Weight check
        val weight = answers["weight"] ?: ""
        if (weight == "under_50") {
            worstType = DeferralType.PERMANENT
            reason = "Donors must weigh at least 50 kg (110 lbs)."
            reasonBn = "রক্তদাতাদের ওজন কমপক্ষে ৫০ কেজি হতে হবে।"
        }

        // If already permanent, skip temporary checks
        if (worstType != DeferralType.PERMANENT) {
            // Recent illness
            val recentIllness = answers["recent_illness"] ?: ""
            if (recentIllness == "true") {
                worstType = DeferralType.TEMPORARY
                if (14 > maxDeferralDays) {
                    maxDeferralDays = 14
                    reason = "Temporary deferral due to recent illness. Please wait 14 days."
                    reasonBn = "সাম্প্রতিক অসুস্থতার কারণে সাময়িক স্থগিত। অনুগ্রহ করে ১৪ দিন অপেক্ষা করুন।"
                }
            }

            // Tattoo or piercing
            val tattooPiercing = answers["tattoo_piercing"] ?: ""
            if (tattooPiercing == "true") {
                worstType = DeferralType.TEMPORARY
                if (90 > maxDeferralDays) {
                    maxDeferralDays = 90
                    reason = "Temporary deferral due to recent tattoo or piercing. Please wait 90 days."
                    reasonBn = "সাম্প্রতিক ট্যাটু বা পিয়ার্সিংয়ের কারণে সাময়িক স্থগিত। অনুগ্রহ করে ৯০ দিন অপেক্ষা করুন।"
                }
            }

            // Recent travel
            val recentTravel = answers["recent_travel"] ?: ""
            if (recentTravel == "true") {
                worstType = DeferralType.TEMPORARY
                if (90 > maxDeferralDays) {
                    maxDeferralDays = 90
                    reason = "Temporary deferral due to recent travel to a risk area. Please wait 90 days."
                    reasonBn = "ঝুঁকিপূর্ণ এলাকায় সাম্প্রতিক ভ্রমণের কারণে সাময়িক স্থগিত। অনুগ্রহ করে ৯০ দিন অপেক্ষা করুন।"
                }
            }

            // Pregnancy
            val pregnancy = answers["pregnancy"] ?: ""
            if (pregnancy == "true") {
                worstType = DeferralType.TEMPORARY
                if (180 > maxDeferralDays) {
                    maxDeferralDays = 180
                    reason = "Temporary deferral due to pregnancy or recent childbirth. Please wait 180 days."
                    reasonBn = "গর্ভাবস্থা বা সাম্প্রতিক সন্তান প্রসবের কারণে সাময়িক স্থগিত। অনুগ্রহ করে ১৮০ দিন অপেক্ষা করুন।"
                }
            }

            // Medications with deferral
            val medications = answers["medications"] ?: ""
            if (medications == "aspirin") {
                worstType = DeferralType.TEMPORARY
                if (2 > maxDeferralDays) {
                    maxDeferralDays = 2
                    reason = "Temporary deferral due to aspirin use. Please wait 2 days after last dose."
                    reasonBn = "অ্যাসপিরিন ব্যবহারের কারণে সাময়িক স্থগিত। শেষ ডোজের পর ২ দিন অপেক্ষা করুন।"
                }
            }
            if (medications == "antibiotics") {
                worstType = DeferralType.TEMPORARY
                if (7 > maxDeferralDays) {
                    maxDeferralDays = 7
                    reason = "Temporary deferral due to antibiotic use. Please wait 7 days after completing course."
                    reasonBn = "অ্যান্টিবায়োটিক ব্যবহারের কারণে সাময়িক স্থগিত। কোর্স শেষ হওয়ার ৭ দিন পর আসুন।"
                }
            }
            if (medications == "blood_thinners") {
                worstType = DeferralType.TEMPORARY
                if (30 > maxDeferralDays) {
                    maxDeferralDays = 30
                    reason = "Temporary deferral due to blood thinner use. Please wait 30 days after stopping medication."
                    reasonBn = "রক্ত পাতলাকারী ওষুধ ব্যবহারের কারণে সাময়িক স্থগিত। ওষুধ বন্ধের ৩০ দিন পর আসুন।"
                }
            }
            if (medications == "isotretinoin") {
                worstType = DeferralType.TEMPORARY
                if (30 > maxDeferralDays) {
                    maxDeferralDays = 30
                    reason = "Temporary deferral due to isotretinoin (Accutane) use. Please wait 30 days."
                    reasonBn = "আইসোট্রেটিনোইন ব্যবহারের কারণে সাময়িক স্থগিত। অনুগ্রহ করে ৩০ দিন অপেক্ষা করুন।"
                }
            }
        }

        val deferralEndDate: Long? = when (worstType) {
            DeferralType.TEMPORARY -> now + (maxDeferralDays.toLong() * 24 * 60 * 60 * 1000)
            else -> null
        }

        val profile = UserEligibilityProfile(
            overallStatus = worstType,
            deferralEndDate = deferralEndDate,
            deferralReason = reason,
            deferralReasonBn = reasonBn,
            quizAnswers = answers,
            lastQuizDate = now,
        )

        saveUserProfile(profile)
        return profile
    }

    override suspend fun getUserProfile(): UserEligibilityProfile? {
        val statusStr = preferencesManager.eligibilityStatus.first()
        if (statusStr.isEmpty()) return null

        val status = try {
            DeferralType.valueOf(statusStr)
        } catch (_: IllegalArgumentException) {
            DeferralType.ELIGIBLE
        }

        val deferralEndDate = preferencesManager.deferralEndDate.first()
        val deferralReason = preferencesManager.deferralReason.first().ifEmpty { null }
        val deferralReasonBn = preferencesManager.deferralReasonBn.first().ifEmpty { null }
        val quizAnswersJson = preferencesManager.quizAnswersJson.first()
        val lastQuizDate = preferencesManager.lastQuizDate.first()

        val quizAnswers = if (quizAnswersJson.isNotEmpty()) {
            val jsonObj = JSONObject(quizAnswersJson)
            val map = mutableMapOf<String, String>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObj.getString(key)
            }
            map
        } else {
            emptyMap()
        }

        return UserEligibilityProfile(
            overallStatus = status,
            deferralEndDate = if (deferralEndDate == 0L) null else deferralEndDate,
            deferralReason = deferralReason,
            deferralReasonBn = deferralReasonBn,
            quizAnswers = quizAnswers,
            lastQuizDate = lastQuizDate,
        )
    }

    override suspend fun saveUserProfile(profile: UserEligibilityProfile) {
        preferencesManager.setEligibilityStatus(profile.overallStatus.name)
        preferencesManager.setDeferralEndDate(profile.deferralEndDate ?: 0L)
        preferencesManager.setDeferralReason(profile.deferralReason ?: "")
        preferencesManager.setDeferralReasonBn(profile.deferralReasonBn ?: "")
        preferencesManager.setLastQuizDate(profile.lastQuizDate)

        val answersJson = JSONObject(profile.quizAnswers).toString()
        preferencesManager.setQuizAnswersJson(answersJson)
    }
}
