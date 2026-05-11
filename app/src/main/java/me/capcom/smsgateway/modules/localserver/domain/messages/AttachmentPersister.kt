package me.capcom.smsgateway.modules.localserver.domain.messages

import android.content.Context
import android.util.Base64
import me.capcom.smsgateway.domain.MessageContent
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Persist incoming Attachment (base64 + contentType + filename) to app cache
 * directory and return MessageContent.AttachmentRef list referencing the cache
 * files. Persisting avoids bloating Room rows with multi-MB byte blobs while
 * still keeping enqueued messages durable (cache survives across worker runs
 * until the OS clears it; SendMessagesWorker re-reads paths on each attempt).
 *
 * Caller is responsible for cleanup after final delivery state — MessagesService
 * deletes the files in the SENT / FAILED finalization path.
 */
object AttachmentPersister {
    fun persist(
        context: Context,
        attachments: List<Attachment>,
    ): List<MessageContent.AttachmentRef> {
        if (attachments.isEmpty()) return emptyList()
        val dir = File(context.cacheDir, "mms-attachments")
        if (!dir.exists()) dir.mkdirs()
        return attachments.map { att ->
            val bytes = Base64.decode(att.data, Base64.DEFAULT)
            val ext = guessExtension(att.contentType, att.filename)
            val file = File(dir, "att-${UUID.randomUUID()}${ext}")
            FileOutputStream(file).use { it.write(bytes) }
            MessageContent.AttachmentRef(
                contentType = att.contentType,
                filePath = file.absolutePath,
                filename = att.filename,
            )
        }
    }

    private fun guessExtension(contentType: String, filename: String?): String {
        if (filename != null) {
            val dot = filename.lastIndexOf('.')
            if (dot in 1 until filename.length - 1) {
                return filename.substring(dot)
            }
        }
        return when (contentType.lowercase()) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/gif" -> ".gif"
            "image/webp" -> ".webp"
            "application/pdf" -> ".pdf"
            else -> ".dat"
        }
    }
}
