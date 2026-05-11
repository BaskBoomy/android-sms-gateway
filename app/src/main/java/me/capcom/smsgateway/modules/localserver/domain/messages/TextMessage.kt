package me.capcom.smsgateway.modules.localserver.domain.messages

data class TextMessage(
    val text: String,
    /**
     * 선택 첨부 — 있으면 메시지가 MMS path 로 발송된다 (text 길이 무관).
     * 한국 통신사는 contentType 으로 MMS 라우팅을 결정.
     */
    val attachments: List<Attachment>? = null,
)