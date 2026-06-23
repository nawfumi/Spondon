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
    data object Permissions : Routes("permissions")

    // Onboarding (new sub-routes)
    data object InitialSetup : Routes("initial_setup")
    data object OnboardingWelcome : Routes("onboarding_welcome")
    data object OnboardingQuiz : Routes("onboarding_quiz")
    data object OnboardingTipsPreview : Routes("onboarding_tips_preview")
    data object OnboardingComplete : Routes("onboarding_complete")

    // Main
    data object Home : Routes("home")

    // Community
    data object CommunityList : Routes("community_list")
    data object CommunityDetail : Routes("community_detail/{communityId}")
    data object CreateCommunity : Routes("create_community")
    data object EditCommunity : Routes("edit_community/{communityId}")
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
    data object TipsLibrary : Routes("tips_library")

    // Profile
    data object Profile : Routes("profile")
    data object EditProfile : Routes("edit_profile")

    // Settings & Notifications
    data object Settings : Routes("settings")
    data object SecuritySettings : Routes("security_settings")
    data object Notifications : Routes("notifications")
    data object NotificationDetail : Routes("notification_detail/{notificationId}")
    data object About : Routes("about")
    data object Support : Routes("support")
    data object SendFeedback : Routes("send_feedback")

    // Spondon Global Community
    data object SpondonCommunity : Routes("spondon_community")
    data object CreateSpondonPost : Routes("create_spondon_post")

    // Info Screens (Members/About/Manage extracted from detail screens)
    data object CommunityInfo : Routes("community_info/{communityId}")
    data object SpondonInfo : Routes("spondon_info")

    // Legal
    data object TermsOfService : Routes("terms_of_service")
    data object PrivacyPolicy : Routes("privacy_policy")
}
