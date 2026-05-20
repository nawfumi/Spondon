package com.spondon.app.core.ui.i18n

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal that provides the current app language code ("bn" or "en").
 * Default is "bn" (Bangla).
 */
val LocalAppLanguage = compositionLocalOf { "bn" }

/**
 * All user-facing strings in the Spondon app, with Bangla (bn) and English (en) variants.
 * Usage: `S.strings.home` returns the correct translation based on [LocalAppLanguage].
 *
 * For screens: call `val s = S.strings` at the top level of the composable.
 */
object S {

    /** Convenience accessor from any @Composable context */
    val strings: SpondonStrings
        @androidx.compose.runtime.Composable
        get() = if (LocalAppLanguage.current == "bn") Bn else En

    fun of(lang: String): SpondonStrings = if (lang == "bn") Bn else En
}

data class SpondonStrings(
    // ─── Bottom nav ──────────────────────
    val home: String,
    val communities: String,
    val createRequest: String,
    val findDonor: String,
    val profile: String,

    // ─── Home ─────────────────────────────
    val homeTitle: String,
    val urgentRequests: String,
    val viewAll: String,
    val bloodRequests: String,
    val nearbyDonors: String,
    val myCommunities: String,
    val quickActions: String,
    val donateBlood: String,
    val myRequests: String,
    val donationHistory: String,
    val achievements: String,

    // ─── Profile ─────────────────────────
    val myProfile: String,
    val editProfile: String,
    val settings: String,
    val notifications: String,
    val availableToDonate: String,
    val unavailable: String,
    val availableIn: String, // "Available in %d days"
    val quickLinks: String,

    // ─── Edit Profile ───────────────────
    val personalInfo: String,
    val healthInfo: String,
    val location: String,
    val privacy: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val bloodGroup: String,
    val weight: String,
    val weightKg: String,
    val registerAsDonor: String,
    val appearInDonorSearch: String,
    val district: String,
    val selectDistrict: String,
    val upazila: String,
    val selectUpazila: String,
    val showPhoneNumber: String,
    val visibleToMembers: String,
    val saveChanges: String,
    val saving: String,

    // ─── Settings ────────────────────────
    val appearance: String,
    val darkMode: String,
    val darkModeDesc: String,
    val language: String,
    val bangla: String,
    val english: String,
    val notificationSettings: String,
    val newRequests: String,
    val bloodRequestAlerts: String,
    val joinApprovals: String,
    val communityJoinUpdates: String,
    val donationReminders: String,
    val upcomingDonationAlerts: String,
    val adminAlerts: String,
    val adminActionNotifications: String,
    val showPhoneNumberSetting: String,
    val showInDonorSearch: String,
    val appearInSearchResults: String,
    val security: String,
    val biometricLogin: String,
    val biometricDesc: String,
    val biometricNotAvailable: String,
    val account: String,
    val logout: String,
    val logoutDesc: String,
    val logoutConfirm: String,
    val deleteAccount: String,
    val deleteAccountDesc: String,
    val deleteAccountConfirm: String,
    val deleting: String,
    val delete: String,
    val cancel: String,
    val languageChangeRestart: String,

    // ─── Feedback ────────────────────────
    val sendFeedback: String,
    val sendFeedbackDesc: String,
    val feedbackTypeLabel: String,
    val feedbackTypeBug: String,
    val feedbackTypeFeature: String,
    val feedbackTypeComplaint: String,
    val feedbackTypeOther: String,
    val feedbackBodyLabel: String,
    val feedbackBodyPlaceholder: String,
    val feedbackSubmit: String,
    val feedbackSending: String,
    val feedbackSentTitle: String,
    val feedbackSentMessage: String,
    val feedbackInfoText: String,
    val feedbackAutoInfo: String,

    // ─── Support Developer ──────────────
    val supportDeveloper: String,
    val supportDeveloperDesc: String,
    val supportGreeting: String,
    val supportMessage: String,
    val supportButtonText: String,
    val supportFooter: String,


    // ─── Notifications ──────────────────
    val markAllRead: String,
    val noNotifications: String,
    val noNotificationsDesc: String,
    val justNow: String,

    // ─── Auth ────────────────────────────
    val login: String,
    val signUp: String,
    val forgotPassword: String,
    val createAccount: String,
    val password: String,
    val confirmPassword: String,
    val orContinueWith: String,
    val googleSignIn: String,
    val alreadyHaveAccount: String,
    val dontHaveAccount: String,
    val yourLocation: String,
    val step3of3: String,
    val useMyLocation: String,
    val detectingLocation: String,
    val orSelectManually: String,
    val completeSetup: String,
    val creatingAccount: String,
    val privacyNote: String,

    // ─── Community ──────────────────────
    val createCommunity: String,
    val joinCommunity: String,
    val members: String,
    val admin: String,
    val pending: String,
    val approved: String,
    val rejected: String,

    // ─── Requests ────────────────────────
    val critical: String,
    val moderate: String,
    val normal: String,
    val active: String,
    val fulfilled: String,
    val expired: String,
    val cancelled: String,
    val bagsNeeded: String,
    val deadline: String,
    val contactDonor: String,
    val respondToRequest: String,

    // ─── Donor ───────────────────────────
    val searchDonors: String,
    val lastDonation: String,
    val totalDonations: String,
    val callDonor: String,

    // ─── Common ──────────────────────────
    val loading: String,
    val error: String,
    val retry: String,
    val back: String,
    val save: String,
    val confirm: String,
    val search: String,
    val noResults: String,
)

