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
    val createdAt: Date? = null,
)
