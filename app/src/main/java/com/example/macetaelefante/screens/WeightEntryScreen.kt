package com.example.macetaelefante.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var fotoPlanta by remember {
        mutableStateOf<Uri?>(null)
    }

    var fechaHora by rememberSaveable {
        mutableStateOf("")
    }

    var fechaHoraDisponible by rememberSaveable {
        mutableStateOf(false)
    }

    var leyendoPeso by remember {
        mutableStateOf(false)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

    var mensajeLectura by rememberSaveable {
        mutableStateOf("")
    }

    val selectorFotoBascula =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                fotoBascula = uri
                pesoTexto = ""
                mensajeError = ""
                mensajeLectura = "Analizando la fotografía..."
                leyendoPeso = true

                scope.launch {
                    val fechaExif = withContext(Dispatchers.IO) {
                        leerFechaHoraExif(context, uri)
                    }

                    if (fechaExif != null) {
                        fechaHora = fechaExif
                        fechaHoraDisponible = true
                    } else {
                        fechaHora = ""
                        fechaHoraDisponible = false
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

                                    mensajeLectura =
                                        "Peso detectado. Verifica que " +
                                                "el número sea correcto."
                                } else {
                                    mensajeLectura =
                                        "No se detectó el peso. " +
                                                "Puedes introducirlo manualmente."
                                }

                                if (!fechaHoraDisponible) {
                                    mensajeLectura +=
                                        "\nLa foto no contiene una fecha " +
                                                "EXIF válida. Usa una foto " +
                                                "original que conserve sus metadatos."
                                }

                                leyendoPeso = false
                                reconocedor.close()
                            }
                            .addOnFailureListener {
                                leyendoPeso = false
                                mensajeLectura =
                                    "No se pudo leer el peso. " +
                                            "Introdúcelo manualmente."

                                if (!fechaHoraDisponible) {
                                    mensajeLectura +=
                                        "\nTampoco se encontró fecha EXIF."
                                }

                                reconocedor.close()
                            }
                    } catch (e: Exception) {
                        leyendoPeso = false
                        mensajeLectura =
                            "No se pudo analizar la foto. " +
                                    "Introduce el peso manualmente."
                    }
                }
            }
        }

    val selectorFotoPlanta =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                fotoPlanta = uri
                mensajeError = ""
            }
        }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("← Volver")
            }

            Text(
                text = "Registrar peso",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Maceta Elefante",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Fecha y hora de la fotografía",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (fechaHoraDisponible) {
                            fechaHora
                        } else {
                            "Fecha EXIF no disponible"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = if (fechaHoraDisponible) {
                            "Tomada de los metadatos de la foto de la báscula."
                        } else {
                            "Selecciona una fotografía original que conserve " +
                                    "la fecha y hora en sus metadatos."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "Fotografía de la báscula",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            PhotoPreview(
                uri = fotoBascula,
                context = context,
                emoji = "⚖️",
                description = "Fotografía de la báscula"
            )

            OutlinedButton(
                onClick = {
                    selectorFotoBascula.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seleccionar foto de la báscula")
            }

            if (leyendoPeso) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()

                    Text(
                        text = "Analizando la fotografía...",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (mensajeLectura.isNotBlank()) {
                Text(
                    text = mensajeLectura,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Peso de la maceta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = pesoTexto,
                onValueChange = {
                    pesoTexto = it
                    mensajeError = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Peso en gramos")
                },
                placeholder = {
                    Text("Ej. 1810")
                },
                singleLine = true,
                suffix = {
                    Text("g")
                }
            )

            Text(
                text = "Revisa el peso detectado y corrígelo " +
                        "si es necesario antes de guardar.",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Fotografía de la planta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            PhotoPreview(
                uri = fotoPlanta,
                context = context,
                emoji = "🪴",
                description = "Fotografía de tu planta"
            )

            OutlinedButton(
                onClick = {
                    selectorFotoPlanta.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seleccionar foto de la planta")
            }

            OutlinedTextField(
                value = notas,
                onValueChange = {
                    notas = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Notas (opcional)")
                },
                placeholder = {
                    Text("Aspecto de la planta, ubicación...")
                },
                minLines = 2
            )

            if (mensajeError.isNotBlank()) {
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !guardando
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
                        } else if (!fechaHoraDisponible) {
                            mensajeError =
                                "No se puede guardar: la fotografía de la " +
                                        "báscula no contiene una fecha EXIF válida."
                        } else {
                            guardando = true
                            mensajeError = ""

                            scope.launch {
                                try {
                                    val rutas = withContext(
                                        Dispatchers.IO
                                    ) {
                                        val rutaBascula =
                                            fotoBascula?.let {
                                                copiarFotoPrivada(
                                                    context,
                                                    it,
                                                    "bascula"
                                                )
                                            }

                                        val rutaPlanta =
                                            fotoPlanta?.let {
                                                copiarFotoPrivada(
                                                    context,
                                                    it,
                                                    "planta"
                                                )
                                            }

                                        if (
                                            fotoBascula != null &&
                                            rutaBascula == null
                                        ) {
                                            error(
                                                "No se pudo guardar " +
                                                        "la foto de la báscula."
                                            )
                                        }

                                        if (
                                            fotoPlanta != null &&
                                            rutaPlanta == null
                                        ) {
                                            error(
                                                "No se pudo guardar " +
                                                        "la foto de la planta."
                                            )
                                        }

                                        Pair(
                                            rutaBascula,
                                            rutaPlanta
                                        )
                                    }

                                    if (onSaveWithPhotos != null) {
                                        onSaveWithPhotos(
                                            peso,
                                            fechaHora,
                                            notas.trim(),
                                            rutas.first,
                                            rutas.second
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
                                            ?: "No se pudieron guardar las fotografías."
                                } finally {
                                    guardando = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !leyendoPeso && !guardando
                ) {
                    if (guardando) {
                        CircularProgressIndicator()
                    } else {
                        Text("Guardar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
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

                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            } ?: return null

        val formatoExif = SimpleDateFormat(
            "yyyy:MM:dd HH:mm:ss",
            Locale.US
        ).apply {
            isLenient = false
        }

        val fecha = formatoExif.parse(fechaExif) ?: return null

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
    emoji: String,
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
            .height(190.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
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
                    text = emoji,
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
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
    val carpeta = File(context.filesDir, "fotografias")

    if (!carpeta.exists() && !carpeta.mkdirs()) {
        return null
    }

    val archivoDestino = File(
        carpeta,
        "${prefijo}_${System.currentTimeMillis()}.img"
    )

    return try {
        val input = context.contentResolver.openInputStream(uri)
            ?: return null

        input.use { origen ->
            archivoDestino.outputStream().use { destino ->
                origen.copyTo(destino)
            }
        }

        archivoDestino.absolutePath
    } catch (e: Exception) {
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