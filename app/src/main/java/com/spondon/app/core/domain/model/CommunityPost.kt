package com.spondon.app.core.domain.model

import java.util.Date

/**
 * A general-purpose post in the Spondon community feed.
 * Unlike [BloodRequest], this supports free-form text + optional image,
 * similar to a Facebook-style post.
 */
data class CommunityPost(
    val id: String = "",
    val communityId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val pinnedAt: Date? = null,
    val createdAt: Date? = null,
    val likedByIds: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
)

/**
 * A comment on a [CommunityPost].
 */
data class PostComment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val createdAt: Date? = null,
)
