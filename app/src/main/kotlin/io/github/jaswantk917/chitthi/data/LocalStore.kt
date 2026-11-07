package io.github.jaswantk917.chitthi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.jaswantk917.chitthi.model.Category

/**
 * What Chitthi knows on top of the system's SMS store: Gemini Nano's second opinions, the user's
 * own "move to" choices, pins, brand names guessed for unknown headers, and read marks for when
 * Chitthi isn't the default app and so can't write the system's read flag.
 */
class LocalStore(context: Context) : SQLiteOpenHelper(context, "chitthi.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE nano_verdict (msg_key TEXT PRIMARY KEY, category INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE sender_override (sender_key TEXT PRIMARY KEY, category INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE pinned (sender_key TEXT PRIMARY KEY, at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE brand_guess (header TEXT PRIMARY KEY, name TEXT, domain TEXT)")
        db.execSQL("CREATE TABLE read_mark (sender_key TEXT PRIMARY KEY, at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun nanoVerdicts(): Map<String, Category> = readableDatabase
        .rawQuery("SELECT msg_key, category FROM nano_verdict", null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), Category.entries[c.getInt(1)]) }
        }

    fun putNanoVerdict(msgKey: String, category: Category) = upsert(
        "nano_verdict", ContentValues().apply { put("msg_key", msgKey); put("category", category.ordinal) }
    )

    fun overrides(): Map<String, Category> = readableDatabase
        .rawQuery("SELECT sender_key, category FROM sender_override", null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), Category.entries[c.getInt(1)]) }
        }

    fun putOverride(senderKey: String, category: Category) = upsert(
        "sender_override", ContentValues().apply { put("sender_key", senderKey); put("category", category.ordinal) }
    )

    fun pinned(): Set<String> = readableDatabase.rawQuery("SELECT sender_key FROM pinned", null).use { c ->
        buildSet { while (c.moveToNext()) add(c.getString(0)) }
    }

    fun setPinned(senderKey: String, pinned: Boolean) {
        if (pinned) {
            upsert("pinned", ContentValues().apply { put("sender_key", senderKey); put("at", System.currentTimeMillis()) })
        } else {
            writableDatabase.delete("pinned", "sender_key = ?", arrayOf(senderKey))
        }
    }

    /** null name and domain = Nano was asked and didn't know; don't ask again */
    fun brandGuesses(): Map<String, Brand?> = readableDatabase
        .rawQuery("SELECT header, name, domain FROM brand_guess", null).use { c ->
            buildMap {
                while (c.moveToNext()) {
                    val name = c.getString(1)
                    put(c.getString(0), name?.let { Brand(it, c.getString(2).orEmpty()) })
                }
            }
        }

    fun putBrandGuess(header: String, brand: Brand?) = upsert(
        "brand_guess",
        ContentValues().apply { put("header", header); put("name", brand?.name); put("domain", brand?.domain) },
    )

    fun readMarks(): Map<String, Long> = readableDatabase
        .rawQuery("SELECT sender_key, at FROM read_mark", null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getLong(1)) }
        }

    fun putReadMark(senderKey: String, at: Long) = upsert(
        "read_mark", ContentValues().apply { put("sender_key", senderKey); put("at", at) }
    )

    private fun upsert(table: String, values: ContentValues) {
        writableDatabase.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
