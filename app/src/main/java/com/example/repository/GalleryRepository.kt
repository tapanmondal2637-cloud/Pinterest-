package com.example.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.CommentEntity
import com.example.data.GalleryDao
import com.example.data.ImageEntity
import com.example.data.LikeEntity
import com.example.data.SaveEntity
import com.example.data.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class GalleryRepository(private val db: AppDatabase) {
    private val dao: GalleryDao = db.galleryDao

    init {
        // Run database seeding inside background dispatcher on startup
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabaseIfEmpty()
        }
    }

    // === Password Hashing Helper (SHA-256) ===
    fun hashPassword(password: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray(Charsets.UTF_8))
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback in case of emergency
        }
    }

    // === Database Seeding ===
    private suspend fun seedDatabaseIfEmpty() {
        val totalUsers = dao.getAllUsers()
        if (totalUsers.isEmpty()) {
            // Seed Admin User
            val adminPasswordHash = hashPassword("admin123")
            val seedAdmin = UserEntity(
                email = "admin@gallery.com",
                name = "Admin Pro",
                passwordHash = adminPasswordHash,
                role = "admin",
                signupDate = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                uploadCount = 0,
                downloadCount = 0
            )
            // Seed Standard User
            val userPasswordHash = hashPassword("user123")
            val seedUser = UserEntity(
                email = "user@gallery.com",
                name = "Tapan Mondal",
                passwordHash = userPasswordHash,
                role = "user",
                signupDate = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                uploadCount = 0,
                downloadCount = 0
            )
            dao.insertUser(seedAdmin)
            dao.insertUser(seedUser)
        }

        val totalImages = dao.getAllImages()
        if (totalImages.isEmpty()) {
            val seedPhotos = listOf(
                // NATURE
                ImageEntity(
                    id = "nature_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Emerald Mountain Valley",
                    description = "A quiet green valley flanked by mist and towering, sun-kissed peaks.",
                    imageUrl = "https://images.unsplash.com/photo-1501854140801-50d01698950b?q=80&w=1200",
                    category = "Nature",
                    tags = "nature,valley,mountains,scenery,mist,scenic",
                    uploadDate = System.currentTimeMillis() - 3600000 * 10,
                    pixelSize = "4000x2667",
                    fileSize = "3.7 MB"
                ),
                ImageEntity(
                    id = "nature_2",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    title = "Winding Forest Creek",
                    description = "Sparkling clean water flows gently through an ancient mossy rainforest.",
                    imageUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=1200",
                    category = "Nature",
                    tags = "creek,forest,river,green,rainforest,serene",
                    uploadDate = System.currentTimeMillis() - 3600000 * 9,
                    pixelSize = "4800x3200",
                    fileSize = "4.9 MB"
                ),
                // ANIMALS
                ImageEntity(
                    id = "animals_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Golden Alert Fox",
                    description = "An elegant wild red fox sits contentedly in a natural golden meadow.",
                    imageUrl = "https://images.unsplash.com/photo-1474511320723-9a56873867b5?q=80&w=1200",
                    category = "Animals",
                    tags = "fox,wildlife,predator,orange,meadow,nature",
                    uploadDate = System.currentTimeMillis() - 3600000 * 8,
                    pixelSize = "3000x2000",
                    fileSize = "2.8 MB"
                ),
                ImageEntity(
                    id = "animals_2",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Majestic Lion Portrait",
                    description = "High-contrast portrait of a male lion displaying an impressive golden mane.",
                    imageUrl = "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=1200",
                    category = "Animals",
                    tags = "lion,wild,bigcat,predator,portrait,mane",
                    uploadDate = System.currentTimeMillis() - 3600000 * 7,
                    pixelSize = "3840x2560",
                    fileSize = "5.1 MB"
                ),
                // CARS
                ImageEntity(
                    id = "cars_1",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    title = "Classic Vintage Porsche 911",
                    description = "A clean, dark metallic vintage sports coupe parked elegantly on a forest roadside.",
                    imageUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?q=80&w=1200",
                    category = "Cars",
                    tags = "porsche,vintage,silver,retro,forest,sports",
                    uploadDate = System.currentTimeMillis() - 3600000 * 6,
                    pixelSize = "3500x2333",
                    fileSize = "3.6 MB"
                ),
                ImageEntity(
                    id = "cars_2",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Midnight Sport Hatchback",
                    description = "Aggressive modern performance sports car parked under neon twilight city lights.",
                    imageUrl = "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?q=80&w=1200",
                    category = "Cars",
                    tags = "supercar,luxury,speed,black,city,neon",
                    uploadDate = System.currentTimeMillis() - 3600000 * 5,
                    pixelSize = "4500x3000",
                    fileSize = "4.8 MB"
                ),
                // GAMING
                ImageEntity(
                    id = "gaming_1",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    title = "Neon Cyberpunk Battlestation",
                    description = "A dazzling desktop PC setup with dual ultrawide screens and intense RGB styling.",
                    imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?q=80&w=1200",
                    category = "Gaming",
                    tags = "gaming,rgb,pc,hardware,setup,cyberpunk",
                    uploadDate = System.currentTimeMillis() - 3600000 * 4,
                    pixelSize = "3840x2160",
                    fileSize = "3.4 MB"
                ),
                ImageEntity(
                    id = "gaming_2",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Ultimate Console Controller",
                    description = "Ergonomic handheld controller displayed against vibrant vaporwave neon tubes.",
                    imageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=1200",
                    category = "Gaming",
                    tags = "joystick,controller,console,neon,synthwave",
                    uploadDate = System.currentTimeMillis() - 3600000 * 3,
                    pixelSize = "4200x2800",
                    fileSize = "3.1 MB"
                ),
                // TECHNOLOGY
                ImageEntity(
                    id = "tech_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Micro-Architecture Silicone",
                    description = "Close-up macro photography of a modern high-power central processing chip.",
                    imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=1200",
                    category = "Technology",
                    tags = "cpu,silicone,chip,motherboard,silicon",
                    uploadDate = System.currentTimeMillis() - 3600000 * 2,
                    pixelSize = "4608x3072",
                    fileSize = "5.5 MB"
                ),
                ImageEntity(
                    id = "tech_2",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    title = "Modern Developer Workplace",
                    description = "High-spec machine illustrating complex lines of IDE code displaying clean syntax.",
                    imageUrl = "https://images.unsplash.com/photo-1488590528505-98d2b5aba04b?q=80&w=1200",
                    category = "Technology",
                    tags = "laptop,code,developer,design,minimal,programming",
                    uploadDate = System.currentTimeMillis() - 3600000 * 1,
                    pixelSize = "3900x2600",
                    fileSize = "3.9 MB"
                ),
                // SPORTS
                ImageEntity(
                    id = "sports_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Sprint Off the Block",
                    description = "Athletic runner in explosive starting posture on an outdoor stadium track.",
                    imageUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?q=80&w=1200",
                    category = "Sports",
                    tags = "run,athlete,sprint,track,power,stadium",
                    uploadDate = System.currentTimeMillis() - 1800000,
                    pixelSize = "4100x2733",
                    fileSize = "4.3 MB"
                ),
                ImageEntity(
                    id = "sports_2",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    title = "Indoor Basketball Session",
                    description = "Crisp visual detailing of a spinning basketball bouncing off a hardwood court.",
                    imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                    category = "Sports",
                    tags = "basketball,court,ball,play,orange,athletic",
                    uploadDate = System.currentTimeMillis() - 1500000,
                    pixelSize = "3600x2400",
                    fileSize = "2.9 MB"
                ),
                // WALLPAPERS
                ImageEntity(
                    id = "wall_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Vivid Painting Splashes",
                    description = "Abstract, modern combination of swirling fluorescent acrylic colors.",
                    imageUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?q=80&w=1200",
                    category = "Wallpapers",
                    tags = "paint,custom,wallpaper,abstract,color,acrylic",
                    uploadDate = System.currentTimeMillis() - 1200000,
                    pixelSize = "3120x4160",
                    fileSize = "4.1 MB"
                ),
                ImageEntity(
                    id = "wall_2",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    title = "Deep Ocean Flow",
                    description = "Liquid marble-like deep blue texture with gold metallic veins.",
                    imageUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=1200",
                    category = "Wallpapers",
                    tags = "fluid,ocean,gold,liquid,marble,abstract",
                    uploadDate = System.currentTimeMillis() - 900000,
                    pixelSize = "3840x2160",
                    fileSize = "3.8 MB"
                ),
                // ART
                ImageEntity(
                    id = "art_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    title = "Artisanal Studio Texture",
                    description = "Messy, inspiring palette board containing multi-layered dried oil colors.",
                    imageUrl = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?q=80&w=1200",
                    category = "Art",
                    tags = "palette,color,brush,canvas,oil,studio",
                    uploadDate = System.currentTimeMillis() - 600000,
                    pixelSize = "3500x2333",
                    fileSize = "3.2 MB"
                ),
                ImageEntity(
                    id = "art_2",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    title = "Vibrant Watercolor Dream",
                    description = "Soft, flowing watercolor splashes rendering an organic cosmic flower form.",
                    imageUrl = "https://images.unsplash.com/photo-1513364776144-60967b0f800f?q=80&w=1200",
                    category = "Art",
                    tags = "watercolor,dream,painting,cosmic,flower",
                    uploadDate = System.currentTimeMillis() - 300000,
                    pixelSize = "4000x3000",
                    fileSize = "4.5 MB"
                )
            )

            for (photo in seedPhotos) {
                dao.insertImage(photo)
            }

            // Update Seeding Statistics for Seed Users
            val adminUser = dao.getUserByEmail("admin@gallery.com")
            if (adminUser != null) {
                dao.updateUser(adminUser.copy(uploadCount = 8))
            }
            val normUser = dao.getUserByEmail("user@gallery.com")
            if (normUser != null) {
                dao.updateUser(normUser.copy(uploadCount = 8))
            }

            // Seed Some Default Comments to Make UI Live and Engaging
            val seedComments = listOf(
                CommentEntity(
                    id = "comment_1",
                    imageId = "nature_1",
                    authorEmail = "user@gallery.com",
                    authorName = "Tapan Mondal",
                    commentText = "Wow! Absolutely breathtaking mountains. The lighting is immaculate.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 30
                ),
                CommentEntity(
                    id = "comment_2",
                    imageId = "nature_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    commentText = "Thank you! Took this on a cool autumn morning.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 20
                ),
                CommentEntity(
                    id = "comment_3",
                    imageId = "cars_1",
                    authorEmail = "admin@gallery.com",
                    authorName = "Admin Pro",
                    commentText = "Ultimate classic! This Porsche look is timeless.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 15
                )
            )
            for (comment in seedComments) {
                dao.insertComment(comment)
            }
        }
    }

    // === User Operations ===
    suspend fun getUserByEmail(email: String): UserEntity? {
        return dao.getUserByEmail(email)
    }

    suspend fun registerUser(name: String, email: String, passwordRaw: String, role: String = "user"): Boolean {
        if (dao.getUserByEmail(email) != null) {
            return false // Email already registered
        }
        val user = UserEntity(
            email = email,
            name = name,
            passwordHash = hashPassword(passwordRaw),
            role = role,
            signupDate = System.currentTimeMillis()
        )
        dao.insertUser(user)
        return true
    }

    suspend fun loginUser(email: String, passwordRaw: String): UserEntity? {
        val user = dao.getUserByEmail(email) ?: return null
        if (user.passwordHash == hashPassword(passwordRaw)) {
            return user
        }
        return null
    }

    suspend fun suspendUser(email: String, isSuspended: Boolean) {
        val user = dao.getUserByEmail(email)
        if (user != null) {
            dao.updateUser(user.copy(isSuspended = isSuspended))
        }
    }

    suspend fun deleteUser(email: String) {
        dao.deleteUserByEmail(email)
    }

    fun getAllUsersFlow(): Flow<List<UserEntity>> {
        return dao.getAllUsersFlow()
    }

    // === Image Operations ===
    fun getAllImagesFlow(): Flow<List<ImageEntity>> {
        return dao.getAllImagesFlow()
    }

    fun getImagesByCategoryFlow(category: String): Flow<List<ImageEntity>> {
        return dao.getImagesByCategoryFlow(category)
    }

    fun getImagesByAuthorFlow(email: String): Flow<List<ImageEntity>> {
        return dao.getImagesByAuthorFlow(email)
    }

    suspend fun getImageById(id: String): ImageEntity? {
        return dao.getImageById(id)
    }

    suspend fun uploadImage(
        authorEmail: String,
        authorName: String,
        title: String,
        description: String,
        imageUrl: String,
        category: String,
        tags: String,
        pixelSize: String = "3840x2160",
        fileSize: String = "4.5 MB"
    ): ImageEntity {
        val newPhoto = ImageEntity(
            id = UUID.randomUUID().toString(),
            authorEmail = authorEmail,
            authorName = authorName,
            title = title,
            description = description,
            imageUrl = imageUrl,
            category = category,
            tags = tags.lowercase().replace(" ", ""),
            uploadDate = System.currentTimeMillis(),
            pixelSize = pixelSize,
            fileSize = fileSize,
            isCustomUploaded = true
        )
        dao.insertImage(newPhoto)

        // Increment upload stats for the user
        val user = dao.getUserByEmail(authorEmail)
        if (user != null) {
            dao.updateUser(user.copy(uploadCount = user.uploadCount + 1))
        }
        return newPhoto
    }

    suspend fun deleteImage(id: String) {
        val image = dao.getImageById(id) ?: return
        dao.deleteImage(image)

        // Decrement user upload stats
        val user = dao.getUserByEmail(image.authorEmail)
        if (user != null && user.uploadCount > 0) {
            dao.updateUser(user.copy(uploadCount = user.uploadCount - 1))
        }
    }

    // === Liked Operations ===
    suspend fun getLike(email: String, imageId: String): LikeEntity? = dao.getLike(email, imageId)

    suspend fun toggleLike(email: String, imageId: String): Boolean {
        val image = dao.getImageById(imageId) ?: return false
        val existingLike = dao.getLike(email, imageId)
        return if (existingLike != null) {
            dao.deleteLike(existingLike)
            dao.updateImage(image.copy(likeCount = maxOf(0, image.likeCount - 1)))
            false
        } else {
            dao.insertLike(LikeEntity(email, imageId))
            dao.updateImage(image.copy(likeCount = image.likeCount + 1))
            true
        }
    }

    fun getLikedImagesFlow(email: String): Flow<List<ImageEntity>> = dao.getLikedImagesFlow(email)

    // === Saved Operations ===
    suspend fun getSave(email: String, imageId: String): SaveEntity? = dao.getSave(email, imageId)

    suspend fun toggleSave(email: String, imageId: String): Boolean {
        val existingSave = dao.getSave(email, imageId)
        return if (existingSave != null) {
            dao.deleteSave(existingSave)
            false
        } else {
            dao.insertSave(SaveEntity(email, imageId))
            true
        }
    }

    fun getSavedImagesFlow(email: String): Flow<List<ImageEntity>> = dao.getSavedImagesFlow(email)

    // === Comments operations ===
    fun getCommentsFlow(imageId: String): Flow<List<CommentEntity>> = dao.getCommentsForImageFlow(imageId)

    suspend fun addComment(imageId: String, authorEmail: String, authorName: String, text: String): CommentEntity {
        val newComment = CommentEntity(
            id = UUID.randomUUID().toString(),
            imageId = imageId,
            authorEmail = authorEmail,
            authorName = authorName,
            commentText = text,
            timestamp = System.currentTimeMillis()
        )
        dao.insertComment(newComment)
        return newComment
    }

    suspend fun deleteComment(commentId: String) {
        dao.deleteCommentById(commentId)
    }

    // === Download Counter ===
    suspend fun incrementDownload(imageId: String, userEmail: String?) {
        val image = dao.getImageById(imageId) ?: return
        dao.updateImage(image.copy(downloadCount = image.downloadCount + 1))

        if (userEmail != null) {
            val user = dao.getUserByEmail(userEmail)
            if (user != null) {
                dao.updateUser(user.copy(downloadCount = user.downloadCount + 1))
            }
        }
    }

    // === Instagram Connection and Sync Support ===
    fun getInstagramConnectionFlow(email: String): kotlinx.coroutines.flow.Flow<com.example.data.InstagramConnectionEntity?> {
        return dao.getInstagramConnectionFlow(email)
    }

    suspend fun getInstagramConnection(email: String): com.example.data.InstagramConnectionEntity? {
        return dao.getInstagramConnection(email)
    }

    suspend fun connectInstagram(email: String, userId: String, username: String, accessToken: String) {
        val connection = com.example.data.InstagramConnectionEntity(
            userEmail = email,
            instagramUserId = userId,
            instagramUsername = username,
            accessToken = accessToken,
            lastSyncTime = System.currentTimeMillis(),
            syncStatus = "Connected"
        )
        dao.insertInstagramConnection(connection)
    }

    suspend fun disconnectInstagram(email: String) {
        dao.deleteInstagramConnection(email)
        dao.clearInstagramImages(email)
    }

    fun getInstagramImagesFlow(email: String): kotlinx.coroutines.flow.Flow<List<ImageEntity>> {
        return dao.getInstagramImagesFlow(email)
    }

    suspend fun importInstagramMediaList(email: String, username: String, mediaList: List<com.example.data.InstagramMediaData>) {
        val currentConnection = dao.getInstagramConnection(email) ?: return

        // Prevent importing content from other accounts:
        // Filter out any media if the publisher's username does not match the actual connected Instagram username
        val validMedia = mediaList.filter { it.username.equals(currentConnection.instagramUsername, ignoreCase = true) }

        for (media in validMedia) {
            val user = dao.getUserByEmail(email)
            val authorName = user?.name ?: "Instagram User"

            // Convert to Local Image Entity
            val localImage = ImageEntity(
                id = "ig_${media.id}",
                authorEmail = email,
                authorName = authorName,
                title = media.caption?.take(60) ?: "Instagram Snap",
                description = media.caption ?: "Imported photo from Instagram (@${media.username})",
                imageUrl = media.mediaUrl,
                category = "Art", // Categorized inside dynamic gallery tab
                tags = "instagram,synced,external,social",
                uploadDate = try {
                    // Instagram ISO-8601 timestamp parse (e.g. 2016-09-28T16:12:12+0000)
                    java.time.Instant.parse(media.timestamp).toEpochMilli()
                } catch(e: Exception) {
                    System.currentTimeMillis()
                },
                pixelSize = "1080x1080",
                fileSize = "1.2 MB",
                isCustomUploaded = false,
                isInstagramPost = true,
                instagramPostId = media.id
            )
            dao.insertImage(localImage)
        }

        // Update connection meta with sync status
        dao.insertInstagramConnection(currentConnection.copy(
            lastSyncTime = System.currentTimeMillis(),
            syncStatus = "Connected"
        ))
    }
}
