
package com.example.macetaelefante

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macetaelefante.data.WeightRecord
import com.example.macetaelefante.data.WeightRepository
import com.example.macetaelefante.ui.screens.WeightEntryScreen
import com.example.macetaelefante.ui.screens.WeightHistoryScreen
import com.example.macetaelefante.ui.theme.MacetaElefanteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MacetaElefanteTheme {
                MacetaElefanteApp()
            }
        }
    }
}

@Composable
fun MacetaElefanteApp() {

    val context = LocalContext.current

    var pantalla by rememberSaveable {
        mutableStateOf("inicio")
    }

    var ultimoPeso by rememberSaveable {
        mutableStateOf<Double?>(null)
    }

    var fechaUltimoPeso by rememberSaveable {
        mutableStateOf("")
    }

    var ultimaNota by rememberSaveable {
        mutableStateOf("")
    }

    var fotoPlantaPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var mensaje by rememberSaveable {
        mutableStateOf("")
    }

    var cambioPeso by rememberSaveable {
        mutableStateOf("Registra otra medición para comparar.")
    }

    LaunchedEffect(Unit) {
        val registros = withContext(Dispatchers.IO) {
            WeightRepository.obtenerTodos(context)
        }

        val ultimoRegistro = registros.firstOrNull()
        val registroAnterior = registros.getOrNull(1)

        if (ultimoRegistro != null) {
            ultimoPeso = ultimoRegistro.weightGrams
            fechaUltimoPeso = ultimoRegistro.dateTime
            ultimaNota = ultimoRegistro.notes
            fotoPlantaPath = ultimoRegistro.fotoPlantaPath
        }

        cambioPeso = calcularCambioPeso(
            ultimo = ultimoRegistro,
            anterior = registroAnterior
        )
    }

    // Botón Atrás del sistema:
    // si estamos en otra pantalla, regresamos al inicio.
    BackHandler(enabled = pantalla != "inicio") {
        pantalla = "inicio"
        mensaje = ""
    }

    when (pantalla) {

        "registro_peso" -> {
            WeightEntryScreen(
                onBack = {
                    pantalla = "inicio"
                    mensaje = ""
                },
                onSave = { peso, fecha, notas ->

                    val registro = WeightRecord(
                        id = System.currentTimeMillis(),
                        weightGrams = peso,
                        dateTime = fecha,
                        notes = notas
                    )

                    WeightRepository.guardar(
                        context,
                        registro
                    )

                    ultimoPeso = peso
                    fechaUltimoPeso = fecha
                    ultimaNota = notas
                    fotoPlantaPath = null

                    val registros = WeightRepository.obtenerTodos(context)

                    cambioPeso = calcularCambioPeso(
                        ultimo = registros.firstOrNull(),
                        anterior = registros.getOrNull(1)
                    )

                    pantalla = "inicio"
                    mensaje = "Peso guardado correctamente."
                },
                onSaveWithPhotos = {
                        peso,
                        fecha,
                        notas,
                        rutaBascula,
                        rutaPlanta ->

                    val registro = WeightRecord(
                        id = System.currentTimeMillis(),
                        weightGrams = peso,
                        dateTime = fecha,
                        notes = notas,
                        fotoBasculaPath = rutaBascula,
                        fotoPlantaPath = rutaPlanta
                    )

                    WeightRepository.guardar(
                        context,
                        registro
                    )

                    ultimoPeso = peso
                    fechaUltimoPeso = fecha
                    ultimaNota = notas
                    fotoPlantaPath = rutaPlanta

                    val registros = WeightRepository.obtenerTodos(context)

                    cambioPeso = calcularCambioPeso(
                        ultimo = registros.firstOrNull(),
                        anterior = registros.getOrNull(1)
                    )

                    pantalla = "inicio"
                    mensaje = "Peso y fotografías guardados correctamente."
                }
            )
        }

        "historial" -> {
            WeightHistoryScreen(
                onBack = {
                    pantalla = "inicio"
                }
            )
        }

        else -> {
            HomeScreen(
                ultimoPeso = ultimoPeso,
                fechaUltimoPeso = fechaUltimoPeso,
                ultimaNota = ultimaNota,
                fotoPlantaPath = fotoPlantaPath,
                cambioPeso = cambioPeso,
                mensaje = mensaje,
                onRegistrarPeso = {
                    pantalla = "registro_peso"
                    mensaje = ""
                },
                onRegistrarRiego = {
                    mensaje =
                        "El registro de riego estará disponible " +
                                "en un siguiente paso."
                },
                onVerHistorial = {
                    pantalla = "historial"
                    mensaje = ""
                }
            )
        }
    }
}

