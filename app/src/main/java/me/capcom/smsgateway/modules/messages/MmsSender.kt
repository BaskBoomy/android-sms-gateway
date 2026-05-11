package me.capcom.smsgateway.modules.messages

import android.content.Context
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction

/**
 * Sends a text message as MMS so Korean carriers (SK/KT/LG/MVNO) auto-route it
 * as LMS (Long Message Service) — a single message on the receiver — instead
 * of splitting into N concatenated SMS parts (the default Android behaviour
 * via sendMultipartTextMessage that violates Korean LMS expectations).
 *
 * Backed by klinker41/android-smsmms with `useSystemSending = true`, which
 * routes through Android 21+ public `SmsManager.sendMultimediaMessage` API.
 */
internal object MmsSender {

    /**
     * Send the given text to a single recipient as a text-only MMS. Returns
     * synchronously after the OS-level MmsService accepts the request; actual
     * delivery is async and not currently surfaced here (parity with the
     * existing SMS path, which marks Processed immediately on enqueue success).
     *
     * @param subscriptionId optional SIM subscription id (NOT slot index).
     *   Null lets the OS pick the default SIM. Multi-SIM gateways should
     *   resolve slot → subscriptionId via SubscriptionsHelper before calling.
     * @throws Exception if Transaction throws (caller must handle and mark
     *   ProcessingState.Failed). Mirrors the existing sendSMS try/catch.
     */
    fun sendTextMms(
        context: Context,
        recipient: String,
        text: String,
        subscriptionId: Int? = null,
    ) {
        val settings = Settings().apply {
            useSystemSending = true
            sendLongAsMms = true
            sendLongAsMmsAfter = 1
            deliveryReports = false
            split = false
            stripUnicode = false
            subscriptionId?.let { this.subscriptionId = it }
        }
        val message = Message(text, recipient).apply {
            save = false
        }
        Transaction(context, settings).sendNewMessage(message, Transaction.NO_THREAD_ID)
    }
}
