
package com.example.macetaelefante.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object WeightRepository {

    private const val FILE_NAME = "weight_records.json"

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun obtenerTodos(context: Context): List<WeightRecord> {
        val file = getFile(context)

        if (!file.exists()) {
            return emptyList()
        }

        return try {
            val contenido = file.readText()
            val jsonArray = JSONArray(contenido)
            val registros = mutableListOf<WeightRecord>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                val fotoBascula = if (
                    item.isNull("fotoBasculaPath")
                ) {
                    null
                } else {
                    item.optString("fotoBasculaPath")
                        .takeIf { it.isNotBlank() && it != "null" }
                }

                val fotoPlanta = if (
                    item.isNull("fotoPlantaPath")
                ) {
                    null
                } else {
                    item.optString("fotoPlantaPath")
                        .takeIf { it.isNotBlank() && it != "null" }
                }

                registros.add(
                    WeightRecord(
                        id = item.getLong("id"),
                        weightGrams = item.getDouble("weightGrams"),
                        dateTime = item.getString("dateTime"),
                        notes = item.optString("notes", ""),
                        fotoBasculaPath = fotoBascula,
                        fotoPlantaPath = fotoPlanta
                    )
                )
            }

            registros.sortedByDescending { it.id }

        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardar(
        context: Context,
        registro: WeightRecord
    ) {
        val registros = obtenerTodos(context).toMutableList()

        registros.removeAll { it.id == registro.id }
        registros.add(registro)

        escribirRegistros(context, registros)
    }

    // Elimina solamente la medición cuyo ID se recibe.
    // Devuelve true si encontró y eliminó el registro.
    fun eliminar(
        context: Context,
        id: Long
    ): Boolean {
        val registros = obtenerTodos(context).toMutableList()
        val cantidadAnterior = registros.size

        registros.removeAll { it.id == id }

        if (registros.size == cantidadAnterior) {
            return false
        }

        escribirRegistros(context, registros)
        return true
    }

    private fun escribirRegistros(
        context: Context,
        registros: List<WeightRecord>
    ) {
        val jsonArray = JSONArray()

        registros.forEach { registro ->
            val item = JSONObject().apply {
                put("id", registro.id)
                put("weightGrams", registro.weightGrams)
                put("dateTime", registro.dateTime)
                put("notes", registro.notes)

                put(
                    "fotoBasculaPath",
                    registro.fotoBasculaPath ?: JSONObject.NULL
                )

                put(
                    "fotoPlantaPath",
                    registro.fotoPlantaPath ?: JSONObject.NULL
                )
            }

            jsonArray.put(item)
        }

        val file = getFile(context)

        val archivoTemporal = File(
            context.filesDir,
            "$FILE_NAME.tmp"
        )

        archivoTemporal.writeText(jsonArray.toString())

        if (!archivoTemporal.renameTo(file)) {
            file.writeText(jsonArray.toString())
            archivoTemporal.delete()
        }
    }

    fun obtenerUltimo(context: Context): WeightRecord? {
        return obtenerTodos(context).maxByOrNull { it.id }
    }

    fun eliminarTodos(context: Context) {
        getFile(context).delete()
    }
}