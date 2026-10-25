

package com.example.macetaelefante.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.macetaelefante.data.WeightRecord
import com.example.macetaelefante.data.WeightRepository
import com.example.macetaelefante.data.WateringRecord
import com.example.macetaelefante.data.WateringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size

private val VerdeBosque = Color(0xFF355E3B)
private val VerdeSalvia = Color(0xFF8A9A5B)
private val Terracota = Color(0xFFB77950)
private val Arena = Color(0xFFF3EBDD)
private val Blanco = Color(0xFFFFFFFF)
private val TextoOscuro = Color(0xFF343A30)
private val VerdeClaro = Color(0xFFE8EDDF)
private val TerracotaClaro = Color(0xFFF4E5DA)

private const val MILISEGUNDOS_DIA = 86_400_000L

private fun convertirFechaAMillis(
    fecha: String,
    idAlternativo: Long
): Long {
    return try {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).parse(fecha)?.time ?: idAlternativo
    } catch (e: Exception) {
        idAlternativo
    }
}

private data class EventoHistorial(
    val clave: String,
    val fechaMillis: Long,
    val peso: WeightRecord? = null,
    val riego: WateringRecord? = null
)

@Composable
fun WeightHistoryScreen(
    onBack: () -> Unit,
    onRegistroEliminado: () -> Unit = {}
) {
    val context = LocalContext.current

    var registros by remember {
        mutableStateOf<List<WeightRecord>>(emptyList())
    }

    var riegos by remember {
        mutableStateOf<List<WateringRecord>>(emptyList())
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var registroPendienteEliminar by remember {
        mutableStateOf<WeightRecord?>(null)
    }
    var riegoPendienteEliminar by remember {
        mutableStateOf<WateringRecord?>(null)
    }

    var eliminando by remember {
        mutableStateOf(false)
    }

    var mensaje by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        val resultado = withContext(Dispatchers.IO) {
            Pair(
                WeightRepository.obtenerTodos(context),
                WateringRepository.obtenerTodos(context)
            )
        }

        registros = resultado.first
        riegos = resultado.second
        cargando = false
    }

    val registrosGrafica = remember(registros) {
        registros.sortedBy {
            convertirFechaAMillis(it.dateTime, it.id)
        }
    }

    val eventosHistorial = remember(registros, riegos) {
        val eventos = mutableListOf<EventoHistorial>()

        registros.forEach { registro ->
            eventos.add(
                EventoHistorial(
                    clave = "peso_${registro.id}",
                    fechaMillis = convertirFechaAMillis(
                        registro.dateTime,
                        registro.id
                    ),
                    peso = registro
                )
            )
        }

        riegos.forEach { riego ->
            eventos.add(
                EventoHistorial(
                    clave = "riego_${riego.id}",
                    fechaMillis = convertirFechaAMillis(
                        riego.dateTime,
                        riego.id
                    ),
                    riego = riego
                )
            )
        }

        eventos.sortedByDescending { it.fechaMillis }
    }

    if (registroPendienteEliminar != null) {
        val registro = registroPendienteEliminar!!

        AlertDialog(onDismissRequest = {
            if (!eliminando) {
                registroPendienteEliminar = null
            }
        },
            title = {
                Text("Eliminar medición")
            },
            text = {
                Text(
                    "¿Quieres eliminar la medición de " +
                            String.format(
                                Locale.getDefault(),
                                "%.1f g",
                                registro.weightGrams
                            ) +
                            " del ${registro.dateTime}?\n\n" +
                            "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !eliminando,
                    onClick = {
                        eliminando = true
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !eliminando,
                    onClick = {
                        registroPendienteEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    if (riegoPendienteEliminar != null) {
        val riego = riegoPendienteEliminar!!

        AlertDialog(
            onDismissRequest = {
                if (!eliminando) {
                    riegoPendienteEliminar = null
                }
            },
            title = {
                Text("Eliminar riego")
            },
            text = {
                Text(
                    "¿Quieres eliminar el riego del ${riego.dateTime} " +
                            "con ${riego.waterAmountMl} ml de agua?\n\n" +
                            "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !eliminando,
                    onClick = {
                        eliminando = true
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !eliminando,
                    onClick = {
                        riegoPendienteEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    LaunchedEffect(
        eliminando,
        registroPendienteEliminar,
        riegoPendienteEliminar
    ) {
        if (!eliminando) return@LaunchedEffect

        val registroPeso = registroPendienteEliminar
        val registroRiego = riegoPendienteEliminar

        if (registroPeso != null) {
            val eliminado = withContext(Dispatchers.IO) {
                WeightRepository.eliminar(context, registroPeso.id)
            }

            if (eliminado) {
                registros = withContext(Dispatchers.IO) {
                    WeightRepository.obtenerTodos(context)
                }

                mensaje = "Medición eliminada correctamente."
                onRegistroEliminado()
            } else {
                mensaje = "No se pudo eliminar la medición."
            }

            registroPendienteEliminar = null
        } else if (registroRiego != null) {
            withContext(Dispatchers.IO) {
                WateringRepository.eliminar(context, registroRiego.id)
            }

            riegos = withContext(Dispatchers.IO) {
                WateringRepository.obtenerTodos(context)
            }

            mensaje = "Riego eliminado correctamente."
            onRegistroEliminado()

            riegoPendienteEliminar = null
        }

        eliminando = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Arena
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            Text(
                text = "Historial",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (mensaje != null) {
                Text(
                    text = mensaje!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdeBosque
                )

                TextButton(
                    onClick = { mensaje = null }
                ) {
                    Text("Cerrar aviso")
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            when {
                cargando -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cargando registros...")
                    }
                }

                registros.isEmpty() && riegos.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Todavía no hay registros.\n\n" +
                                    "Registra tu primer peso o riego " +
                                    "para que aparezca aquí."
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "analisis_riego") {
                            AnalisisDesdeUltimoRiego(
                                registros = registros,
                                riegos = riegos
                            )
                        }

                        item(key = "grafica_peso") {
                            GraficaEvolucionPeso(
                                registros = registrosGrafica,
                                riegos = riegos
                            )
                        }

                        item(key = "titulo_historial_combinado") {
                            Text(
                                text = "Historial combinado",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = VerdeBosque
                            )
                        }

                        items(
                            items = eventosHistorial,
                            key = { it.clave }
                        ) { evento ->
                            if (evento.peso != null) {
                                RegistroPesoCard(
                                    registro = evento.peso,
                                    onEliminar = {
                                        registroPendienteEliminar =
                                            evento.peso
                                    }
                                )
                            } else if (evento.riego != null) {
                                RegistroRiegoCard(
                                    registro = evento.riego,
                                    onEliminar = {
                                        riegoPendienteEliminar = evento.riego
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AnalisisDesdeUltimoRiego(
    registros: List<WeightRecord>,
    riegos: List<WateringRecord>
) {
    val riegoReciente = riegos.maxByOrNull {
        convertirFechaAMillis(it.dateTime, it.id)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Blanco
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Análisis desde el último riego",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (riegoReciente == null) {
                Text(
                    text = "Todavía no hay riegos registrados. " +
                            "Cuando registres uno, podrás comparar " +
                            "las mediciones posteriores.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            val fechaRiego = convertirFechaAMillis(
                riegoReciente.dateTime,
                riegoReciente.id
            )

            val pesosPosterioresAlRiego = registros
                .filter {
                    convertirFechaAMillis(it.dateTime, it.id) > fechaRiego
                }
                .sortedBy {
                    convertirFechaAMillis(it.dateTime, it.id)
                }

            val pesoDespuesRegistrado = riegoReciente.weightAfterGrams
            val primeraMedicionPosterior =
                pesosPosterioresAlRiego.firstOrNull()

            val pesoReferencia: Double? =
                pesoDespuesRegistrado
                    ?: primeraMedicionPosterior?.weightGrams

            val fechaReferencia: Long? =
                if (pesoDespuesRegistrado != null) {
                    fechaRiego
                } else {
                    primeraMedicionPosterior?.let {
                        convertirFechaAMillis(it.dateTime, it.id)
                    }
                }

            Text(
                text = "Último riego: ${riegoReciente.dateTime}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Agua aplicada: ${riegoReciente.waterAmountMl} ml",
                style = MaterialTheme.typography.bodyMedium
            )

            if (pesoReferencia == null || fechaReferencia == null) {
                Text(
                    text = "Todavía falta una medición de peso posterior " +
                            "al riego para establecer una referencia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoOscuro
                )
                return@Column
            }

            val etiquetaReferencia =
                if (pesoDespuesRegistrado != null) {
                    "Peso de referencia (registrado después del riego)"
                } else {
                    "Peso de referencia (primera medición posterior)"
                }

            Text(
                text = etiquetaReferencia,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoOscuro
            )

            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%.1f g",
                    pesoReferencia
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            val medicionesPosteriores = registros
                .filter {
                    convertirFechaAMillis(it.dateTime, it.id) >
                            fechaReferencia
                }
                .sortedBy {
                    convertirFechaAMillis(it.dateTime, it.id)
                }

            val ultimaMedicion = medicionesPosteriores.lastOrNull()

            if (ultimaMedicion == null) {
                Text(
                    text = "Aún no hay mediciones posteriores a la " +
                            "referencia para calcular la variación.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val fechaUltimaMedicion = convertirFechaAMillis(
                    ultimaMedicion.dateTime,
                    ultimaMedicion.id
                )

                val cambio = ultimaMedicion.weightGrams - pesoReferencia
                val cambioAbsoluto = abs(cambio)

                val descripcionCambio = when {
                    cambio < -0.05 -> "Pérdida de peso"
                    cambio > 0.05 -> "Ganancia de peso"
                    else -> "Peso prácticamente estable"
                }

                val diasDesdeRiego =
                    ((fechaUltimaMedicion - fechaRiego)
                        .coerceAtLeast(0L) / MILISEGUNDOS_DIA)

                val milisegundosDesdeReferencia =
                    (fechaUltimaMedicion - fechaReferencia)
                        .coerceAtLeast(0L)

                val diasDesdeReferencia =
                    milisegundosDesdeReferencia / MILISEGUNDOS_DIA

                val horasDesdeReferencia =
                    (milisegundosDesdeReferencia % MILISEGUNDOS_DIA) /
                            3_600_000L

                val diasParaCalculo =
                    milisegundosDesdeReferencia.toDouble() /
                            MILISEGUNDOS_DIA.toDouble()

                val cambioPorDia =
                    if (diasParaCalculo > 0.0) {
                        cambio / diasParaCalculo
                    } else {
                        null
                    }

                Text(
                    text = "Última medición: ${ultimaMedicion.dateTime}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "Peso actual registrado: %.1f g",
                        ultimaMedicion.weightGrams
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "%s: %.1f g",
                        descripcionCambio,
                        cambioAbsoluto
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        cambio < -0.05 ->
                            Terracota
                        cambio > 0.05 ->
                            VerdeBosque
                        else ->
                            TextoOscuro
                    }
                )

                Text(
                    text = if (diasDesdeRiego == 1L) {
                        "Tiempo desde el riego: 1 día"
                    } else {
                        "Tiempo desde el riego: $diasDesdeRiego días"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Tiempo desde el peso de referencia: " +
                            "$diasDesdeReferencia días y " +
                            "$horasDesdeReferencia horas",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (cambioPorDia != null) {
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "Cambio promedio: %+.1f g por día",
                            cambioPorDia
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Registra otra medición en un momento " +
                                "posterior para calcular el cambio promedio " +
                                "por día.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "La variación de peso no determina por sí sola " +
                        "si debes regar. También considera la humedad " +
                        "del sustrato y el estado de la planta.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoOscuro
            )
        }
    }
}


@Composable
private fun RegistroRiegoCard(
    registro: WateringRecord,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Blanco
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💧 Riego registrado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = registro.dateTime,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoOscuro
            )

            Text(
                text = "Cantidad de agua: ${registro.waterAmountMl} ml",
                style = MaterialTheme.typography.bodyLarge
            )

            registro.weightBeforeGrams?.let { peso ->
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "Peso antes del riego: %.1f g",
                        peso
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            registro.weightAfterGrams?.let { peso ->
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "Peso después del riego: %.1f g",
                        peso
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (registro.notes.isNotBlank()) {
                Text(
                    text = "Notas: ${registro.notes}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            HorizontalDivider()

            TextButton(
                onClick = onEliminar,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Eliminar riego",
                    color = Terracota
                )
            }
        }
    }
}

@Composable
private fun GraficaEvolucionPeso(
    registros: List<WeightRecord>,
    riegos: List<WateringRecord>
){
    val colorLinea = Color(0xFF355E3B)
    val colorTexto = Color(0xFF343A30)
    val colorCuadricula = Color(0xFFD5D0C4)
    val colorFondo = Blanco

    val activity = LocalContext.current as? Activity

    var graficaAmpliada by rememberSaveable {
        mutableStateOf(false)
    }

    var orientacionAnterior by rememberSaveable {
        mutableStateOf(
            activity?.requestedOrientation
                ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorFondo
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EncabezadoGraficaPeso()

            if (registros.size < 2) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Necesitas al menos dos mediciones " +
                            "para mostrar la evolución de tu planta.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                GraficaLineal(
                    registros = registros,
                    riegos = riegos,
                    colorLinea = colorLinea,
                    colorTexto = colorTexto,
                    colorCuadricula = colorCuadricula
                )

                Text(
                    text = "${registros.size} mediciones registradas",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoOscuro
                )

                TextButton(
                    onClick = {
                        activity?.let {
                            orientacionAnterior = it.requestedOrientation
                            it.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }

                        graficaAmpliada = true
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Ampliar gráfica",
                        color = Terracota
                    )
                }
            }
        }
    }

    if (graficaAmpliada && registros.size >= 2) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                graficaAmpliada = false
                activity?.requestedOrientation = orientacionAnterior
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .background(Arena)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EncabezadoGraficaPeso(ampliado = true)



                GraficaLineal(
                    registros = registros,
                    riegos = riegos,
                    colorLinea = colorLinea,
                    colorTexto = colorTexto,
                    colorCuadricula = colorCuadricula,
                    alturaGrafica = 0.dp,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = {
                        graficaAmpliada = false
                        activity?.requestedOrientation = orientacionAnterior
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Cerrar gráfica",
                        color = Terracota
                    )
                }
            }
        }
    }
}

@Composable
private fun EncabezadoGraficaPeso(
    ampliado: Boolean = false
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Evolución del peso",
            style = if (ampliado) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.Bold,
            color = VerdeBosque,
            modifier = Modifier.weight(1f)
        )

        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = VerdeBosque,
                            shape = CircleShape
                        )
                )

                Text(
                    text = "Peso",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "💧",
                    color = Terracota,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Riego",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
@Composable
private fun GraficaLineal(
    registros: List<WeightRecord>,
    riegos: List<WateringRecord>,
    colorLinea: Color,
    colorTexto: Color,
    colorCuadricula: Color,
    alturaGrafica: androidx.compose.ui.unit.Dp = 300.dp,
    modifier: Modifier = Modifier
) {
    var indiceSeleccionado by remember(registros, riegos) {
        mutableStateOf<Int?>(null)
    }

    var indiceRiegoSeleccionado by remember(registros, riegos) {
        mutableStateOf<Int?>(null)
    }

    val fechasPesos = remember(registros) {
        registros.map {
            convertirFechaAMillis(it.dateTime, it.id)
        }
    }

    val fechasRiegos = remember(riegos) {
        riegos.map {
            convertirFechaAMillis(it.dateTime, it.id)
        }
    }

    val todasLasFechas = fechasPesos + fechasRiegos
    val fechaMinima = todasLasFechas.minOrNull() ?: 0L
    val fechaMaxima = todasLasFechas.maxOrNull() ?: fechaMinima

    val pesos = registros.map {
        it.weightGrams.coerceAtLeast(0.0)
    }

    val pesoMinimo = pesos.minOrNull() ?: 0.0
    val pesoMaximo = pesos.maxOrNull() ?: 0.0

    val limiteInferior =
        (kotlin.math.floor(pesoMinimo / 10.0) * 10.0)
            .coerceAtLeast(0.0)

    val limiteSuperior = (
            ceil((pesoMaximo - limiteInferior) / 4.0)
                .toInt()
                .coerceAtLeast(1) * 4.0 + limiteInferior
            ).coerceAtLeast(limiteInferior + 4.0)

    val colorLineaArgb = colorLinea.toArgb()
    val colorTextoArgb = colorTexto.toArgb()
    val colorCuadriculaArgb = colorCuadricula.toArgb()
    val colorRiego = Color(0xFFB77950)
    val colorRiegoArgb = colorRiego.toArgb()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (alturaGrafica > 0.dp) {
                        Modifier.height(alturaGrafica)
                    } else {
                        Modifier.weight(1f)
                    }
                )
                .pointerInput(registros, riegos, fechaMinima, fechaMaxima) {
                    detectTapGestures { toque ->
                        val escala = density
                        val margenIzquierdo = 58f * escala
                        val margenDerecho = 14f * escala
                        val margenSuperior = 18f * escala
                        val margenInferior = 40f * escala

                        val anchoGrafica =
                            size.width - margenIzquierdo - margenDerecho
                        val altoGrafica =
                            size.height - margenSuperior - margenInferior

                        if (anchoGrafica <= 0f || altoGrafica <= 0f) {
                            return@detectTapGestures
                        }

                        if (
                            toque.x < margenIzquierdo ||
                            toque.x > margenIzquierdo + anchoGrafica ||
                            registros.isEmpty()
                        ) {
                            return@detectTapGestures
                        }

                        fun xFecha(fecha: Long): Float {
                            val proporcion = if (fechaMaxima == fechaMinima) {
                                0.5f
                            } else {
                                (fecha - fechaMinima).toDouble()
                                    .div((fechaMaxima - fechaMinima).toDouble())
                                    .toFloat()
                            }

                            return margenIzquierdo +
                                    anchoGrafica * proporcion
                        }

                        fun yPeso(peso: Double): Float {
                            val pesoLimitado = peso.coerceIn(
                                limiteInferior,
                                limiteSuperior
                            )

                            val proporcion =
                                ((pesoLimitado - limiteInferior) /
                                        (limiteSuperior - limiteInferior))
                                    .toFloat()

                            return margenSuperior +
                                    altoGrafica * (1f - proporcion)
                        }

                        val yBaseRiegos =
                            margenSuperior + altoGrafica - 5f * escala

                        var distanciaMinima = Float.MAX_VALUE
                        var pesoCercano: Int? = null
                        var riegoCercano: Int? = null

                        registros.forEachIndexed { indice, registro ->
                            val dx = toque.x - xFecha(fechasPesos[indice])
                            val dy = toque.y - yPeso(registro.weightGrams)
                            val distancia = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (distancia < distanciaMinima) {
                                distanciaMinima = distancia
                                pesoCercano = indice
                                riegoCercano = null
                            }
                        }

                        riegos.forEachIndexed { indice, riego ->
                            val dx = toque.x - xFecha(fechasRiegos[indice])
                            val dy = toque.y - yBaseRiegos
                            val distancia = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (distancia < distanciaMinima) {
                                distanciaMinima = distancia
                                pesoCercano = null
                                riegoCercano = indice
                            }
                        }

                        if (distanciaMinima <= 36f * escala) {
                            indiceSeleccionado = pesoCercano
                            indiceRiegoSeleccionado = riegoCercano
                        }
                    }
                }
        ) {
            val canvas = drawContext.canvas.nativeCanvas
            val escala = density

            val margenIzquierdo = 58f * escala
            val margenDerecho = 14f * escala
            val margenSuperior = 18f * escala
            val margenInferior = 40f * escala

            val anchoGrafica =
                size.width - margenIzquierdo - margenDerecho

            val altoGrafica =
                size.height - margenSuperior - margenInferior

            if (anchoGrafica <= 0f || altoGrafica <= 0f) {
                return@Canvas
            }

            val pinturaTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorTextoArgb
                textSize = 11f * escala
                textAlign = Paint.Align.RIGHT
            }

            val pinturaEtiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorTextoArgb
                textSize = 10f * escala
                textAlign = Paint.Align.CENTER
            }

            val pinturaCuadricula = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorCuadriculaArgb
                strokeWidth = 1f * escala
                style = Paint.Style.STROKE
            }

            val pinturaLinea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorLineaArgb
                strokeWidth = 3f * escala
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                style = Paint.Style.STROKE
            }

            val pinturaPunto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorLineaArgb
                style = Paint.Style.FILL
            }

            val pinturaCentroPunto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }

            val pinturaPuntoSeleccionado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorTextoArgb
                style = Paint.Style.STROKE
                strokeWidth = 2f * escala
            }

            val pinturaRiego = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorRiegoArgb
                style = Paint.Style.FILL
            }

            val pinturaRiegoSeleccionado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorTextoArgb
                style = Paint.Style.STROKE
                strokeWidth = 2f * escala
            }

            for (i in 0..4) {
                val proporcion = i / 4f
                val y = margenSuperior + altoGrafica * (1f - proporcion)
                val valor = (
                        limiteInferior +
                                (limiteSuperior - limiteInferior) * proporcion
                        ).toInt()

                canvas.drawLine(
                    margenIzquierdo,
                    y,
                    margenIzquierdo + anchoGrafica,
                    y,
                    pinturaCuadricula
                )

                canvas.drawText(
                    "$valor g",
                    margenIzquierdo - 8f * escala,
                    y + 4f * escala,
                    pinturaTexto
                )
            }

            fun posicionX(fecha: Long): Float {
                val proporcion = if (fechaMaxima == fechaMinima) {
                    0.5f
                } else {
                    (fecha - fechaMinima).toDouble()
                        .div((fechaMaxima - fechaMinima).toDouble())
                        .toFloat()
                }

                return margenIzquierdo + anchoGrafica * proporcion
            }

            fun posicionY(peso: Double): Float {
                val pesoLimitado = peso.coerceIn(
                    limiteInferior,
                    limiteSuperior
                )

                val proporcion =
                    ((pesoLimitado - limiteInferior) /
                            (limiteSuperior - limiteInferior)).toFloat()

                return margenSuperior + altoGrafica * (1f - proporcion)
            }

            val ruta = Path()

            registros.forEachIndexed { indice, registro ->
                val x = posicionX(fechasPesos[indice])
                val y = posicionY(registro.weightGrams)

                if (indice == 0) {
                    ruta.moveTo(x, y)
                } else {
                    ruta.lineTo(x, y)
                }
            }

            canvas.drawPath(ruta, pinturaLinea)

            val separacionEtiquetas = ceil(
                (registros.size - 1) / 4.0
            ).toInt().coerceAtLeast(1)

            registros.forEachIndexed { indice, registro ->
                val x = posicionX(fechasPesos[indice])
                val y = posicionY(registro.weightGrams)

                canvas.drawCircle(
                    x,
                    y,
                    5f * escala,
                    pinturaPunto
                )

                canvas.drawCircle(
                    x,
                    y,
                    2f * escala,
                    pinturaCentroPunto
                )

                if (
                    indice == 0 ||
                    indice == registros.lastIndex ||
                    indice % separacionEtiquetas == 0
                ) {
                    val fecha = SimpleDateFormat(
                        "dd MMM",
                        Locale.getDefault()
                    ).format(Date(fechasPesos[indice]))

                    canvas.drawText(
                        fecha,
                        x,
                        margenSuperior + altoGrafica + 20f * escala,
                        pinturaEtiqueta
                    )
                }
            }

            // Marcadores de riego: gotas azules en la parte inferior.
            val yBaseRiegos =
                margenSuperior + altoGrafica - 5f * escala

            riegos.forEachIndexed { indice, riego ->
                val x = posicionX(fechasRiegos[indice])
                val tamano = 8f * escala

                val gota = Path().apply {
                    moveTo(x, yBaseRiegos - tamano)
                    cubicTo(
                        x - tamano * 0.15f,
                        yBaseRiegos - tamano * 0.35f,
                        x - tamano,
                        yBaseRiegos + tamano * 0.1f,
                        x - tamano * 0.7f,
                        yBaseRiegos + tamano * 0.6f
                    )
                    cubicTo(
                        x - tamano * 0.35f,
                        yBaseRiegos + tamano * 1.1f,
                        x + tamano * 0.35f,
                        yBaseRiegos + tamano * 1.1f,
                        x + tamano * 0.7f,
                        yBaseRiegos + tamano * 0.6f
                    )
                    cubicTo(
                        x + tamano,
                        yBaseRiegos + tamano * 0.1f,
                        x + tamano * 0.15f,
                        yBaseRiegos - tamano * 0.35f,
                        x,
                        yBaseRiegos - tamano
                    )
                    close()
                }

                canvas.drawPath(gota, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(33, 150, 243)
                    style = Paint.Style.FILL
                })

                if (indiceRiegoSeleccionado == indice) {
                    canvas.drawCircle(
                        x,
                        yBaseRiegos,
                        13f * escala,
                        pinturaRiegoSeleccionado
                    )
                }
            }

            val seleccionado = indiceSeleccionado

            if (
                seleccionado != null &&
                seleccionado in registros.indices
            ) {
                val registro = registros[seleccionado]

                canvas.drawCircle(
                    posicionX(fechasPesos[seleccionado]),
                    posicionY(registro.weightGrams),
                    8f * escala,
                    pinturaPuntoSeleccionado
                )
            }
        }

        val seleccionado = indiceSeleccionado
        val riegoSeleccionado = indiceRiegoSeleccionado

        when {
            seleccionado != null && seleccionado in registros.indices -> {
                val registro = registros[seleccionado]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = VerdeClaro
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Medición seleccionada",
                            style = MaterialTheme.typography.labelMedium,
                            color = VerdeBosque
                        )

                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "%.1f g",
                                    registro.weightGrams
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = VerdeBosque,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = registro.dateTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoOscuro,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            riegoSeleccionado != null &&
                    riegoSeleccionado in riegos.indices -> {
                val riego = riegos[riegoSeleccionado]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TerracotaClaro
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Primer renglón: cantidad de agua, fecha y hora.
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "💧",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "${riego.waterAmountMl} ml",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Terracota
                                )
                            }

                            Text(
                                text = riego.dateTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoOscuro,
                                maxLines = 1
                            )
                        }

                        // Segundo renglón: pesos antes y después.
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Antes: ${
                                    riego.weightBeforeGrams?.let {
                                        String.format(Locale.getDefault(), "%.1f g", it)
                                    } ?: "—"
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoOscuro,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "Después: ${
                                    riego.weightAfterGrams?.let {
                                        String.format(Locale.getDefault(), "%.1f g", it)
                                    } ?: "—"
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoOscuro,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }}
            }

            else -> {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}