// ──────────────────────────────────────────────────────────────
// Bangla translations
// ──────────────────────────────────────────────────────────────
val Bn = SpondonStrings(
    // Bottom nav
    home = "হোম",
    communities = "কমিউনিটি",
    createRequest = "রক্ত চাই",
    findDonor = "ডোনার খুঁজুন",
    profile = "প্রোফাইল",

    // Home
    homeTitle = "স্পন্দন",
    urgentRequests = "জরুরি অনুরোধ",
    viewAll = "সব দেখুন",
    bloodRequests = "রক্তের অনুরোধ",
    nearbyDonors = "কাছের ডোনার",
    myCommunities = "আমার কমিউনিটি",
    quickActions = "দ্রুত কাজ",
    donateBlood = "রক্ত দিন",
    myRequests = "আমার অনুরোধ",
    donationHistory = "ডোনেশন ইতিহাস",
    achievements = "অর্জন",

    // Profile
    myProfile = "আমার প্রোফাইল",
    editProfile = "প্রোফাইল সম্পাদনা",
    settings = "সেটিংস",
    notifications = "বিজ্ঞপ্তি",
    availableToDonate = "রক্তদানে প্রস্তুত",
    unavailable = "অপ্রস্তুত",
    availableIn = "আরও %d দিন পর প্রস্তুত",
    quickLinks = "দ্রুত লিংক",

    // Edit Profile
    personalInfo = "ব্যক্তিগত তথ্য",
    healthInfo = "স্বাস্থ্য তথ্য",
    location = "অবস্থান",
    privacy = "গোপনীয়তা",
    fullName = "পূর্ণ নাম",
    phone = "ফোন",
    email = "ইমেইল",
    bloodGroup = "রক্তের গ্রুপ",
    weight = "ওজন",
    weightKg = "ওজন (কেজি)",
    registerAsDonor = "ডোনার হিসেবে নিবন্ধন",
    appearInDonorSearch = "ডোনার সার্চে দেখান",
    district = "জেলা",
    selectDistrict = "জেলা নির্বাচন",
    upazila = "উপজেলা",
    selectUpazila = "উপজেলা নির্বাচন",
    showPhoneNumber = "ফোন নম্বর দেখান",
    visibleToMembers = "কমিউনিটি সদস্যদের কাছে দৃশ্যমান",
    saveChanges = "পরিবর্তন সংরক্ষণ",
    saving = "সংরক্ষণ হচ্ছে...",

    // Settings
    appearance = "থিম",
    darkMode = "ডার্ক মোড",
    darkModeDesc = "ডার্ক কালার স্কিম ব্যবহার করুন",
    language = "ভাষা",
    bangla = "বাংলা",
    english = "English",
    notificationSettings = "বিজ্ঞপ্তি",
    newRequests = "নতুন অনুরোধ",
    bloodRequestAlerts = "রক্তের অনুরোধের সতর্কতা",
    joinApprovals = "যোগদান অনুমোদন",
    communityJoinUpdates = "কমিউনিটি যোগদান আপডেট",
    donationReminders = "ডোনেশন রিমাইন্ডার",
    upcomingDonationAlerts = "আসন্ন ডোনেশন সতর্কতা",
    adminAlerts = "অ্যাডমিন সতর্কতা",
    adminActionNotifications = "অ্যাডমিন কার্যকলাপ বিজ্ঞপ্তি",
    showPhoneNumberSetting = "ফোন নম্বর দেখান",
    showInDonorSearch = "ডোনার সার্চে দেখান",
    appearInSearchResults = "সার্চ ফলাফলে দেখান",
    security = "নিরাপত্তা",
    biometricLogin = "বায়োমেট্রিক লগইন",
    biometricDesc = "ফিঙ্গারপ্রিন্ট বা ফেস দিয়ে আনলক করুন",
    biometricNotAvailable = "এই ডিভাইসে বায়োমেট্রিক সমর্থিত নয়",
    account = "অ্যাকাউন্ট",
    logout = "লগআউট",
    logoutDesc = "আপনার অ্যাকাউন্ট থেকে সাইন আউট করুন",
    logoutConfirm = "আপনি কি সাইন আউট করতে চান?",
    deleteAccount = "অ্যাকাউন্ট মুছুন",
    deleteAccountDesc = "আপনার সমস্ত তথ্য স্থায়ীভাবে মুছে ফেলুন",
    deleteAccountConfirm = "এটি আপনার অ্যাকাউন্ট এবং সম্পর্কিত সমস্ত তথ্য স্থায়ীভাবে মুছে ফেলবে। এই কাজটি পূর্বাবস্থায় ফেরানো যাবে না।",
    deleting = "মুছে ফেলা হচ্ছে...",
    delete = "মুছুন",
    cancel = "বাতিল",
    languageChangeRestart = "ভাষা পরিবর্তন হয়েছে। পরিবর্তন কার্যকর হবে।",

    // Feedback
    sendFeedback = "মতামত দিন",
    sendFeedbackDesc = "বাগ রিপোর্ট, ফিচার রিকোয়েস্ট বা মতামত পাঠান",
    feedbackTypeLabel = "ফিডব্যাকের ধরন",
    feedbackTypeBug = "বাগ",
    feedbackTypeFeature = "ফিচার",
    feedbackTypeComplaint = "অভিযোগ",
    feedbackTypeOther = "অন্যান্য",
    feedbackBodyLabel = "বিবরণ",
    feedbackBodyPlaceholder = "আপনার মতামত বিস্তারিত লিখুন...",
    feedbackSubmit = "পাঠান",
    feedbackSending = "পাঠানো হচ্ছে...",
    feedbackSentTitle = "ধন্যবাদ! \uD83D\uDE4F",
    feedbackSentMessage = "আপনার মতামত সফলভাবে পাঠানো হয়েছে। আমরা শীঘ্রই পর্যালোচনা করব।",
    feedbackInfoText = "আপনার মতামত আমাদের অ্যাপটি আরও ভালো করতে সাহায্য করে। বাগ, ফিচার রিকোয়েস্ট বা যেকোনো মতামত জানান।",
    feedbackAutoInfo = "ডিভাইসের তথ্য ও অ্যাপ ভার্সন স্বয়ংক্রিয়ভাবে সংযুক্ত হবে।",

    // Support Developer
    supportDeveloper = "সাপোর্ট করুন",
    supportDeveloperDesc = "এই অ্যাপটি ভালো লাগলে ডেভেলপারকে সাপোর্ট করতে পারেন",
    supportGreeting = "আসসালামু আলাইকুম! \uD83D\uDE4F",
    supportMessage = "স্পন্দন একটি ফ্রি এবং ওপেন-সোর্স অ্যাপ যা রক্তদাতা ও রক্তপ্রার্থীদের সংযুক্ত করতে তৈরি করা হয়েছে। আপনার সাপোর্ট এই অ্যাপটিকে আরও উন্নত করতে এবং নতুন ফিচার যোগ করতে সাহায্য করবে। প্রতিটি ছোট অনুদানও অনেক বড় পরিবর্তন আনতে পারে। ❤\uFE0F",
    supportButtonText = "সাপোর্ট করুন",
    supportFooter = "আপনার সাপোর্টের জন্য আন্তরিক ধন্যবাদ ❤\uFE0F",


    // Notifications
    markAllRead = "সব পড়া হয়েছে",
    noNotifications = "কোনো বিজ্ঞপ্তি নেই",
    noNotificationsDesc = "রক্তের অনুরোধ,\nকমিউনিটি আপডেট এবং আরো\nএখানে দেখতে পাবেন।",
    justNow = "এইমাত্র",

    // Auth
    login = "লগইন",
    signUp = "সাইন আপ",
    forgotPassword = "পাসওয়ার্ড ভুলে গেছেন?",
    createAccount = "অ্যাকাউন্ট তৈরি করুন",
    password = "পাসওয়ার্ড",
    confirmPassword = "পাসওয়ার্ড নিশ্চিত করুন",
    orContinueWith = "অথবা এর সাথে চালিয়ে যান",
    googleSignIn = "Google দিয়ে সাইন ইন",
    alreadyHaveAccount = "ইতিমধ্যে অ্যাকাউন্ট আছে?",
    dontHaveAccount = "অ্যাকাউন্ট নেই?",
    yourLocation = "আপনার অবস্থান",
    step3of3 = "ধাপ ৩/৩ — কাছের ডোনার খুঁজতে সাহায্য করুন",
    useMyLocation = "আমার বর্তমান অবস্থান ব্যবহার করুন",
    detectingLocation = "অবস্থান সনাক্ত হচ্ছে...",
    orSelectManually = "অথবা নিজে নির্বাচন করুন",
    completeSetup = "সেটআপ সম্পন্ন করুন",
    creatingAccount = "অ্যাকাউন্ট তৈরি হচ্ছে...",
    privacyNote = "আপনার সঠিক অবস্থান কখনোই শেয়ার করা হয় না। এটি শুধুমাত্র কাছের রক্তের অনুরোধের সাথে মিলানোর জন্য ব্যবহৃত হয়।",

    // Community
    createCommunity = "কমিউনিটি তৈরি",
    joinCommunity = "কমিউনিটিতে যোগদান",
    members = "সদস্য",
    admin = "অ্যাডমিন",
    pending = "অপেক্ষমান",
    approved = "অনুমোদিত",
    rejected = "প্রত্যাখ্যাত",

    // Requests
    critical = "জরুরি",
    moderate = "মাঝারি",
    normal = "সাধারণ",
    active = "সক্রিয়",
    fulfilled = "পূরণ হয়েছে",
    expired = "মেয়াদ শেষ",
    cancelled = "বাতিল",
    bagsNeeded = "ব্যাগ প্রয়োজন",
    deadline = "সময়সীমা",
    contactDonor = "ডোনারের সাথে যোগাযোগ",
    respondToRequest = "সাড়া দিন",

    // Donor
    searchDonors = "ডোনার খুঁজুন",
    lastDonation = "শেষ ডোনেশন",
    totalDonations = "মোট ডোনেশন",
    callDonor = "ডোনারকে কল করুন",

    // Common
    loading = "লোড হচ্ছে...",
    error = "ত্রুটি",
    retry = "আবার চেষ্টা করুন",
    back = "পেছনে",
    save = "সংরক্ষণ",
    confirm = "নিশ্চিত",
    search = "অনুসন্ধান",
    noResults = "কোনো ফলাফল পাওয়া যায়নি",
)

