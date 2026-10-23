
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
import com.example.macetaelefante.ui.screens.WeightEntryScreen
import com.example.macetaelefante.ui.screens.WeightHistoryScreen
import com.example.macetaelefante.ui.theme.MacetaElefanteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.border

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
            ordenarRegistrosPorFechaImagen(WeightRepository.obtenerTodos(context))
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

                    val registros = ordenarRegistrosPorFechaImagen(
                        WeightRepository.obtenerTodos(context)
                    )
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

                    val registros = ordenarRegistrosPorFechaImagen(
                        WeightRepository.obtenerTodos(context)
                    )
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

        "historial_pesos" -> {
            WeightHistoryScreen(
                onBack = {
                    pantalla = "inicio"
                },
                onRegistroEliminado = {
                    val registros = ordenarRegistrosPorFechaImagen(
                        WeightRepository.obtenerTodos(context)
                    )

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
                    pantalla = "historial_pesos"
                }
            )
        }
    }
}



private fun ordenarRegistrosPorFechaImagen(
    registros: List<WeightRecord>
): List<WeightRecord> {
    val formatoFecha = SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).apply {
        isLenient = false
    }

    return registros.sortedWith(
        compareByDescending<WeightRecord> { registro ->
            formatoFecha.parse(registro.dateTime)?.time ?: registro.id
        }.thenByDescending { registro -> registro.id }
    )
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
    val verdeBosque = Color(0xFF355E3B)
    val verdeSalvia = Color(0xFF8A9A5B)
    val terracota = Color(0xFFB77950)
    val arena = Color(0xFFF3EBDD)
    val blanco = Color(0xFFFFFFFF)
    val textoOscuro = Color(0xFF343A30)
    val verdeClaro = Color(0xFFE8EDDF)
    val terracotaClaro = Color(0xFFF4E5DA)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = arena
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Encabezado
            Text(
                text = "Mi jardín",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = verdeBosque
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Control y cuidado de mis plantas",
                fontSize = 15.sp,
                color = textoOscuro,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Tarjeta principal de la planta
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = verdeSalvia,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = verdeSalvia
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MACETA ELEFANTE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = verdeSalvia,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pie de elefante",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = verdeBosque,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Beaucarnea recurvata",
                        fontSize = 14.sp,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    FotoPlantaInicio(ruta = fotoPlantaPath)
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Título de seguimiento
            Text(
                text = "Resumen de seguimiento",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = verdeBosque,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tarjeta de peso
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = verdeClaro
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "ÚLTIMO PESO REGISTRADO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = verdeBosque,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = ultimoPeso?.let {
                            "${String.format(Locale.getDefault(), "%.1f", it)} g"
                        } ?: "Sin mediciones",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = verdeBosque
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (fechaUltimoPeso.isNotBlank()) {
                            "Fecha: $fechaUltimoPeso"
                        } else {
                            "Fecha: sin registro"
                        },
                        fontSize = 14.sp,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = verdeSalvia.copy(alpha = 0.4f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Comparación con la medición anterior",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = cambioPeso,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = verdeBosque
                    )

                    if (ultimaNota.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Última nota",
                            fontWeight = FontWeight.SemiBold,
                            color = textoOscuro
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = ultimaNota,
                            color = textoOscuro,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta de riego
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = terracotaClaro
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "ÚLTIMO RIEGO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = terracota,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = cantidadUltimoRiego?.let {
                            "$it ml de agua"
                        } ?: "Todavía no hay riegos registrados",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (fechaUltimoRiego.isNotBlank()) {
                            "Fecha y hora: $fechaUltimoRiego"
                        } else {
                            "Fecha y hora: sin registro"
                        },
                        fontSize = 14.sp,
                        color = textoOscuro
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = calcularTiempoDesdeRiego(idUltimoRiego),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = terracota
                    )
                }
            }

            // Mensaje de confirmación
            if (mensaje.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = verdeClaro
                    )
                ) {
                    Text(
                        text = mensaje,
                        modifier = Modifier.padding(14.dp),
                        color = verdeBosque,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "¿Qué deseas hacer?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = verdeBosque,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Botón de peso
            Button(
                onClick = onRegistrarPeso,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = verdeBosque,
                    contentColor = blanco
                )
            ) {
                Text(
                    text = "Registrar peso",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón de riego
            Button(
                onClick = onRegistrarRiego,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = verdeSalvia,
                    contentColor = blanco
                )
            ) {
                Text(
                    text = "Registrar riego",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón de historial
            Button(
                onClick = onVerHistorial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = terracota,
                    contentColor = blanco
                )
            ) {
                Text(
                    text = "Ver historial",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
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
