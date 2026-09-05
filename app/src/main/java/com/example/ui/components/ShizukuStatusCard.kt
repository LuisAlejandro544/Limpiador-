package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShizukuInfo
import com.example.model.ShizukuStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.ShizukuViolet
import com.example.ui.theme.ShizukuVioletSurface
import com.example.ui.theme.TechDarkCardBorder
import com.example.ui.theme.TechDarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ShizukuStatusCard(
    shizukuInfo: ShizukuInfo,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onOpenShizuku: (() -> Unit)? = null,
    onAutoGrantStorage: (() -> Unit)? = null,
    onTrimSystemCaches: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    val (statusColor, statusText, statusIcon) = when (shizukuInfo.status) {
        ShizukuStatus.AUTHORIZED -> Triple(EmeraldSuccess, "Conectado (Nivel ADB)", Icons.Default.CheckCircle)
        ShizukuStatus.PERMISSION_REQUIRED -> Triple(AmberWarning, "Requiere Autorización", Icons.Default.Bolt)
        ShizukuStatus.SERVICE_STOPPED -> Triple(AmberWarning, "Servicio Inactivo", Icons.Default.Warning)
        ShizukuStatus.NOT_INSTALLED -> Triple(RedDanger, "No Detectado", Icons.Default.Info)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shizuku_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ShizukuVioletSurface),
        border = BorderStroke(1.dp, ShizukuViolet.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ShizukuViolet.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Shizuku",
                            tint = ShizukuViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Integración con Shizuku",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Limpieza profunda sin Root",
                            fontSize = 11.sp,
                            color = ShizukuViolet.copy(alpha = 0.9f)
                        )
                    }
                }

                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(36.dp).testTag("shizuku_help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Ayuda de Shizuku",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TechDarkSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                when (shizukuInfo.status) {
                    ShizukuStatus.PERMISSION_REQUIRED -> {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = ShizukuViolet),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("shizuku_authorize_button")
                        ) {
                            Text("Autorizar", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    ShizukuStatus.SERVICE_STOPPED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (onOpenShizuku != null) {
                                Button(
                                    onClick = onOpenShizuku,
                                    colors = ButtonDefaults.buttonColors(containerColor = ShizukuViolet),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp).testTag("shizuku_open_app_button")
                                ) {
                                    Text("Abrir App", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            OutlinedButton(
                                onClick = onRefresh,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("shizuku_retry_button")
                            ) {
                                Text("Reintentar", fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                    ShizukuStatus.NOT_INSTALLED -> {
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("shizuku_refresh_button")
                        ) {
                            Text("Reintentar", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                    ShizukuStatus.AUTHORIZED -> {
                        Text(
                            text = "v${shizukuInfo.version}",
                            fontSize = 11.sp,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = shizukuInfo.message,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )

            if (shizukuInfo.status == ShizukuStatus.AUTHORIZED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onTrimSystemCaches != null) {
                        OutlinedButton(
                            onClick = onTrimSystemCaches,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("shizuku_trim_caches_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recortar Caché SO",
                                fontSize = 11.sp,
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (onAutoGrantStorage != null) {
                        Button(
                            onClick = onAutoGrantStorage,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("shizuku_grant_storage_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ShizukuViolet)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-conceder",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "¿Qué es Shizuku y cómo funciona?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Shizuku es una herramienta que otorga a las aplicaciones permisos de nivel ADB (Android Debug Bridge) de forma segura sin requerir Root.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Beneficios en Limpieza Profunda:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanPrimary
                    )
                    Text(
                        text = "• Vaciar la caché oculta del sistema y de todas las apps instaladas mediante 'pm trim-caches'.\n" +
                                "• Acceso para limpiar archivos en /sdcard/Android/data y /data/local/tmp.\n" +
                                "• Sin necesidad de conectar a una PC si tienes Android 11+ (se inicia mediante 'Depuración Inalámbrica' directamente en el móvil).",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nota: En este prototipo, la app ya detecta Shizuku y cuenta con el limpiador de almacenamiento local funcional.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Entendido", color = CyanPrimary)
                }
            },
            containerColor = TechDarkSurface
        )
    }
}
