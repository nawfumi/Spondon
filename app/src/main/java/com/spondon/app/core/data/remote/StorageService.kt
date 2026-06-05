package com.spondon.app.core.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.spondon.app.core.common.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage,
) {
    /**
     * Uploads an image to Firebase Storage at the given path.
     * Returns the download URL on success.
     */
    suspend fun uploadImage(path: String, uri: Uri): Resource<String> {
        return try {
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Image upload failed", e)
        }
    }

    /**
     * Uploads a profile avatar for a user.
     */
    suspend fun uploadAvatar(userId: String, uri: Uri): Resource<String> {
        return uploadImage("avatars/$userId/${System.currentTimeMillis()}.jpg", uri)
    }

    /**
     * Uploads a community cover photo.
     */
    suspend fun uploadCommunityCover(communityId: String, uri: Uri): Resource<String> {
        return uploadImage("communities/$communityId/cover_${System.currentTimeMillis()}.jpg", uri)
    }

    /**
     * Uploads an image for a community post.
     */
    suspend fun uploadPostImage(postId: String, uri: Uri): Resource<String> {
        return uploadImage("community_posts/$postId/${System.currentTimeMillis()}.jpg", uri)
    }

    /**
     * Deletes a file at the given storage path.
     */
    suspend fun deleteFile(path: String): Resource<Unit> {
        return try {
            storage.reference.child(path).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "File deletion failed", e)
        }
    }
}