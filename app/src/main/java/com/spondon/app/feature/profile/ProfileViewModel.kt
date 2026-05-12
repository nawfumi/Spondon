package com.spondon.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.StorageService
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ProfileState(
    val user: User? = null,
    val communities: List<Community> = emptyList(),
    val isAvailable: Boolean = false,
    val cooldownDaysRemaining: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)

data class EditProfileState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val bloodGroup: String = "",
    val weight: String = "",
    val district: String = "",
    val upazila: String = "",
    val isDonor: Boolean = true,
    val isPhoneVisible: Boolean = true,
    val isUploadingAvatar: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val communityRepository: CommunityRepository,
    private val storageService: StorageService,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val currentUserId get() = auth.currentUser?.uid ?: ""

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _editState = MutableStateFlow(EditProfileState())
    val editState: StateFlow<EditProfileState> = _editState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }

            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            if (user == null) {
                _profileState.update { it.copy(isLoading = false, error = "Failed to load profile") }
                return@launch
            }

            // Load communities
            val communities = mutableListOf<Community>()
            for (id in user.communityIds.take(20)) {
                val comm = communityRepository.getCommunity(id)
                if (comm is Resource.Success) communities.add(comm.data)
            }

            // Calculate availability
            val (isAvailable, cooldownDays) = checkAvailability(user)

            _profileState.update {
                it.copy(
                    user = user,
                    communities = communities,
                    isAvailable = isAvailable,
                    cooldownDaysRemaining = cooldownDays,
                    isLoading = false,
                )
            }
        }
    }

    fun loadEditProfile() {
        viewModelScope.launch {
            _editState.update { it.copy(isLoading = true, error = null) }

            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            if (user == null) {
                _editState.update { it.copy(isLoading = false, error = "Failed to load profile") }
                return@launch
            }

            _editState.update {
                it.copy(
                    name = user.name,
                    phone = user.phone,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                    bloodGroup = user.bloodGroup,
                    weight = if (user.weight > 0) user.weight.toString() else "",
                    district = user.district,
                    upazila = user.upazila,
                    isDonor = user.isDonor,
                    isPhoneVisible = user.isPhoneVisible,
                    isLoading = false,
                )
            }
        }
    }

    // Edit Profile field updates
    fun updateName(name: String) = _editState.update { it.copy(name = name, error = null) }
    fun updatePhone(phone: String) = _editState.update { it.copy(phone = phone, error = null) }
    fun updateEmail(email: String) = _editState.update { it.copy(email = email, error = null) }
    fun updateWeight(w: String) = _editState.update { it.copy(weight = w, error = null) }
    fun updateDistrict(d: String) = _editState.update { it.copy(district = d, upazila = "", error = null) }
    fun updateUpazila(u: String) = _editState.update { it.copy(upazila = u, error = null) }
    fun toggleDonor() = _editState.update { it.copy(isDonor = !it.isDonor) }
    fun togglePhoneVisible() = _editState.update { it.copy(isPhoneVisible = !it.isPhoneVisible) }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _editState.update { it.copy(isUploadingAvatar = true, error = null) }
            when (val result = storageService.uploadAvatar(currentUserId, uri)) {
                is Resource.Success -> {
                    val url = result.data
                    // Update local state immediately
                    _editState.update { it.copy(avatarUrl = url, isUploadingAvatar = false) }
                    // Persist to Firestore right away (independent of the Save button)
                    val userResult = userRepository.getUser(currentUserId)
                    val existing = (userResult as? Resource.Success)?.data ?: User(uid = currentUserId)
                    userRepository.updateUser(existing.copy(avatarUrl = url))
                    // Also refresh the profile screen state
                    _profileState.update { s ->
                        s.copy(user = s.user?.copy(avatarUrl = url))
                    }
                }
                is Resource.Error -> {
                    _editState.update { it.copy(isUploadingAvatar = false, error = "Avatar upload failed: ${result.message}") }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true, error = null) }

            val s = _editState.value
            val userResult = userRepository.getUser(currentUserId)
            val existing = (userResult as? Resource.Success)?.data ?: User(uid = currentUserId)

            val updated = existing.copy(
                name = s.name,
                phone = s.phone,
                email = s.email,
                avatarUrl = s.avatarUrl,
                weight = s.weight.toFloatOrNull() ?: existing.weight,
                district = s.district,
                upazila = s.upazila,
                isDonor = s.isDonor,
                isPhoneVisible = s.isPhoneVisible,
            )

            when (userRepository.updateUser(updated)) {
                is Resource.Success -> {
                    _editState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is Resource.Error -> {
                    _editState.update { it.copy(isSaving = false, error = "Failed to save changes") }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            onComplete()
        }
    }

    private fun checkAvailability(user: User): Pair<Boolean, Int> {
        if (!user.isDonor) return false to 0
        val lastDonation = user.lastDonationDate ?: return true to 0
        val daysSince = TimeUnit.MILLISECONDS.toDays(Date().time - lastDonation.time).toInt()
        val requiredDays = if (user.availabilityOverride) Constants.MIN_OVERRIDE_DAYS else user.donationInterval
        return if (daysSince >= requiredDays) true to 0 else false to (requiredDays - daysSince)
    }
}
