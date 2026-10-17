
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
import com.example.macetaelefante.data.WateringRepository
import com.example.macetaelefante.ui.screens.WateringEntryScreen
import com.example.macetaelefante.ui.screens.WateringHistoryScreen
import com.example.macetaelefante.ui.screens.WeightEntryScreen
import com.example.macetaelefante.ui.screens.WeightHistoryScreen
import com.example.macetaelefante.ui.theme.MacetaElefanteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        mutableStateOf("Todavía no hay mediciones.")
    }

    var fechaUltimoRiego by rememberSaveable {
        mutableStateOf("")
    }

    var cantidadUltimoRiego by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    var idUltimoRiego by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(Unit) {
        val registrosPeso = withContext(Dispatchers.IO) {
            WeightRepository.obtenerTodos(context)
        }

        val ultimoRegistro = registrosPeso.firstOrNull()
        val registroAnterior = registrosPeso.getOrNull(1)

        if (ultimoRegistro != null) {
            ultimoPeso = ultimoRegistro.weightGrams
            fechaUltimoPeso = ultimoRegistro.dateTime
            ultimaNota = ultimoRegistro.notes
            fotoPlantaPath = ultimoRegistro.fotoPlantaPath
        } else {
            ultimoPeso = null
            fechaUltimoPeso = ""
            ultimaNota = ""
            fotoPlantaPath = null
        }

        cambioPeso = calcularCambioPeso(
            ultimo = ultimoRegistro,
            anterior = registroAnterior
        )

        val ultimoRiego = withContext(Dispatchers.IO) {
            WateringRepository.obtenerUltimo(context)
        }

        fechaUltimoRiego = ultimoRiego?.dateTime.orEmpty()
        cantidadUltimoRiego = ultimoRiego?.waterAmountMl
        idUltimoRiego = ultimoRiego?.id
    }

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

                    WeightRepository.guardar(context, registro)

                    ultimoPeso = peso
                    fechaUltimoPeso = fecha
                    ultimaNota = notas

                    val registros = WeightRepository.obtenerTodos(context)

                    cambioPeso = calcularCambioPeso(
                        ultimo = registros.firstOrNull(),
                        anterior = registros.getOrNull(1)
                    )

                    pantalla = "inicio"
                    mensaje = "Medición guardada correctamente."
                },
                onSaveWithPhotos = {
                        peso, fecha, notas, rutaBascula, rutaPlanta ->

                    val registro = WeightRecord(
                        id = System.currentTimeMillis(),
                        weightGrams = peso,
                        dateTime = fecha,
                        notes = notas,
                        fotoBasculaPath = rutaBascula,
                        fotoPlantaPath = rutaPlanta
                    )

                    WeightRepository.guardar(context, registro)

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
                    mensaje = "Medición guardada correctamente."
                }
            )
        }

        "registro_riego" -> {
            WateringEntryScreen(
                onGuardado = {
                    val ultimoRiego =
                        WateringRepository.obtenerUltimo(context)

                    fechaUltimoRiego = ultimoRiego?.dateTime.orEmpty()
                    cantidadUltimoRiego = ultimoRiego?.waterAmountMl
                    idUltimoRiego = ultimoRiego?.id

                    pantalla = "inicio"
                    mensaje = "Riego guardado correctamente."
                },
                onCancelar = {
                    pantalla = "inicio"
                    mensaje = ""
                }
            )
        }

        "seleccionar_historial" -> {
            SeleccionarHistorialScreen(
                onHistorialPesos = {
                    pantalla = "historial_pesos"
                },
                onHistorialRiegos = {
                    pantalla = "historial_riegos"
                },
                onVolver = {
                    pantalla = "inicio"
                }
            )
        }

        "historial_pesos" -> {
            WeightHistoryScreen(
                onBack = {
                    pantalla = "seleccionar_historial"
                },
                onRegistroEliminado = {
                    val registros = WeightRepository.obtenerTodos(context)

                    val ultimoRegistro = registros.firstOrNull()
                    val registroAnterior = registros.getOrNull(1)

                    ultimoPeso = ultimoRegistro?.weightGrams
                    fechaUltimoPeso = ultimoRegistro?.dateTime.orEmpty()
                    ultimaNota = ultimoRegistro?.notes.orEmpty()
                    fotoPlantaPath = ultimoRegistro?.fotoPlantaPath

                    cambioPeso = calcularCambioPeso(
                        ultimo = ultimoRegistro,
                        anterior = registroAnterior
                    )

                    mensaje = "Historial actualizado."
                }
            )
        }

        "historial_riegos" -> {
            WateringHistoryScreen(
                onBack = {
                    pantalla = "seleccionar_historial"
                },
                onRegistroEliminado = {
                    val ultimoRiego =
                        WateringRepository.obtenerUltimo(context)

                    fechaUltimoRiego = ultimoRiego?.dateTime.orEmpty()
                    cantidadUltimoRiego = ultimoRiego?.waterAmountMl
                    idUltimoRiego = ultimoRiego?.id

                    mensaje = "Historial de riegos actualizado."
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
                fechaUltimoRiego = fechaUltimoRiego,
                cantidadUltimoRiego = cantidadUltimoRiego,
                idUltimoRiego = idUltimoRiego,
                mensaje = mensaje,
                onRegistrarPeso = {
                    mensaje = ""
                    pantalla = "registro_peso"
                },
                onRegistrarRiego = {
                    mensaje = ""
                    pantalla = "registro_riego"
                },
                onVerHistorial = {
                    mensaje = ""
                    pantalla = "seleccionar_historial"
                }
            )
        }
    }
}

