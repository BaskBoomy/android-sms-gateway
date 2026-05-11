package me.capcom.smsgateway.modules.localserver.domain.messages

import me.capcom.smsgateway.modules.messages.MessagesSettings

/**
 * Validate attachments against runtime-configurable settings (앱 설정 UI 에서
 * 운영자가 조정 가능한 값). PostMessageRequest.validate() 의 정적 검사 통과 후
 * MessagesRoutes 가 호출.
 *
 * - maxAttachments: 메시지당 첨부 개수 상한
 * - maxAttachmentBytes: 첨부 1개당 decode 후 바이트 상한
 *   (base64 char × 3/4 ≈ byte. 정확치는 디코드 결과 길이로 봐도 무방하나
 *    routes 단계는 빠른 reject 우선이라 char 길이로 근사.)
 */
fun validateAttachments(request: PostMessageRequest, settings: MessagesSettings) {
    val atts = request.textMessage?.attachments ?: return
    val maxCount = settings.maxAttachments
    val maxBytes = settings.maxAttachmentBytes
    val maxBase64Chars = ((maxBytes.toLong() * 4 + 2) / 3).toInt()

    if (atts.size > maxCount) {
        throw IllegalArgumentException("attachments must be at most $maxCount")
    }
    for (a in atts) {
        if (a.data.length > maxBase64Chars) {
            throw IllegalArgumentException(
                "attachment ${a.filename ?: "?"} exceeds max size ($maxBytes bytes after decode)"
            )
        }
    }
}
