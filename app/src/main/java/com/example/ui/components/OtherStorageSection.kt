package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OtherStorageItem
import com.example.model.SafetyLevel
import com.example.model.formatStorageSize
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.ShizukuViolet
import com.example.ui.theme.TechDarkBackground
import com.example.ui.theme.TechDarkCardBorder
import com.example.ui.theme.TechDarkSurface
import com.example.ui.theme.TechDarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OtherStorageSection(
    items: List<OtherStorageItem>,
    isScanning: Boolean,
    statusMessage: String,
    totalSafeBytes: Long,
    totalSelectedBytes: Long,
    onStartScan: () -> Unit,
    onToggleItem: (String) -> Unit,
    onSelectAllSafe: () -> Unit,
    onDeselectAll: () -> Unit,
    onCleanSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<SafetyLevel?>(null) }
    val filteredItems = remember(items, selectedFilter) {
        if (selectedFilter == null) items else items.filter { it.safety == selectedFilter }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("other_storage_card")
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TechDarkSurface),
        border = BorderStroke(1.dp, TechDarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Cabecera de la sección
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ShizukuViolet.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ShizukuViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Desglose de «Otros»",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analizador profundo con Script Shell",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = onStartScan,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        disabledContainerColor = CyanPrimary.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("scan_other_storage_btn")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Escaneando..." else "Escanear",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Explicación de qué es "Otros" si no se ha escaneado
            if (items.isEmpty() && !isScanning) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TechDarkSurfaceVariant,
                    border = BorderStroke(1.dp, TechDarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Android agrupa en «Otros» todo lo que no es video, música o fotos estándar: miniaturas gigantes (.thumbnails), cachés no indexadas de Telegram/WhatsApp, temporales y datos de juegos. Toca «Escanear» para inspeccionarlo detalladamente.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Estado durante el escaneo
            if (isScanning) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CyanPrimary,
                    trackColor = TechDarkSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage.ifEmpty { "Analizando almacenamiento con script Shell..." },
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Resultados del escaneo
            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Resumen de pesos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniCard(
                        title = "Seguro de borrar",
                        value = formatStorageSize(totalSafeBytes),
                        color = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = "Seleccionado",
                        value = formatStorageSize(totalSelectedBytes),
                        color = CyanPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chips de filtro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Todos (${items.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TechDarkSurfaceVariant,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == SafetyLevel.SAFE,
                        onClick = { selectedFilter = if (selectedFilter == SafetyLevel.SAFE) null else SafetyLevel.SAFE },
                        label = { Text("Seguros", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSuccess.copy(alpha = 0.25f),
                            selectedLabelColor = EmeraldSuccess
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == SafetyLevel.CAUTION,
                        onClick = { selectedFilter = if (selectedFilter == SafetyLevel.CAUTION) null else SafetyLevel.CAUTION },
                        label = { Text("Precaución", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarning.copy(alpha = 0.25f),
                            selectedLabelColor = AmberWarning
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == SafetyLevel.KEEP,
                        onClick = { selectedFilter = if (selectedFilter == SafetyLevel.KEEP) null else SafetyLevel.KEEP },
                        label = { Text("Vital", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedDanger.copy(alpha = 0.25f),
                            selectedLabelColor = RedDanger
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Acciones rápidas de selección
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Text(
                            text = "Marcar solo seguros",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onSelectAllSafe() }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Desmarcar todos",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { onDeselectAll() }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }

                    if (totalSelectedBytes > 0) {
                        Button(
                            onClick = onCleanSelected,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("clean_selected_other_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limpiar (${formatStorageSize(totalSelectedBytes)})",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lista de elementos encontrados en "Otros"
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredItems.forEach { item ->
                        OtherStorageItemCard(
                            item = item,
                            onToggle = { onToggleItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = TechDarkSurfaceVariant,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OtherStorageItemCard(
    item: OtherStorageItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor: Color = when (item.safety) {
        SafetyLevel.SAFE -> EmeraldSuccess
        SafetyLevel.CAUTION -> AmberWarning
        SafetyLevel.KEEP -> RedDanger
    }
    val badgeIcon: androidx.compose.ui.graphics.vector.ImageVector = when (item.safety) {
        SafetyLevel.SAFE -> Icons.Default.CheckCircle
        SafetyLevel.CAUTION -> Icons.Default.Warning
        SafetyLevel.KEEP -> Icons.Default.Lock
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.safety != SafetyLevel.KEEP) { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = TechDarkSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (item.isSelected) badgeColor.copy(alpha = 0.5f) else TechDarkCardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.safety == SafetyLevel.KEEP) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(RedDanger.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Protegido",
                        tint = RedDanger,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = badgeColor,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = Color.Black
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatStorageSize(item.sizeBytes),
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.category} • ${item.safety.label}",
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
