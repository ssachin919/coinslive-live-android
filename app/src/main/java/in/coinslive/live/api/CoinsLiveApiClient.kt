package `in`.coinslive.live.api

import android.net.Uri
import `in`.coinslive.live.config.AppConfig
import `in`.coinslive.live.theme.CoinsLiveFormat
import `in`.coinslive.live.util.nilIfEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** User-visible API failure. */
class CoinsLiveApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        fun unauthorized() = CoinsLiveApiException(
            "Could not authorize this request. Check that you are signed in.",
            statusCode = 401,
        )
    }
}

/**
 * Thin client over existing Next.js LiveKit + host APIs.
 * Auth: `Authorization: Bearer <Clerk session JWT>`.
 *
 * Next.js uses `trailingSlash: true`: every path is normalised to end with `/` *before*
 * the query string so a 308 redirect never drops the Authorization header.
 */
class CoinsLiveApiClient(
    private val baseUrl: String = AppConfig.apiBaseUrl,
    private val http: OkHttpClient = defaultHttpClient,
) {
    // region Config

    suspend fun fetchMobileConfig(): MobileConfigResponse = get("/api/mobile/config")

    // endregion

    // region Lists

    suspend fun listWebinars(bearerToken: String): List<WebinarSummary> =
        get<WebinarListResponse>("/api/workshop", bearerToken).webinars

    suspend fun listAdhyayanSessions(bearerToken: String, mine: Boolean = true): List<AdhyayanSummary> =
        get<AdhyayanListResponse>(
            "/api/adhyayan",
            bearerToken,
            query = if (mine) mapOf("mine" to "1") else emptyMap(),
        ).sessions

    // endregion

    // region Create / register

    suspend fun createWebinar(
        title: String,
        description: String?,
        scheduledAtMillis: Long?,
        maxAttendees: Int?,
        bearerToken: String,
    ): WebinarSummary = post<CreateWebinarBody, CreateWebinarResponse>(
        "/api/workshop",
        CreateWebinarBody(
            title = title,
            description = description,
            scheduledAt = scheduledAtMillis?.let(CoinsLiveFormat::iso8601),
            maxAttendees = maxAttendees,
        ),
        bearerToken,
    ).webinar

    suspend fun createAdhyayan(
        title: String,
        topic: String?,
        description: String?,
        learningGoal: String?,
        scheduledAtMillis: Long?,
        accessMode: String,
        capacity: Int,
        bearerToken: String,
    ): AdhyayanSummary = post<CreateAdhyayanBody, CreateAdhyayanResponse>(
        "/api/adhyayan",
        CreateAdhyayanBody(
            title = title,
            topic = topic,
            description = description,
            learningGoal = learningGoal,
            scheduledAt = scheduledAtMillis?.let(CoinsLiveFormat::iso8601),
            accessMode = accessMode,
            capacity = capacity,
        ),
        bearerToken,
    ).session

    suspend fun registerForWebinar(
        slug: String,
        name: String?,
        bearerToken: String,
    ): RegisterWebinarResponse = post(
        "/api/workshop/register",
        RegisterWebinarBody(webinarSlug = slug, name = name),
        bearerToken,
    )

    // endregion

    // region Tokens

    suspend fun fetchLiveToken(request: LiveJoinRequest, bearerToken: String): LiveTokenResponse =
        when (request.kind) {
            LiveRoomKind.WEBINAR -> post(
                "/api/workshop/${enc(request.slug)}/token",
                WebinarTokenBody(
                    asHost = if (request.asHost) true else null,
                    accessToken = if (request.asHost) null else request.accessToken,
                ),
                bearerToken,
            )

            LiveRoomKind.ADHYAYAN -> {
                val response = fetchAdhyayanToken(request.slug, bearerToken)
                LiveTokenResponse(
                    ok = response.ok,
                    token = response.token,
                    url = response.url,
                    role = response.role,
                    roomName = null,
                    identity = response.identity,
                    status = response.status,
                    displayName = null,
                    error = response.error,
                )
            }
        }

    suspend fun fetchAdhyayanToken(slug: String, bearerToken: String): AdhyayanTokenResponse =
        postEmpty("/api/adhyayan/${enc(slug)}/token", bearerToken)

    // endregion

    // region Workshop lifecycle / room

    suspend fun fetchWebinar(
        slug: String,
        fields: String? = null,
        bearerToken: String? = null,
    ): WebinarDetail = get<WebinarDetailResponse>(
        "/api/workshop/${enc(slug)}",
        bearerToken,
        query = if (fields != null) mapOf("fields" to fields) else emptyMap(),
    ).webinar

    suspend fun postWebinarStatus(slug: String, action: String, bearerToken: String): WebinarStatusResponse =
        post("/api/workshop/${enc(slug)}/status", WebinarStatusBody(action), bearerToken)

    suspend fun listWebinarParticipants(slug: String, bearerToken: String): WebinarParticipantsResponse =
        get("/api/workshop/${enc(slug)}/participants", bearerToken)

    suspend fun fetchAblyToken(slug: String, bearerToken: String): AblyTokenResponse =
        get("/api/workshop/${enc(slug)}/ably-token", bearerToken)

    suspend fun listQuestions(
        slug: String,
        bearerToken: String? = null,
        accessToken: String? = null,
    ): List<HostQuestion> = get<QuestionsResponse>(
        "/api/workshop/${enc(slug)}/questions",
        bearerToken,
        query = accessQuery(accessToken),
    ).questions

    suspend fun askQuestion(
        slug: String,
        body: String,
        accessToken: String,
        bearerToken: String? = null,
    ): HostQuestion? = post<AskQuestionBody, AskQuestionResponse>(
        "/api/workshop/${enc(slug)}/questions",
        AskQuestionBody(body = body, accessToken = accessToken),
        bearerToken,
    ).question

    suspend fun questionAction(
        slug: String,
        id: String,
        action: String,
        answerText: String? = null,
        publish: Boolean? = null,
        bearerToken: String,
    ) {
        post<QuestionActionBody, OkResponse>(
            "/api/workshop/${enc(slug)}/questions/${enc(id)}",
            QuestionActionBody(action = action, answerText = answerText, publish = publish),
            bearerToken,
        )
    }

    suspend fun listMessages(
        slug: String,
        bearerToken: String? = null,
        accessToken: String? = null,
    ): List<HostInboxMessage> = get<MessagesResponse>(
        "/api/workshop/${enc(slug)}/messages",
        bearerToken,
        query = accessQuery(accessToken),
    ).messages

    suspend fun sendWebinarMessage(
        slug: String,
        body: String,
        accessToken: String,
        bearerToken: String? = null,
    ): HostInboxMessage? = post<SendWebinarMessageBody, SendWebinarMessageResponse>(
        "/api/workshop/${enc(slug)}/messages",
        SendWebinarMessageBody(body = body, accessToken = accessToken),
        bearerToken,
    ).message

    suspend fun messageAction(
        slug: String,
        id: String,
        action: String,
        hostReply: String? = null,
        bearerToken: String,
    ) {
        post<MessageActionBody, OkResponse>(
            "/api/workshop/${enc(slug)}/messages/${enc(id)}",
            MessageActionBody(action = action, hostReply = hostReply),
            bearerToken,
        )
    }

    suspend fun listBroadcasts(slug: String, bearerToken: String): List<WebinarBroadcast> =
        get<BroadcastsResponse>("/api/workshop/${enc(slug)}/broadcasts", bearerToken).broadcasts

    suspend fun createBroadcast(slug: String, body: String, bearerToken: String) {
        post<BroadcastCreateBody, OkResponse>(
            "/api/workshop/${enc(slug)}/broadcasts",
            BroadcastCreateBody(body),
            bearerToken,
        )
    }

    // endregion

    // region Adhyayan lifecycle / room

    suspend fun fetchAdhyayan(slug: String, bearerToken: String): AdhyayanDetailResponse =
        get("/api/adhyayan/${enc(slug)}", bearerToken)

    suspend fun joinAdhyayan(slug: String, roomCode: String?, bearerToken: String): AdhyayanJoinResponse =
        post("/api/adhyayan/${enc(slug)}/join", AdhyayanJoinBody(roomCode), bearerToken)

    suspend fun postAdhyayanStatus(slug: String, action: String, bearerToken: String): AdhyayanStatusResponse =
        post("/api/adhyayan/${enc(slug)}/status", AdhyayanStatusBody(action), bearerToken)

    // endregion

    // region HTTP

    private fun accessQuery(accessToken: String?): Map<String, String> {
        val token = accessToken?.nilIfEmpty() ?: return emptyMap()
        return mapOf("accessToken" to token)
    }

    private suspend inline fun <reified T> get(
        path: String,
        bearerToken: String? = null,
        query: Map<String, String> = emptyMap(),
    ): T {
        val request = buildRequest("GET", path, query, bearerToken, null)
        return decode(executeRaw(request))
    }

    private suspend inline fun <reified Body, reified T> post(
        path: String,
        body: Body,
        bearerToken: String? = null,
    ): T {
        val payload = json.encodeToString(body).toRequestBody(jsonMediaType)
        val request = buildRequest("POST", path, emptyMap(), bearerToken, payload)
        return decode(executeRaw(request))
    }

    private suspend inline fun <reified T> postEmpty(path: String, bearerToken: String): T {
        val request = buildRequest("POST", path, emptyMap(), bearerToken, "{}".toRequestBody(jsonMediaType))
        return decode(executeRaw(request))
    }

    private inline fun <reified T> decode(raw: String): T = try {
        json.decodeFromString<T>(raw)
    } catch (e: SerializationException) {
        throw CoinsLiveApiException("Unexpected response: ${e.message}", cause = e)
    } catch (e: IllegalArgumentException) {
        throw CoinsLiveApiException("Unexpected response: ${e.message}", cause = e)
    }

    private fun buildRequest(
        method: String,
        path: String,
        query: Map<String, String>,
        bearerToken: String?,
        body: okhttp3.RequestBody?,
    ): Request {
        val full = baseUrl.trimEnd('/') + ensureTrailingSlash(path)
        val httpUrl = full.toHttpUrlOrNull()
            ?: throw CoinsLiveApiException("Invalid server address")
        val urlBuilder = httpUrl.newBuilder()
        query.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val builder = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")
        if (!bearerToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }
        when (method) {
            "GET" -> builder.get()
            else -> builder.method(method, body ?: "".toRequestBody(null))
        }
        return builder.build()
    }

    private suspend fun executeRaw(request: Request): String = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                when {
                    response.isRedirect -> {
                        val location = response.header("Location").orEmpty()
                        throw CoinsLiveApiException(
                            "Server redirected to $location. Check the API base URL.",
                            statusCode = response.code,
                        )
                    }

                    response.code == 401 -> throw CoinsLiveApiException.unauthorized()

                    !response.isSuccessful -> {
                        val message = runCatching {
                            json.decodeFromString<ApiErrorBody>(text).error
                        }.getOrNull()
                        throw CoinsLiveApiException(
                            message?.takeIf { it.isNotBlank() } ?: "Request failed (${response.code})",
                            statusCode = response.code,
                        )
                    }

                    else -> text
                }
            }
        } catch (e: IOException) {
            throw CoinsLiveApiException("Network error: ${e.message ?: "check your connection"}", cause = e)
        }
    }

    // endregion

    companion object {
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
            isLenient = true
        }

        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // Never silently follow redirects: surface them so a dropped
                // Authorization header cannot masquerade as "sign in required".
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        }

        private fun enc(segment: String): String = Uri.encode(segment)

        /** Ensures the path ends with `/` (before any query string). */
        fun ensureTrailingSlash(path: String): String {
            val queryIndex = path.indexOf('?')
            if (queryIndex >= 0) {
                var pathOnly = path.substring(0, queryIndex)
                val query = path.substring(queryIndex)
                if (!pathOnly.endsWith("/")) pathOnly += "/"
                return pathOnly + query
            }
            return if (path.endsWith("/")) path else "$path/"
        }
    }
}
