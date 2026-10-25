
package com.example.macetaelefante.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val VerdeBosque = Color(0xFF355E3B)
private val VerdeSalvia = Color(0xFF8A9A5B)
private val Terracota = Color(0xFFB77950)
private val Arena = Color(0xFFF3EBDD)
private val Blanco = Color(0xFFFFFFFF)
private val TextoOscuro = Color(0xFF343A30)
private val VerdeClaro = Color(0xFFE8EDDF)
private val TerracotaClaro = Color(0xFFF4E5DA)

@Composable
fun WeightEntryScreen(
    onBack: () -> Unit = {},
    onSave: (Double, String, String) -> Unit = { _, _, _ -> },
    onSaveWithPhotos: (
        (Double, String, String, String?, String?) -> Unit
    )? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pesoRequester = remember {
        BringIntoViewRequester()
    }

    val notasRequester = remember {
        BringIntoViewRequester()
    }

    var pesoTexto by rememberSaveable {
        mutableStateOf("")
    }

    var notas by rememberSaveable {
        mutableStateOf("")
    }

    var mensajeError by rememberSaveable {
        mutableStateOf("")
    }

    var fotoBascula by remember {
        mutableStateOf<Uri?>(null)
    }

    var fechaHora by rememberSaveable {
        mutableStateOf("")
    }

    var leyendoPeso by remember {
        mutableStateOf(false)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

    fun calendarioSeleccionado(): Calendar {
        val calendario = Calendar.getInstance()

        if (fechaHora.isNotBlank()) {
            try {
                val formato = SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                ).apply {
                    isLenient = false
                }

                val fecha = formato.parse(fechaHora)

                if (fecha != null) {
                    calendario.time = fecha
                }
            } catch (_: Exception) {
                // Usar la fecha actual.
            }
        }

        return calendario
    }

    fun actualizarFecha(
        anio: Int,
        mes: Int,
        dia: Int
    ) {
        val calendario = calendarioSeleccionado()

        calendario.set(Calendar.YEAR, anio)
        calendario.set(Calendar.MONTH, mes)
        calendario.set(Calendar.DAY_OF_MONTH, dia)

        fechaHora = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(calendario.time)

        mensajeError = ""
    }

    fun actualizarHora(
        hora: Int,
        minuto: Int
    ) {
        val calendario = calendarioSeleccionado()

        calendario.set(Calendar.HOUR_OF_DAY, hora)
        calendario.set(Calendar.MINUTE, minuto)

        fechaHora = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(calendario.time)

        mensajeError = ""
    }

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fotoBascula = uri
            pesoTexto = ""
            mensajeError = ""
            leyendoPeso = true

            scope.launch {
                val fechaExif = withContext(Dispatchers.IO) {
                    leerFechaHoraExif(context, uri)
                }

                if (fechaExif != null) {
                    fechaHora = fechaExif
                }

                try {
                    val imagen = InputImage.fromFilePath(
                        context,
                        uri
                    )

                    val reconocedor = TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                    )

                    reconocedor.process(imagen)
                        .addOnSuccessListener { resultado ->
                            val patron = Regex(
                                """(?<!\d)\d{1,6}(?:[.,]\d{1,2})?(?!\d)"""
                            )

                            val coincidencia = patron.find(
                                resultado.text
                            )

                            if (coincidencia != null) {
                                pesoTexto = coincidencia.value
                                    .replace(',', '.')
                            }

                            leyendoPeso = false
                            reconocedor.close()
                        }
                        .addOnFailureListener {
                            leyendoPeso = false
                            reconocedor.close()
                        }
                } catch (_: Exception) {
                    leyendoPeso = false
                }
            }
        }
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
                text = "Registrar peso",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            Text(
                text = "Foto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            PhotoPreview(
                uri = fotoBascula,
                context = context,
                description = "Fotografía de la maceta y la báscula"
            )

            Button(
                onClick = {
                    selectorFoto.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeBosque,
                    contentColor = Blanco
                )
            ) {
                Text(
                    if (fotoBascula == null) {
                        "Seleccionar fotografía"
                    } else {
                        "Cambiar fotografía"
                    }
                )
            }

            if (fotoBascula != null) {
                Button(
                    onClick = {
                        fotoBascula = null
                        mensajeError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracota,
                        contentColor = Blanco
                    )
                ) {
                    Text("Quitar fotografía")
                }
            }

            if (leyendoPeso) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = VerdeBosque
                    )

                    Text(
                        text = "Analizando la fotografía...",
                        modifier = Modifier.padding(top = 8.dp),
                        color = TextoOscuro
                    )
                }
            }

            // Fecha y hora con botones de iconos.
            Text(
                text = "Fecha y hora",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (fechaHora.isNotBlank()) {
                        fechaHora
                    } else {
                        "Selecciona fecha y hora"
                    },
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
                            val calendario =
                                calendarioSeleccionado()

                            DatePickerDialog(
                                context,
                                { _, anio, mes, dia ->
                                    actualizarFecha(
                                        anio,
                                        mes,
                                        dia
                                    )
                                },
                                calendario.get(Calendar.YEAR),
                                calendario.get(Calendar.MONTH),
                                calendario.get(
                                    Calendar.DAY_OF_MONTH
                                )
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
                            val calendario =
                                calendarioSeleccionado()

                            TimePickerDialog(
                                context,
                                { _, hora, minuto ->
                                    actualizarHora(
                                        hora,
                                        minuto
                                    )
                                },
                                calendario.get(
                                    Calendar.HOUR_OF_DAY
                                ),
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

            Text(
                text = "Peso de la maceta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )

            OutlinedTextField(
                value = pesoTexto,
                onValueChange = {
                    pesoTexto = it
                    mensajeError = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(pesoRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                delay(300)
                                pesoRequester.bringIntoView()
                            }
                        }
                    },
                label = {
                    Text("Peso en gramos")
                },
                placeholder = {
                    Text("Ej. 1810")
                },
                singleLine = true,
                suffix = {
                    Text("g")
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedSuffixColor = Color.Black,
                    unfocusedSuffixColor = Color.Black,
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
                label = {
                    Text("Notas (opcional)")
                },
                placeholder = {
                    Text("Aspecto de la planta, ubicación...")
                },
                minLines = 2,
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

            if (mensajeError.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = TerracotaClaro
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mensajeError,
                        color = TextoOscuro,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !guardando,
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
                        val peso = pesoTexto
                            .trim()
                            .replace(',', '.')
                            .toDoubleOrNull()

                        if (
                            peso == null ||
                            !peso.isFinite() ||
                            peso <= 0.0
                        ) {
                            mensajeError =
                                "Introduce un peso válido mayor que cero."
                        } else if (fechaHora.isBlank()) {
                            mensajeError =
                                "Selecciona la fecha y hora del registro."
                        } else {
                            guardando = true
                            mensajeError = ""

                            scope.launch {
                                try {
                                    val rutaFoto: String? =
                                        if (fotoBascula != null) {
                                            withContext(Dispatchers.IO) {
                                                copiarFotoPrivada(
                                                    context,
                                                    fotoBascula!!,
                                                    "maceta"
                                                )
                                            } ?: error(
                                                "No se pudo guardar la fotografía."
                                            )
                                        } else {
                                            null
                                        }

                                    if (onSaveWithPhotos != null) {
                                        onSaveWithPhotos(
                                            peso,
                                            fechaHora,
                                            notas.trim(),
                                            rutaFoto,
                                            rutaFoto
                                        )
                                    } else {
                                        onSave(
                                            peso,
                                            fechaHora,
                                            notas.trim()
                                        )
                                    }
                                } catch (e: Exception) {
                                    mensajeError =
                                        e.message
                                            ?: "No se pudo guardar el registro."
                                } finally {
                                    guardando = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !leyendoPeso && !guardando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdeBosque,
                        contentColor = Blanco
                    )
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            color = Blanco
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }
    }
}

private fun leerFechaHoraExif(
    context: Context,
    uri: Uri
): String? {
    return try {
        val fechaExif = context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                val exif = ExifInterface(input)

                exif.getAttribute(
                    ExifInterface.TAG_DATETIME_ORIGINAL
                ) ?: exif.getAttribute(
                    ExifInterface.TAG_DATETIME_DIGITIZED
                ) ?: exif.getAttribute(
                    ExifInterface.TAG_DATETIME
                )
            } ?: return null

        val formatoExif = SimpleDateFormat(
            "yyyy:MM:dd HH:mm:ss",
            Locale.US
        ).apply {
            isLenient = false
        }

        val fecha = formatoExif.parse(fechaExif)
            ?: return null

        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(fecha)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun PhotoPreview(
    uri: Uri?,
    context: Context,
    description: String
) {
    val bitmap: Bitmap? by produceState(
        initialValue = null,
        key1 = uri
    ) {
        value = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                cargarBitmapOrientado(context, uri)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = VerdeClaro
        )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Aquí aparecerá tu fotografía",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoOscuro
                )
            }
        }
    }
}

private fun copiarFotoPrivada(
    context: Context,
    uri: Uri,
    prefijo: String
): String? {
    val carpeta = File(
        context.filesDir,
        "fotografias"
    )

    if (!carpeta.exists() && !carpeta.mkdirs()) {
        return null
    }

    val archivoDestino = File(
        carpeta,
        "${prefijo}_${System.currentTimeMillis()}.img"
    )

    return try {
        val input = context.contentResolver
            .openInputStream(uri) ?: return null

        input.use { origen ->
            archivoDestino.outputStream().use { destino ->
                origen.copyTo(destino)
            }
        }

        archivoDestino.absolutePath
    } catch (_: Exception) {
        archivoDestino.delete()
        null
    }
}

private fun cargarBitmapOrientado(
    context: Context,
    uri: Uri
): Bitmap? {
    return try {
        val bitmapOriginal = context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return null

        val orientacion = context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

        val matriz = Matrix()

        when (orientacion) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matriz.setScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matriz.setRotate(180f)
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matriz.setScale(1f, -1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matriz.setRotate(90f)
                matriz.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matriz.setRotate(90f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matriz.setRotate(-90f)
                matriz.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matriz.setRotate(270f)
            }

            else -> {
                return bitmapOriginal
            }
        }

        Bitmap.createBitmap(
            bitmapOriginal,
            0,
            0,
            bitmapOriginal.width,
            bitmapOriginal.height,
            matriz,
            true
        )
    } catch (_: Exception) {
        null
    }
}
