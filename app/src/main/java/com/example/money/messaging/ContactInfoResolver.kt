package com.example.money.messaging

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.money.business.sms.PhoneNormalizer

object ContactInfoResolver {

    fun resolve(context: Context, contactUri: Uri): MessagingContact? {
        val resolver = context.contentResolver
        val contactId = resolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0) to cursor.getString(1)?.trim().orEmpty()
            } else {
                null
            }
        } ?: return null

        val (id, name) = contactId
        val phone = resolvePrimaryPhone(resolver, id)
        val email = resolvePrimaryEmail(resolver, id)
        return MessagingContact(phone = phone.orEmpty(), email = email.orEmpty(), name = name)
    }

    fun resolveFromStored(
        phoneField: String,
        emailField: String,
        contactUri: String?,
        context: Context,
    ): MessagingContact {
        val normalizedPhone = PhoneNormalizer.normalize(phoneField)
        val trimmedEmail = emailField.trim()
        if (!normalizedPhone.isNullOrBlank() || trimmedEmail.isNotBlank()) {
            return MessagingContact(phone = normalizedPhone.orEmpty(), email = trimmedEmail)
        }
        if (!contactUri.isNullOrBlank()) {
            resolve(context, Uri.parse(contactUri))?.let { return it }
        }
        return MessagingContact()
    }

    private fun resolvePrimaryPhone(
        resolver: android.content.ContentResolver,
        contactId: Long,
    ): String? {
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC",
        )?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                PhoneNormalizer.normalize(cursor.getString(index))?.let { return it }
            }
        }
        return null
    }

    private fun resolvePrimaryEmail(
        resolver: android.content.ContentResolver,
        contactId: Long,
    ): String? {
        resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            "${ContactsContract.CommonDataKinds.Email.IS_PRIMARY} DESC",
        )?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val email = cursor.getString(index)?.trim()
                if (!email.isNullOrBlank()) return email
            }
        }
        return null
    }
}
