package com.laprevia.restobar.presentation.screens.waiter.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCard(
    table: Table,
    onClick: () -> Unit
) {
    val backgroundColor = when (table.status) {
        TableStatus.LIBRE -> Color(0xFF4CAF50) // Verde
        TableStatus.OCUPADA -> Color(0xFFFF9800) // Naranja
        TableStatus.RESERVADA -> Color(0xFF2196F3) // Azul

        else -> Color(0xFF9E9E9E) // Gris por defecto
    }

    val textColor = Color.White

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Mesa ${table.number}",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = table.status.name.lowercase().replaceFirstChar { it.uppercase() },
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            if (table.capacity > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${table.capacity} personas",
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}