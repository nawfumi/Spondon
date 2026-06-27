package com.spondon.app.core.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.spondon.app.core.common.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.scale

@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context,
) {
    /**
     * Compresses an image from a [Uri] before uploading to Firebase Storage.
     *
     * - Downscales to a max dimension of [maxDimension] px (default 1280).
     * - Compresses to JPEG at [quality]% (default 70).
     * - Falls back to raw upload if the image can't be decoded (e.g. non-image file).
     */
    suspend fun uploadImage(path: String, uri: Uri): Resource<String> {
        return try {
            val ref = storage.reference.child(path)

            val compressedBytes = compressImage(uri)
            if (compressedBytes != null) {
                ref.putBytes(compressedBytes).await()
            } else {
                // Fallback: upload raw if compression fails
                ref.putFile(uri).await()
            }

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

    // ── Compression ─────────────────────────────────────────────────

    /**
     * Decodes, downscales and JPEG-compresses an image.
     *
     * @return compressed bytes, or null if the URI can't be decoded.
     */
    private suspend fun compressImage(
        uri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 70,
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 1. Decode bounds only (no memory allocation)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            val origW = options.outWidth
            val origH = options.outHeight
            if (origW <= 0 || origH <= 0) return@withContext null

            // 2. Calculate sub-sample to avoid OOM on very large images
            var inSampleSize = 1
            if (origW > maxDimension || origH > maxDimension) {
                val halfW = origW / 2
                val halfH = origH / 2
                while (halfW / inSampleSize >= maxDimension && halfH / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 3. Decode with sub-sampling
            val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return@withContext null

            // 4. Scale down to maxDimension if still larger
            val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = minOf(
                    maxDimension.toFloat() / bitmap.width,
                    maxDimension.toFloat() / bitmap.height,
                )
                val newW = (bitmap.width * ratio).toInt()
                val newH = (bitmap.height * ratio).toInt()
                bitmap.scale(newW, newH).also {
                    if (it !== bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            // 5. Compress to JPEG
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            scaled.recycle()

            baos.toByteArray()
        } catch (_: Exception) {
            null // Fallback to raw upload
        }
    }
}