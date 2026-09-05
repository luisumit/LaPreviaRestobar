package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laprevia.restobar.data.model.Order
import java.util.Locale

private fun splitMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

/**
 * Calcula como dividir la cuenta de una mesa: en partes iguales o asignando cada
 * producto a un comensal. Es solo informativo (para cobrar a cada persona); no
 * cambia como se guarda ni se cobra el pedido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    val items = order.items
    val total = order.total.takeIf { it > 0.0 } ?: items.sumOf { it.subtotal }

    var people by remember { mutableIntStateOf(2) }
    var byProduct by remember { mutableStateOf(false) }
    // itemIndex -> numero de comensal (1..people)
    val assignment = remember { mutableStateMapOf<Int, Int>() }

    // Mantiene las asignaciones dentro del rango cuando cambia el numero de comensales.
    items.indices.forEach { i -> if ((assignment[i] ?: 1) > people) assignment[i] = people }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Dividir cuenta - Mesa ${order.tableNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Total: S/ ${splitMoney(total)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                // Numero de comensales
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Comensales:", color = MaterialTheme.colorScheme.onSurface)
                    OutlinedButton(onClick = { if (people > 1) people-- }) { Text("-") }
                    Text("$people", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedButton(onClick = { if (people < 12) people++ }) { Text("+") }
                }

                // Modo
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !byProduct, onClick = { byProduct = false }, label = { Text("Partes iguales") })
                    FilterChip(selected = byProduct, onClick = { byProduct = true }, label = { Text("Por producto") })
                }

                HorizontalDivider()

                if (!byProduct) {
                    val perPerson = com.laprevia.restobar.domain.SplitBill.perPerson(total, people)
                    Text("Cada persona paga:", color = MaterialTheme.colorScheme.onSurface)
                    Text("S/ ${splitMoney(perPerson)}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "$people x S/ ${splitMoney(perPerson)} = S/ ${splitMoney(perPerson * people)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                } else {
                    Text("Asigna cada producto a un comensal:", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    items.forEachIndexed { index, item ->
                        val current = assignment[index] ?: 1
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "${item.quantity} x ${item.productName}",
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                                Text("S/ ${splitMoney(item.subtotal)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..people).forEach { c ->
                                    FilterChip(
                                        selected = current == c,
                                        onClick = { assignment[index] = c },
                                        label = { Text("C$c") }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Total por comensal:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    val perComensal = com.laprevia.restobar.domain.SplitBill.perComensal(
                        subtotals = items.map { it.subtotal },
                        assignment = assignment,
                        people = people
                    )
                    (1..people).forEach { c ->
                        val subtotal = perComensal[c] ?: 0.0
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Comensal $c", color = MaterialTheme.colorScheme.onSurface)
                            Text("S/ ${splitMoney(subtotal)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider()
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
            }
        }
    }
}
