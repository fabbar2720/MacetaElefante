package com.example.macetaelefante.data

data class WateringRecord(
    val id: Long,
    val dateTime: String,
    val waterAmountMl: Int,
    val weightBeforeGrams: Double? = null,
    val weightAfterGrams: Double? = null,
    val notes: String = ""
)