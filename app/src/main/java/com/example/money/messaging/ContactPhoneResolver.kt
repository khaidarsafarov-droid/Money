package com.example.money.messaging

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactPhoneResolver {

    fun resolvePhones(context: Context, contactUri: Uri): List<String> {
        if (!SmsPermissions.canReadContacts(context)) return emptyList()
        val resolver = context.contentResolver
        val contactId = resolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        } ?: return emptyList()

        val phones = mutableListOf<String>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                PhoneNormalizer.normalize(cursor.getString(index))?.let { phones.add(it) }
            }
        }
        return phones.distinct()
    }

    fun primaryPhoneForClient(phoneField: String, contactUri: String?, context: Context): String? {
        PhoneNormalizer.normalize(phoneField)?.let { return it }
        if (contactUri.isNullOrBlank()) return null
        return resolvePhones(context, Uri.parse(contactUri)).firstOrNull()
    }
}
