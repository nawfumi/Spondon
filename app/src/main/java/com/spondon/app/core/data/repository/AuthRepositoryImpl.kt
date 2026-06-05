package com.spondon.app.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.domain.model.User
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestoreService: FirestoreService,
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Sign in failed")
            when (val userResult = firestoreService.getUser(uid)) {
                is Resource.Success -> Resource.Success(mapToUser(uid, userResult.data))
                is Resource.Error -> Resource.Error(userResult.message)
                is Resource.Loading -> Resource.Loading
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sign in failed", e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, user: User): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Sign up failed")
            val userData = mapOf(
                "uid" to uid,
                "name" to user.name,
                "phone" to user.phone,
                "email" to email,
                "bloodGroup" to user.bloodGroup,
                "dob" to user.dob,
                "weight" to user.weight,
                "isDonor" to user.isDonor,
                "lastDonationDate" to user.lastDonationDate,
                "donationInterval" to user.donationInterval,
                "availabilityOverride" to false,
                "totalDonations" to 0,
                "communityIds" to emptyList<String>(),
                "district" to user.district,
                "upazila" to user.upazila,
                "isPhoneVisible" to true,
                "badges" to emptyList<String>(),
                "avatarUrl" to "",
                "fcmToken" to "",
                "createdAt" to Date(),
            )
            when (val createResult = firestoreService.createUser(uid, userData)) {
                is Resource.Success -> {
                    // Auto-add to Spondon community
                    autoJoinSpondonCommunity(uid)
                    Resource.Success(user.copy(uid = uid, createdAt = Date()))
                }
                is Resource.Error -> Resource.Error(createResult.message)
                is Resource.Loading -> Resource.Loading
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sign up failed", e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Resource.Error("Google sign in failed")
            val uid = firebaseUser.uid

            // Check if user exists in Firestore
            when (val userResult = firestoreService.getUser(uid)) {
                is Resource.Success -> Resource.Success(mapToUser(uid, userResult.data))
                is Resource.Error -> {
                    // New Google user — create a profile stub
                    val userData = mapOf(
                        "uid" to uid,
                        "name" to (firebaseUser.displayName ?: ""),
                        "email" to (firebaseUser.email ?: ""),
                        "phone" to (firebaseUser.phoneNumber ?: ""),
                        "avatarUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                        "bloodGroup" to "",
                        "isDonor" to false,
                        "totalDonations" to 0,
                        "communityIds" to emptyList<String>(),
                        "district" to "",
                        "upazila" to "",
                        "isPhoneVisible" to true,
                        "badges" to emptyList<String>(),
                        "donationInterval" to 120,
                        "availabilityOverride" to false,
                        "fcmToken" to "",
                        "createdAt" to Date(),
                    )
                    firestoreService.createUser(uid, userData)
                    // Auto-add to Spondon community
                    autoJoinSpondonCommunity(uid)
                    Resource.Success(
                        User(
                            uid = uid,
                            name = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                            createdAt = Date(),
                        )
                    )
                }
                is Resource.Loading -> Resource.Loading
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Google sign in failed", e)
        }
    }

    override suspend fun sendOtp(phoneNumber: String): Resource<String> {
        // OTP sending requires an Activity reference for PhoneAuthProvider.
        // This is handled in the ViewModel/Screen layer via PhoneAuthProvider.verifyPhoneNumber().
        // The repository stores the verification ID once received via callback.
        return Resource.Error("OTP sending must be initiated from UI layer with Activity reference")
    }

    override suspend fun verifyOtp(verificationId: String, code: String): Resource<Boolean> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            auth.signInWithCredential(credential).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "OTP verification failed", e)
        }
    }

    override suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Resource<Boolean> {
        return try {
            auth.signInWithCredential(credential).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Phone sign in failed", e)
        }
    }

    override suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Password reset failed", e)
        }
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun signOut() {
        auth.signOut()
    }

    /**
     * Silently adds a user to the Spondon global community.
     * Never throws — failure is ignored so registration isn't blocked.
     */
    private suspend fun autoJoinSpondonCommunity(userId: String) {
        try {
            val spondonId = firestoreService.getSpondonCommunityId() ?: return
            firestoreService.joinCommunity(spondonId, userId)
        } catch (_: Exception) {
            // Fail silently — user can be added later when they open the community list
        }
    }

    private fun mapToUser(uid: String, data: Map<String, Any>): User {
        return User(
            uid = uid,
            name = data["name"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            email = data["email"] as? String ?: "",
            avatarUrl = data["avatarUrl"] as? String ?: "",
            bloodGroup = data["bloodGroup"] as? String ?: "",
            isDonor = data["isDonor"] as? Boolean ?: false,
            weight = (data["weight"] as? Number)?.toFloat() ?: 0f,
            donationInterval = (data["donationInterval"] as? Number)?.toInt() ?: 120,
            availabilityOverride = data["availabilityOverride"] as? Boolean ?: false,
            totalDonations = (data["totalDonations"] as? Number)?.toInt() ?: 0,
            district = data["district"] as? String ?: "",
            upazila = data["upazila"] as? String ?: "",
            isPhoneVisible = data["isPhoneVisible"] as? Boolean ?: true,
            communityIds = (data["communityIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            badges = (data["badges"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        )
    }
}
