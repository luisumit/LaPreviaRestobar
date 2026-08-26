package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laprevia.restobar.data.model.Order
import java.util.Locale

private fun cashMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

/**
 * Captura el efectivo recibido y calcula el vuelto antes de cobrar.
 * Solo se usa para pagos en efectivo.
 */
@Composable
fun CashPaymentDialog(
    order: Order,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val total = order.total.takeIf { it > 0.0 } ?: order.items.sumOf { it.subtotal }
    var receivedText by remember { mutableStateOf("") }

    val received = receivedText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val change = (received - total).coerceAtLeast(0.0)
    val enough = received >= total && received > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Pago en efectivo - Mesa ${order.tableNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Total: S/ ${cashMoney(total)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = receivedText,
                    onValueChange = { receivedText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("¿Con cuánto paga? (S/)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Montos rapidos
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { receivedText = cashMoney(total) }, modifier = Modifier.weight(1f)) { Text("Exacto") }
                    listOf(20.0, 50.0, 100.0).forEach { amount ->
                        OutlinedButton(onClick = { receivedText = cashMoney(amount) }, modifier = Modifier.weight(1f)) { Text("${amount.toInt()}") }
                    }
                }

                Divider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vuelto:", color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "S/ ${cashMoney(change)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (enough) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                if (!enough && receivedText.isNotBlank()) {
                    Text("El monto recibido es menor al total.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = { onConfirm(received) },
                        modifier = Modifier.weight(1f),
                        enabled = enough
                    ) { Text("Cobrar") }
                }
            }
        }
    }
}
