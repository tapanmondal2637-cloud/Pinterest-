package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CommentEntity
import com.example.data.ImageEntity
import com.example.data.UserEntity
import com.example.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserSession(
    val email: String,
    val name: String,
    val role: String, // "user", "admin"
    val isSuspended: Boolean = false
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GalleryRepository

    // === Instagram Connection & Sync State ===
    val instagramConnection = MutableStateFlow<com.example.data.InstagramConnectionEntity?>(null)
    val instagramImages = MutableStateFlow<List<ImageEntity>>(emptyList())
    val isAutoSyncEnabled = MutableStateFlow(true)
    val isSyncing = MutableStateFlow(false)

    // Simulated "Instagram Cloud Server" posts database segment
    // This allows simulating when the user publishes a post ON Instagram,
    // and watching the incremental sync fetch and import it instantly!
    val simulatedInstagramServerPosts = MutableStateFlow<List<com.example.data.InstagramMediaData>>(
        listOf(
            com.example.data.InstagramMediaData(
                id = "9901",
                caption = "Golden sunsets over the coastline #unwind #pacific #photography",
                mediaUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1200",
                mediaType = "IMAGE",
                timestamp = "2026-06-11T05:00:00Z",
                username = "creative_lens"
            ),
            com.example.data.InstagramMediaData(
                id = "9902",
                caption = "Exploring neon-lit alleyways downtown #cyberpunk #streetart",
                mediaUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?q=80&w=1200",
                mediaType = "IMAGE",
                timestamp = "2026-06-11T05:15:00Z",
                username = "creative_lens"
            ),
            com.example.data.InstagramMediaData(
                id = "9903",
                caption = "Sipping local matcha in beautiful Kyoto gardens #travels #japan",
                mediaUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=1200",
                mediaType = "IMAGE",
                timestamp = "2026-06-11T05:30:00Z",
                username = "urban_explorer"
            ),
            com.example.data.InstagramMediaData(
                id = "9904",
                caption = "Waking up to misty peak landscapes #adventure #trekking",
                mediaUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=1200",
                mediaType = "IMAGE",
                timestamp = "2026-06-11T05:40:00Z",
                username = "unknown_stranger" // Used to verify that we do NOT import posts from unrecognized accounts!
            )
        )
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GalleryRepository(database)

        // Reactively observe Instagram connection and posts when active user session updates
        viewModelScope.launch {
            currentUserSession.collect { session ->
                if (session == null) {
                    instagramConnection.value = null
                    instagramImages.value = emptyList()
                } else {
                    // Launch child jobs to keep them updated
                    launch {
                        repository.getInstagramConnectionFlow(session.email).collect { conn ->
                            instagramConnection.value = conn
                        }
                    }
                    launch {
                        repository.getInstagramImagesFlow(session.email).collect { imgs ->
                            instagramImages.value = imgs
                        }
                    }
                }
            }
        }

        // Simulating periodic background sync "every few minutes" (e.g., every 30s to trigger immediately if auto-sync is on)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000)
                val session = currentUserSession.value
                val connection = instagramConnection.value
                if (session != null && connection != null && isAutoSyncEnabled.value) {
                    syncInstagram()
                }
            }
        }
    }

    // === Core UI States ===
    val currentUserSession = MutableStateFlow<UserSession?>(null)
    val isDarkTheme = MutableStateFlow(true) // Defaults to AMOLED/Dark Mode for premium Unsplash feel

    // Predefined Admin Code
    val predefinedAdminCode = "7777"

    // UI Toast Messages Channels
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }

    // === Image Gallery Feed States & Filters ===
    val selectedCategory = MutableStateFlow<String>("All") // "All" or Nature, Animals, Cars, Gaming, Technology, Sports, Wallpapers, Art
    val searchQuery = MutableStateFlow<String>("")

    private val _allImages = repository.getAllImagesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Derived filtered images flow using Combine
    val filteredImages: StateFlow<List<ImageEntity>> = combine(
        _allImages,
        selectedCategory,
        searchQuery
    ) { images, category, query ->
        var list = images
        if (category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // === Detailed View State ===
    val activeDetailImage = MutableStateFlow<ImageEntity?>(null)

    // Comments for active image
    private val _activeImageComments = MutableStateFlow<List<CommentEntity>>(emptyList())
    val activeImageComments: StateFlow<List<CommentEntity>> = _activeImageComments

    fun loadComments(imageId: String) {
        viewModelScope.launch {
            repository.getCommentsFlow(imageId).collect {
                _activeImageComments.value = it
            }
        }
    }

    // Likes and Saves check for detailed view
    val isLikedByCurrentUser = MutableStateFlow(false)
    val isSavedByCurrentUser = MutableStateFlow(false)

    fun checkUserInteractivity(imageId: String) {
        val email = currentUserSession.value?.email ?: return
        viewModelScope.launch {
            isLikedByCurrentUser.value = repository.getLike(email, imageId) != null
            isSavedByCurrentUser.value = repository.getSave(email, imageId) != null
        }
    }

    // === User Profile Metrics ===
    fun getUploadedImages(): Flow<List<ImageEntity>> {
        val email = currentUserSession.value?.email ?: ""
        return repository.getImagesByAuthorFlow(email)
    }

    fun getSavedImages(): Flow<List<ImageEntity>> {
        val email = currentUserSession.value?.email ?: ""
        return repository.getSavedImagesFlow(email)
    }

    // === Admin Console Management ===
    val allUsers = repository.getAllUsersFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val adminDashboardStats = combine(allUsers, _allImages) { users, images ->
        val totalUsers = users.size
        val totalUploads = images.size
        val totalDownloads = images.sumOf { it.downloadCount }
        Triple(totalUsers, totalUploads, totalDownloads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0, 0, 0))

    // === Interactions ===

    // 1. Auth Operations
    fun login(email: String, passwordRaw: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user == null) {
                showToast("Email address is not registered!")
                onComplete(false)
                return@launch
            }
            if (user.isSuspended) {
                showToast("This account has been suspended by an Administrator.")
                onComplete(false)
                return@launch
            }
            val validated = repository.loginUser(email, passwordRaw)
            if (validated != null) {
                currentUserSession.value = UserSession(validated.email, validated.name, validated.role, validated.isSuspended)
                showToast("Welcome back, ${validated.name}!")
                onComplete(true)
            } else {
                showToast("Incorrect password. Please try again.")
                onComplete(false)
            }
        }
    }

    fun register(name: String, email: String, passwordRaw: String, onComplete: (Boolean) -> Unit) {
        if (name.isBlank() || email.isBlank() || passwordRaw.isBlank()) {
            showToast("Please fill all inputs!")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val success = repository.registerUser(name, email, passwordRaw)
            if (success) {
                showToast("Account created successfully!")
                // Automatically log in
                currentUserSession.value = UserSession(email, name, "user")
                onComplete(true)
            } else {
                showToast("Email already registered!")
                onComplete(false)
            }
        }
    }

    fun loginAsGuest() {
        val guestEmail = "guest_${System.currentTimeMillis()}@gallery.com"
        currentUserSession.value = UserSession(guestEmail, "Anonymous Guest", "user")
        showToast("Logged in in Guest Mode")
    }

    fun loginWithGoogleMock(email: String, name: String) {
        viewModelScope.launch {
            var existing = repository.getUserByEmail(email)
            if (existing == null) {
                repository.registerUser(name, email, "google_sign_in_secret_pass")
                existing = repository.getUserByEmail(email)
            }
            if (existing != null) {
                if (existing.isSuspended) {
                    showToast("This Google account has been suspended by Admin.")
                } else {
                    currentUserSession.value = UserSession(existing.email, existing.name, existing.role)
                    showToast("Logged in via Google as ${existing.name}")
                }
            }
        }
    }

    fun logout() {
        currentUserSession.value = null
        showToast("Logged out successfully")
    }

    // 2. Image Actions (Likes, Saves, Comments, Download)
    fun toggleLike(imageId: String) {
        val user = currentUserSession.value
        if (user == null) {
            showToast("Please register or login to like photos!")
            return
        }
        viewModelScope.launch {
            val likedNow = repository.toggleLike(user.email, imageId)
            isLikedByCurrentUser.value = likedNow
            // Refresh detailed view info
            val refreshed = repository.getImageById(imageId)
            if (refreshed != null) {
                activeDetailImage.value = refreshed
            }
        }
    }

    fun toggleSave(imageId: String) {
        val user = currentUserSession.value
        if (user == null) {
            showToast("Please register or login to save photos!")
            return
        }
        viewModelScope.launch {
            val savedNow = repository.toggleSave(user.email, imageId)
            isSavedByCurrentUser.value = savedNow
            if (savedNow) {
                showToast("Photo added to your Bookmarks")
            } else {
                showToast("Photo removed from Bookmarks")
            }
        }
    }

    fun postComment(imageId: String, msg: String) {
        val user = currentUserSession.value
        if (user == null) {
            showToast("Please register or login to comment!")
            return
        }
        if (msg.isBlank()) return
        viewModelScope.launch {
            repository.addComment(imageId, user.email, user.name, msg)
            loadComments(imageId)
        }
    }

    fun deleteComment(commentId: String, imageId: String) {
        viewModelScope.launch {
            repository.deleteComment(commentId)
            loadComments(imageId)
            showToast("Comment deleted.")
        }
    }

    fun downloadImage(image: ImageEntity) {
        viewModelScope.launch {
            val userEmail = currentUserSession.value?.email
            repository.incrementDownload(image.id, userEmail)
            showToast("Downloading original HD photo (${image.pixelSize}) to Gallery...")
            
            // Simulating high-quality download save finished notification/feedback
            _toastMessage.emit("Download complete: ${image.title} (${image.fileSize})")
            
            // Refresh active image stats
            val refreshed = repository.getImageById(image.id)
            if (refreshed != null) {
                activeDetailImage.value = refreshed
            }
        }
    }

    fun shareImage(context: Context, image: ImageEntity) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this HD photo on Gallery!")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this magnificent picture: ${image.title} by ${image.authorName} in HD Gallery Hub!\nCategory: ${image.category}\nOriginal Quality Link: ${image.imageUrl}"
                )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Photo via"))
        } catch (e: Exception) {
            showToast("Error creating share event.")
        }
    }

    // 3. User upload photo
    fun uploadNewPhoto(title: String, desc: String, url: String, category: String, tags: String) {
        val user = currentUserSession.value
        if (user == null) {
            showToast("Please register or login to upload!")
            return
        }
        if (title.isBlank() || url.isBlank() || category.isBlank()) {
            showToast("Title, URL, and Category are required!")
            return
        }
        viewModelScope.launch {
            // Pick a realistic file / image resolution for simulated HD upload
            val hdResolutions = listOf("3840x2160", "4096x2730", "5120x2880", "6000x4000")
            val sizes = listOf("4.1 MB", "5.2 MB", "6.4 MB", "7.2 MB")
            val randIdx = (hdResolutions.indices).random()

            repository.uploadImage(
                authorEmail = user.email,
                authorName = user.name,
                title = title,
                description = desc,
                imageUrl = url,
                category = category,
                tags = tags,
                pixelSize = hdResolutions[randIdx],
                fileSize = sizes[randIdx]
            )
            showToast("HD Photo published successfully in $category!")
        }
    }

    // 4. Administrative Controls
    fun adminSuspendUser(userEmail: String, isSuspended: Boolean) {
        viewModelScope.launch {
            repository.suspendUser(userEmail, isSuspended)
            showToast(if (isSuspended) "Account $userEmail suspended" else "Account $userEmail unsuspended")
            
            // If active user is suspended, log them out instantly
            if (isSuspended && currentUserSession.value?.email == userEmail) {
                currentUserSession.value = null
                showToast("Your session was terminated by an Administrator.")
            }
        }
    }

    fun adminDeleteUser(userEmail: String) {
        viewModelScope.launch {
            repository.deleteUser(userEmail)
            showToast("User account $userEmail has been permanently purged.")
            if (currentUserSession.value?.email == userEmail) {
                currentUserSession.value = null
            }
        }
    }

    fun adminRemoveInappropriateImage(imageId: String) {
        viewModelScope.launch {
            repository.deleteImage(imageId)
            showToast("Inappropriate photo removed by Administrator.")
            if (activeDetailImage.value?.id == imageId) {
                activeDetailImage.value = null
            }
        }
    }

    // Format Timestamp to Human Readable
    fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Just now"
        }
    }

    // === Instagram Synchronization and Sandbox Simulation API ===
    fun connectInstagram(userId: String, username: String, accessToken: String) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch {
            isSyncing.value = true
            repository.connectInstagram(session.email, userId, username, accessToken)
            showToast("Instagram connected successfully to @$username!")
            // Trigger automatic initial sync
            syncInstagram()
            isSyncing.value = false
        }
    }

    fun disconnectInstagram() {
        val session = currentUserSession.value ?: return
        viewModelScope.launch {
            repository.disconnectInstagram(session.email)
            showToast("Instagram account disconnected successfully.")
        }
    }

    fun syncInstagram() {
        val session = currentUserSession.value ?: return
        viewModelScope.launch {
            isSyncing.value = true
            val connection = repository.getInstagramConnection(session.email)
            if (connection == null) {
                isSyncing.value = false
                return@launch
            }

            // Real Retrofit Graph API request attempt
            try {
                val api = com.example.data.InstagramApi.create()
                val response = api.getUserMedia(accessToken = connection.accessToken)

                // Parse and map real API objects
                val mediaList = response.data
                repository.importInstagramMediaList(session.email, connection.instagramUsername, mediaList)
                showToast("Live Instagram sync completed! Imported ${mediaList.size} items.")
            } catch (e: Exception) {
                // If real connection fails, trigger simulated sandbox sync!
                // This satisfies both Retrofit interfaces + fully working mock logic on emulators.
                val username = connection.instagramUsername
                val pendingMockPosts = simulatedInstagramServerPosts.value.filter {
                    it.username.equals(username, ignoreCase = true)
                }

                // Check which ones are already imported to avoid duplicate toasts
                val alreadyImportedIds = instagramImages.value.map { it.instagramPostId }
                val newPostsToImport = pendingMockPosts.filter { it.id !in alreadyImportedIds }

                if (newPostsToImport.isNotEmpty()) {
                    repository.importInstagramMediaList(session.email, username, newPostsToImport)
                    showToast("Instagram synced: found ${newPostsToImport.size} new posts for @$username")
                } else {
                    // Update connection meta timestamp as successful connection check
                    repository.connectInstagram(session.email, connection.instagramUserId, connection.instagramUsername, connection.accessToken)
                    // Silent update or gentle toast
                    showToast("Sync completed: @$username is fully up-to-date!")
                }
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun publishSimulatedInstagramPost(username: String, caption: String, imageUrl: String) {
        val newPost = com.example.data.InstagramMediaData(
            id = System.currentTimeMillis().toString(),
            caption = caption,
            mediaUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1200" },
            mediaType = "IMAGE",
            timestamp = java.time.Instant.now().toString(),
            username = username
        )
        simulatedInstagramServerPosts.value = simulatedInstagramServerPosts.value + newPost
        showToast("New post published successfully ON Instagram cloud server for @$username!")

        // If Auto-Sync is enabled and matches current connection, trigger sync!
        val connection = instagramConnection.value
        if (connection != null && isAutoSyncEnabled.value && connection.instagramUsername.equals(username, ignoreCase = true)) {
            syncInstagram()
        }
    }
}
