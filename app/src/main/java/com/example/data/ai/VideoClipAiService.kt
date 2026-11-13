package com.example.data.ai

import android.util.Log
import com.example.data.model.ClipProject
import com.example.data.model.GenerationPreferences
import com.example.data.model.TranscriptSegment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "VideoClipAiService"

class VideoClipAiService {

    private val clipSchema = Schema.obj(
        properties = mapOf(
            "clips" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Título chamativo do corte em Português (máximo 90 caracteres)"),
                        "hook" to Schema.string(description = "Frase de gancho nos primeiros 3 segundos para prender retenção"),
                        "viralReason" to Schema.string(description = "Explicação objetiva de por que este corte tem alto potencial viral"),
                        "viralScore" to Schema.integer(description = "Pontuação viral de 70 a 99"),
                        "startTimeSec" to Schema.integer(description = "Segundo inicial do corte no vídeo original"),
                        "endTimeSec" to Schema.integer(description = "Segundo final do corte no vídeo original"),
                        "transcript" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "startSec" to Schema.integer(description = "Segundo inicial da fala"),
                                    "endSec" to Schema.integer(description = "Segundo final da fala"),
                                    "text" to Schema.string(description = "Legenda dinâmica da fala em Português")
                                )
                            ),
                            description = "Lista de 4 a 6 segmentos de legenda sincronizados"
                        ),
                        "hashtags" to Schema.array(
                            items = Schema.string(),
                            description = "De 3 a 5 hashtags relevantes começando com #"
                        )
                    )
                ),
                description = "Exatamente 3 cortes virais otimizados"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = clipSchema
                temperature = 0.7f
            }
        )
    }

    suspend fun generateViralClips(
        userId: String,
        prefs: GenerationPreferences
    ): List<ClipProject> {
        val durationLabel = "${prefs.durationSec} segundos"
        val videoLengthHint = prefs.localVideoDurationSec?.let {
            "O vídeo enviado tem duração total de $it segundos. Mantenha startTimeSec e endTimeSec dentro de 0..$it."
        } ?: "Simule timestamps realistas dentro de um vídeo longo (ex.: entre 00:45 e 18:30)."

        val promptText = """
            Você é o motor de edição viral do app Encurtador Webb.
            Gere EXATAMENTE 3 cortes curtos de alto potencial viral em Português do Brasil com base nas preferências abaixo:
            - Tipo de entrada: ${prefs.sourceType}
            - Conteúdo / Link / Tema: ${prefs.sourceValue}
            - Formato alvo: ${prefs.aspectRatio}
            - Duração ideal de cada corte: aproximadamente $durationLabel
            - Estilo / Tom: ${prefs.tone}
            - Estilo de legenda: ${prefs.captionStyle}
            - $videoLengthHint

            Regras:
            1. Cada corte deve ter ângulo único (ex.: Gancho de Curiosidade, Virada Prática, Polêmica/Insight).
            2. A legenda (transcript) deve conter entre 4 e 6 frases curtas e rítmicas, com timestamps sequenciais coerentes entre startTimeSec e endTimeSec.
            3. Todo o conteúdo deve estar em Português do Brasil natural, direto e engajador.
        """.trimIndent()

        val response = try {
            withTimeout(60_000L) {
                if (prefs.videoFrames.isNotEmpty()) {
                    model.generateContent(
                        content {
                            prefs.videoFrames.take(3).forEach { frame ->
                                image(frame)
                            }
                            text(promptText)
                        }
                    )
                } else {
                    model.generateContent(promptText)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI generation timed out", e)
            throw IllegalStateException("A análise da IA expirou após 60 segundos. Tente novamente.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Falha ao processar vídeo com Firebase AI Logic.",
                e
            )
        }

        val jsonString = response.text
        if (jsonString.isNullOrBlank()) {
            throw IllegalStateException("O modelo retornou uma resposta vazia.")
        }

        return parseClipsJson(userId, prefs, jsonString)
    }

    private fun parseClipsJson(
        userId: String,
        prefs: GenerationPreferences,
        jsonString: String
    ): List<ClipProject> {
        val root = JSONObject(jsonString)
        val clipsArray = root.getJSONArray("clips")
        val result = mutableListOf<ClipProject>()

        for (i in 0 until clipsArray.length()) {
            val obj = clipsArray.getJSONObject(i)
            val title = obj.optString("title", "Corte Viral #${i + 1}").take(140)
            val hook = obj.optString("hook", "Assista aos primeiros 3 segundos!").take(240)
            val viralReason = obj.optString("viralReason", "Ritmo dinâmico com forte gancho inicial.").take(500)
            val viralScore = obj.optLong("viralScore", 88L).coerceIn(0L, 100L)

            val rawStart = obj.optLong("startTimeSec", (i * 65L) + 15L).coerceAtLeast(0L)
            val rawEnd = obj.optLong("endTimeSec", rawStart + prefs.durationSec)
            val safeEnd = maxOf(rawEnd, rawStart + 10L).coerceAtMost(86400L)

            val transcriptJson = obj.optJSONArray("transcript")
            val segments = mutableListOf<TranscriptSegment>()
            if (transcriptJson != null) {
                for (j in 0 until transcriptJson.length()) {
                    val segObj = transcriptJson.getJSONObject(j)
                    val segStart = segObj.optLong("startSec", rawStart + j * 8L).coerceIn(0L, safeEnd)
                    val segEnd = maxOf(
                        segObj.optLong("endSec", segStart + 8L),
                        segStart + 1L
                    ).coerceAtMost(safeEnd)
                    val segText = segObj.optString("text", "").trim()
                    if (segText.isNotEmpty()) {
                        segments.add(
                            TranscriptSegment(
                                id = "seg_${i}_${j + 1}",
                                startSec = segStart,
                                endSec = segEnd,
                                text = segText
                            )
                        )
                    }
                }
            }
            if (segments.isEmpty()) {
                segments.add(
                    TranscriptSegment(
                        id = "seg_${i}_1",
                        startSec = rawStart,
                        endSec = safeEnd,
                        text = hook
                    )
                )
            }

            val hashtagsJson = obj.optJSONArray("hashtags")
            val hashtags = mutableListOf<String>()
            if (hashtagsJson != null) {
                for (k in 0 until minOf(hashtagsJson.length(), 10)) {
                    val tag = hashtagsJson.optString(k, "").trim()
                    if (tag.isNotEmpty()) {
                        hashtags.add(if (tag.startsWith("#")) tag else "#$tag")
                    }
                }
            }

            result.add(
                ClipProject(
                    userId = userId,
                    sourceType = prefs.sourceType,
                    sourceValue = prefs.sourceValue.take(500),
                    title = title,
                    hook = hook,
                    viralReason = viralReason,
                    viralScore = viralScore,
                    startTimeSec = rawStart,
                    endTimeSec = safeEnd,
                    aspectRatio = prefs.aspectRatio,
                    tone = prefs.tone,
                    captionStyle = prefs.captionStyle,
                    captionPosition = "Base",
                    themeIndex = (i % 3).toLong(),
                    transcript = segments,
                    hashtags = hashtags
                )
            )
        }

        if (result.isEmpty()) {
            throw IllegalStateException("Não foi possível extrair cortes válidos da resposta da IA.")
        }
        return result
    }
}
