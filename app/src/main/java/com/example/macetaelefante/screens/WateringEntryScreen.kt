
package com.example.macetaelefante.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.macetaelefante.data.WateringRecord
import com.example.macetaelefante.data.WateringRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val VerdeBosque = Color(0xFF355E3B)
private val VerdeSalvia = Color(0xFF8A9A5B)
private val Terracota = Color(0xFFB77950)
private val Arena = Color(0xFFF3EBDD)
private val Blanco = Color(0xFFFFFFFF)
private val TextoOscuro = Color(0xFF343A30)

@Composable
fun WateringEntryScreen(
    onGuardado: () -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cantidadAgua by remember {
        mutableStateOf("")
    }

    var pesoAntes by remember {
        mutableStateOf("")
    }

    var pesoDespues by remember {
        mutableStateOf("")
    }

    var notas by remember {
        mutableStateOf("")
    }

    val cantidadRequester = remember {
        BringIntoViewRequester()
    }

    val antesRequester = remember {
        BringIntoViewRequester()
    }

    val despuesRequester = remember {
        BringIntoViewRequester()
    }

    val notasRequester = remember {
        BringIntoViewRequester()
    }

    val formatoFechaHora = remember {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        )
    }

    var fechaHora by remember {
        mutableStateOf(
            formatoFechaHora.format(Date())
        )
    }

    fun obtenerCalendario(): Calendar {
        val calendario = Calendar.getInstance()

        try {
            val fecha = formatoFechaHora.parse(fechaHora)
            if (fecha != null) {
                calendario.time = fecha
            }
        } catch (_: Exception) {
            // Se conserva la fecha actual.
        }

        return calendario
    }

    Scaffold(
        containerColor = Arena
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Registrar riego",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            // Fecha, calendario y reloj en un solo renglón.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {

                Text(
                    text = fechaHora,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextoOscuro
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VerdeBosque
                ) {
                    IconButton(
                        onClick = {
                            val calendario = obtenerCalendario()

                            DatePickerDialog(
                                context,
                                { _, anio, mes, dia ->
                                    val seleccion = obtenerCalendario()

                                    seleccion.set(
                                        Calendar.YEAR,
                                        anio
                                    )
                                    seleccion.set(
                                        Calendar.MONTH,
                                        mes
                                    )
                                    seleccion.set(
                                        Calendar.DAY_OF_MONTH,
                                        dia
                                    )

                                    fechaHora = formatoFechaHora.format(
                                        seleccion.time
                                    )
                                },
                                calendario.get(Calendar.YEAR),
                                calendario.get(Calendar.MONTH),
                                calendario.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Text(
                            text = "📅",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VerdeBosque
                ) {
                    IconButton(
                        onClick = {
                            val calendario = obtenerCalendario()

                            TimePickerDialog(
                                context,
                                { _, hora, minuto ->
                                    val seleccion = obtenerCalendario()

                                    seleccion.set(
                                        Calendar.HOUR_OF_DAY,
                                        hora
                                    )
                                    seleccion.set(
                                        Calendar.MINUTE,
                                        minuto
                                    )

                                    fechaHora = formatoFechaHora.format(
                                        seleccion.time
                                    )
                                },
                                calendario.get(Calendar.HOUR_OF_DAY),
                                calendario.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                    ) {
                        Text(
                            text = "🕒",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            OutlinedTextField(
                value = cantidadAgua,
                onValueChange = { entrada ->
                    cantidadAgua = entrada.filter {
                        it.isDigit()
                    }
                },
                label = {
                    Text("Cantidad de agua (ml)")
                },
                placeholder = {
                    Text("Ej. 100")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(cantidadRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                delay(300)
                                cantidadRequester.bringIntoView()
                            }
                        }
                    },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedContainerColor = Blanco,
                    unfocusedContainerColor = Blanco,
                    focusedIndicatorColor = VerdeBosque,
                    unfocusedIndicatorColor = VerdeSalvia,
                    cursorColor = VerdeBosque
                )
            )

            OutlinedTextField(
                value = pesoAntes,
                onValueChange = {
                    pesoAntes = it
                },
                label = {
                    Text("Peso antes del riego (g, opcional)")
                },
                placeholder = {
                    Text("Ej. 2450.5")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(antesRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                delay(300)
                                antesRequester.bringIntoView()
                            }
                        }
                    },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedContainerColor = Blanco,
                    unfocusedContainerColor = Blanco,
                    focusedIndicatorColor = VerdeBosque,
                    unfocusedIndicatorColor = VerdeSalvia,
                    cursorColor = VerdeBosque
                )
            )

            OutlinedTextField(
                value = pesoDespues,
                onValueChange = {
                    pesoDespues = it
                },
                label = {
                    Text("Peso después del riego (g, opcional)")
                },
                placeholder = {
                    Text("Ej. 2550.5")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(despuesRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                delay(300)
                                despuesRequester.bringIntoView()
                            }
                        }
                    },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedContainerColor = Blanco,
                    unfocusedContainerColor = Blanco,
                    focusedIndicatorColor = VerdeBosque,
                    unfocusedIndicatorColor = VerdeSalvia,
                    cursorColor = VerdeBosque
                )
            )

            OutlinedTextField(
                value = notas,
                onValueChange = {
                    notas = it
                },
                label = {
                    Text("Notas (opcional)")
                },
                placeholder = {
                    Text(
                        "Ej. Regué lentamente alrededor de la base"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(notasRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                delay(300)
                                notasRequester.bringIntoView()
                            }
                        }
                    },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedContainerColor = Blanco,
                    unfocusedContainerColor = Blanco,
                    focusedIndicatorColor = VerdeBosque,
                    unfocusedIndicatorColor = VerdeSalvia,
                    cursorColor = VerdeBosque
                )
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracota,
                        contentColor = Blanco
                    )
                ) {
                    Text("Cancelar")
                }

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

                        if (
                            pesoAntes.isNotBlank() &&
                            antes == null
                        ) {
                            Toast.makeText(
                                context,
                                "El peso anterior no es válido",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        if (
                            pesoDespues.isNotBlank() &&
                            despues == null
                        ) {
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
                            WateringRepository.guardar(
                                context,
                                registro
                            )

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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdeBosque,
                        contentColor = Blanco
                    )
                ) {
                    Text("Guardar riego")
                }
            }
        }
    }
}
