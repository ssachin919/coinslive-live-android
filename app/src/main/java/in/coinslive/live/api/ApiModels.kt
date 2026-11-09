package `in`.coinslive.live.api

import kotlinx.serialization.Serializable

// region Join / token

enum class LiveRoomKind {
    /** Broadcast workshop (`/api/workshop`). Kept as `WEBINAR` for wire compatibility. */
    WEBINAR,

    /** Peer study session (`/api/adhyayan`). */
    ADHYAYAN,
}

data class LiveJoinRequest(
    val kind: LiveRoomKind,
    val slug: String,
    /** Workshop host path sets this true. Adhyayan role is server-side. */
    val asHost: Boolean = false,
    /** Workshop attendee registration token from `POST /api/workshop/register`. */
    val accessToken: String? = null,
)

enum class WebinarClientRole(val wire: String) {
    HOST("host"),
    ATTENDEE("attendee");

    companion object {
        fun fromWire(value: String?): WebinarClientRole =
            entries.firstOrNull { it.wire == value } ?: HOST
    }
}

@Serializable
data class LiveTokenResponse(
    val ok: Boolean? = null,
    val token: String,
    val url: String,
    val role: String,
    val roomName: String? = null,
    val identity: String,
    val status: String? = null,
    val displayName: String? = null,
    val error: String? = null,
)

@Serializable
data class ApiErrorBody(
    val error: String? = null,
    val phase: String? = null,
    val status: String? = null,
)

@Serializable
data class MobileConfigResponse(
    val ok: Boolean = true,
    val apiBaseUrl: String? = null,
    val deepLinkScheme: String? = null,
    val minAppVersion: String? = null,
    val notes: String? = null,
)

@Serializable
data class OkResponse(val ok: Boolean? = null)

// endregion

// region Workshop (webinar)

@Serializable
data class WebinarSummary(
    val id: String,
    val slug: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val scheduledAt: String? = null,
    val livekitRoomName: String? = null,
    val maxAttendees: Int? = null,
    val createdAt: String? = null,
)

@Serializable
data class WebinarListResponse(
    val ok: Boolean? = null,
    val webinars: List<WebinarSummary> = emptyList(),
)

@Serializable
data class WebinarDetail(
    val id: String? = null,
    val slug: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String,
    val scheduledAt: String? = null,
    val maxAttendees: Int? = null,
    val attendeeCount: Int? = null,
    val isHost: Boolean? = null,
)

@Serializable
data class WebinarDetailResponse(
    val ok: Boolean? = null,
    val webinar: WebinarDetail,
)

@Serializable
data class WebinarStatusBody(val action: String)

@Serializable
data class WebinarStatusResponse(
    val ok: Boolean? = null,
    val webinar: WebinarDetail? = null,
)

@Serializable
data class WebinarParticipant(
    val identity: String,
    val name: String,
)

@Serializable
data class WebinarParticipantsResponse(
    val ok: Boolean? = null,
    val count: Int? = null,
    val participants: List<WebinarParticipant> = emptyList(),
)

@Serializable
data class HostQuestion(
    val id: String,
    val webinarId: String? = null,
    val registrationId: String? = null,
    val body: String,
    val status: String,
    val answerText: String? = null,
    val published: Boolean? = null,
    val createdAt: Double? = null,
    val authorName: String? = null,
)

@Serializable
data class QuestionsResponse(
    val ok: Boolean? = null,
    val questions: List<HostQuestion> = emptyList(),
)

@Serializable
data class HostInboxMessage(
    val id: String,
    val webinarId: String? = null,
    val registrationId: String? = null,
    val body: String,
    val status: String,
    val hostReply: String? = null,
    val createdAt: Double? = null,
    val authorName: String? = null,
)

@Serializable
data class MessagesResponse(
    val ok: Boolean? = null,
    val messages: List<HostInboxMessage> = emptyList(),
)

@Serializable
data class WebinarBroadcast(
    val id: String,
    val webinarId: String? = null,
    val body: String,
    val createdAt: Double? = null,
)

@Serializable
data class BroadcastsResponse(
    val ok: Boolean? = null,
    val broadcasts: List<WebinarBroadcast> = emptyList(),
)

