package me.capcom.smsgateway.modules.messages

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import com.google.android.mms.ContentType
import com.google.android.mms.pdu_alt.CharacterSets
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.PduBody
import com.google.android.mms.pdu_alt.PduComposer
import com.google.android.mms.pdu_alt.PduPart
import com.google.android.mms.pdu_alt.SendReq
import me.capcom.smsgateway.modules.logs.LogsService
import me.capcom.smsgateway.modules.logs.db.LogEntry
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Sends a text message as text-only MMS so Korean carriers (SK/KT/LG/MVNO)
 * auto-route it as LMS (Long Message Service) — a single message on the
 * receiver — instead of splitting into N concatenated SMS parts.
 *
 * Path: build SendReq PDU with PduComposer → write to app cache → expose via
 * FileProvider as content:// URI → call public SmsManager.sendMultimediaMessage.
 *
 * This bypasses the default-SMS-app restriction that klinker41's
 * Transaction(useSystemSending = true) trips on: that path uses PduPersister,
 * which only the user-selected default SMS app may write to. By providing our
 * own contentUri pointing at a FileProvider-served file, the OS MmsService
 * reads the PDU directly without touching the SMS Provider, and any app with
 * SEND_SMS can send.
 */
internal object MmsSender {

    private const val MODULE = "MmsSender"

    fun sendTextMms(
        context: Context,
        recipient: String,
        text: String,
        sentIntent: PendingIntent,
        logs: LogsService,
        smsManager: SmsManager,
    ) {
        logs.insert(
            LogEntry.Priority.INFO,
            MODULE,
            "MMS send start",
            mapOf(
                "recipient" to recipient,
                "textLength" to text.length.toString(),
            ),
        )
        try {
            val pdu = buildTextMmsPdu(context, recipient, text)
            val contentUri = writePduToCache(context, pdu)
            // Grant the OS MMS service read access to our cache file.
            // com.android.phone is the package that hosts MmsService on
            // stock Android; we grant via FLAG to cover OEM variants too.
            context.grantUriPermission(
                "com.android.phone",
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            smsManager.sendMultimediaMessage(
                context,
                contentUri,
                null, // locationUrl: use default MMSC from APN config
                null, // configOverrides
                sentIntent,
            )
            logs.insert(
                LogEntry.Priority.INFO,
                MODULE,
                "MMS handed to OS MmsService",
                mapOf(
                    "recipient" to recipient,
                    "contentUri" to contentUri.toString(),
                    "pduBytes" to pdu.size.toString(),
                ),
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

    private fun buildTextMmsPdu(context: Context, recipient: String, text: String): ByteArray {
        val sendReq = SendReq().apply {
            addTo(EncodedStringValue(recipient))
            messageClass = "personal".toByteArray()
            val body = PduBody()
            val textPart = PduPart().apply {
                contentType = ContentType.TEXT_PLAIN.toByteArray()
                charset = CharacterSets.UTF_8
                contentLocation = "text_0.txt".toByteArray()
                contentId = "<text_0>".toByteArray()
                data = text.toByteArray(Charsets.UTF_8)
            }
            body.addPart(textPart)
            this.body = body
        }
        return PduComposer(context, sendReq).make()
    }

    private fun writePduToCache(context: Context, pdu: ByteArray): android.net.Uri {
        val mmsDir = File(context.cacheDir, "mms")
        if (!mmsDir.exists()) mmsDir.mkdirs()
        val cacheFile = File(mmsDir, "out-${UUID.randomUUID()}.dat")
        FileOutputStream(cacheFile).use { it.write(pdu) }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile,
        )
    }
}
