package com.spondon.app.feature.superadmin.developerinfo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.StorageService
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SADeveloperInfoState(
    val name: String = "",
    val subtitle: String = "",
    val profilePhotoUrl: String = "",
    val supportUrl: String = "",
    val facebook: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val linkedin: String = "",
    val github: String = "",
    val twitter: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class SADeveloperInfoViewModel @Inject constructor(
    private val saRepository: SARepository,
    private val storageService: StorageService,
) : ViewModel() {

    private val _state = MutableStateFlow(SADeveloperInfoState())
    val state: StateFlow<SADeveloperInfoState> = _state.asStateFlow()

    init {
        loadInfo()
    }

    private fun loadInfo() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getDeveloperInfo()) {
                is Resource.Success -> {
                    val info = result.data ?: SARepository.DeveloperInfo()
                    _state.update {
                        it.copy(
                            name = info.name,
                            subtitle = info.subtitle,
                            profilePhotoUrl = info.profilePhotoUrl,
                            supportUrl = info.supportUrl,
                            facebook = info.facebook,
                            whatsapp = info.whatsapp,
                            instagram = info.instagram,
                            linkedin = info.linkedin,
                            github = info.github,
                            twitter = info.twitter,
                            isLoading = false,
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun updateName(value: String) { _state.update { it.copy(name = value) } }
    fun updateSubtitle(value: String) { _state.update { it.copy(subtitle = value) } }
    fun updateSupportUrl(value: String) { _state.update { it.copy(supportUrl = value) } }
    fun updateFacebook(value: String) { _state.update { it.copy(facebook = value) } }
    fun updateWhatsapp(value: String) { _state.update { it.copy(whatsapp = value) } }
    fun updateInstagram(value: String) { _state.update { it.copy(instagram = value) } }
    fun updateLinkedin(value: String) { _state.update { it.copy(linkedin = value) } }
    fun updateGithub(value: String) { _state.update { it.copy(github = value) } }
    fun updateTwitter(value: String) { _state.update { it.copy(twitter = value) } }

    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, error = null) }
            when (val result = storageService.uploadImage(
                "developer_info/profile_${System.currentTimeMillis()}.jpg",
                uri,
            )) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            profilePhotoUrl = result.data ?: "",
                            isUploading = false,
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isUploading = false, error = result.message)
                    }
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            val info = SARepository.DeveloperInfo(
                name = s.name,
                subtitle = s.subtitle,
                profilePhotoUrl = s.profilePhotoUrl,
                supportUrl = s.supportUrl,
                facebook = s.facebook,
                whatsapp = s.whatsapp,
                instagram = s.instagram,
                linkedin = s.linkedin,
                github = s.github,
                twitter = s.twitter,
            )
            when (val result = saRepository.saveDeveloperInfo(info)) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false, error = result.message) }
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }
}
