
package com.example.macetaelefante.data

data class WeightRecord(
    val id: Long,
    val weightGrams: Double,
    val dateTime: String,
    val notes: String = "",
    val fotoBasculaPath: String? = null,
    val fotoPlantaPath: String? = null
)
