package com.laprevia.restobar.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laprevia.restobar.data.local.entity.CashClosureEntity
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.repository.GitLiveCashClosureRepository
import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.domain.service.CashRegisterCalculator
import com.laprevia.restobar.domain.service.SalesCalculator
import com.laprevia.restobar.platform.randomUuid
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reporte con rango de fechas (presets + rango manual dd/MM/yyyy) y export CSV
 * a la carpeta Descargas. Las agregaciones vienen del dominio compartido.
 */

private val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

private fun startOfDay(t: Long): Long = Calendar.getInstance().apply {
    timeInMillis = t
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfDay(t: Long): Long = startOfDay(t) + 24L * 3600_000 - 1

private fun parseMoneyText(value: String): Double =
    value.trim().replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

private fun moneyText(value: Double): String =
    String.format(Locale.US, "%.2f", value)

private enum class RangePreset(val label: String) { HOY("HOY"), AYER("AYER"), SEMANA("7 DÍAS"), MES("30 DÍAS"), MANUAL("RANGO") }

private fun presetBounds(preset: RangePreset): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val day = 24L * 3600_000
    return when (preset) {
        RangePreset.HOY -> startOfDay(now) to endOfDay(now)
        RangePreset.AYER -> startOfDay(now - day) to endOfDay(now - day)
        RangePreset.SEMANA -> startOfDay(now - 6 * day) to endOfDay(now)
        RangePreset.MES -> startOfDay(now - 29 * day) to endOfDay(now)
        RangePreset.MANUAL -> startOfDay(now) to endOfDay(now)
    }
}

