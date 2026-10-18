
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

        AlertDialog(
            onDismissRequest = {
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

    LaunchedEffect(eliminando, registroPendienteEliminar) {
        if (eliminando && registroPendienteEliminar != null) {
            val registro = registroPendienteEliminar!!

            val eliminado = withContext(Dispatchers.IO) {
                WeightRepository.eliminar(context, registro.id)
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

            eliminando = false
            registroPendienteEliminar = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("← Volver al inicio")
            }

            Text(
                text = "Historial de peso",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Observa cómo cambia el peso de la maceta " +
                        "entre riegos y consulta todos tus registros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (mensaje != null) {
                Text(
                    text = mensaje!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
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
                                registros = registrosGrafica
                            )
                        }

                        item(key = "titulo_historial_combinado") {
                            Column(
                                verticalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Historial combinado",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Pesos y riegos ordenados del " +
                                            "más reciente al más antiguo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                )
                            }
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
                                    registro = evento.riego
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
            containerColor = MaterialTheme.colorScheme.surface
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

            val pesoDespuesRegistrado =
                riegoReciente.weightAfterGrams

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            val medicionesComparables = registros
                .filter {
                    convertirFechaAMillis(it.dateTime, it.id) >
                            fechaReferencia
                }
                .sortedBy {
                    convertirFechaAMillis(it.dateTime, it.id)
                }

            val ultimaMedicion = medicionesComparables.lastOrNull()

            if (ultimaMedicion == null) {
                Text(
                    text = "Aún no hay mediciones posteriores a la " +
                            "referencia para calcular la variación.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val cambio =
                    ultimaMedicion.weightGrams - pesoReferencia

                val descripcionCambio = when {
                    cambio > 0.05 -> "El peso aumentó"
                    cambio < -0.05 -> "El peso disminuyó"
                    else -> "El peso prácticamente no cambió"
                }

                Text(
                    text = "Última medición posterior: " +
                            ultimaMedicion.dateTime,
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
                        "%s: %s%.1f g",
                        descripcionCambio,
                        if (cambio > 0.05) "+" else "",
                        cambio
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        cambio < -0.05 ->
                            MaterialTheme.colorScheme.error
                        cambio > 0.05 ->
                            MaterialTheme.colorScheme.primary
                        else ->
                            MaterialTheme.colorScheme.onSurface
                    }
                )

                val diasTranscurridos =
                    ((convertirFechaAMillis(
                        ultimaMedicion.dateTime,
                        ultimaMedicion.id
                    ) - fechaRiego) / MILISEGUNDOS_DIA)
                        .coerceAtLeast(0L)

                Text(
                    text = if (diasTranscurridos == 1L) {
                        "Ha pasado 1 día desde el riego."
                    } else {
                        "Han pasado $diasTranscurridos días desde el riego."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "La variación de peso no determina por sí sola " +
                        "si debes regar. También considera la humedad " +
                        "del sustrato y el estado de la planta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RegistroRiegoCard(
    registro: WateringRecord
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}

@Composable
private fun GraficaEvolucionPeso(
    registros: List<WeightRecord>
) {
    val colorLinea = MaterialTheme.colorScheme.primary
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorCuadricula = MaterialTheme.colorScheme.outlineVariant
    val colorFondo = MaterialTheme.colorScheme.surface

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
            Text(
                text = "Evolución del peso",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "De la medición más antigua a la más reciente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                    colorLinea = colorLinea,
                    colorTexto = colorTexto,
                    colorCuadricula = colorCuadricula
                )

                Text(
                    text = "Cada punto representa una medición guardada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GraficaLineal(
    registros: List<WeightRecord>,
    colorLinea: Color,
    colorTexto: Color,
    colorCuadricula: Color
) {
    val pesos = registros.map { it.weightGrams }

    val pesoMinimo = pesos.minOrNull() ?: 0.0
    val pesoMaximo = pesos.maxOrNull() ?: 0.0

    val diferencia = pesoMaximo - pesoMinimo
    val margen = if (diferencia == 0.0) {
        maxOf(abs(pesoMaximo) * 0.05, 1.0)
    } else {
        diferencia * 0.15
    }

    val limiteInferior = pesoMinimo - margen
    val limiteSuperior = pesoMaximo + margen
    val rango = (limiteSuperior - limiteInferior).coerceAtLeast(1.0)

    val colorLineaArgb = colorLinea.toArgb()
    val colorTextoArgb = colorTexto.toArgb()
    val colorCuadriculaArgb = colorCuadricula.toArgb()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        val escala = density
        val margenIzquierdo = 58f * escala
        val margenDerecho = 14f * escala
        val margenSuperior = 18f * escala
        val margenInferior = 32f * escala

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

        val pasos = 4

        for (i in 0..pasos) {
            val proporcion = i.toFloat() / pasos
            val y = margenSuperior + altoGrafica * proporcion

            canvas.drawLine(
                margenIzquierdo,
                y,
                margenIzquierdo + anchoGrafica,
                y,
                pinturaCuadricula
            )

            val valor = limiteSuperior - rango * proporcion

            canvas.drawText(
                String.format(
                    Locale.getDefault(),
                    "%.1f",
                    valor
                ),
                margenIzquierdo - 8f * escala,
                y + 4f * escala,
                pinturaTexto
            )
        }

        val ruta = Path()

        registros.forEachIndexed { indice, registro ->
            val x = if (registros.size == 1) {
                margenIzquierdo + anchoGrafica / 2f
            } else {
                margenIzquierdo +
                        anchoGrafica * indice /
                        (registros.size - 1).toFloat()
            }

            val proporcion = (
                    (limiteSuperior - registro.weightGrams) / rango
                    ).toFloat()

            val y = margenSuperior + altoGrafica * proporcion

            if (indice == 0) {
                ruta.moveTo(x, y)
            } else {
                ruta.lineTo(x, y)
            }

            canvas.drawText(
                (indice + 1).toString(),
                x,
                margenSuperior + altoGrafica + 20f * escala,
                pinturaEtiqueta
            )
        }

        canvas.drawPath(ruta, pinturaLinea)

        registros.forEachIndexed { indice, registro ->
            val x = if (registros.size == 1) {
                margenIzquierdo + anchoGrafica / 2f
            } else {
                margenIzquierdo +
                        anchoGrafica * indice /
                        (registros.size - 1).toFloat()
            }

            val proporcion = (
                    (limiteSuperior - registro.weightGrams) / rango
                    ).toFloat()

            val y = margenSuperior + altoGrafica * proporcion

            canvas.drawCircle(x, y, 5f * escala, pinturaPunto)
            canvas.drawCircle(x, y, 2f * escala, pinturaCentroPunto)
        }
    }

    Text(
        text = "Mediciones en orden cronológico",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RegistroPesoCard(
    registro: WeightRecord,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.error
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
