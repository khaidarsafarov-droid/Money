package com.example.money.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactResolver {
    fun resolve(context: Context, uri: Uri): PickedContact? {
        val resolver = context.contentResolver
        resolver.query(
            uri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.PHOTO_URI,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)

            val displayName = cursor.getString(nameIndex)?.trim().orEmpty()
            if (displayName.isBlank()) return null

            val lookupKey = cursor.getString(lookupIndex)?.trim()
                ?: cursor.getLong(idIndex).toString()
            val photoUri = cursor.getString(photoIndex)

            return PickedContact(
                displayName = displayName,
                lookupKey = lookupKey,
                contactUri = uri.toString(),
                photoUri = photoUri,
            )
        }
        return null
    }
}
