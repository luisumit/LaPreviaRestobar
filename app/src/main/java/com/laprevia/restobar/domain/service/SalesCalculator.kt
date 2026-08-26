package com.laprevia.restobar.domain.service

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.model.DailySalesPoint
import com.laprevia.restobar.domain.model.ProductSalesPoint
import java.util.Calendar

/**
 * Domain Service (DDD): agregaciones PURAS para reportes y cierre de caja — total de un
 * pedido, totales por método de pago, ganancia bruta, top productos y ventas por hora.
 * Sin Android ni dependencias, para poder probarlas con casos concretos.
 */
object SalesCalculator {

    /** Total de un pedido: usa el total guardado, o suma los items como respaldo. */
    fun orderTotal(order: Order): Double =
        order.total.takeIf { it > 0.0 } ?: order.items.sumOf { item ->
            item.subtotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity)
        }

    /** Suma de lo cobrado con un método de pago. */
    fun paymentTotal(orders: List<Order>, method: PaymentMethod): Double =
        orders.filter { it.paymentMethod == method }.sumOf { orderTotal(it) }

    /** Ganancia bruta = venta - costo de cada item (costo desde el producto). */
    fun grossProfit(orders: List<Order>, products: List<Product>): Double {
        val productsById = products.associateBy { it.id }
        return orders.sumOf { order ->
            if (order.items.isEmpty()) {
                orderTotal(order)
            } else {
                order.items.sumOf { item ->
                    val sale = item.subtotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity)
                    val cost = (productsById[item.productId]?.costPrice ?: 0.0) * item.quantity
                    sale - cost
                }
            }
        }
    }

    /** Cantidad total de productos vendidos. */
    fun productsSold(orders: List<Order>): Int =
        orders.flatMap { it.items }.sumOf { it.quantity }

    /** Top productos por cantidad vendida. */
    fun topProducts(orders: List<Order>, limit: Int = 5): List<ProductSalesPoint> =
        orders.flatMap { it.items }
            .groupBy { it.productName.ifBlank { "Producto sin nombre" } }
            .map { (name, items) ->
                ProductSalesPoint(
                    name = name,
                    quantity = items.sumOf { it.quantity },
                    total = items.sumOf { item ->
                        item.subtotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity)
                    }
                )
            }
            .sortedByDescending { it.quantity }
            .take(limit)

    /** Ventas agrupadas por hora del día (para el gráfico de horas pico). */
    fun salesByHour(orders: List<Order>): List<DailySalesPoint> =
        orders.groupBy { hourOf(it.paidAt ?: it.createdAt) }
            .toSortedMap()
            .map { (hour, hourOrders) ->
                DailySalesPoint(label = "${hour}h", total = hourOrders.sumOf { orderTotal(it) })
            }

    private fun hourOf(timestamp: Long): Int =
        Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
}
