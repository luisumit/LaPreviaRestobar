package com.laprevia.restobar

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.repositories.FakeProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRepositoryTest {

    @Test
    fun `getActiveProducts devuelve solo productos activos`() = runTest {
        val repository = FakeProductRepository()
        repository.createProduct(Product(id = "p1", name = "Lomo", isActive = true))
        repository.createProduct(Product(id = "p2", name = "Oculto", isActive = false))

        val activeProducts = repository.getActiveProducts().first()

        assertEquals(1, activeProducts.size)
        assertEquals("p1", activeProducts.first().id)
    }

    @Test
    fun `getProductsWithInventory devuelve solo productos con inventario`() = runTest {
        val repository = FakeProductRepository()
        repository.createProduct(Product(id = "p1", name = "Inca Kola", trackInventory = true))
        repository.createProduct(Product(id = "p2", name = "Servicio", trackInventory = false))

        val inventoryProducts = repository.getProductsWithInventory().first()

        assertEquals(1, inventoryProducts.size)
        assertEquals("Inca Kola", inventoryProducts.first().name)
    }

    @Test
    fun `updateProductStock actualiza stock del producto`() = runTest {
        val repository = FakeProductRepository()
        repository.createProduct(Product(id = "p1", name = "Cerveza", stock = 10.0))

        repository.updateProductStock("p1", 6.0)

        assertEquals(6.0, repository.getProductStock("p1"), 0.001)
    }

    @Test
    fun `searchProducts busca ignorando mayusculas`() = runTest {
        val repository = FakeProductRepository()
        repository.createProduct(Product(id = "p1", name = "Arroz chaufa"))
        repository.createProduct(Product(id = "p2", name = "Ceviche"))

        val result = repository.searchProducts("CHAUFA")

        assertEquals(1, result.size)
        assertEquals("p1", result.first().id)
    }

    @Test
    fun `productExists indica si el producto esta registrado`() = runTest {
        val repository = FakeProductRepository()
        repository.createProduct(Product(id = "p1", name = "Pollo broaster"))

        assertTrue(repository.productExists("p1"))
        assertFalse(repository.productExists("p2"))
    }
}
