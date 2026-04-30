package com.spondon.app.navigation

sealed class Routes(val route: String) {
    // Auth
    data object Splash : Routes("splash")
    data object Onboarding : Routes("onboarding")
    data object Login : Routes("login")
    data object SignUp : Routes("signup")
    data object DonorProfileSetup : Routes("donor_profile_setup")
    data object LocationSetup : Routes("location_setup")
    data object Otp : Routes("otp/{phone}")
    data object ForgotPassword : Routes("forgot_password")
    data object PhoneLogin : Routes("phone_login")

    // Main
    data object Home : Routes("home")

    // Community
    data object CommunityList : Routes("community_list")
    data object CommunityDetail : Routes("community_detail/{communityId}")
    data object CreateCommunity : Routes("create_community")
    data object JoinRequest : Routes("join_request/{communityId}")
    data object AdminDashboard : Routes("admin_dashboard/{communityId}")

    // Request
    data object CreateRequest : Routes("create_request")
    data object RequestDetail : Routes("request_detail/{requestId}")
    data object RequestFeed : Routes("request_feed")

    // Donor
    data object FindDonor : Routes("find_donor")
    data object DonorProfile : Routes("donor_profile/{userId}")
    data object DonationHistory : Routes("donation_history")
    data object Achievements : Routes("achievements")

    // Profile
    data object Profile : Routes("profile")
    data object EditProfile : Routes("edit_profile")

    // Settings & Notifications
    data object Settings : Routes("settings")
    data object SecuritySettings : Routes("security_settings")
    data object Notifications : Routes("notifications")
    data object About : Routes("about")
    data object Support : Routes("support")
}