
package com.example.macetaelefante.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object WateringRepository {

    private const val FILE_NAME = "watering_records.json"

    fun obtenerTodos(context: Context): List<WateringRecord> {
        val archivo = File(context.filesDir, FILE_NAME)

        if (!archivo.exists()) {
            return emptyList()
        }

        return try {
            val contenido = archivo.readText()
            val jsonArray = JSONArray(contenido)

            val registros = mutableListOf<WateringRecord>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                registros.add(
                    WateringRecord(
                        id = json.getLong("id"),
                        dateTime = json.getString("dateTime"),
                        waterAmountMl = json.getInt("waterAmountMl"),
                        weightBeforeGrams = if (
                            json.isNull("weightBeforeGrams")
                        ) {
                            null
                        } else {
                            json.getDouble("weightBeforeGrams")
                        },
                        weightAfterGrams = if (
                            json.isNull("weightAfterGrams")
                        ) {
                            null
                        } else {
                            json.getDouble("weightAfterGrams")
                        },
                        notes = json.optString("notes", "")
                    )
                )
            }

            registros.sortedByDescending { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardar(context: Context, registro: WateringRecord) {
        val registros = obtenerTodos(context).toMutableList()

        registros.removeAll { it.id == registro.id }
        registros.add(registro)

        val jsonArray = JSONArray()

        registros.forEach { item ->
            val json = JSONObject()

            json.put("id", item.id)
            json.put("dateTime", item.dateTime)
            json.put("waterAmountMl", item.waterAmountMl)

            json.put(
                "weightBeforeGrams",
                item.weightBeforeGrams ?: JSONObject.NULL
            )

            json.put(
                "weightAfterGrams",
                item.weightAfterGrams ?: JSONObject.NULL
            )

            json.put("notes", item.notes)

            jsonArray.put(json)
        }

        File(context.filesDir, FILE_NAME).writeText(
            jsonArray.toString()
        )
    }

    fun obtenerUltimo(context: Context): WateringRecord? {
        return obtenerTodos(context).maxByOrNull { it.id }
    }

    fun eliminar(context: Context, id: Long) {
        val registros = obtenerTodos(context)
            .filterNot { it.id == id }

        val jsonArray = JSONArray()

        registros.forEach { item ->
            val json = JSONObject()

            json.put("id", item.id)
            json.put("dateTime", item.dateTime)
            json.put("waterAmountMl", item.waterAmountMl)

            json.put(
                "weightBeforeGrams",
                item.weightBeforeGrams ?: JSONObject.NULL
            )

            json.put(
                "weightAfterGrams",
                item.weightAfterGrams ?: JSONObject.NULL
            )

            json.put("notes", item.notes)

            jsonArray.put(json)
        }

        File(context.filesDir, FILE_NAME).writeText(
            jsonArray.toString()
        )
    }

    fun eliminarTodos(context: Context) {
        File(context.filesDir, FILE_NAME).writeText("[]")
    }
}