@Composable
fun ReporteView(orders: List<Order>, userEmail: String) {
    val scope = rememberCoroutineScope()
    val closureRepo = remember { GitLiveCashClosureRepository() }
    val closures by closureRepo.getClosures().collectAsState(initial = emptyList())
    var preset by remember { mutableStateOf(RangePreset.HOY) }
    var fromText by remember { mutableStateOf(dayFormat.format(Date())) }
    var toText by remember { mutableStateOf(dayFormat.format(Date())) }
    var status by remember { mutableStateOf<String?>(null) }
    var confirmClose by remember { mutableStateOf(false) }
    var openingAmountText by remember { mutableStateOf("0.00") }
    var expenseAmountText by remember { mutableStateOf("0.00") }
    var actualCashText by remember { mutableStateOf("0.00") }

    val (start, end) = if (preset == RangePreset.MANUAL) {
        val from = runCatching { dayFormat.parse(fromText)!!.time }.getOrNull()
        val to = runCatching { dayFormat.parse(toText)!!.time }.getOrNull()
        if (from != null && to != null) startOfDay(minOf(from, to)) to endOfDay(maxOf(from, to))
        else presetBounds(RangePreset.HOY)
    } else presetBounds(preset)

    val charged = orders.filter { it.status == OrderStatus.COMPLETED && it.createdAt in start..end }
    val total = charged.sumOf { SalesCalculator.orderTotal(it) }
    val cash = SalesCalculator.paymentTotal(charged, PaymentMethod.CASH)
    val yape = SalesCalculator.paymentTotal(charged, PaymentMethod.YAPE_PLIN)
    val card = SalesCalculator.paymentTotal(charged, PaymentMethod.CARD)
    val top = SalesCalculator.topProducts(charged, 5)

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Selector de rango
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RangePreset.entries.forEach { p ->
                val selected = preset == p
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Lp.Amber.copy(alpha = 0.14f) else Color.Transparent)
                        .border(1.dp, if (selected) Lp.Amber.copy(alpha = 0.5f) else Lp.FieldBorder, RoundedCornerShape(999.dp))
                        .lpHover()
                        .clickable { preset = p }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        p.label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
                        color = if (selected) Lp.Amber else Lp.TextDim
                    )
                }
            }
        }
        if (preset == RangePreset.MANUAL) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(fromText, { fromText = it }, label = { Text("Desde (dd/MM/yyyy)") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.width(200.dp))
                OutlinedTextField(toText, { toText = it }, label = { Text("Hasta (dd/MM/yyyy)") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.width(200.dp))
            }
        }
        Text(
            "Período: ${dayFormat.format(Date(start))} — ${dayFormat.format(Date(end))}",
            color = Lp.TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )

        // Hero row: los 5 KPI de la noche repartidos a todo el ancho
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCard("TOTAL VENDIDO", Money(total).formatted(), Lp.Amber, Modifier.weight(1f))
            StatCard("PEDIDOS", charged.size.toString(), Lp.Green, Modifier.weight(1f))
            StatCard("PRODUCTOS", SalesCalculator.productsSold(charged).toString(), Lp.Coral, Modifier.weight(1f))
            StatCard(
                "TICKET PROMEDIO",
                Money(if (charged.isEmpty()) 0.0 else total / charged.size).formatted(),
                Lp.Warn, Modifier.weight(1f)
            )
            StatCard(
                "DESCUENTOS",
                Money(charged.sumOf { it.discountAmount ?: 0.0 }).formatted(),
                Lp.Red, Modifier.weight(1f)
            )
        }

        // Metodos de pago con barras proporcionales
        ReportCard("MÉTODOS DE PAGO") {
            PaymentBar("Efectivo", cash, total, Lp.Green)
            PaymentBar("Yape/Plin", yape, total, Lp.Warn)
            PaymentBar("Tarjeta", card, total, Lp.TextSoft)
        }

        ReportCard("TOP PRODUCTOS DEL PERÍODO") {
            if (top.isEmpty()) Text("Sin ventas en el período.", color = Lp.TextDim, fontWeight = FontWeight.SemiBold)
            else top.forEachIndexed { i, p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).background(Lp.Amber.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", color = Lp.Amber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(p.name, color = Lp.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${p.quantity} u", color = Lp.TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Text(Money(p.total).formatted(), color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            LpBigButton(
                text = "EXPORTAR CSV A DESCARGAS",
                enabled = charged.isNotEmpty(),
                filled = false,
                modifier = Modifier.weight(1f)
            ) {
                status = exportCsv(charged, start, end, total, cash, yape, card).fold(
                    onSuccess = { "✅ CSV guardado en: ${it.absolutePath}" },
                    onFailure = { "❌ ${it.message}" }
                )
            }
            LpBigButton(
                text = "CERRAR CAJA DEL PERÍODO",
                enabled = charged.isNotEmpty(),
                filled = true,
                modifier = Modifier.weight(1f)
            ) {
                openingAmountText = "0.00"
                expenseAmountText = "0.00"
                actualCashText = moneyText(CashRegisterCalculator.incomeAmount(cash))
                confirmClose = true
            }
        }

        status?.let { Text(it, color = Lp.Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

        if (closures.isNotEmpty()) {
            ReportCard("CIERRES DE CAJA GUARDADOS") {
                closures.take(8).forEach { c ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Lp.Field)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${dayFormat.format(Date(c.periodStart))} — ${dayFormat.format(Date(c.periodEnd))}",
                                color = Lp.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                            Text(Money(c.totalSales).formatted(), color = Lp.Amber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            "Efec. ${Money(c.cashSales).formatted()}  ·  Yape ${Money(c.yapePlinSales).formatted()}  ·  " +
                                "Tarj. ${Money(c.cardSales).formatted()}  ·  ${c.createdBy}",
                            color = Lp.TextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Esperado ${Money(c.expectedCash).formatted()}  ·  Real ${Money(c.actualCash).formatted()}  ·  " +
                                "Dif. ${Money(c.cashDifference).formatted()}",
                            color = if (c.cashDifference == 0.0) Lp.TextDim else Lp.Amber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmClose) {
        val incomeAmount = CashRegisterCalculator.incomeAmount(cash)
        val expectedCash = CashRegisterCalculator.expectedCash(
            openingAmount = parseMoneyText(openingAmountText),
            incomeAmount = incomeAmount,
            expenseAmount = parseMoneyText(expenseAmountText)
        )
        val cashDifference = CashRegisterCalculator.cashDifference(
            actualCash = parseMoneyText(actualCashText),
            expectedCash = expectedCash
        )
        Dialog(onDismissRequest = { confirmClose = false }) {
            LpDialogCard(width = 460) {
                Text("CONFIRMAR CIERRE DE CAJA", fontFamily = BebasFamily, fontSize = 24.sp, letterSpacing = 1.5.sp, color = Lp.Text)
                Text("Período: ${dayFormat.format(Date(start))} — ${dayFormat.format(Date(end))}", color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(Money(total).formatted(), color = Lp.Amber, fontFamily = BebasFamily, fontSize = 34.sp, letterSpacing = 1.sp)
                Text("Efectivo ${Money(cash).formatted()}  ·  Yape ${Money(yape).formatted()}  ·  Tarjeta ${Money(card).formatted()}", color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Pedidos cobrados: ${charged.size}", color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = openingAmountText,
                    onValueChange = { openingAmountText = it },
                    label = { Text("Monto inicial") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expenseAmountText,
                    onValueChange = { expenseAmountText = it },
                    label = { Text("Egresos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = actualCashText,
                    onValueChange = { actualCashText = it },
                    label = { Text("Efectivo real contado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Efectivo esperado: ${Money(expectedCash).formatted()}", color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Diferencia de caja: ${Money(cashDifference).formatted()}",
                    color = if (cashDifference == 0.0) Lp.TextSoft else Lp.Amber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { confirmClose = false },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Lp.FieldBorder),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar", color = Lp.TextSoft) }
                    Button(
                        onClick = {
                            confirmClose = false
                            scope.launch {
                                val closure = CashClosureEntity(
                                    id = randomUuid(),
                                    periodStart = start,
                                    periodEnd = end,
                                    totalSales = total,
                                    grossProfit = 0.0, // costo por producto no disponible en el panel v1
                                    chargedOrders = charged.size,
                                    cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED && it.createdAt in start..end },
                                    productsSold = SalesCalculator.productsSold(charged),
                                    cashSales = cash,
                                    yapePlinSales = yape,
                                    cardSales = card,
                                    bestSellingProduct = top.firstOrNull()?.let { "${it.name} (${it.quantity})" } ?: "Sin ventas",
                                    openingAmount = parseMoneyText(openingAmountText),
                                    incomeAmount = incomeAmount,
                                    expenseAmount = parseMoneyText(expenseAmountText),
                                    expectedCash = expectedCash,
                                    actualCash = parseMoneyText(actualCashText),
                                    cashDifference = cashDifference,
                                    createdBy = "Caja PC ($userEmail)"
                                )
                                runCatching { closureRepo.saveClosure(closure) }
                                    .onSuccess { status = "✅ Cierre de caja guardado en Firebase" }
                                    .onFailure { status = "❌ Error guardando cierre: ${it.message}" }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cerrar caja", fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().lpCard(16.dp).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = Lp.TextDim)
        content()
    }
}

@Composable
private fun PaymentBar(label: String, value: Double, total: Double, color: Color) {
    val fraction = if (total > 0) (value / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(Money(value).formatted(), color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.06f))) {
            if (fraction > 0f) {
                Box(
                    Modifier.fillMaxWidth(fraction).fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun LpBigButton(text: String, enabled: Boolean, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    if (filled) {
        Box(
            modifier.height(48.dp).clip(shape)
                .background(
                    if (enabled) Brush.linearGradient(listOf(Lp.Amber, Lp.AmberDeep))
                    else Brush.linearGradient(listOf(Lp.Amber.copy(alpha = 0.3f), Lp.AmberDeep.copy(alpha = 0.3f)))
                )
                .lpHover(0.10f, enabled = enabled)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.5.sp, color = Lp.OnAccent)
        }
    } else {
        Box(
            modifier.height(48.dp).clip(shape)
                .border(1.dp, if (enabled) Lp.FieldBorder else Lp.FieldBorder.copy(alpha = 0.05f), shape)
                .lpHover(enabled = enabled)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.5.sp,
                color = if (enabled) Lp.TextSoft else Lp.TextMuted
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.lpCard(16.dp).padding(18.dp)
    ) {
        Text(title, color = Lp.TextDim, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = accent, fontFamily = BebasFamily, fontSize = 40.sp, letterSpacing = 1.sp, style = TabularNumbers)
    }
}

private fun exportCsv(
    charged: List<Order>, start: Long, end: Long,
    total: Double, cash: Double, yape: Double, card: Double
): Result<File> = runCatching {
    val rangeName = SimpleDateFormat("yyyyMMdd", Locale.US).let { "${it.format(Date(start))}-${it.format(Date(end))}" }
    val downloads = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
    val file = File(downloads, "reporte_laprevia_$rangeName.csv")
    val money = { v: Double -> String.format(Locale.US, "%.2f", v) }

    file.writeText(buildString {
        append('﻿') // BOM para que Excel muestre bien los acentos
        appendLine("REPORTE DE VENTAS - LA PREVIA RESTOBAR")
        appendLine("Periodo,${dayFormat.format(Date(start))} - ${dayFormat.format(Date(end))}")
        appendLine("Total vendido,S/ ${money(total)}")
        appendLine("Pedidos cobrados,${charged.size}")
        appendLine("Efectivo,S/ ${money(cash)}")
        appendLine("Yape/Plin,S/ ${money(yape)}")
        appendLine("Tarjeta,S/ ${money(card)}")
        appendLine()
        appendLine("Fecha,Mesa,Ticket,Metodo,Descuento,Total,Productos")
        charged.sortedBy { it.createdAt }.forEach { order ->
            val items = order.items.joinToString(" | ") { "${it.quantity}x ${it.productName}" }
            appendLine(
                "${dateTimeFormat.format(Date(order.paidAt ?: order.createdAt))}," +
                    "M${order.tableNumber.toString().padStart(2, '0')}," +
                    "${order.receiptNumber ?: ""}," +
                    "${order.paymentMethod.label}," +
                    "S/ ${money(order.discountAmount ?: 0.0)}," +
                    "S/ ${money(SalesCalculator.orderTotal(order))}," +
                    "\"$items\""
            )
        }
    }, Charsets.UTF_8)
    file
}
