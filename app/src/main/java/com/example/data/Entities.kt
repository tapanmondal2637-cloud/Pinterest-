package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String, // email is primary key to prevent duplication
    val name: String,
    val passwordHash: String, // encrypted/hashed password
    val role: String, // "admin" or "user" or "guest"
    val signupDate: Long, // timestamp
    val isSuspended: Boolean = false,
    val uploadCount: Int = 0,
    val downloadCount: Int = 0
)

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey val id: String, // UUID
    val authorEmail: String,
    val authorName: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val category: String, // Nature, Animals, Cars, Gaming, Technology, Sports, Wallpapers, Art
    val tags: String, // Comma separated tags
    val uploadDate: Long,
    val pixelSize: String, // e.g., "3840x2160"
    val fileSize: String, // e.g., "4.2 MB"
    val likeCount: Int = 0,
    val downloadCount: Int = 0,
    val isCustomUploaded: Boolean = false, // Track true uploaded images
    val isInstagramPost: Boolean = false,
    val instagramPostId: String? = null
)

@Entity(tableName = "likes", primaryKeys = ["email", "imageId"])
data class LikeEntity(
    val email: String,
    val imageId: String
)

@Entity(tableName = "saves", primaryKeys = ["email", "imageId"])
data class SaveEntity(
    val email: String,
    val imageId: String
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String, // UUID
    val imageId: String,
    val authorEmail: String,
    val authorName: String,
    val commentText: String,
    val timestamp: Long
)

@Entity(tableName = "instagram_connections")
data class InstagramConnectionEntity(
    @PrimaryKey val userEmail: String,
    val instagramUserId: String,
    val instagramUsername: String,
    val accessToken: String,
    val lastSyncTime: Long,
    val syncStatus: String // "Connected", "Syncing", "Failed", "Disconnected"
)
