package me.capcom.smsgateway.modules.messages

import android.content.Context
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import me.capcom.smsgateway.modules.logs.LogsService
import me.capcom.smsgateway.modules.logs.db.LogEntry

/**
 * Sends a text message as MMS so Korean carriers (SK/KT/LG/MVNO) auto-route it
 * as LMS (Long Message Service) — a single message on the receiver — instead
 * of splitting into N concatenated SMS parts (the default Android behaviour
 * via sendMultipartTextMessage that violates Korean LMS expectations).
 *
 * Backed by klinker41/android-smsmms with `useSystemSending = true`, which
 * routes through Android 21+ public `SmsManager.sendMultimediaMessage` API.
 *
 * IMPORTANT: `Message.save` MUST stay true. Klinker41's `sendMmsThroughSystem`
 * silently returns when `save=false` and no `messageUri` is provided
 * (PduPersister isn't called → contentUri null → swallowed Log.e + no
 * sendMultimediaMessage call). Side effect: outgoing MMS appears in the
 * phone's stock messaging app conversation list. Acceptable for gateway use.
 */
internal object MmsSender {

    private const val MODULE = "MmsSender"

    fun sendTextMms(
        context: Context,
        recipient: String,
        text: String,
        logs: LogsService,
        subscriptionId: Int? = null,
    ) {
        logs.insert(
            LogEntry.Priority.INFO,
            MODULE,
            "MMS send start",
            mapOf(
                "recipient" to recipient,
                "textLength" to text.length.toString(),
                "subscriptionId" to (subscriptionId?.toString() ?: "default"),
            ),
        )
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
            save = true
        }
        try {
            Transaction(context, settings).sendNewMessage(message, Transaction.NO_THREAD_ID)
            logs.insert(
                LogEntry.Priority.INFO,
                MODULE,
                "MMS handed to OS MmsService",
                mapOf("recipient" to recipient),
            )
        } catch (th: Throwable) {
            logs.insert(
                LogEntry.Priority.ERROR,
                MODULE,
                "MMS send threw: ${th.message}",
                mapOf(
                    "recipient" to recipient,
                    "stacktrace" to th.stackTraceToString(),
                ),
            )
            throw th
        }
    }
}
