package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    // === Users ===
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY signupDate DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY signupDate DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)

    // === Images ===
    @Query("SELECT * FROM images ORDER BY uploadDate DESC")
    fun getAllImagesFlow(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images ORDER BY uploadDate DESC")
    suspend fun getAllImages(): List<ImageEntity>

    @Query("SELECT * FROM images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: String): ImageEntity?

    @Query("SELECT * FROM images WHERE category = :category ORDER BY uploadDate DESC")
    fun getImagesByCategoryFlow(category: String): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE authorEmail = :email ORDER BY uploadDate DESC")
    fun getImagesByAuthorFlow(email: String): Flow<List<ImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Update
    suspend fun updateImage(image: ImageEntity)

    @Delete
    suspend fun deleteImage(image: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: String)

    // === Likes ===
    @Query("SELECT * FROM likes WHERE email = :email AND imageId = :imageId LIMIT 1")
    suspend fun getLike(email: String, imageId: String): LikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Delete
    suspend fun deleteLike(like: LikeEntity)

    @Query("SELECT * FROM images WHERE id IN (SELECT imageId FROM likes WHERE email = :email) ORDER BY uploadDate DESC")
    fun getLikedImagesFlow(email: String): Flow<List<ImageEntity>>

    // === Saves ===
    @Query("SELECT * FROM saves WHERE email = :email AND imageId = :imageId LIMIT 1")
    suspend fun getSave(email: String, imageId: String): SaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSave(save: SaveEntity)

    @Delete
    suspend fun deleteSave(save: SaveEntity)

    @Query("SELECT * FROM images WHERE id IN (SELECT imageId FROM saves WHERE email = :email) ORDER BY uploadDate DESC")
    fun getSavedImagesFlow(email: String): Flow<List<ImageEntity>>

    // === Comments ===
    @Query("SELECT * FROM comments WHERE imageId = :imageId ORDER BY timestamp ASC")
    fun getCommentsForImageFlow(imageId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteCommentById(id: String)

    @Query("DELETE FROM comments WHERE imageId = :imageId")
    suspend fun buildClearCommentsForImage(imageId: String)
}
