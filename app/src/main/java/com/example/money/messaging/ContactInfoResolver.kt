package com.example.money.messaging

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

object ContactInfoResolver {

    /**
     * Reads name, phone, and email for a contact the user just picked.
     * The picker grants access to that contact URI, so the phone is loaded from
     * its entity rows even when READ_CONTACTS has not been granted yet.
     */
    fun resolve(context: Context, contactUri: Uri): MessagingContact? {
        val fromGrantedUri = readGrantedContact(context.contentResolver, contactUri)
        val fromTables = if (SmsPermissions.canReadContacts(context)) {
            readContactTables(context.contentResolver, contactUri)
        } else {
            null
        }
        return mergeContactDetails(fromGrantedUri, fromTables)
    }

    fun resolveFromStored(
        phoneField: String,
        emailField: String,
        contactUri: String?,
        context: Context,
    ): MessagingContact {
        var phone = PhoneNormalizer.normalize(phoneField).orEmpty()
        var email = emailField.trim()
        var name = ""

        if (!contactUri.isNullOrBlank()) {
            resolve(context, Uri.parse(contactUri))?.let { fromContact ->
                if (phone.isBlank()) phone = fromContact.phone
                if (email.isBlank()) email = fromContact.email
                name = fromContact.name
            }
            if (phone.isBlank()) {
                ContactPhoneResolver.primaryPhoneForClient(phoneField, contactUri, context)?.let { phone = it }
            }
        }

        return MessagingContact(phone = phone, email = email, name = name)
    }

    private fun readGrantedContact(resolver: ContentResolver, contactUri: Uri): MessagingContact? {
        val entityUri = Uri.withAppendedPath(
            contactUri,
            ContactsContract.Contacts.Entity.CONTENT_DIRECTORY,
        )
        return runCatching {
            resolver.query(
                entityUri,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.Entity.MIMETYPE,
                    ContactsContract.Contacts.Entity.DATA1,
                    ContactsContract.Contacts.Entity.IS_PRIMARY,
                ),
                null,
                null,
                null,
            )?.use { cursor -> cursor.toContact() }
        }.getOrNull()
    }

    private fun readContactTables(resolver: ContentResolver, contactUri: Uri): MessagingContact? {
        val contact = runCatching {
            resolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getLong(0) to cursor.getString(1)?.trim().orEmpty()
            }
        }.getOrNull() ?: return null

        val (id, name) = contact
        val phone = resolvePrimaryPhone(resolver, id)
        val email = resolvePrimaryEmail(resolver, id)
        if (name.isBlank() && phone.isNullOrBlank() && email.isNullOrBlank()) return null
        return MessagingContact(phone = phone.orEmpty(), email = email.orEmpty(), name = name)
    }

    private fun resolvePrimaryPhone(resolver: ContentResolver, contactId: Long): String? =
        runCatching {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC",
            )?.use { cursor ->
                val index = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    PhoneNormalizer.normalize(cursor.getString(index))?.let { return@use it }
                }
                null
            }
        }.getOrNull()

    private fun resolvePrimaryEmail(resolver: ContentResolver, contactId: Long): String? =
        runCatching {
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
                    if (!email.isNullOrBlank()) return@use email
                }
                null
            }
        }.getOrNull()
}

internal data class ContactDataRow(
    val mimeType: String,
    val value: String?,
    val primary: Boolean,
)

internal fun mergeContactDetails(
    granted: MessagingContact?,
    tables: MessagingContact?,
): MessagingContact? {
    val name = granted?.name?.takeIf { it.isNotBlank() } ?: tables?.name.orEmpty()
    val phone = granted?.phone?.takeIf { it.isNotBlank() } ?: tables?.phone.orEmpty()
    val email = granted?.email?.takeIf { it.isNotBlank() } ?: tables?.email.orEmpty()
    if (name.isBlank() && phone.isBlank() && email.isBlank()) return null
    return MessagingContact(phone = phone, email = email, name = name)
}

internal fun pickContactFields(displayName: String, rows: List<ContactDataRow>): MessagingContact =
    MessagingContact(
        phone = firstContactValue(rows, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            ?.let(PhoneNormalizer::normalize)
            .orEmpty(),
        email = firstContactValue(rows, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
            ?.trim()
            .orEmpty(),
        name = displayName.trim(),
    )

private fun firstContactValue(rows: List<ContactDataRow>, mimeType: String): String? {
    val matches = rows.filter { it.mimeType == mimeType && !it.value.isNullOrBlank() }
    return (matches.firstOrNull { it.primary } ?: matches.firstOrNull())?.value
}

private fun Cursor.toContact(): MessagingContact {
    val nameIndex = getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
    val mimeIndex = getColumnIndex(ContactsContract.Contacts.Entity.MIMETYPE)
    val dataIndex = getColumnIndex(ContactsContract.Contacts.Entity.DATA1)
    val primaryIndex = getColumnIndex(ContactsContract.Contacts.Entity.IS_PRIMARY)
    var name = ""
    val rows = mutableListOf<ContactDataRow>()
    while (moveToNext()) {
        if (name.isBlank() && nameIndex >= 0) {
            name = getString(nameIndex)?.trim().orEmpty()
        }
        if (mimeIndex < 0 || dataIndex < 0) continue
        rows += ContactDataRow(
            mimeType = getString(mimeIndex).orEmpty(),
            value = getString(dataIndex),
            primary = primaryIndex >= 0 && getInt(primaryIndex) == 1,
        )
    }
    return pickContactFields(name, rows)
}
