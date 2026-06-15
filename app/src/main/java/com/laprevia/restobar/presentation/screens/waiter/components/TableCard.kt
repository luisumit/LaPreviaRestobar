package com.laprevia.restobar.presentation.screens.waiter.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.presentation.theme.SuccessGreen
import com.laprevia.restobar.presentation.theme.WarningOrange
import com.laprevia.restobar.presentation.theme.InfoBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCard(
    table: Table,
    onClick: () -> Unit
) {
    val (backgroundColor, contentColor, statusIcon) = when (table.status) {
        TableStatus.LIBRE -> Triple(SuccessGreen, MaterialTheme.colorScheme.onPrimary, Icons.Default.TableBar)
        TableStatus.OCUPADA -> Triple(WarningOrange, MaterialTheme.colorScheme.onSecondary, Icons.Default.EventSeat)
        TableStatus.RESERVADA -> Triple(InfoBlue, MaterialTheme.colorScheme.onSurface, Icons.Default.EventSeat)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.TableBar)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .size(120.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mesa ${table.number}",
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = table.status.name.lowercase().replaceFirstChar { it.uppercase() },
                color = contentColor.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            if (table.capacity > 0) {
                Text(
                    text = "${table.capacity} pers.",
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
