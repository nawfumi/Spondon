package com.spondon.app.feature.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SendFeedbackState(
    val feedbackType: String = "BUG",
    val body: String = "",
    val isSending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SendFeedbackViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _state = MutableStateFlow(SendFeedbackState())
    val state: StateFlow<SendFeedbackState> = _state.asStateFlow()

    fun setType(type: String) = _state.update { it.copy(feedbackType = type) }
    fun setBody(body: String) = _state.update { it.copy(body = body) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun submit() {
        val s = _state.value
        if (s.body.isBlank()) {
            _state.update { it.copy(error = "Please describe your feedback") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            try {
                val uid = auth.currentUser?.uid ?: ""
                // Get user name
                val userName = try {
                    val doc = firestore.collection("users").document(uid).get().await()
                    doc.getString("name") ?: ""
                } catch (_: Exception) { "" }

                // Get device info
                val deviceModel = android.os.Build.MODEL
                val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
                val appVersion = try {
                    com.spondon.app.BuildConfig.VERSION_NAME
                } catch (_: Exception) { "" }

                val feedbackData = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "userName" to userName,
                    "type" to s.feedbackType,
                    "body" to s.body.trim(),
                    "screenshotUrl" to null,
                    "appVersion" to appVersion,
                    "deviceModel" to deviceModel,
                    "osVersion" to osVersion,
                    "status" to "UNREAD",
                    "createdAt" to FieldValue.serverTimestamp(),
                )

                firestore.collection("feedback").add(feedbackData).await()
                _state.update { it.copy(isSending = false, sent = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false, error = e.message ?: "Failed to send feedback") }
            }
        }
    }
}