// ──────────────────────────────────────────────────────────────
// English translations
// ──────────────────────────────────────────────────────────────
val En = SpondonStrings(
    // Bottom nav
    home = "Home",
    communities = "Community",
    createRequest = "Request",
    findDonor = "Find Donor",
    profile = "Profile",

    // Home
    homeTitle = "Spondon",
    urgentRequests = "Urgent Requests",
    viewAll = "View All",
    bloodRequests = "Requests",
    nearbyDonors = "Nearby Donors",
    myCommunities = "My Communities",
    quickActions = "Quick Actions",
    donateBlood = "Donate Blood",
    myRequests = "My Requests",
    donationHistory = "Donation History",
    achievements = "Achievements",

    // Profile
    myProfile = "My Profile",
    editProfile = "Edit Profile",
    settings = "Settings",
    notifications = "Notifications",
    availableToDonate = "Available to Donate",
    unavailable = "Unavailable",
    availableIn = "Available in %d days",
    quickLinks = "Quick Links",

    // Edit Profile
    personalInfo = "Personal Information",
    healthInfo = "Health Information",
    location = "Location",
    privacy = "Privacy",
    fullName = "Full Name",
    phone = "Phone",
    email = "Email",
    bloodGroup = "Blood Group",
    weight = "Weight",
    weightKg = "Weight (kg)",
    registerAsDonor = "Register as Donor",
    appearInDonorSearch = "Appear in donor searches",
    district = "District",
    selectDistrict = "Select District",
    upazila = "Upazila",
    selectUpazila = "Select Upazila",
    showPhoneNumber = "Show Phone Number",
    visibleToMembers = "Visible to community members",
    saveChanges = "Save Changes",
    saving = "Saving...",

    // Settings
    appearance = "Appearance",
    darkMode = "Dark Mode",
    darkModeDesc = "Use dark color scheme",
    language = "Language",
    bangla = "বাংলা (Bangla)",
    english = "English",
    notificationSettings = "Notifications",
    newRequests = "New Requests",
    bloodRequestAlerts = "Blood request alerts",
    joinApprovals = "Join Approvals",
    communityJoinUpdates = "Community join updates",
    donationReminders = "Donation Reminders",
    upcomingDonationAlerts = "Upcoming donation alerts",
    adminAlerts = "Admin Alerts",
    adminActionNotifications = "Admin action notifications",
    showPhoneNumberSetting = "Show Phone Number",
    showInDonorSearch = "Show in Donor Search",
    appearInSearchResults = "Appear in search results",
    security = "Security",
    biometricLogin = "Biometric Login",
    biometricDesc = "Use fingerprint or face to unlock",
    biometricNotAvailable = "Biometric not available on this device",
    account = "Account",
    logout = "Logout",
    logoutDesc = "Sign out of your account",
    logoutConfirm = "Are you sure you want to sign out?",
    deleteAccount = "Delete Account",
    deleteAccountDesc = "Permanently remove your data",
    deleteAccountConfirm = "This will permanently delete your account and all associated data. This action cannot be undone.",
    deleting = "Deleting...",
    delete = "Delete",
    cancel = "Cancel",
    languageChangeRestart = "Language changed. Changes will take effect.",

    // Feedback
    sendFeedback = "Send Feedback",
    sendFeedbackDesc = "Report bugs, request features, or share your thoughts",
    feedbackTypeLabel = "Feedback Type",
    feedbackTypeBug = "Bug",
    feedbackTypeFeature = "Feature",
    feedbackTypeComplaint = "Complaint",
    feedbackTypeOther = "Other",
    feedbackBodyLabel = "Description",
    feedbackBodyPlaceholder = "Describe your feedback in detail...",
    feedbackSubmit = "Submit",
    feedbackSending = "Sending...",
    feedbackSentTitle = "Thank You! \uD83D\uDE4F",
    feedbackSentMessage = "Your feedback has been submitted successfully. We'll review it soon.",
    feedbackInfoText = "Your feedback helps us make the app better. Report bugs, request features, or share any thoughts.",
    feedbackAutoInfo = "Device info and app version will be attached automatically.",

    // Support Developer
    supportDeveloper = "Support Us",
    supportDeveloperDesc = "If you find this app helpful, consider supporting the developer",
    supportGreeting = "Assalamu Alaikum! \uD83D\uDE4F",
    supportMessage = "Spondon is a free, open-source app built to connect blood donors with those in need. Your support helps us keep improving the app and adding new features. Every small contribution can make a big difference. ❤\uFE0F",
    supportButtonText = "Support Now",
    supportFooter = "Thank you so much for your kindness ❤\uFE0F",


    // Notifications
    markAllRead = "Mark all read",
    noNotifications = "No notifications yet",
    noNotificationsDesc = "You'll see alerts for blood requests,\ncommunity updates, and more here.",
    justNow = "Just now",

    // Auth
    login = "Login",
    signUp = "Sign Up",
    forgotPassword = "Forgot Password?",
    createAccount = "Create Account",
    password = "Password",
    confirmPassword = "Confirm Password",
    orContinueWith = "or continue with",
    googleSignIn = "Sign in with Google",
    alreadyHaveAccount = "Already have an account?",
    dontHaveAccount = "Don't have an account?",
    yourLocation = "Your Location",
    step3of3 = "Step 3 of 3 — Help us find donors near you",
    useMyLocation = "Use my current location",
    detectingLocation = "Detecting location...",
    orSelectManually = "or select manually",
    completeSetup = "Complete Setup",
    creatingAccount = "Creating account...",
    privacyNote = "Your exact location is never shared. It is used only for matching you with nearby blood requests.",

    // Community
    createCommunity = "Create Community",
    joinCommunity = "Join Community",
    members = "Members",
    admin = "Admin",
    pending = "Pending",
    approved = "Approved",
    rejected = "Rejected",

    // Requests
    critical = "Critical",
    moderate = "Moderate",
    normal = "Normal",
    active = "Active",
    fulfilled = "Fulfilled",
    expired = "Expired",
    cancelled = "Cancelled",
    bagsNeeded = "bags needed",
    deadline = "Deadline",
    contactDonor = "Contact Donor",
    respondToRequest = "Respond to Request",

    // Donor
    searchDonors = "Search Donors",
    lastDonation = "Last Donation",
    totalDonations = "Total Donations",
    callDonor = "Call Donor",

    // Common
    loading = "Loading...",
    error = "Error",
    retry = "Retry",
    back = "Back",
    save = "Save",
    confirm = "Confirm",
    search = "Search",
    noResults = "No results found",
)