@Composable
private fun SeleccionarHistorialScreen(
    onHistorialPesos: () -> Unit,
    onHistorialRiegos: () -> Unit,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Historial",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "¿Qué registros deseas consultar?",
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onHistorialPesos,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Historial de pesos")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onHistorialRiegos,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Historial de riegos")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al inicio")
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
            "Ganó ${
                String.format(
                    Locale.getDefault(),
                    "%.1f",
                    diferencia
                )
            } g"
        }

        diferencia < 0 -> {
            "Perdió ${
                String.format(
                    Locale.getDefault(),
                    "%.1f",
                    -diferencia
                )
            } g"
        }

        else -> "Sin cambio (0 g)"
    }
}

private fun calcularTiempoDesdeRiego(id: Long?): String {
    if (id == null) {
        return "Sin riegos registrados."
    }

    val diferencia = System.currentTimeMillis() - id

    if (diferencia < 0) {
        return "Fecha futura."
    }

    val dias = TimeUnit.MILLISECONDS.toDays(diferencia)

    return when {
        dias == 0L -> "Hace menos de 1 día"
        dias == 1L -> "Hace 1 día"
        else -> "Hace $dias días"
    }
}

@Composable
fun HomeScreen(
    ultimoPeso: Double?,
    fechaUltimoPeso: String,
    ultimaNota: String,
    fotoPlantaPath: String?,
    cambioPeso: String,
    fechaUltimoRiego: String,
    cantidadUltimoRiego: Int?,
    idUltimoRiego: Long?,
    mensaje: String,
    onRegistrarPeso: () -> Unit,
    onRegistrarRiego: () -> Unit,
    onVerHistorial: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mi jardín",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Control y cuidado de mis plantas",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Maceta Elefante",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Planta: Pie de elefante",
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Beaucarnea recurvata",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FotoPlantaInicio(ruta = fotoPlantaPath)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Resumen de seguimiento",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Resumen del peso
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Último peso registrado",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = ultimoPeso?.let {
                            "${String.format(Locale.getDefault(), "%.1f", it)} g"
                        } ?: "Sin mediciones",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (fechaUltimoPeso.isNotBlank()) {
                            "Fecha: $fechaUltimoPeso"
                        } else {
                            "Fecha: sin registro"
                        },
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Comparación con la medición anterior",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = cambioPeso,
                        fontSize = 16.sp
                    )

                    if (ultimaNota.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Última nota",
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(text = ultimaNota)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resumen del riego
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Último riego",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = cantidadUltimoRiego?.let {
                            "$it ml de agua"
                        } ?: "Todavía no hay riegos registrados",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (fechaUltimoRiego.isNotBlank()) {
                            "Fecha y hora: $fechaUltimoRiego"
                        } else {
                            "Fecha y hora: sin registro"
                        },
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = calcularTiempoDesdeRiego(idUltimoRiego),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (mensaje.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "¿Qué deseas hacer?",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRegistrarPeso,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Registrar peso",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRegistrarRiego,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Registrar riego",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onVerHistorial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Ver historial",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FotoPlantaInicio(
    ruta: String?
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = ruta
    ) {
        value = if (ruta.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                cargarFotoPlanta(ruta)
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Fotografía de la planta",
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aquí aparecerá la foto de tu planta",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun cargarFotoPlanta(
    ruta: String
): Bitmap? {
    return try {
        val opciones = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(ruta, opciones)

        opciones.inSampleSize = 2
        opciones.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeFile(
            ruta,
            opciones
        ) ?: return null

        val exif = ExifInterface(ruta)

        val orientacion = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matriz = Matrix()

        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matriz.postRotate(90f)
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matriz.postRotate(180f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matriz.postRotate(270f)
            }

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matriz.preScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matriz.preScale(1f, -1f)
            }
        }

        if (!matriz.isIdentity) {
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matriz,
                true
            )
        } else {
            bitmap
        }
    } catch (_: Exception) {
        null
    }
}
