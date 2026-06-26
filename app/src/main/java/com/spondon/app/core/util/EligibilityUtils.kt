package com.spondon.app.core.util

import com.spondon.app.core.common.Constants
import com.spondon.app.core.domain.model.User
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Shared donor availability / eligibility logic.
 *
 * Extracted from duplicated implementations in RequestViewModel,
 * DonorViewModel, and ProfileViewModel.
 */
object EligibilityUtils {

    /**
     * Returns a pair of (isEligible, cooldownDaysRemaining).
     *
     * @param user the donor user, or null if not loaded.
     * @return `(true, 0)` if eligible now, `(false, remainingDays)` otherwise.
     */
    fun checkAvailability(user: User?): Pair<Boolean, Int> {
        if (user == null) return false to 0
        if (!user.isDonor) return false to 0

        val lastDonation = user.lastDonationDate ?: return true to 0

        val daysSince = TimeUnit.MILLISECONDS.toDays(
            Date().time - lastDonation.time,
        ).toInt()

        val requiredDays = if (user.availabilityOverride) {
            Constants.MIN_OVERRIDE_DAYS
        } else {
            user.donationInterval
        }

        return if (daysSince >= requiredDays) {
            true to 0
        } else {
            false to (requiredDays - daysSince)
        }
    }
}
