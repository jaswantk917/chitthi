package io.github.jaswantk917.chitthi.ai

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import io.github.jaswantk917.chitthi.data.Brand
import io.github.jaswantk917.chitthi.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class NanoStatus { Checking, Ready, Downloading, Unavailable }

/**
 * Gemini Nano through AICore (Pixel 9 and newer). AICore only serves the app in front, so
 * everything here runs while Chitthi is open; arriving messages are sorted by the rules alone.
 */
class Nano {
    private val model by lazy { Generation.getClient() }
    private val scope = CoroutineScope(Dispatchers.IO)
    private var download: Job? = null
    private val busy = Mutex()
    private val _status = MutableStateFlow(NanoStatus.Checking)
    val status: StateFlow<NanoStatus> = _status

    suspend fun isReady(): Boolean {
        val status = try {
            model.checkStatus()
        } catch (e: Exception) {
            Log.w(TAG, "AICore unavailable", e)
            _status.value = NanoStatus.Unavailable
            return false
        }
        _status.value = when (status) {
            FeatureStatus.AVAILABLE -> NanoStatus.Ready
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> NanoStatus.Downloading
            else -> NanoStatus.Unavailable
        }
        if (status == FeatureStatus.DOWNLOADABLE && download?.isActive != true) {
            download = scope.launch {
                try {
                    model.download().collect {}
                    _status.value = NanoStatus.Ready
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini Nano download failed", e)
                }
            }
        }
        return status == FeatureStatus.AVAILABLE
    }

    suspend fun categorize(sender: String, body: String): Category? {
        val reply = generate(
            system = """
                You sort SMS messages in India into one folder.
                IMPORTANT: one-time codes, bank and card transactions, bills and due dates, orders
                and deliveries, tickets and bookings, appointments, security alerts, service
                updates the person needs to act on or know.
                PROMOTIONS: offers, discounts, ads, loan or card pitches, newsletters, greetings,
                awareness campaigns, anything selling or announcing to everyone.
                Reply with exactly one word: IMPORTANT or PROMOTIONS.
            """.trimIndent(),
            prompt = "From: $sender\n${body.take(MAX_BODY)}",
            maxTokens = 4,
        ) ?: return null
        return when {
            reply.contains("IMPORTANT", ignoreCase = true) -> Category.Important
            reply.contains("PROMO", ignoreCase = true) -> Category.Promotions
            else -> null
        }
    }

    /** The company behind an SMS header like "JBRRIA", from a few of its messages. */
    suspend fun nameSender(header: String, samples: List<String>): Brand? {
        val reply = generate(
            system = """
                Indian businesses send SMS from 6-letter headers. Given a header and some of its
                messages, name the company or app that sent them, as people know it (e.g.
                "bigbasket", "ICICI Bank", "Seekho"), and its main website domain.
                Reply exactly as: Name | domain.com
                If the messages don't make it clear, reply exactly: UNKNOWN
            """.trimIndent(),
            prompt = "Header: $header\n" + samples.joinToString("\n") { "- " + it.take(MAX_BODY / 2) },
            maxTokens = 16,
        ) ?: return null
        val line = reply.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (line.contains("UNKNOWN", ignoreCase = true) || !line.contains('|')) return null
        val name = line.substringBefore('|').trim().trim('"', '*')
        val domain = line.substringAfter('|').trim().trim('"', '*', '.').lowercase()
            .removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/')
        if (name.isEmpty() || name.length > MAX_NAME || !domain.contains('.')) return null
        return Brand(name, domain)
    }

    /** Up to three short replies in the language of the chat, for the last message received. */
    suspend fun smartReplies(chat: List<Pair<Boolean, String>>): List<String> {
        val reply = generate(
            system = """
                Suggest three short replies (2 to 6 words each) the person could send next in this
                SMS chat. Match the chat's language: English, Hindi, or Hinglish in Roman letters.
                Casual and natural, no emojis, no numbering. One reply per line, nothing else.
            """.trimIndent(),
            prompt = chat.takeLast(CHAT_TURNS).joinToString("\n") { (mine, text) ->
                (if (mine) "Me: " else "Them: ") + text.take(MAX_BODY / 2)
            },
            maxTokens = 40,
            temperature = 0.5f,
        ) ?: return emptyList()
        return reply.lines()
            .map { it.trim().trimStart('-', '*', '•', ' ').trim('"') }
            .filter { it.isNotEmpty() && it.length <= MAX_REPLY && !it.startsWith("Me:") }
            .distinct()
            .take(3)
    }

    private suspend fun generate(system: String, prompt: String, maxTokens: Int, temperature: Float = 0f): String? =
        try {
            busy.withLock {
                val options: GenerateContentRequest.Builder.() -> Unit = {
                    this.temperature = temperature
                    topK = TOP_K
                    maxOutputTokens = maxTokens
                }
                model.generateContent(
                    generateContentRequest(SystemInstruction(system), TextPart(prompt), options)
                ).candidates.firstOrNull()?.text
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini Nano generation failed", e)
            null
        }

    companion object {
        private const val TAG = "Nano"
        private const val TOP_K = 16
        private const val MAX_BODY = 600
        private const val MAX_NAME = 32
        private const val MAX_REPLY = 40
        private const val CHAT_TURNS = 8
    }
}
