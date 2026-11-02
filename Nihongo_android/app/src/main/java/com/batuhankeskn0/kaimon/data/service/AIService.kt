package com.batuhankeskn0.kaimon.data.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI servis hataları (`Services/AIService.swift` -> `AIError` karşılığı).
 */
sealed class AIError(override val message: String) : Exception(message) {
    data object InvalidURL : AIError("Geçersiz URL.")
    data object InvalidResponse : AIError("Sunucudan geçersiz bir yanıt geldi.")
    data class ApiError(val errorMessage: String) : AIError(errorMessage)
}

/**
 * HTTP yanıt zarfı.
 */
data class AiHttpResponse(
    val statusCode: Int,
    val body: String
)

/**
 * Ağ katmanı soyutlaması. Cihazda `HttpURLConnection` kullanır, birim testlerde sahte yanıtlarla test edilir.
 */
fun interface AiHttpTransport {
    @Throws(IOException::class)
    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        bodyJson: String
    ): AiHttpResponse
}

/**
 * Standart `HttpURLConnection` tabanlı transport.
 * Güvenlik kuralı gereği URL sorgu parametrelerini veya Authorization başlıklarını asla loglamaz.
 */
class HttpUrlConnectionTransport(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectTimeoutMs: Int = 25_000,
    private val readTimeoutMs: Int = 25_000
) : AiHttpTransport {
    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        bodyJson: String
    ): AiHttpResponse = withContext(ioDispatcher) {
        val parsedUrl = runCatching { URL(url) }.getOrElse {
            throw AIError.InvalidURL
        }
        val connection = (parsedUrl.openConnection() as? HttpURLConnection)
            ?: throw AIError.InvalidResponse

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            connection.outputStream.use { output ->
                output.write(bodyJson.toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            AiHttpResponse(statusCode = statusCode, body = responseBody)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Türkçe-Japonca AI Dil Partneri Servisi.
 * iOS `Services/AIService.swift` sınıfının birebir Kotlin Coroutines karşılığıdır.
 */
@Singleton
class AIService(
    private val transport: AiHttpTransport = HttpUrlConnectionTransport(),
    private val apiKeyProvider: () -> String = { Secrets.aiAPIKey },
    private val providerProvider: () -> String = { Secrets.aiProvider },
    private val retryDelayMs: Long = 600L
) {
    @Inject
    constructor() : this(
        transport = HttpUrlConnectionTransport(),
        apiKeyProvider = { Secrets.aiAPIKey },
        providerProvider = { Secrets.aiProvider },
        retryDelayMs = 600L
    )

    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT

    val geminiModels: List<String> = DEFAULT_GEMINI_MODELS

    private val mutex = Mutex()
    private val messageHistory = mutableListOf<Map<String, String>>()

    init {
        messageHistory.add(mapOf("role" to "system", "content" to systemPrompt))
    }

    /**
     * Mevcut konuşma geçmişinin salt-okunur kopyasını döner.
     */
    fun getMessageHistory(): List<Map<String, String>> = synchronized(messageHistory) {
        messageHistory.toList()
    }

    /**
     * Konuşma geçmişini sıfırlar ve yalnızca sistem promptunu korur.
     */
    fun resetConversation() {
        synchronized(messageHistory) {
            messageHistory.clear()
            messageHistory.add(mapOf("role" to "system", "content" to systemPrompt))
        }
    }

    /**
     * Kullanıcı mesajını seçili AI sağlayıcısına (Gemini veya OpenAI) gönderir ve yanıtı döner.
     * Hata durumunda eklenen son kullanıcı mesajını geçmişten geri alır (`removeLast`).
     */
    suspend fun sendMessage(userText: String): String = mutex.withLock {
        val rawKey = apiKeyProvider().trim()
        if (!Secrets.isKeyConfigured(rawKey)) {
            throw AIError.ApiError("Lütfen local.properties dosyasına API anahtarını girin.")
        }

        val snapshotForRequest: List<Map<String, String>>
        synchronized(messageHistory) {
            messageHistory.add(mapOf("role" to "user", "content" to userText))
            snapshotForRequest = messageHistory.toList()
        }

        try {
            val provider = providerProvider().trim().lowercase()
            val response = if (provider == "gemini") {
                sendToGemini(apiKey = rawKey, historySnapshot = snapshotForRequest)
            } else {
                sendToOpenAI(apiKey = rawKey, historySnapshot = snapshotForRequest)
            }
            synchronized(messageHistory) {
                messageHistory.add(mapOf("role" to "assistant", "content" to response))
            }
            response
        } catch (error: Throwable) {
            synchronized(messageHistory) {
                if (messageHistory.isNotEmpty() && messageHistory.last()["role"] == "user") {
                    messageHistory.removeAt(messageHistory.lastIndex)
                }
            }
            if (error is AIError) {
                throw error
            } else if (error is IOException) {
                throw AIError.ApiError(OFFLINE_ERROR_MESSAGE)
            } else {
                throw AIError.ApiError(error.message ?: OFFLINE_ERROR_MESSAGE)
            }
        }
    }

    // MARK: - OpenAI Implementation

    private suspend fun sendToOpenAI(
        apiKey: String,
        historySnapshot: List<Map<String, String>>
    ): String {
        val safeKey = apiKey.trim()
        val payload = buildOpenAiPayload(historySnapshot)

        val response = try {
            transport.postJson(
                url = OPENAI_ENDPOINT,
                headers = mapOf(
                    "Authorization" to "Bearer $safeKey",
                    "Content-Type" to "application/json"
                ),
                bodyJson = payload
            )
        } catch (e: IOException) {
            throw AIError.ApiError(OFFLINE_ERROR_MESSAGE)
        }

        if (response.statusCode == 200) {
            val content = parseOpenAiResponseText(response.body)
            return content ?: "Cevap alınamadı."
        }

        when (response.statusCode) {
            429 -> throw AIError.ApiError(RATE_LIMIT_ERROR_MESSAGE)
            503 -> throw AIError.ApiError("OpenAI sunucularında şu anda yoğunluk yaşanıyor. Lütfen birkaç saniye sonra tekrar deneyin.")
            401, 403 -> throw AIError.ApiError("OpenAI API anahtarı geçersiz veya yetkisiz.")
            else -> throw AIError.ApiError("OpenAI Sunucu Hatası: ${response.statusCode}")
        }
    }

    // MARK: - Gemini Implementation

    private suspend fun sendToGemini(
        apiKey: String,
        historySnapshot: List<Map<String, String>>
    ): String {
        val safeKey = runCatching {
            URLEncoder.encode(apiKey.trim(), Charsets.UTF_8.name())
        }.getOrDefault(apiKey.trim())

        val bodyJson = buildGeminiPayload(
            systemPrompt = systemPrompt,
            historySnapshot = historySnapshot
        )

        var lastError: Throwable? = null
        var lastStatusCode = 0
        var lastErrorMessage = ""

        for ((index, model) in geminiModels.withIndex()) {
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$safeKey"

            try {
                val httpResponse = transport.postJson(
                    url = urlString,
                    headers = mapOf("Content-Type" to "application/json"),
                    bodyJson = bodyJson
                )

                if (httpResponse.statusCode == 200) {
                    val parsedText = parseGeminiResponseText(httpResponse.body)
                    if (!parsedText.isNullOrBlank()) {
                        return parsedText
                    }
                }

                lastStatusCode = httpResponse.statusCode
                lastErrorMessage = parseApiErrorMessage(httpResponse.body)

                // 503 (yoğunluk), 429 (oran aşımı) veya 404 (model sürümü kapalı) durumunda diğer modele geç
                val isRetryableStatus =
                    httpResponse.statusCode == 503 ||
                        httpResponse.statusCode == 429 ||
                        httpResponse.statusCode == 404
                if (isRetryableStatus && index < geminiModels.lastIndex) {
                    if (retryDelayMs > 0) {
                        delay(retryDelayMs)
                    }
                    continue
                } else {
                    break
                }
            } catch (e: Throwable) {
                lastError = e
                if (index < geminiModels.lastIndex) {
                    continue
                }
            }
        }

        // Tüm modeller başarısız olduysa kullanıcı dostu Türkçe hata döndür
        return when {
            lastStatusCode == 503 -> throw AIError.ApiError(GEMINI_OVERLOAD_ERROR_MESSAGE)
            lastStatusCode == 429 -> throw AIError.ApiError(RATE_LIMIT_ERROR_MESSAGE)
            lastStatusCode == 401 || lastStatusCode == 403 -> throw AIError.ApiError(GEMINI_AUTH_ERROR_MESSAGE)
            lastErrorMessage.isNotBlank() -> throw AIError.ApiError("Gemini yanıt veremedi: $lastErrorMessage")
            lastError is IOException -> throw AIError.ApiError(OFFLINE_ERROR_MESSAGE)
            lastError is AIError -> throw lastError as AIError
            lastError != null -> throw AIError.ApiError(OFFLINE_ERROR_MESSAGE)
            else -> throw AIError.ApiError(OFFLINE_ERROR_MESSAGE)
        }
    }

    companion object {
        const val OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions"

        const val GEMINI_OVERLOAD_ERROR_MESSAGE =
            "Gemini sunucularında şu anda yoğunluk yaşanıyor. Lütfen birkaç saniye sonra tekrar deneyin."
        const val RATE_LIMIT_ERROR_MESSAGE =
            "İstek limiti aşıldı. Lütfen bir süre bekleyip tekrar deneyin."
        const val GEMINI_AUTH_ERROR_MESSAGE =
            "Gemini API anahtarı geçersiz veya yetkisiz."
        const val OFFLINE_ERROR_MESSAGE =
            "Sunucuya ulaşılamadı. Lütfen internet bağlantınızı kontrol edip tekrar deneyin."

        val DEFAULT_GEMINI_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash",
            "gemini-flash-latest"
        )

        val DEFAULT_SYSTEM_PROMPT = """
            Sen bir Japonca dil partnerisin. Görevin öğrencinin Japonca pratik yapmasına yardımcı olmak.
            Kullanıcı sana Japonca veya Türkçe yazabilir. 
            Sen her zaman DOĞAL ve GÜNLÜK JAPONCA (Desu/Masu formu, N5/N4 seviyesine uygun) ile cevap vermelisin.
            Ayrıca her Japonca cümlenin yanına mutlaka parantez içinde (Türkçe Çevirisini) yazmalısın.
            Kullanıcı hatalı bir Japonca kurarsa, nazikçe düzelt.
            Çok uzun cevaplar verme, kısa ve sohbeti devam ettirecek sorular sor.
            Örnek Cevap Formatı:
            こんにちは！お元気ですか？ (Merhaba! Nasılsın?)
        """.trimIndent()

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        internal fun buildGeminiPayload(
            systemPrompt: String,
            historySnapshot: List<Map<String, String>>
        ): String {
            val payloadObj: JsonObject = buildJsonObject {
                put(
                    "systemInstruction",
                    buildJsonObject {
                        put(
                            "parts",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("text", JsonPrimitive(systemPrompt))
                                    }
                                )
                            }
                        )
                    }
                )
                put(
                    "contents",
                    buildJsonArray {
                        historySnapshot
                            .filter { it["role"] != "system" }
                            .forEach { msg ->
                                val role = if (msg["role"] == "user") "user" else "model"
                                val text = msg["content"].orEmpty()
                                add(
                                    buildJsonObject {
                                        put("role", JsonPrimitive(role))
                                        put(
                                            "parts",
                                            buildJsonArray {
                                                add(
                                                    buildJsonObject {
                                                        put("text", JsonPrimitive(text))
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                    }
                )
            }
            return payloadObj.toString()
        }

        internal fun buildOpenAiPayload(historySnapshot: List<Map<String, String>>): String {
            val payloadObj: JsonObject = buildJsonObject {
                put("model", JsonPrimitive("gpt-4o-mini"))
                put(
                    "messages",
                    buildJsonArray {
                        historySnapshot.forEach { msg ->
                            add(
                                buildJsonObject {
                                    put("role", JsonPrimitive(msg["role"].orEmpty()))
                                    put("content", JsonPrimitive(msg["content"].orEmpty()))
                                }
                            )
                        }
                    }
                )
                put("temperature", JsonPrimitive(0.7))
            }
            return payloadObj.toString()
        }

        internal fun parseGeminiResponseText(rawBody: String): String? = runCatching {
            val root = json.parseToJsonElement(rawBody).jsonObject
            val candidates = root["candidates"]?.jsonArray ?: return null
            val firstCandidate = candidates.firstOrNull()?.jsonObject ?: return null
            val content = firstCandidate["content"]?.jsonObject ?: return null
            val parts = content["parts"]?.jsonArray ?: return null
            parts.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
        }.getOrNull()

        internal fun parseOpenAiResponseText(rawBody: String): String? = runCatching {
            val root = json.parseToJsonElement(rawBody).jsonObject
            val choices = root["choices"]?.jsonArray ?: return null
            val firstChoice = choices.firstOrNull()?.jsonObject ?: return null
            val message = firstChoice["message"]?.jsonObject ?: return null
            message["content"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()

        internal fun parseApiErrorMessage(rawBody: String): String {
            val parsed = runCatching {
                val root = json.parseToJsonElement(rawBody).jsonObject
                root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            return parsed ?: rawBody.trim()
        }

        @Volatile
        private var sharedInstance: AIService = AIService()

        val shared: AIService
            get() = sharedInstance

        internal fun setSharedForTesting(service: AIService) {
            sharedInstance = service
        }
    }
}
