package com.example.commingsoon.ui.screens.localopenguesser

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "local_openguesser_photo_index.db"
private const val DATABASE_VERSION = 1
private const val TABLE_PHOTOS = "photos"
private const val COL_MEDIA_ID = "media_id"
private const val COL_DATE_MODIFIED = "date_modified"
private const val COL_SIZE = "size"
private const val COL_LATITUDE = "latitude"
private const val COL_LONGITUDE = "longitude"
private const val COL_COUNTRY = "country"
private const val COL_UNREADABLE = "unreadable"
private const val COL_INDEX_VERSION = "index_version"

/** Increment when EXIF extraction or offline country data semantics change. */
internal const val PHOTO_INDEX_VERSION = 1

internal data class IndexedPhoto(
    val mediaId: Long,
    val dateModified: Long,
    val size: Long,
    val latitude: Double?,
    val longitude: Double?,
    val country: String?,
    val unreadable: Boolean,
    val indexVersion: Int = PHOTO_INDEX_VERSION
) {
    fun matches(dateModified: Long, size: Long): Boolean =
        this.dateModified == dateModified &&
            this.size == size &&
            indexVersion == PHOTO_INDEX_VERSION
}

internal class PhotoIndexDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_PHOTOS (
                $COL_MEDIA_ID INTEGER PRIMARY KEY,
                $COL_DATE_MODIFIED INTEGER NOT NULL,
                $COL_SIZE INTEGER NOT NULL,
                $COL_LATITUDE REAL,
                $COL_LONGITUDE REAL,
                $COL_COUNTRY TEXT,
                $COL_UNREADABLE INTEGER NOT NULL,
                $COL_INDEX_VERSION INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL("DROP TABLE IF EXISTS $TABLE_PHOTOS")
        onCreate(database)
    }

    fun readAll(): Map<Long, IndexedPhoto> {
        val photos = mutableMapOf<Long, IndexedPhoto>()
        readableDatabase.query(TABLE_PHOTOS, null, null, null, null, null, null).use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(COL_MEDIA_ID)
            val modifiedColumn = cursor.getColumnIndexOrThrow(COL_DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(COL_SIZE)
            val latitudeColumn = cursor.getColumnIndexOrThrow(COL_LATITUDE)
            val longitudeColumn = cursor.getColumnIndexOrThrow(COL_LONGITUDE)
            val countryColumn = cursor.getColumnIndexOrThrow(COL_COUNTRY)
            val unreadableColumn = cursor.getColumnIndexOrThrow(COL_UNREADABLE)
            val versionColumn = cursor.getColumnIndexOrThrow(COL_INDEX_VERSION)
            while (cursor.moveToNext()) {
                val photo = IndexedPhoto(
                    mediaId = cursor.getLong(idColumn),
                    dateModified = cursor.getLong(modifiedColumn),
                    size = cursor.getLong(sizeColumn),
                    latitude = cursor.getNullableDouble(latitudeColumn),
                    longitude = cursor.getNullableDouble(longitudeColumn),
                    country = cursor.getNullableString(countryColumn),
                    unreadable = cursor.getInt(unreadableColumn) != 0,
                    indexVersion = cursor.getInt(versionColumn)
                )
                photos[photo.mediaId] = photo
            }
        }
        return photos
    }

    fun applyChanges(changedPhotos: List<IndexedPhoto>, activeMediaIds: Set<Long>) {
        val database = writableDatabase
        database.beginTransaction()
        try {
            val deleteStatement = database.compileStatement(
                "DELETE FROM $TABLE_PHOTOS WHERE $COL_MEDIA_ID = ?"
            )
            readAll().keys.asSequence()
                .filterNot(activeMediaIds::contains)
                .forEach { staleId ->
                    deleteStatement.clearBindings()
                    deleteStatement.bindLong(1, staleId)
                    deleteStatement.executeUpdateDelete()
                }

            val insertStatement = database.compileStatement(
                """
                INSERT OR REPLACE INTO $TABLE_PHOTOS (
                    $COL_MEDIA_ID, $COL_DATE_MODIFIED, $COL_SIZE, $COL_LATITUDE,
                    $COL_LONGITUDE, $COL_COUNTRY, $COL_UNREADABLE, $COL_INDEX_VERSION
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
            changedPhotos.forEach { photo ->
                insertStatement.clearBindings()
                insertStatement.bindLong(1, photo.mediaId)
                insertStatement.bindLong(2, photo.dateModified)
                insertStatement.bindLong(3, photo.size)
                insertStatement.bindNullableDouble(4, photo.latitude)
                insertStatement.bindNullableDouble(5, photo.longitude)
                insertStatement.bindNullableString(6, photo.country)
                insertStatement.bindLong(7, if (photo.unreadable) 1 else 0)
                insertStatement.bindLong(8, photo.indexVersion.toLong())
                insertStatement.executeInsert()
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}

private fun android.database.Cursor.getNullableDouble(column: Int): Double? =
    if (isNull(column)) null else getDouble(column)

private fun android.database.Cursor.getNullableString(column: Int): String? =
    if (isNull(column)) null else getString(column)

private fun android.database.sqlite.SQLiteStatement.bindNullableDouble(index: Int, value: Double?) {
    if (value == null) bindNull(index) else bindDouble(index, value)
}

private fun android.database.sqlite.SQLiteStatement.bindNullableString(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindString(index, value)
}
