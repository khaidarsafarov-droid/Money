package com.example.money.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager

object SmsSender {

    fun sendDirect(context: Context, phone: String, message: String): MessageSendResult {
        if (!SmsPermissions.canSendDirect(context)) {
            return MessageSendResult.FAILED
        }
        val normalized = PhoneNormalizer.normalize(phone) ?: return MessageSendResult.FAILED
        return try {
            val manager = context.getSystemService(SmsManager::class.java)
                ?: @Suppress("DEPRECATION") SmsManager.getDefault()
            val parts = manager.divideMessage(message)
            if (parts.size > 1) {
                manager.sendMultipartTextMessage(normalized, null, parts, null, null)
            } else {
                manager.sendTextMessage(normalized, null, message, null, null)
            }
            MessageSendResult.SENT
        } catch (_: SecurityException) {
            MessageSendResult.FAILED
        } catch (_: Exception) {
            MessageSendResult.FAILED
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
}
