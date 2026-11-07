package io.github.jaswantk917.chitthi.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone

data class Contact(val name: String, val photo: Uri?, val number: String)

/** Every phone number in the address book, keyed by its last 10 digits. One query, no per-thread lookups. */
object Contacts {
    fun load(context: Context): Map<String, Contact> {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyMap()
        }
        val byNumber = HashMap<String, Contact>()
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME_PRIMARY, Phone.PHOTO_THUMBNAIL_URI),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(0) ?: continue
                val digits = number.filter { it.isDigit() }
                if (digits.length < 7) continue
                val name = c.getString(1) ?: continue
                val existing = byNumber[digits.takeLast(10)]
                if (existing == null || existing.photo == null) {
                    byNumber[digits.takeLast(10)] = Contact(name, c.getString(2)?.let(Uri::parse), number)
                }
            }
        }
        return byNumber
    }
}
