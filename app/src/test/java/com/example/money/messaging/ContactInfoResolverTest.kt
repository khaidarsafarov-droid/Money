package com.example.money.messaging

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ContactInfoResolverTest {

    @Test
    fun pickedContact_fillsPrimaryPhoneWithoutContactsPermission() {
        val context = RuntimeEnvironment.getApplication()
        val provider = ContactEntityProvider(
            displayName = "Safar. Kv",
            phone = "+998 90 123 45 67",
            secondaryPhone = "+998 91 000 00 00",
            email = "safar@example.com",
        )
        provider.attachInfo(
            context,
            ProviderInfo().apply {
                authority = AUTHORITY
                exported = false
            },
        )
        ShadowContentResolver.registerProviderInternal(AUTHORITY, provider)

        val contact = ContactInfoResolver.resolve(
            context,
            Uri.parse("content://$AUTHORITY/contacts/lookup/safar/5"),
        )

        assertEquals("Safar. Kv", contact?.name)
        assertEquals("+998901234567", contact?.phone)
        assertEquals("safar@example.com", contact?.email)
    }

    @Test
    fun pickContactFields_usesPrimaryPhoneAndSkipsBlankValues() {
        val contact = pickContactFields(
            displayName = " Ann ",
            rows = listOf(
                ContactDataRow(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "  ", false),
                ContactDataRow(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "111", false),
                ContactDataRow(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "+222", true),
                ContactDataRow(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, "a@b.c", false),
            ),
        )

        assertEquals("Ann", contact.name)
        assertEquals("+222", contact.phone)
        assertEquals("a@b.c", contact.email)
    }

    @Test
    fun mergeContactDetails_keepsGrantedPhoneAndFillsMissingEmail() {
        val merged = mergeContactDetails(
            granted = MessagingContact(phone = "+111", email = "", name = "Ann"),
            tables = MessagingContact(phone = "+999", email = "a@b.c", name = ""),
        )

        assertEquals(MessagingContact(phone = "+111", email = "a@b.c", name = "Ann"), merged)
        assertNull(mergeContactDetails(null, MessagingContact()))
    }

    private class ContactEntityProvider(
        private val displayName: String,
        private val phone: String,
        private val secondaryPhone: String,
        private val email: String,
    ) : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? {
            if (!uri.path.orEmpty().endsWith("/entities")) return null
            val cursor = MatrixCursor(
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.Entity.MIMETYPE,
                    ContactsContract.Contacts.Entity.DATA1,
                    ContactsContract.Contacts.Entity.IS_PRIMARY,
                ),
            )
            cursor.addRow(
                arrayOf<Any>(
                    displayName,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    secondaryPhone,
                    0,
                ),
            )
            cursor.addRow(
                arrayOf<Any>(
                    displayName,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    phone,
                    1,
                ),
            )
            cursor.addRow(
                arrayOf<Any>(
                    displayName,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    email,
                    1,
                ),
            )
            return cursor
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private companion object {
        const val AUTHORITY = "com.example.money.test.contacts"
    }
}
