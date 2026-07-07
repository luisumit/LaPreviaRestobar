package com.laprevia.restobar

import com.laprevia.restobar.data.local.entity.OrderEntity
import com.laprevia.restobar.data.local.entity.ProductEntity
import com.laprevia.restobar.data.mapper.toDomain
import com.laprevia.restobar.data.mapper.toEntity
import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperCoverageTest {

    @Test
    fun orderToEntityAndBackKeepsImportantFields() {
        val order = Order(
            id = "order-1",
            tableId = 2,
            tableNumber = 4,
            items = listOf(
                OrderItem(
                    productId = "prod-1",
                    productName = "Hamburguesa",
                    productPrice = 18.0,
                    productCategory = "Comida",
                    quantity = 2,
                    unitPrice = 18.0,
                    subtotal = 36.0
                )
            ),
            status = OrderStatus.LISTO,
            createdAt = 100L,
            updatedAt = 200L,
            total = 36.0,
            waiterId = "waiter-1",
            waiterName = "Luis",
            notes = "Sin cebolla"
        )

        val entity = order.toEntity()
        val domain = entity.toDomain()

        assertEquals("order-1", entity.id)
        assertEquals("PENDING", entity.syncStatus)
        assertEquals(OrderStatus.LISTO, domain.status)
        assertEquals(1, domain.items.size)
        assertEquals("Hamburguesa", domain.items.first().productName)
        assertEquals(36.0, domain.calculateTotal(), 0.0)
        assertTrue(domain.isValid())
    }

    @Test
    fun invalidOrderJsonReturnsEmptyItems() {
        val entity = OrderEntity(
            id = "order-bad-json",
            tableId = 1,
            tableNumber = 1,
            status = "PENDING",
            total = 0.0,
            createdAt = 10L,
            updatedAt = 20L,
            waiterId = null,
            waiterName = null,
            notes = null,
            itemsJson = "{bad-json"
        )

        val domain = entity.toDomain()

        assertEquals(OrderStatus.PENDING, domain.status)
        assertTrue(domain.items.isEmpty())
    }

    @Test
    fun inventoryMapperKeepsStockFields() {
        val inventory = Inventory(
            productId = "prod-2",
            productName = "Cerveza",
            currentStock = 8.0,
            unitOfMeasure = "und",
            minimumStock = 3.0,
            category = "Bebidas"
        )

        val entity = inventory.toEntity()
        val domain = entity.toDomain()

        assertEquals("prod-2", entity.productId)
        assertEquals("PENDING", entity.syncStatus)
        assertEquals("Cerveza", domain.productName)
        assertEquals(8.0, domain.currentStock, 0.0)
        assertEquals(3.0, domain.minimumStock, 0.0)
    }

    @Test
    fun productMapperKeepsPricesAndActiveState() {
        val product = Product(
            id = "prod-3",
            name = "Salchipapa",
            description = "Personal",
            category = "Comida",
            salePrice = 14.0,
            costPrice = 8.0,
            trackInventory = true,
            stock = 5.0,
            minStock = 2.0,
            isActive = false,
            updatedAt = 300L
        )

        val entity = product.toEntity()
        val domain = entity.toDomain()

        assertEquals("Salchipapa", entity.name)
        assertEquals("PENDING", entity.syncStatus)
        assertEquals(14.0, domain.salePrice ?: 0.0, 0.0)
        assertEquals(8.0, domain.costPrice ?: 0.0, 0.0)
        assertTrue(domain.trackInventory)
        assertFalse(domain.isActive)
    }

    @Test
    fun productEntityToDomainUsesLastModifiedAsCreatedAt() {
        val entity = ProductEntity(
            id = "prod-4",
            name = "Gaseosa",
            description = "500 ml",
            category = "Bebidas",
            salePrice = 5.0,
            costPrice = 3.0,
            trackInventory = true,
            stock = 20.0,
            minStock = 4.0,
            isActive = true,
            version = 9L,
            lastModified = 1234L,
            updatedAt = 5678L
        )

        val domain = entity.toDomain()

        assertEquals(1234L, domain.createdAt)
        assertEquals(5678L, domain.updatedAt)
        assertEquals(9L, domain.version)
    }

    @Test
    fun orderItemValidationAndProductConversionWork() {
        val item = OrderItem(
            productId = "prod-5",
            productName = "Alitas",
            productDescription = "Picantes",
            productPrice = 22.0,
            productCategory = "Comida",
            trackInventory = true,
            quantity = 1,
            unitPrice = 22.0,
            subtotal = 22.0
        )

        val product = item.toProduct()

        assertTrue(item.isValid())
        assertEquals("Alitas", product.name)
        assertEquals(22.0, product.salePrice ?: 0.0, 0.0)
        assertTrue(product.trackInventory)
    }
}
