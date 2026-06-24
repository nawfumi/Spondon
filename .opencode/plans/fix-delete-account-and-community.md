# Fix Plan: Delete Account + Community Creation

## Root Causes

### 1. Community Creation — "permission denied"
**File:** `CommunityRepositoryImpl.kt:61`

The Firestore rules say:
```
allow create: if request.auth != null && (
    !request.resource.data.keys().hasAny(['isSerialEnabled'])
    || isSuperAdmin(request.auth.uid)
);
```

The code sends `isSerialEnabled` in the creation data map (line 61). Since the key exists, `hasAny(['isSerialEnabled'])` is `true`, `!true` = `false`, and the rule becomes `false || isSuperAdmin(uid)` — denied for non-SuperAdmins.

**Fix:** Remove `"isSerialEnabled" to community.isSerialEnabled,` from the data map in `createCommunity()`.

### 2. Delete Account — Firestore document not deleted
**File:** `SettingsViewModel.kt:143-157` + Firestore rules

The Firestore rules say `allow delete: if false;` on `/users/{userId}`. This means **no user can ever delete their own user document**. The `firestoreService.deleteUserAccount()` call silently fails (error is returned but ignored), then `auth.currentUser?.delete()` succeeds, user signs out — but the Firestore document persists.

**Fix (two parts):**
- **Rules:** Change `allow delete: if false;` → `allow delete: if request.auth.uid == userId;`
- **Code:** Add error handling so deletion failures are surfaced to the user, with a re-authentication fallback

---

## Changes

### Change 1: `firestore.rules` (NEW FILE — project root)

The only change from the user's existing rules: line in the USERS section.

**Before:**
```
allow delete: if false;
```

**After:**
```
allow delete: if request.auth.uid == userId;
```

Everything else stays identical to what the user already has deployed.

---

### Change 2: `CommunityRepositoryImpl.kt` — Remove `isSerialEnabled` from creation data

**File:** `app/src/main/java/com/spondon/app/core/data/repository/CommunityRepositoryImpl.kt`
**Line:** 61

**Remove this line:**
```kotlin
"isSerialEnabled" to community.isSerialEnabled,
```

The data map in `createCommunity()` should go from:
```kotlin
val data = mapOf<String, Any?>(
    "name" to community.name,
    "description" to community.description,
    "coverUrl" to community.coverUrl,
    "type" to community.type.name,
    "adminIds" to community.adminIds,
    "moderatorIds" to community.moderatorIds,
    "memberIds" to community.memberIds,
    "pendingIds" to community.pendingIds,
    "district" to community.district,
    "upazila" to community.upazila,
    "bloodGroups" to community.bloodGroups,
    "memberCount" to community.memberCount,
    "donationCount" to community.donationCount,
    "isVerified" to community.isVerified,
    "isSerialEnabled" to community.isSerialEnabled,   // ← DELETE THIS LINE
    "createdAt" to Timestamp.now(),
)
```

To:
```kotlin
val data = mapOf<String, Any?>(
    "name" to community.name,
    "description" to community.description,
    "coverUrl" to community.coverUrl,
    "type" to community.type.name,
    "adminIds" to community.adminIds,
    "moderatorIds" to community.moderatorIds,
    "memberIds" to community.memberIds,
    "pendingIds" to community.pendingIds,
    "district" to community.district,
    "upazila" to community.upazila,
    "bloodGroups" to community.bloodGroups,
    "memberCount" to community.memberCount,
    "donationCount" to community.donationCount,
    "isVerified" to community.isVerified,
    "createdAt" to Timestamp.now(),
)
```

---

### Change 3: `SettingsViewModel.kt` — Add re-authentication + error handling

**File:** `app/src/main/java/com/spondon/app/feature/settings/SettingsViewModel.kt`

#### 3a. Update `SettingsState` data class (add 4 fields)

```kotlin
data class SettingsState(
    // ... existing fields ...
    val showDeleteDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    // NEW fields:
    val showReAuthDialog: Boolean = false,
    val reAuthPassword: String = "",
    val reAuthError: String? = null,
    val deleteSuccess: Boolean = false,
)
```

#### 3b. Add imports

```kotlin
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
```

#### 3c. Rewrite `deleteAccount()` method

Replace the existing method (lines 143-157):

