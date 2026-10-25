
package com.example.macetaelefante.data

import android.content.Context
import com.example.macetaelefante.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PlantCareInfo(
    val title: String,
    val description: String,
    val extract: String,
    val sourceUrl: String,
    val updatedAt: Long,
    val luz: String,
    val riego: String,
    val sustrato: String,
    val temperatura: String,
    val alertas: String
)

object PlantCareRepository {

    private const val PREFS_NAME = "plant_care_preferences"

    private const val KEY_TITLE = "title"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_EXTRACT = "extract"
    private const val KEY_SOURCE_URL = "source_url"
    private const val KEY_UPDATED_AT = "updated_at"

    private const val KEY_LUZ = "care_luz"
    private const val KEY_RIEGO = "care_riego"
    private const val KEY_SUSTRATO = "care_sustrato"
    private const val KEY_TEMPERATURA = "care_temperatura"
    private const val KEY_ALERTAS = "care_alertas"

    private const val SPECIES_ID = 1192

    private const val URL_PERENUAL =
        "https://perenual.com/api/species-care-guide-list"

    private const val URL_WIKIPEDIA =
        "https://es.wikipedia.org/wiki/Beaucarnea_recurvata"

    private const val URL_WIKIPEDIA_API =
        "https://es.wikipedia.org/w/api.php"

    private fun cuidadosBase(): Map<String, String> {
        return mapOf(
            KEY_LUZ to
                    "Prefiere un lugar muy iluminado. Acostúmbrala gradualmente al sol directo.",

            KEY_RIEGO to
                    "Deja que el sustrato se seque antes de volver a regar. Evita el exceso de agua.",

            KEY_SUSTRATO to
                    "Utiliza un sustrato aireado y de buen drenaje.",

            KEY_TEMPERATURA to
                    "Evita las heladas y el frío intenso.",

            KEY_ALERTAS to
                    "Vigila el exceso de humedad y retira las hojas viejas cuando sea necesario."
        )
    }

    private fun solicitarJson(urlTexto: String): JSONObject {
        val connection = URL(urlTexto).openConnection()
                as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "User-Agent",
                "MacetaElefante/1.0 (Android; plant care)"
            )

            val status = connection.responseCode

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val contenido = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: ""

            if (status !in 200..299) {
                throw Exception(
                    "Error HTTP $status al consultar información."
                )
            }

