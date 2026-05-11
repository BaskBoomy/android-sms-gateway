package me.capcom.smsgateway.modules.localserver.domain.messages

/**
 * MMS 발송 시 본문과 함께 보낼 첨부 파일. base64 인코딩된 데이터를 받아
 * MmsSender 가 디코딩 후 PduBody 에 part 로 추가한다.
 *
 * - 1 첨부당 디코딩 후 최대 1 MB (~1,400,000 base64 chars 이하)
 * - 메시지당 최대 20 개 (PostMessageRequest validation 에서 확인)
 *
 * Korean LMS / MMS 라우팅은 통신사가 contentType 보고 결정 — 이미지/문서
 * 모두 동일 spec. receiver 측 단말이 contentType 못 열면 saved file 로
 * 도착하거나 reject 될 수 있음 (운영 정책).
 */
data class Attachment(
    val contentType: String,
    val data: String, // base64
    val filename: String? = null,
)
