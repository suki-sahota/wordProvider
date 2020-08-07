package com.example.wordprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log


class WordProvider: ContentProvider() {
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        Log.d(TAG,
            "insert: contentValues.getAsString(CONTENT_PATH) " +
                    "is ${ contentValues?.getAsString(CONTENT_PATH) }")
        Log.d(TAG, "insert: size of mData is ${ mData.size }")
        // We replace mData with an array of size 4 when onCreate is called
        mData.add(contentValues?.getAsString(CONTENT_PATH) ?: "")
        return null
    }

    /*
     * uri
     * projection
     * selection
     * selectionArgs
     * sortOrder
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        var id = 0
        when (uriMatcher.match(uri)) {
            0 -> {
                id = ALL_ITEMS
                if (selection != null) {
                    id = selectionArgs?.get(0)!!.toInt()
                }
            }
            1 -> {
                //content://com.example.wordprovider.provider/words
                id = uri.lastPathSegment?.toIntOrNull() ?: -1
            }
            else -> {
                id = -1
            }
        }
        return populateCursor(id)
    }

    private fun populateCursor(id: Int): Cursor {
        val cursor = MatrixCursor(arrayOf(CONTENT_PATH))
        if (id == ALL_ITEMS) {
            for (element in mData) {
                cursor.addRow(arrayOf<Any>(element))
            }
        } else if (id >= 0) {
            val word = mData.elementAt(id)
            cursor.addRow(arrayOf<Any>(word))
        }
        return cursor
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        return 0
    }

    /*
     * Similar to the activity onCreate
     * To init your DB.
     * Init your Collection
     */
    override fun onCreate(): Boolean {
        context?.resources?.let {
            mData = it.getStringArray(R.array.words).toMutableSet()
        }
        uriMatcher.addURI(AUTHORITY, CONTENT_PATH, 0)
        uriMatcher.addURI(AUTHORITY, "$CONTENT_PATH/#", 1)
        return true
    }

    /*
     * Defines the URI transaction of this CP
     */
    override fun getType(p0: Uri): String? {
        return when (uriMatcher.match(p0)) {
            0 -> SINGLE_RECORD
            1 -> MULTIPLE_RECORD
            else -> throw NullPointerException() // Kotlin exception (distinct from Java)
        }
    }

    private var mData = mutableSetOf<String>()
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    companion object {
        const val AUTHORITY = "com.example.wordprovider.provider"
        const val CONTENT_PATH = "words"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$CONTENT_PATH")
        const val ALL_ITEMS = -2
        const val WORD_ID = "id"
        const val SINGLE_RECORD = "vnd.android.cursor.id/vnd.$AUTHORITY.$CONTENT_PATH"
        const val MULTIPLE_RECORD = "vnd.android.cursor.dir/vnd.$AUTHORITY.$CONTENT_PATH"
    }
}