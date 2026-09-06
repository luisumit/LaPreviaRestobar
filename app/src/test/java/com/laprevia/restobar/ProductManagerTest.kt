package com.laprevia.restobar

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.ProductManager
import com.laprevia.restobar.repositories.FakeProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductManagerTest {

    @Test
    fun `add update and remove product delegates to repository`() = runTest {
        val repository = FakeProductRepository()
        val manager = ProductManager(repository)

        manager.addProduct(Product(id = "p1", name = "Hamburguesa", isActive = true))
        manager.updateProduct(Product(id = "p1", name = "Hamburguesa doble", isActive = true))
        val productsAfterUpdate = manager.products.first().toList()

        manager.removeProduct("p1")

        assertEquals(1, productsAfterUpdate.size)
        assertEquals("Hamburguesa doble", productsAfterUpdate.first().name)
        assertTrue(manager.products.first().isEmpty())
    }

    @Test
    fun `sellable products exposes only active products`() = runTest {
        val repository = FakeProductRepository()
        val manager = ProductManager(repository)

        manager.addProduct(Product(id = "p1", name = "Cerveza", isActive = true))
        manager.addProduct(Product(id = "p2", name = "Producto oculto", isActive = false))

        val sellableProducts = manager.sellableProducts.first()

        assertEquals(1, sellableProducts.size)
        assertEquals("Cerveza", sellableProducts.first().name)
    }
}
