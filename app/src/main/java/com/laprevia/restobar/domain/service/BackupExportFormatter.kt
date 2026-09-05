package com.laprevia.restobar.domain.service

import com.laprevia.restobar.data.local.entity.AuditLogEntity
import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.data.model.Table
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExportFormatter {

    fun auditCsv(logs: List<AuditLogEntity>, generatedAt: Long = System.currentTimeMillis()): String = buildString {
        appendLine("AUDITORIA - LA PREVIA RESTOBAR")
        appendLine("Generado,${formatDate(generatedAt)}")
        appendLine()
        appendLine("Fecha,Rol,Usuario,Accion,Tipo,ID,Detalle")
        logs.forEach { log ->
            appendLine(
                "${csv(formatDate(log.createdAt))},${csv(log.actorRole)},${csv(log.actorName)}," +
                    "${csv(log.action)},${csv(log.targetType)},${csv(log.targetId)},${csv(log.detail)}"
            )
        }
    }

    fun backupCsv(
        products: List<Product>,
        orders: List<Order>,
        inventory: List<Inventory>,
        tables: List<Table>,
        generatedAt: Long = System.currentTimeMillis()
    ): String = buildString {
        appendLine("BACKUP LA PREVIA RESTOBAR")
        appendLine("Generado,${formatDate(generatedAt)}")
        appendLine()
        appendLine("[PRODUCTOS]")
        appendLine("ID,Nombre,Categoria,Precio venta,Costo,Stock,Stock minimo,Activo")
        products.forEach { product ->
            appendLine(
                "${csv(product.id)},${csv(product.name)},${csv(product.category)}," +
                    "${product.salePrice ?: 0.0},${product.costPrice ?: 0.0},${product.stock},${product.minStock},${product.isActive}"
            )
        }
        appendLine()
        appendLine("[PEDIDOS]")
        appendLine("ID,Mesa,Estado,Total,Fecha,Mesero,Notas")
        orders.forEach { order ->
            appendLine(
                "${csv(order.id)},${order.tableNumber},${order.status.name},${SalesCalculator.orderTotal(order)}," +
                    "${csv(formatDate(order.createdAt))},${csv(order.waiterName.orEmpty())},${csv(order.notes.orEmpty())}"
            )
        }
        appendLine()
        appendLine("[INVENTARIO]")
        appendLine("Producto ID,Producto,Stock actual,Unidad,Stock minimo,Categoria")
        inventory.forEach { item ->
            appendLine(
                "${csv(item.productId)},${csv(item.productName)},${item.currentStock},${csv(item.unitOfMeasure)}," +
                    "${item.minimumStock},${csv(item.category.orEmpty())}"
            )
        }
        appendLine()
        appendLine("[MESAS]")
        appendLine("ID,Numero,Estado,Pedido actual,Capacidad")
        tables.forEach { table ->
            appendLine("${table.id},${table.number},${table.status.name},${csv(table.currentOrderId.orEmpty())},${table.capacity}")
        }
    }

    fun backupJson(
        products: List<Product>,
        orders: List<Order>,
        inventory: List<Inventory>,
        tables: List<Table>,
        generatedAt: Long = System.currentTimeMillis()
    ): String = buildString {
        appendLine("{")
        appendLine("  \"generatedAt\": \"${escapeJson(formatDate(generatedAt))}\",")
        appendLine("  \"products\": [")
        products.forEachIndexed { index, product ->
            append("    {")
            append("\"id\":\"${escapeJson(product.id)}\",")
            append("\"name\":\"${escapeJson(product.name)}\",")
            append("\"description\":\"${escapeJson(product.description)}\",")
            append("\"category\":\"${escapeJson(product.category)}\",")
            append("\"salePrice\":${product.salePrice ?: 0.0},")
            append("\"costPrice\":${product.costPrice ?: 0.0},")
            append("\"trackInventory\":${product.trackInventory},")
            append("\"stock\":${product.stock},")
            append("\"minStock\":${product.minStock},")
            append("\"isActive\":${product.isActive}")
            append("}")
            appendLine(if (index == products.lastIndex) "" else ",")
        }
        appendLine("  ],")
        appendLine("  \"orders\": [")
        orders.forEachIndexed { index, order ->
            append("    {")
            append("\"id\":\"${escapeJson(order.id)}\",")
            append("\"tableId\":${order.tableId},")
            append("\"tableNumber\":${order.tableNumber},")
            append("\"status\":\"${order.status.name}\",")
            append("\"total\":${SalesCalculator.orderTotal(order)},")
            append("\"createdAt\":${order.createdAt},")
            append("\"createdAtText\":\"${escapeJson(formatDate(order.createdAt))}\",")
            append("\"waiterName\":\"${escapeJson(order.waiterName.orEmpty())}\",")
            append("\"notes\":\"${escapeJson(order.notes.orEmpty())}\",")
            append("\"items\":[")
            order.items.forEachIndexed { itemIndex, item ->
                append("{")
                append("\"productId\":\"${escapeJson(item.productId)}\",")
                append("\"productName\":\"${escapeJson(item.productName)}\",")
                append("\"quantity\":${item.quantity},")
                append("\"unitPrice\":${item.unitPrice},")
                append("\"subtotal\":${item.subtotal}")
                append("}")
                if (itemIndex != order.items.lastIndex) append(",")
            }
            append("]}")
            appendLine(if (index == orders.lastIndex) "" else ",")
        }
        appendLine("  ],")
        appendLine("  \"inventory\": [")
        inventory.forEachIndexed { index, item ->
            append("    {")
            append("\"productId\":\"${escapeJson(item.productId)}\",")
            append("\"productName\":\"${escapeJson(item.productName)}\",")
            append("\"currentStock\":${item.currentStock},")
            append("\"unitOfMeasure\":\"${escapeJson(item.unitOfMeasure)}\",")
            append("\"minimumStock\":${item.minimumStock},")
            append("\"category\":\"${escapeJson(item.category.orEmpty())}\"")
            append("}")
            appendLine(if (index == inventory.lastIndex) "" else ",")
        }
        appendLine("  ],")
        appendLine("  \"tables\": [")
        tables.forEachIndexed { index, table ->
            append("    {")
            append("\"id\":${table.id},")
            append("\"number\":${table.number},")
            append("\"status\":\"${table.status.name}\",")
            append("\"currentOrderId\":\"${escapeJson(table.currentOrderId.orEmpty())}\",")
            append("\"capacity\":${table.capacity}")
            append("}")
            appendLine(if (index == tables.lastIndex) "" else ",")
        }
        appendLine("  ]")
        appendLine("}")
    }

    private fun formatDate(value: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
