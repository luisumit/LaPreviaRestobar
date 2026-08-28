package com.laprevia.restobar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.domain.service.SalesCalculator
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

private enum class RangePreset(val label: String) { HOY("Hoy"), AYER("Ayer"), SEMANA("7 dias"), MES("30 dias"), MANUAL("Rango") }

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
fun ReporteView(orders: List<Order>) {
    var preset by remember { mutableStateOf(RangePreset.HOY) }
    var fromText by remember { mutableStateOf(dayFormat.format(Date())) }
    var toText by remember { mutableStateOf(dayFormat.format(Date())) }
    var status by remember { mutableStateOf<String?>(null) }

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

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Selector de rango
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RangePreset.entries.forEach { p ->
                FilterChip(selected = preset == p, onClick = { preset = p }, label = { Text(p.label) })
            }
        }
        if (preset == RangePreset.MANUAL) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(fromText, { fromText = it }, label = { Text("Desde (dd/MM/yyyy)") }, singleLine = true, modifier = Modifier.width(200.dp))
                OutlinedTextField(toText, { toText = it }, label = { Text("Hasta (dd/MM/yyyy)") }, singleLine = true, modifier = Modifier.width(200.dp))
            }
        }
        Text(
            "Periodo: ${dayFormat.format(Date(start))} — ${dayFormat.format(Date(end))}",
            color = Color(0xFFF5F5F5).copy(alpha = 0.6f), fontSize = 12.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Total vendido", Money(total).formatted(), Color(0xFFFFB300))
            StatCard("Pedidos cobrados", charged.size.toString(), Color(0xFF66BB6A))
            StatCard("Productos vendidos", SalesCalculator.productsSold(charged).toString(), Color(0xFFFF6E40))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Metodos de pago", fontWeight = FontWeight.Bold, color = Color(0xFFF5F5F5))
                Text("Efectivo: ${Money(cash).formatted()}", color = Color(0xFF66BB6A))
                Text("Yape/Plin: ${Money(yape).formatted()}", color = Color(0xFFFFB74D))
                Text("Tarjeta: ${Money(card).formatted()}", color = Color(0xFFF5F5F5))
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Top productos del periodo", fontWeight = FontWeight.Bold, color = Color(0xFFF5F5F5))
                if (top.isEmpty()) Text("Sin ventas en el periodo.", color = Color(0xFFF5F5F5).copy(alpha = 0.6f))
                else top.forEachIndexed { i, p ->
                    Text("${i + 1}. ${p.name} — ${p.quantity} u  ·  ${Money(p.total).formatted()}", color = Color(0xFFF5F5F5).copy(alpha = 0.85f))
                }
            }
        }

        Button(
            onClick = {
                status = exportCsv(charged, start, end, total, cash, yape, card).fold(
                    onSuccess = { "✅ CSV guardado en: ${it.absolutePath}" },
                    onFailure = { "❌ ${it.message}" }
                )
            },
            enabled = charged.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) { Text("📊 Exportar CSV a Descargas", fontWeight = FontWeight.Bold) }
        status?.let { Text(it, color = Color(0xFFFFB300), fontSize = 12.sp) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun StatCard(title: String, value: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(18.dp).width(180.dp)) {
            Text(title, color = Color(0xFFF5F5F5).copy(alpha = 0.65f), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
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
