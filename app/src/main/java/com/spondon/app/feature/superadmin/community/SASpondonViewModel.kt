package com.spondon.app.feature.superadmin.community

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.StorageService
import com.spondon.app.core.data.repository.NotificationRepository
import com.spondon.app.core.domain.model.NotificationType
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// ─── Data Models ─────────────────────────────────────────────

data class SASpondonPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val createdAt: Date? = null,
)

data class SASpondonMember(
    val uid: String = "",
    val name: String = "",
    val bloodGroup: String = "",
    val avatarUrl: String = "",
    val role: String = "MEMBER", // ADMIN, MODERATOR, MEMBER
)

// ─── State ───────────────────────────────────────────────────

data class SASpondonState(
    val communityId: String? = null,
    val posts: List<SASpondonPost> = emptyList(),
    val members: List<SASpondonMember> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionMessage: String? = null,

    // Create post
    val showCreateDialog: Boolean = false,
    val createPostContent: String = "",
    val createPostImageUris: List<Uri> = emptyList(),
    val isCreatingPost: Boolean = false,

    // Delete post
    val showDeleteDialog: Boolean = false,
    val postToDelete: SASpondonPost? = null,

    // Member action
    val showMemberActionDialog: Boolean = false,
    val memberToAction: SASpondonMember? = null,
    val memberAction: String = "", // PROMOTE, DEMOTE
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SASpondonViewModel @Inject constructor(
    private val saRepository: SARepository,
    private val storageService: StorageService,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SASpondonState())
    val state: StateFlow<SASpondonState> = _state.asStateFlow()

    init {
        loadSpondonCommunity()
    }

    fun loadSpondonCommunity() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val communityId = saRepository.getSpondonCommunityId()
            if (communityId == null) {
                _state.update {
                    it.copy(isLoading = false, error = "Spondon community not found")
                }
                return@launch
            }

            _state.update { it.copy(communityId = communityId) }

            // Load posts and members in parallel
            val postsResult = saRepository.getSpondonPosts(communityId)
            val membersResult = saRepository.getSpondonMembers(communityId)

            _state.update {
                it.copy(
                    isLoading = false,
                    posts = (postsResult as? Resource.Success)?.data ?: emptyList(),
                    members = (membersResult as? Resource.Success)?.data ?: emptyList(),
                    error = when {
                        postsResult is Resource.Error -> postsResult.message
                        membersResult is Resource.Error -> membersResult.message
                        else -> null
                    },
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Create Post
    // ═══════════════════════════════════════════════════════════

    fun showCreateDialog() = _state.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog() = _state.update {
        it.copy(showCreateDialog = false, createPostContent = "", createPostImageUris = emptyList())
    }

    fun updateCreatePostContent(content: String) =
        _state.update { it.copy(createPostContent = content) }

    fun addCreatePostImageUris(uris: List<Uri>) =
        _state.update { it.copy(createPostImageUris = it.createPostImageUris + uris) }

    fun removeCreatePostImageUri(uri: Uri) =
        _state.update { it.copy(createPostImageUris = it.createPostImageUris - uri) }

    fun createPost() {
        val communityId = _state.value.communityId ?: return
        val content = _state.value.createPostContent
        if (content.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isCreatingPost = true) }
            when (saRepository.createSpondonPost(
                communityId = communityId,
                content = content,
                imageUris = _state.value.createPostImageUris,
                storageService = storageService,
            )) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isCreatingPost = false,
                            showCreateDialog = false,
                            createPostContent = "",
                            createPostImageUris = emptyList(),
                            actionMessage = "Post published!",
                        )
                    }
                    refreshPosts()

                    // Send notification via FCM topic — 1 Firestore doc triggers
                    // the Cloud Function which fans out to all subscribers
                    try {
                        notificationRepository.createNotification(
                            userId = "topic:global_announcements",
                            type = NotificationType.ADMIN,
                            title = "📢 New Spondon Post",
                            body = "Admin posted in Spondon",
                            deepLink = "spondon_community",
                        )
                    } catch (_: Exception) { /* non-critical */ }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isCreatingPost = false, actionMessage = "Failed to create post")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Delete Post
    // ═══════════════════════════════════════════════════════════

    fun showDeleteDialog(post: SASpondonPost) =
        _state.update { it.copy(showDeleteDialog = true, postToDelete = post) }

    fun hideDeleteDialog() =
        _state.update { it.copy(showDeleteDialog = false, postToDelete = null) }

    fun deletePost() {
        val post = _state.value.postToDelete ?: return
        viewModelScope.launch {
            when (saRepository.deleteSpondonPost(post.id)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            showDeleteDialog = false,
                            postToDelete = null,
                            posts = it.posts.filter { p -> p.id != post.id },
                            actionMessage = "Post deleted",
                        )
                    }
                    // Clean up notifications for spondon posts (best-effort)
                    try {
                        notificationRepository.deleteNotificationsByDeepLink("spondon_community")
                    } catch (_: Exception) { /* non-critical */ }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(showDeleteDialog = false, actionMessage = "Failed to delete post")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Member Management
    // ═══════════════════════════════════════════════════════════

    fun showPromoteDialog(member: SASpondonMember) = _state.update {
        it.copy(showMemberActionDialog = true, memberToAction = member, memberAction = "PROMOTE")
    }

    fun showDemoteDialog(member: SASpondonMember) = _state.update {
        it.copy(showMemberActionDialog = true, memberToAction = member, memberAction = "DEMOTE")
    }

    fun hideMemberActionDialog() = _state.update {
        it.copy(showMemberActionDialog = false, memberToAction = null, memberAction = "")
    }

    fun confirmMemberAction() {
        val member = _state.value.memberToAction ?: return
        val communityId = _state.value.communityId ?: return
        val action = _state.value.memberAction

        viewModelScope.launch {
            val result = when (action) {
                "PROMOTE" -> saRepository.promoteSpondonMember(communityId, member.uid)
                "DEMOTE" -> saRepository.demoteSpondonMember(communityId, member.uid)
                else -> return@launch
            }

            when (result) {
                is Resource.Success -> {
                    val msg = if (action == "PROMOTE") "${member.name} promoted to Sub-Admin"
                    else "${member.name} demoted to Member"
                    _state.update {
                        it.copy(
                            showMemberActionDialog = false,
                            memberToAction = null,
                            actionMessage = msg,
                        )
                    }
                    refreshMembers()
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(showMemberActionDialog = false, actionMessage = "Action failed")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Refresh
    // ═══════════════════════════════════════════════════════════

    private fun refreshPosts() {
        val communityId = _state.value.communityId ?: return
        viewModelScope.launch {
            when (val result = saRepository.getSpondonPosts(communityId)) {
                is Resource.Success -> _state.update { it.copy(posts = result.data) }
                else -> {}
            }
        }
    }

    private fun refreshMembers() {
        val communityId = _state.value.communityId ?: return
        viewModelScope.launch {
            when (val result = saRepository.getSpondonMembers(communityId)) {
                is Resource.Success -> _state.update { it.copy(members = result.data) }
                else -> {}
            }
        }
    }

    fun clearActionMessage() = _state.update { it.copy(actionMessage = null) }
}
