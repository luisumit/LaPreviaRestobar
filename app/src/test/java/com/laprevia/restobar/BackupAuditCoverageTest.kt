package com.laprevia.restobar

import com.google.gson.JsonParser
import com.laprevia.restobar.data.local.entity.AppErrorLogEntity
import com.laprevia.restobar.data.local.entity.AuditLogEntity
import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.service.BackupExportFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupAuditCoverageTest {

    private val generatedAt = 1_783_200_000_000L

    private val products = listOf(
        Product(
            id = "p1",
            name = "Combo \"Criollo\"",
            description = "Plato\nespecial",
            category = "Comida",
            salePrice = 25.0,
            costPrice = 14.0,
            trackInventory = true,
            stock = 8.0,
            minStock = 3.0,
            isActive = true
        )
    )

    private val orders = listOf(
        Order(
            id = "o1",
            tableId = 2,
            tableNumber = 2,
            status = OrderStatus.COMPLETED,
            createdAt = generatedAt,
            waiterName = "Luis",
            notes = "Sin cebolla",
            items = listOf(
                OrderItem(
                    productId = "p1",
                    productName = "Combo \"Criollo\"",
                    quantity = 2,
                    unitPrice = 25.0,
                    subtotal = 50.0
                )
            )
        )
    )

    private val inventory = listOf(
        Inventory(
            productId = "p1",
            productName = "Combo \"Criollo\"",
            currentStock = 8.0,
            unitOfMeasure = "und",
            minimumStock = 3.0,
            category = "Comida"
        )
    )

    private val tables = listOf(
        Table(
            id = 2,
            number = 2,
            status = TableStatus.OCUPADA,
            currentOrderId = "o1",
            capacity = 4
        )
    )

    @Test
    fun `backup json incluye productos pedidos inventario y mesas restaurables`() {
        val json = BackupExportFormatter.backupJson(products, orders, inventory, tables, generatedAt)
        val root = JsonParser.parseString(json).asJsonObject

        assertTrue(root.has("generatedAt"))
        assertEquals(1, root.getAsJsonArray("products").size())
        assertEquals(1, root.getAsJsonArray("orders").size())
        assertEquals(1, root.getAsJsonArray("inventory").size())
        assertEquals(1, root.getAsJsonArray("tables").size())

        val product = root.getAsJsonArray("products")[0].asJsonObject
        val order = root.getAsJsonArray("orders")[0].asJsonObject
        val table = root.getAsJsonArray("tables")[0].asJsonObject

        assertEquals("Combo \"Criollo\"", product.get("name").asString)
        assertEquals("Plato\nespecial", product.get("description").asString)
        assertEquals(50.0, order.get("total").asDouble, 0.001)
        assertEquals("COMPLETED", order.get("status").asString)
        assertEquals("o1", table.get("currentOrderId").asString)
    }

    @Test
    fun `backup csv separa secciones principales y escapa comillas`() {
        val csv = BackupExportFormatter.backupCsv(products, orders, inventory, tables, generatedAt)

        assertTrue(csv.contains("[PRODUCTOS]"))
        assertTrue(csv.contains("[PEDIDOS]"))
        assertTrue(csv.contains("[INVENTARIO]"))
        assertTrue(csv.contains("[MESAS]"))
        assertTrue(csv.contains("\"Combo \"\"Criollo\"\"\""))
        assertTrue(csv.contains("OCUPADA"))
        assertFalse(csv.contains("null"))
    }

    @Test
    fun `auditoria csv conserva actor accion objetivo y detalle`() {
        val logs = listOf(
            AuditLogEntity(
                id = "audit-1",
                action = "PEDIDO_CANCELADO",
                actorRole = "MESERO",
                actorName = "Carlos",
                targetType = "ORDER",
                targetId = "o1",
                detail = "Mesa 2, motivo \"cliente cancelo\"",
                createdAt = generatedAt
            ),
            AuditLogEntity(
                id = "audit-2",
                action = "STOCK_ACTUALIZADO",
                actorRole = "ADMIN",
                actorName = "Administrador",
                targetType = "PRODUCT",
                targetId = "p1",
                detail = "Stock 8 -> 5",
                createdAt = generatedAt
            )
        )

        val csv = BackupExportFormatter.auditCsv(logs, generatedAt)

        assertTrue(csv.startsWith("AUDITORIA - LA PREVIA RESTOBAR"))
        assertTrue(csv.contains("\"MESERO\",\"Carlos\",\"PEDIDO_CANCELADO\",\"ORDER\",\"o1\""))
        assertTrue(csv.contains("\"ADMIN\",\"Administrador\",\"STOCK_ACTUALIZADO\",\"PRODUCT\",\"p1\""))
        assertTrue(csv.contains("\"Mesa 2, motivo \"\"cliente cancelo\"\"\""))
    }

    @Test
    fun `entidades de auditoria y errores guardan datos clave`() {
        val audit = AuditLogEntity(
            id = "audit-3",
            action = "CAJA_CERRADA",
            actorRole = "ADMIN",
            actorName = "Administrador",
            targetType = "CASH_CLOSURE",
            targetId = "closure-1",
            detail = "Total S/ 120.00",
            createdAt = generatedAt
        )
        val error = AppErrorLogEntity(
            id = "error-1",
            source = "Admin.restoreBackupFromJson",
            message = "Backup invalido",
            detail = "JsonParserException",
            createdAt = generatedAt
        )

        assertEquals("CAJA_CERRADA", audit.action)
        assertEquals("CASH_CLOSURE", audit.targetType)
        assertEquals("Total S/ 120.00", audit.detail)
        assertEquals("Admin.restoreBackupFromJson", error.source)
        assertEquals("Backup invalido", error.message)
        assertEquals("JsonParserException", error.detail)
    }
}