@Serializable
data class AblyTokenRequest(
    val keyName: String? = null,
    val clientId: String? = null,
    val timestamp: Double? = null,
    val nonce: String? = null,
    val mac: String? = null,
    val ttl: Int? = null,
    val capability: String? = null,
)

@Serializable
data class AblyTokenResponse(
    val ok: Boolean? = null,
    val tokenRequest: AblyTokenRequest,
    val channel: String,
)

@Serializable
data class QuestionActionBody(
    val action: String,
    val answerText: String? = null,
    val publish: Boolean? = null,
)

@Serializable
data class MessageActionBody(
    val action: String,
    val hostReply: String? = null,
)

@Serializable
data class BroadcastCreateBody(val body: String)

@Serializable
data class WebinarTokenBody(
    val asHost: Boolean? = null,
    val accessToken: String? = null,
)

@Serializable
data class CreateWebinarBody(
    val title: String,
    val description: String? = null,
    val scheduledAt: String? = null,
    val maxAttendees: Int? = null,
)

@Serializable
data class CreateWebinarResponse(
    val ok: Boolean? = null,
    val webinar: WebinarSummary,
)

@Serializable
data class RegisterWebinarBody(
    val webinarSlug: String,
    val name: String? = null,
    val email: String? = null,
)

@Serializable
data class RegisterWebinarResponse(
    val ok: Boolean? = null,
    val accessToken: String? = null,
    val alreadyRegistered: Boolean? = null,
    val joinUrl: String? = null,
    val error: String? = null,
)

@Serializable
data class SendWebinarMessageBody(
    val body: String,
    val accessToken: String? = null,
)

@Serializable
data class SendWebinarMessageResponse(
    val ok: Boolean? = null,
    val message: HostInboxMessage? = null,
)

@Serializable
data class AskQuestionBody(
    val body: String,
    val accessToken: String? = null,
)

@Serializable
data class AskQuestionResponse(
    val ok: Boolean? = null,
    val question: HostQuestion? = null,
)

// endregion

// region Adhyayan

@Serializable
data class AdhyayanSummary(
    val id: String,
    val slug: String,
    val title: String,
    val topic: String? = null,
    val description: String? = null,
    val learningGoal: String? = null,
    val status: String,
    val scheduledAt: String? = null,
    val accessMode: String? = null,
    val capacity: Int? = null,
    val isHost: Boolean? = null,
    val roomCode: String? = null,
    val admittedCount: Int? = null,
    val hostName: String? = null,
)

@Serializable
data class AdhyayanListResponse(
    val ok: Boolean? = null,
    val sessions: List<AdhyayanSummary> = emptyList(),
)

@Serializable
data class AdhyayanMe(
    val memberId: String? = null,
    val role: String? = null,
    val joinStatus: String? = null,
    val isPresenter: Boolean? = null,
)

@Serializable
data class AdhyayanDetailResponse(
    val ok: Boolean? = null,
    val session: AdhyayanSummary,
    val me: AdhyayanMe? = null,
)

@Serializable
data class CreateAdhyayanBody(
    val title: String,
    val topic: String? = null,
    val description: String? = null,
    val learningGoal: String? = null,
    val scheduledAt: String? = null,
    val accessMode: String? = null,
    val capacity: Int? = null,
)

@Serializable
data class CreateAdhyayanResponse(
    val ok: Boolean? = null,
    val session: AdhyayanSummary,
)

@Serializable
data class AdhyayanJoinBody(val roomCode: String? = null)

@Serializable
data class AdhyayanJoinResponse(
    val ok: Boolean? = null,
    val memberId: String? = null,
    val role: String? = null,
    val joinStatus: String? = null,
)

@Serializable
data class AdhyayanStatusBody(val action: String)

@Serializable
data class AdhyayanStatusResponse(
    val ok: Boolean? = null,
    val session: AdhyayanSummary? = null,
)

@Serializable
data class AdhyayanTokenResponse(
    val ok: Boolean? = null,
    val token: String,
    val url: String,
    val role: String,
    val identity: String,
    val status: String? = null,
    val memberId: String? = null,
    val isPresenter: Boolean? = null,
    val error: String? = null,
    val phase: String? = null,
)

// endregion
