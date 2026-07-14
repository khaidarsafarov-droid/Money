package com.example.money.business.sms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import com.example.money.domain.model.business.BusinessSmsRecipient
import com.example.money.domain.model.business.BusinessSmsSendResult
import com.example.money.domain.model.business.BusinessSmsStatus

object BusinessSmsSender {

    fun sendDirect(context: Context, phone: String, message: String): BusinessSmsStatus {
        if (!BusinessSmsPermissions.canSendDirect(context)) {
            return BusinessSmsStatus.FAILED
        }
        val normalized = PhoneNormalizer.normalize(phone) ?: return BusinessSmsStatus.FAILED
        return try {
            val manager = context.getSystemService(SmsManager::class.java)
                ?: @Suppress("DEPRECATION") SmsManager.getDefault()
            val parts = manager.divideMessage(message)
            if (parts.size > 1) {
                manager.sendMultipartTextMessage(normalized, null, parts, null, null)
            } else {
                manager.sendTextMessage(normalized, null, message, null, null)
            }
            BusinessSmsStatus.SENT
        } catch (_: SecurityException) {
            BusinessSmsStatus.FAILED
        } catch (_: Exception) {
            BusinessSmsStatus.FAILED
        }
    }

    fun openSmsApp(context: Context, phone: String, message: String): Boolean {
        val normalized = PhoneNormalizer.normalize(phone) ?: return false
        val uri = Uri.parse("smsto:$normalized")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    fun sendBulk(
        context: Context,
        recipients: List<BusinessSmsRecipient>,
        message: String,
        useDirectSend: Boolean,
    ): BusinessSmsSendResult {
        var sent = 0
        var failed = 0
        var openedApp = 0
        recipients.forEach { recipient ->
            val status = if (useDirectSend && BusinessSmsPermissions.canSendDirect(context)) {
                sendDirect(context, recipient.phone, message)
            } else {
                if (openSmsApp(context, recipient.phone, message)) {
                    BusinessSmsStatus.OPENED_APP
                } else {
                    BusinessSmsStatus.FAILED
                }
            }
            when (status) {
                BusinessSmsStatus.SENT -> sent++
                BusinessSmsStatus.OPENED_APP -> openedApp++
                BusinessSmsStatus.FAILED -> failed++
                BusinessSmsStatus.PENDING -> failed++
            }
        }
        return BusinessSmsSendResult(sentCount = sent, failedCount = failed, openedAppCount = openedApp)
    }
}

object PhoneNormalizer {
    fun normalize(raw: String): String? {
        val digits = raw.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) return null
        return digits.trim()
    }
}

object ContactPhoneResolver {

    fun resolvePhones(context: Context, contactUri: Uri): List<String> {
        if (!BusinessSmsPermissions.canReadContacts(context)) return emptyList()
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