private fun calcularCambioPeso(
    ultimo: WeightRecord?,
    anterior: WeightRecord?
): String {

    if (ultimo == null) {
        return "Todavía no hay mediciones."
    }

    if (anterior == null) {
        return "Registra otra medición para comparar."
    }

    val diferencia = ultimo.weightGrams - anterior.weightGrams

    return when {
        diferencia > 0 -> {
            "Ganó ${String.format(Locale.getDefault(), "%.1f", diferencia)} g"
        }

        diferencia < 0 -> {
            "Perdió ${String.format(Locale.getDefault(), "%.1f", -diferencia)} g"
        }

        else -> {
            "Sin cambio (0 g)"
        }
    }
}

@Composable
fun HomeScreen(
    ultimoPeso: Double?,
    fechaUltimoPeso: String,
    ultimaNota: String,
    fotoPlantaPath: String?,
    cambioPeso: String,
    mensaje: String,
    onRegistrarPeso: () -> Unit,
    onRegistrarRiego: () -> Unit,
    onVerHistorial: () -> Unit
) {

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            Column {
                Text(
                    text = "Mi jardín",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Control y cuidado de mis plantas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FotoPlantaInicio(
                            ruta = fotoPlantaPath
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "Maceta Elefante",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Planta: Pie de elefante",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Beaucarnea recurvata",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Resumen de seguimiento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Text("Último peso registrado")

                    Text(
                        text = ultimoPeso?.let {
                            String.format(
                                Locale.getDefault(),
                                "%.1f g",
                                it
                            )
                        } ?: "-- g",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (fechaUltimoPeso.isNotBlank()) {
                            fechaUltimoPeso
                        } else {
                            "Todavía no hay mediciones"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cambio de peso")

                        Text(
                            text = cambioPeso,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                    }

                    if (ultimaNota.isNotBlank()) {
                        Text(
                            text = "Nota: $ultimaNota",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                text = "¿Qué deseas hacer?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onRegistrarPeso,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("⚖️  Registrar peso")
            }

            OutlinedButton(
                onClick = onRegistrarRiego,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("💧  Registrar riego")
            }

            OutlinedButton(
                onClick = onVerHistorial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("📊  Ver historial y estadísticas")
            }

            if (mensaje.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mensaje,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Cada medición nos ayudará a conocer mejor " +
                        "las necesidades de tu planta.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FotoPlantaInicio(
    ruta: String?
) {
    if (ruta.isNullOrBlank()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🪴",
                fontSize = 64.sp
            )

            Text(
                text = "Aquí aparecerá la foto de tu planta",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = ruta
    ) {
        value = withContext(Dispatchers.IO) {
            cargarFotoPlanta(ruta)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Fotografía más reciente de la planta",
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🪴",
                fontSize = 48.sp
            )

            Text(
                text = "Cargando fotografía...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun cargarFotoPlanta(
    ruta: String
): Bitmap? {
    return try {
        val opcionesMedidas = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(
            ruta,
            opcionesMedidas
        )

        if (
            opcionesMedidas.outWidth <= 0 ||
            opcionesMedidas.outHeight <= 0
        ) {
            return null
        }

        var muestra = 1

        while (
            opcionesMedidas.outWidth / muestra > 1600 ||
            opcionesMedidas.outHeight / muestra > 1600
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