@Composable
private fun RegistroPesoCard(
    registro: WeightRecord,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Blanco
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%.1f g",
                    registro.weightGrams
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = registro.dateTime,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoOscuro
            )

            if (registro.notes.isNotBlank()) {
                Text(
                    text = "Notas: ${registro.notes}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            val rutaFoto = registro.fotoBasculaPath
                ?: registro.fotoPlantaPath

            FotoRegistro(
                ruta = rutaFoto,
                titulo = "Fotografía de la maceta y la báscula"
            )

            HorizontalDivider()

            TextButton(
                onClick = onEliminar,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Eliminar medición",
                    color = Terracota
                )
            }
        }
    }
}

private data class EstadoFoto(
    val bitmap: Bitmap? = null,
    val cargada: Boolean = false
)

@Composable
private fun FotoRegistro(
    ruta: String?,
    titulo: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (ruta.isNullOrBlank()) {
            Text(
                text = "No se guardó una fotografía en este registro.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoOscuro
            )
        } else {
            val estado by produceState(
                initialValue = EstadoFoto(),
                key1 = ruta
            ) {
                val bitmap = withContext(Dispatchers.IO) {
                    cargarBitmapOrientado(ruta)
                }

                value = EstadoFoto(
                    bitmap = bitmap,
                    cargada = true
                )
            }

            when {
                !estado.cargada -> {
                    Text("Cargando fotografía...")
                }

                estado.bitmap == null -> {
                    Text(
                        text = "No se pudo cargar esta fotografía.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                else -> {
                    Image(
                        bitmap = estado.bitmap!!.asImageBitmap(),
                        contentDescription = titulo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private fun cargarBitmapOrientado(
    ruta: String
): Bitmap? {
    return try {
        val opcionesMedidas = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(ruta, opcionesMedidas)

        if (
            opcionesMedidas.outWidth <= 0 ||
            opcionesMedidas.outHeight <= 0
        ) {
            return null
        }

        var muestra = 1

        while (
            opcionesMedidas.outWidth / muestra > 1200 ||
            opcionesMedidas.outHeight / muestra > 1200
        ) {
            muestra *= 2
        }

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = muestra
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        val bitmap = BitmapFactory.decodeFile(
            ruta,
            opciones
        ) ?: return null

        val orientacion = try {
            ExifInterface(ruta).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matriz = Matrix()

        when (orientacion) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                matriz.setScale(-1f, 1f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                matriz.setRotate(180f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL ->
                matriz.setScale(1f, -1f)

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matriz.setRotate(90f)
                matriz.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 ->
                matriz.setRotate(90f)

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matriz.setRotate(-90f)
                matriz.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 ->
                matriz.setRotate(270f)
        }

        if (matriz.isIdentity) {
            bitmap
        } else {
            try {
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matriz,
                    true
                ).also { resultado ->
                    if (resultado !== bitmap) {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                bitmap
            }
        }
    } catch (e: Exception) {
        null
    }
}