            return JSONObject(contenido)
        } finally {
            connection.disconnect()
        }
    }

    private fun obtenerDescripcionWikipedia(): String {
        val titulo = URLEncoder.encode(
            "Beaucarnea recurvata",
            "UTF-8"
        )

        val url =
            "$URL_WIKIPEDIA_API?action=query" +
                    "&prop=extracts" +
                    "&exintro=1" +
                    "&explaintext=1" +
                    "&redirects=1" +
                    "&titles=$titulo" +
                    "&format=json"

        val respuesta = solicitarJson(url)

        val paginas = respuesta
            .getJSONObject("query")
            .getJSONObject("pages")

        val claves = paginas.keys()

        while (claves.hasNext()) {
            val clave = claves.next()
            val pagina = paginas.optJSONObject(clave) ?: continue

            val extracto = pagina.optString(
                "extract",
                ""
            ).trim()

            if (extracto.isNotBlank()) {
                return extracto
            }
        }

        throw Exception(
            "Wikipedia no devolvió una descripción general."
        )
    }

    private fun obtenerSeccion(
        secciones: JSONArray,
        tipo: String
    ): String {
        val descripciones = mutableListOf<String>()

        for (i in 0 until secciones.length()) {
            val seccion = secciones.optJSONObject(i) ?: continue

            if (
                seccion.optString("type")
                    .equals(tipo, ignoreCase = true)
            ) {
                val texto = seccion.optString(
                    "description",
                    ""
                ).trim()

                if (texto.isNotBlank()) {
                    descripciones.add(texto)
                }
            }
        }

        return descripciones.distinct().joinToString("\n\n")
    }

    private suspend fun <T> esperarTarea(
        tarea: Task<T>
    ): T = suspendCancellableCoroutine { continuation ->

        tarea.addOnSuccessListener { resultado ->
            if (continuation.isActive) {
                continuation.resume(resultado)
            }
        }

        tarea.addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }

        tarea.addOnCanceledListener {
            continuation.cancel()
        }
    }

    private suspend fun traducir(
        translator: Translator,
        texto: String
    ): String {
        if (texto.isBlank()) return ""

        return esperarTarea(
            translator.translate(texto)
        ).trim()
    }

    suspend fun descargar(context: Context): PlantCareInfo =
        withContext(Dispatchers.IO) {

            val apiKey = BuildConfig.PERENUAL_API_KEY.trim()

            if (apiKey.isBlank()) {
                throw Exception(
                    "No se encontró la clave API de Perenual."
                )
            }

            val claveCodificada = URLEncoder.encode(
                apiKey,
                "UTF-8"
            )

            val url =
                "$URL_PERENUAL?key=$claveCodificada&species_id=$SPECIES_ID"

            val respuesta = solicitarJson(url)

            val datos = respuesta.optJSONArray("data")
                ?: throw Exception(
                    "Perenual no devolvió la lista de cuidados."
                )

            var guia: JSONObject? = null

            for (i in 0 until datos.length()) {
                val elemento = datos.optJSONObject(i) ?: continue

                if (
                    elemento.optInt("species_id", -1) == SPECIES_ID
                ) {
                    guia = elemento
                    break
                }
            }

            val resultado = guia
                ?: throw Exception(
                    "No se encontró la guía de Beaucarnea recurvata."
                )

            val secciones = resultado.optJSONArray("section")
                ?: throw Exception(
                    "La guía no contiene secciones de cuidados."
                )

            val luzOriginal = obtenerSeccion(
                secciones,
                "sunlight"
            )

            val riegoOriginal = obtenerSeccion(
                secciones,
                "watering"
            )

            val podaOriginal = obtenerSeccion(
                secciones,
                "pruning"
            )

            val sueloOriginal = obtenerSeccion(
                secciones,
                "soil"
            )

            val temperaturaOriginal = obtenerSeccion(
                secciones,
                "temperature"
            )

            if (
                luzOriginal.isBlank() &&
                riegoOriginal.isBlank() &&
                podaOriginal.isBlank()
            ) {
                throw Exception(
                    "Perenual no devolvió descripciones detalladas."
                )
            }

            val opciones = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build()

            val translator = Translation.getClient(opciones)

            val luz: String
            val riego: String
            val poda: String
            val suelo: String
            val temperatura: String

            try {
                val condiciones = DownloadConditions.Builder()
                    .build()

                esperarTarea(
                    translator.downloadModelIfNeeded(condiciones)
                )

                luz = traducir(translator, luzOriginal)
                riego = traducir(translator, riegoOriginal)
                poda = traducir(translator, podaOriginal)
                suelo = traducir(translator, sueloOriginal)
                temperatura = traducir(
                    translator,
                    temperaturaOriginal
                )

            } finally {
                translator.close()
            }

            val descripcionWikipedia =
                obtenerDescripcionWikipedia()

            val cuidados = cuidadosBase()

            val info = PlantCareInfo(
                title = "Pie de elefante",
                description = "Beaucarnea recurvata",
                extract = descripcionWikipedia,
                sourceUrl = URL_WIKIPEDIA,
                updatedAt = System.currentTimeMillis(),

                luz = luz.ifBlank {
                    cuidados[KEY_LUZ].orEmpty()
                },

                riego = riego.ifBlank {
                    cuidados[KEY_RIEGO].orEmpty()
                },

                sustrato = suelo.ifBlank {
                    cuidados[KEY_SUSTRATO].orEmpty()
                },

                temperatura = temperatura.ifBlank {
                    cuidados[KEY_TEMPERATURA].orEmpty()
                },

                alertas = poda.ifBlank {
                    cuidados[KEY_ALERTAS].orEmpty()
                }
            )

            guardarLocalmente(context, info)

            info
        }

    fun obtenerGuardada(context: Context): PlantCareInfo? {
        val preferencias = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val fecha = preferencias.getLong(KEY_UPDATED_AT, 0L)

        if (fecha == 0L) return null

        val cuidados = cuidadosBase()

        return PlantCareInfo(
            title = preferencias.getString(
                KEY_TITLE,
                "Pie de elefante"
            ) ?: "Pie de elefante",

            description = preferencias.getString(
                KEY_DESCRIPTION,
                "Beaucarnea recurvata"
            ) ?: "Beaucarnea recurvata",

            extract = preferencias.getString(
                KEY_EXTRACT,
                ""
            ) ?: "",

            sourceUrl = preferencias.getString(
                KEY_SOURCE_URL,
                URL_WIKIPEDIA
            ) ?: URL_WIKIPEDIA,

            updatedAt = fecha,

            luz = preferencias.getString(
                KEY_LUZ,
                cuidados[KEY_LUZ]
            ) ?: "",

            riego = preferencias.getString(
                KEY_RIEGO,
                cuidados[KEY_RIEGO]
            ) ?: "",

            sustrato = preferencias.getString(
                KEY_SUSTRATO,
                cuidados[KEY_SUSTRATO]
            ) ?: "",

            temperatura = preferencias.getString(
                KEY_TEMPERATURA,
                cuidados[KEY_TEMPERATURA]
            ) ?: "",

            alertas = preferencias.getString(
                KEY_ALERTAS,
                cuidados[KEY_ALERTAS]
            ) ?: ""
        )
    }

    private fun guardarLocalmente(
        context: Context,
        info: PlantCareInfo
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(KEY_TITLE, info.title)
            .putString(KEY_DESCRIPTION, info.description)
            .putString(KEY_EXTRACT, info.extract)
            .putString(KEY_SOURCE_URL, info.sourceUrl)
            .putLong(KEY_UPDATED_AT, info.updatedAt)
            .putString(KEY_LUZ, info.luz)
            .putString(KEY_RIEGO, info.riego)
            .putString(KEY_SUSTRATO, info.sustrato)
            .putString(KEY_TEMPERATURA, info.temperatura)
            .putString(KEY_ALERTAS, info.alertas)
            .apply()
    }

    fun obtenerFuentes(): List<Pair<String, String>> {
        return listOf(
            "Wikipedia — Beaucarnea recurvata" to
                    URL_WIKIPEDIA,

            "Perenual — Guía de cuidados" to
                    "https://perenual.com/"
        )
    }
}
