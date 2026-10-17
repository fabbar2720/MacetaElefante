
package com.example.macetaelefante.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.macetaelefante.data.WateringRecord
import com.example.macetaelefante.data.WateringRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WateringEntryScreen(
    onGuardado: () -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current

    var cantidadAgua by remember { mutableStateOf("") }
    var pesoAntes by remember { mutableStateOf("") }
    var pesoDespues by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    val fechaHora = remember {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Registrar riego",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(text = "Fecha y hora: $fechaHora")

        OutlinedTextField(
            value = cantidadAgua,
            onValueChange = { entrada ->
                cantidadAgua = entrada.filter { it.isDigit() }
            },
            label = { Text("Cantidad de agua (ml)") },
            placeholder = { Text("Ej. 100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = pesoAntes,
            onValueChange = { pesoAntes = it },
            label = { Text("Peso antes del riego (g, opcional)") },
            placeholder = { Text("Ej. 2450.5") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = pesoDespues,
            onValueChange = { pesoDespues = it },
            label = { Text("Peso después del riego (g, opcional)") },
            placeholder = { Text("Ej. 2550.5") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = notas,
            onValueChange = { notas = it },
            label = { Text("Notas (opcional)") },
            placeholder = {
                Text("Ej. Regué lentamente alrededor de la base")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val cantidad = cantidadAgua.toIntOrNull()

                if (cantidad == null || cantidad <= 0) {
                    Toast.makeText(
                        context,
                        "Ingresa una cantidad de agua válida en ml",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val antes = pesoAntes
                    .trim()
                    .replace(',', '.')
                    .toDoubleOrNull()

                val despues = pesoDespues
                    .trim()
                    .replace(',', '.')
                    .toDoubleOrNull()

                if (pesoAntes.isNotBlank() && antes == null) {
                    Toast.makeText(
                        context,
                        "El peso anterior no es válido",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                if (pesoDespues.isNotBlank() && despues == null) {
                    Toast.makeText(
                        context,
                        "El peso posterior no es válido",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val registro = WateringRecord(
                    id = System.currentTimeMillis(),
                    dateTime = fechaHora,
                    waterAmountMl = cantidad,
                    weightBeforeGrams = antes,
                    weightAfterGrams = despues,
                    notes = notas.trim()
                )

                try {
                    WateringRepository.guardar(context, registro)

                    Toast.makeText(
                        context,
                        "Riego guardado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    onGuardado()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "No se pudo guardar el riego",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar riego")
        }

        Button(
            onClick = onCancelar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}
