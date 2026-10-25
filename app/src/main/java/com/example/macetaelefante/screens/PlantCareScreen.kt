
package com.example.macetaelefante.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.macetaelefante.data.PlantCareInfo
import com.example.macetaelefante.data.PlantCareRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextDecoration

private val VerdeBosque = Color(0xFF355E3B)
private val VerdeSalvia = Color(0xFF8A9A5B)
private val Terracota = Color(0xFFB77950)
private val Arena = Color(0xFFF3EBDD)
private val Blanco = Color(0xFFFFFFFF)
private val TextoOscuro = Color(0xFF343A30)
private val VerdeClaro = Color(0xFFE8EDDF)
private val TerracotaClaro = Color(0xFFF4E5DA)

@Composable
fun PlantCareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var informacion by remember {
        mutableStateOf<PlantCareInfo?>(null)
    }

    var cargando by remember {
        mutableStateOf(false)
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        informacion = PlantCareRepository.obtenerGuardada(context)
    }

    Scaffold(
        containerColor = Arena
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Guía de cuidados",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            Text(
                text = "Pie de elefante · Beaucarnea recurvata",
                style = MaterialTheme.typography.titleMedium,
                color = TextoOscuro
            )

            Button(
                onClick = {
                    scope.launch {
                        cargando = true
                        mensaje = ""

                        try {
                            informacion =
                                PlantCareRepository.descargar(context)

                            mensaje =
                                "Información actualizada y guardada correctamente."
                        } catch (e: Exception) {
                            mensaje =
                                "No se pudo actualizar la información. " +
                                        "Se conservarán los datos guardados. " +
                                        (e.localizedMessage ?: "")
                        } finally {
                            cargando = false
                        }
                    }
                },
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeBosque,
                    contentColor = Blanco
                )
            ) {
                Text(
                    if (cargando) {
                        "Descargando información..."
                    } else {
                        "Descargar / actualizar información"
                    }
                )
            }

            if (mensaje.isNotBlank()) {
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = VerdeBosque
                )
            }

            val info = informacion

            if (info == null) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = VerdeClaro
                    )
                ) {
                    Text(
                        text = "Todavía no has descargado la información " +
                                "de la planta. Pulsa el botón para obtenerla.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify,
                        color = TextoOscuro
                    )
                }

            } else {
                Text(
                    text = "Información general",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Blanco
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {



                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VerdeBosque
                        )

                        Text(
                            text = info.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoOscuro
                        )

                        Text(
                            text = info.extract,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Justify,
                            color = TextoOscuro
                        )

                        Text(
                            text = "Última descarga: " +
                                    SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(info.updatedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = VerdeSalvia
                        )


                    }
                }
            }

            Text(
                text = "Cuidados generales",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            SeccionCuidado(
                titulo = "Luz",
                contenido = info?.luz ?: (
                        "Prefiere un lugar muy iluminado. " +
                                "Acostúmbrala gradualmente si la cambias " +
                                "a un sitio con sol directo."
                        ),
                colorFondo = VerdeClaro,
                colorTitulo = VerdeBosque
            )

            SeccionCuidado(
                titulo = "Riego",
                contenido = info?.riego ?: (
                        "Es resistente a la sequía. Deja que el " +
                                "sustrato se seque entre riegos y reduce " +
                                "el riego en invierno."
                        ),
                colorFondo = TerracotaClaro,
                colorTitulo = Terracota
            )

            SeccionCuidado(
                titulo = "Sustrato y drenaje",
                contenido = info?.sustrato ?: (
                        "Usa una mezcla aireada y una maceta " +
                                "con orificios de drenaje. Evita dejar " +
                                "agua acumulada."
                        ),
                colorFondo = VerdeClaro,
                colorTitulo = VerdeBosque
            )

            SeccionCuidado(
                titulo = "Temperatura",
                contenido = info?.temperatura ?: (
                        "Evita el frío intenso y las heladas. " +
                                "Protégela de cambios bruscos de temperatura."
                        ),
                colorFondo = TerracotaClaro,
                colorTitulo = Terracota
            )

            SeccionCuidado(
                titulo = "Señales de alerta y poda",
                contenido = info?.alertas ?: (
                        "Vigila si el sustrato permanece húmedo " +
                                "mucho tiempo o si la base del tronco " +
                                "se vuelve blanda."
                        ),
                colorFondo = VerdeClaro,
                colorTitulo = VerdeBosque
            )

            Text(
                text = "Fuentes de información",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            Text(
                text = "Wikipedia — Beaucarnea recurvata",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeBosque,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        abrirEnlace(
                            context,
                            "https://es.wikipedia.org/wiki/Beaucarnea_recurvata"
                        )
                    }
                    .padding(vertical = 6.dp)
            )

            Text(
                text = "Perenual — Cuidados de la planta",
                style = MaterialTheme.typography.bodyMedium,
                color = VerdeBosque,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        abrirEnlace(
                            context,
                            "https://perenual.com/"
                        )
                    }
                    .padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracota,
                    contentColor = Blanco
                )
            ) {
                Text("Volver al inicio")
            }
        }
    }
}

private fun abrirEnlace(
    context: Context,
    url: String
) {
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    } catch (_: Exception) {
        // La pantalla permanece abierta si no se puede abrir el enlace.
    }
}

@Composable
private fun SeccionCuidado(
    titulo: String,
    contenido: String,
    colorFondo: Color,
    colorTitulo: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorFondo
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorTitulo
            )

            Text(
                text = contenido,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify,
                color = TextoOscuro
            )
        }
    }
}