```kotlin
fun deleteAccount(onComplete: () -> Unit) {
    viewModelScope.launch {
        _state.update { it.copy(isDeleting = true, error = null) }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.update { it.copy(isDeleting = false, error = "Not logged in") }
            return@launch
        }

        // Step 1: Delete Firestore user document
        val firestoreResult = firestoreService.deleteUserAccount(uid)
        if (firestoreResult is Resource.Error) {
            _state.update {
                it.copy(
                    isDeleting = false,
                    error = "Failed to delete user data: ${firestoreResult.message}",
                )
            }
            return@launch
        }

        // Step 2: Clean up local data
        preferencesManager.clearAll()

        // Step 3: Delete Firebase Auth account
        try {
            auth.currentUser?.delete().await()
            // Success — Auth account deleted
            auth.signOut()
            _state.update {
                it.copy(isDeleting = false, showDeleteDialog = false, deleteSuccess = true)
            }
            onComplete()
        } catch (e: Exception) {
            if (e is FirebaseAuthRecentLoginRequiredException) {
                // Auth requires recent login — show re-auth dialog
                _state.update {
                    it.copy(
                        isDeleting = false,
                        showDeleteDialog = false,
                        showReAuthDialog = true,
                        reAuthError = null,
                    )
                }
            } else {
                // Other error — sign out anyway, user data is already deleted
                auth.signOut()
                _state.update {
                    it.copy(isDeleting = false, showDeleteDialog = false, deleteSuccess = true)
                }
                onComplete()
            }
        }
    }
}
```

#### 3d. Add new methods after `deleteAccount()`

```kotlin
fun updateReAuthPassword(password: String) =
    _state.update { it.copy(reAuthPassword = password, reAuthError = null) }

fun hideReAuthDialog() = _state.update {
    it.copy(showReAuthDialog = false, reAuthPassword = "", reAuthError = null)
}

fun reauthenticateAndDelete(onComplete: () -> Unit) {
    viewModelScope.launch {
        val password = _state.value.reAuthPassword
        if (password.isBlank()) {
            _state.update { it.copy(reAuthError = "Password is required") }
            return@launch
        }

        _state.update { it.copy(isDeleting = true, reAuthError = null) }

        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            _state.update { it.copy(isDeleting = false, reAuthError = "Session expired. Please log in again.") }
            return@launch
        }

        try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isDeleting = false,
                    reAuthError = "Incorrect password. Please try again.",
                )
            }
            return@launch
        }

        // Re-authentication succeeded — retry deletion
        try {
            user.delete().await()
            auth.signOut()
            _state.update {
                it.copy(
                    isDeleting = false,
                    showReAuthDialog = false,
                    reAuthPassword = "",
                    deleteSuccess = true,
                )
            }
            onComplete()
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isDeleting = false,
                    reAuthError = "Deletion failed: ${e.localizedMessage}",
                )
            }
        }
    }
}
```

---

### Change 4: `SettingsScreen.kt` — Add re-authentication dialog

**File:** `app/src/main/java/com/spondon/app/feature/settings/SettingsScreen.kt`

Add a new dialog after the existing delete dialog (after line 222), before the closing `}`:

```kotlin
// ─── Re-authentication dialog (shown when Auth requires recent login) ──
if (state.showReAuthDialog) {
    AlertDialog(
        onDismissRequest = { viewModel.hideReAuthDialog() },
        title = { Text("Re-authenticate", color = BloodRed) },
        text = {
            Column {
                Text(
                    text = "For security, please enter your password to confirm account deletion.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.reAuthPassword,
                    onValueChange = { viewModel.updateReAuthPassword(it) },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.reAuthError != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.reAuthError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.reAuthError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.reauthenticateAndDelete {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                enabled = !state.isDeleting && state.reAuthPassword.isNotBlank(),
            ) {
                Text(if (state.isDeleting) "Deleting..." else "Delete Account", color = UrgencyCritical)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideReAuthDialog() }) { Text("Cancel") }
        },
    )
}
```

Add these imports at the top of `SettingsScreen.kt`:
```kotlin
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
```

---

### Change 5: `UserRepositoryImpl.kt` — Remove dead code

**File:** `app/src/main/java/com/spondon/app/core/data/repository/UserRepositoryImpl.kt`
**Lines:** 67-76

Remove the unused `deleteAccount()` method:

```kotlin
// DELETE THIS ENTIRE METHOD:
override suspend fun deleteAccount(): Resource<Unit> {
    val uid = auth.currentUser?.uid ?: return Resource.Error("Not logged in")
    return try {
        firestoreService.deleteUser(uid)
        auth.currentUser?.delete()
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Account deletion failed", e)
    }
}
```

Also remove `deleteAccount()` from the `UserRepository` interface (`UserRepository.kt:13`):
```kotlin
// DELETE THIS LINE:
suspend fun deleteAccount(): Resource<Unit>
```

---

## Summary

| # | File | Change |
|---|------|--------|
| 1 | `firestore.rules` (new) | `allow delete: if request.auth.uid == userId;` |
| 2 | `CommunityRepositoryImpl.kt:61` | Remove `isSerialEnabled` from creation data map |
| 3 | `SettingsViewModel.kt` | Add re-auth state, rewrite `deleteAccount()`, add `reauthenticateAndDelete()` |
| 4 | `SettingsScreen.kt` | Add re-authentication AlertDialog with password field |
| 5 | `UserRepositoryImpl.kt:67-76` + `UserRepository.kt:13` | Remove dead `deleteAccount()` method |
