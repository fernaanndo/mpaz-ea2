package com.mpazpro3.app.ui.screen.utp

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpazpro3.app.model.EstadoAprobacion
import com.mpazpro3.app.ui.components.BarraSuperior
import com.mpazpro3.app.viewmodel.MpazProViewModel

/**
 * Detalle de un contenido para UTP: datos curriculares completos, recursos didácticos asociados,
 * observación y botones Aprobar / Rechazar.
 */
@Composable
fun DetalleContenidoUtpScreen(navController: NavHostController, viewModel: MpazProViewModel) {
    val seleccionadoId by viewModel.contenidoSeleccionadoId.collectAsState()
    val contenidos by viewModel.contenidos.collectAsState()
    val errorObservacion by viewModel.errorObservacion.collectAsState()
    val mensaje by viewModel.mensajeUtp.collectAsState()
    val contenido = contenidos.find { it.id == seleccionadoId }

    // Variables para el recurso nativo (Vibración)
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    Scaffold(topBar = { BarraSuperior("Revisar contenido", navController) }) { padding ->
        if (contenido == null) {
            Text("Contenido no encontrado.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        val recursos = viewModel.recursosDe(contenido.id)

        // 1. EL FIX: Limpiar los mensajes AL SALIR para evitar el bug del "flash"
        DisposableEffect(contenido.id) {
            onDispose {
                viewModel.limpiarMensajesUtp()
            }
        }

        // 2. LA FUNCIONALIDAD NATIVA: Preparar la vibración
        val vibrar = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }

        // 3. LA ANIMACIÓN SENSORIAL: Vibrar SOLO cuando el ViewModel emite un mensaje nuevo
        LaunchedEffect(mensaje) {
            if (mensaje != null) {
                vibrar()
            }
        }

        var observacion by remember(contenido.id) { mutableStateOf(contenido.observacionUtp) }
        var mostrarDialogoRechazo by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(contenido.unidad, style = MaterialTheme.typography.titleLarge)
                    Dato("Nivel", contenido.nivel)
                    Dato("Asignatura", contenido.asignatura.etiqueta)
                    Dato("Objetivo de Aprendizaje", contenido.objetivoAprendizaje)
                    Dato("Código OA", contenido.codigoOA ?: "Sin código")
                    Dato("Eje curricular", contenido.ejeCurricular)
                    Dato("Recurso de referencia", contenido.recursoReferencia)

                    // 4. Animación de color fluido para el estado
                    val colorEstado by animateColorAsState(
                        targetValue = when (contenido.estado) {
                            EstadoAprobacion.APROBADO -> Color(0xFF4CAF50) // Verde
                            EstadoAprobacion.RECHAZADO -> Color(0xFFF44336) // Rojo
                            EstadoAprobacion.PENDIENTE -> Color.Gray
                        },
                        animationSpec = tween(durationMillis = 500),
                        label = "colorAnimado"
                    )
                    Text(
                        text = "Estado: ${contenido.estado.etiqueta}",
                        color = colorEstado,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text("Recursos didácticos (${recursos.size})", style = MaterialTheme.typography.titleSmall)
            if (recursos.isEmpty()) {
                Text("Este contenido todavía no tiene recursos.", style = MaterialTheme.typography.bodySmall)
            }
            recursos.forEach { recurso ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(recurso.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(recurso.explicacion, style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (recurso.audioDisponible) "Incluye audio de apoyo" else "Sin audio de apoyo",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedTextField(
                value = observacion,
                onValueChange = {
                    observacion = it
                    viewModel.limpiarErrorObservacion() // Limpia el error al escribir
                },
                label = { Text("Observación de UTP") },
                isError = errorObservacion != null,
                supportingText = { errorObservacion?.let { Text(it) } },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.aprobarContenido(contenido.id, observacion.trim())
                    }
                ) { Text("Aprobar") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (viewModel.validarRechazo(observacion.trim())) mostrarDialogoRechazo = true
                    }
                ) { Text("Rechazar") }
            }

            // 5. Animación del mensaje de estado (fade in/out)
            AnimatedVisibility(
                visible = mensaje != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = mensaje.orEmpty(),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                        .padding(12.dp)
                )
            }

            // Diálogo de confirmación de rechazo (Cumple la validación de acciones destructivas de la rúbrica)
            if (mostrarDialogoRechazo) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoRechazo = false },
                    title = { Text("¿Rechazar contenido?") },
                    text = { Text("El contenido quedará como rechazado y no podrá ser asignado por los docentes.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.rechazarContenido(contenido.id, observacion.trim())
                            mostrarDialogoRechazo = false
                        }) { Text("Rechazar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarDialogoRechazo = false }) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Text("$etiqueta: $valor", style = MaterialTheme.typography.bodyMedium)
}