
package com.example.macetaelefante.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.macetaelefante.data.WateringRecord
import com.example.macetaelefante.data.WateringRepository

@Composable
fun WateringHistoryScreen(
    onBack: () -> Unit,
    onRegistroEliminado: () -> Unit
) {
    val context = LocalContext.current

    var registros by remember {
        mutableStateOf(WateringRepository.obtenerTodos(context))
    }

    var registroParaEliminar by remember {
        mutableStateOf<WateringRecord?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial de riegos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (registros.isEmpty()) {
            Text("Todavía no hay riegos registrados.")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = registros,
                    key = { it.id }
                ) { registro ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = registro.dateTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Agua utilizada: ${registro.waterAmountMl} ml"
                            )

                            registro.weightBeforeGrams?.let {
                                Text(
                                    text = "Peso antes: ${formatearPeso(it)} g"
                                )
                            }

                            registro.weightAfterGrams?.let {
                                Text(
                                    text = "Peso después: ${formatearPeso(it)} g"
                                )
                            }

                            if (registro.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Notas: ${registro.notes}"
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        registroParaEliminar = registro
                                    }
                                ) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    }

    registroParaEliminar?.let { registro ->
        AlertDialog(
            onDismissRequest = {
                registroParaEliminar = null
            },
            title = {
                Text("Eliminar registro")
            },
            text = {
                Text(
                    "¿Quieres eliminar este registro de riego? " +
                            "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        WateringRepository.eliminar(
                            context,
                            registro.id
                        )

                        registros = WateringRepository.obtenerTodos(context)
                        registroParaEliminar = null
                        onRegistroEliminado()
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        registroParaEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun formatearPeso(peso: Double): String {
    return String.format(
        java.util.Locale.getDefault(),
        "%.1f",
        peso
    )
}
