package me.capcom.smsgateway.domain

sealed class MessageContent {
    /**
     * Text content with optional attachments (MMS). attachments 가 비어있지 않으면
     * MessagesService 가 길이 무관 MMS path 로 발사한다. 파일 자체는 cache 경로에
     * 저장돼 있고 여기는 path 만 보관 (DB row 크기 부풀림 방지).
     */
    data class Text(
        val text: String,
        val attachments: List<AttachmentRef>? = null,
    ) : MessageContent() {
        override fun toString(): String {
            return text
        }
    }

    data class Data(val data: String, val port: UShort) : MessageContent() {
        override fun toString(): String {
            return "$data:$port"
        }
    }

    /**
     * 발송 후에도 cleanup 책임은 발사 측. 파일이 존재해야 발사 가능.
     */
    data class AttachmentRef(
        val contentType: String,
        val filePath: String,
        val filename: String? = null,
    )
}