package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.local.entity.CashClosureEntity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cierres de caja MULTIPLATAFORMA en Firebase (nodo `cash_closures`).
 * Permite cerrar caja desde la PC o el celular y ver los cierres desde ambos.
 * (El celular ademas conserva su copia local en Room, como siempre.)
 */
class GitLiveCashClosureRepository {

    private val closuresRef: DatabaseReference get() = Firebase.database.reference("cash_closures")

    /** Cierres ordenados del mas reciente al mas antiguo, en tiempo real. */
    fun getClosures(): Flow<List<CashClosureEntity>> =
        closuresRef.valueEvents.map { snapshot ->
            snapshot.children.mapNotNull { it.toClosure() }
                .sortedByDescending { it.createdAt }
        }

    suspend fun saveClosure(closure: CashClosureEntity) {
        closuresRef.child(closure.id).updateChildren(
            mapOf(
                "id" to closure.id,
                "periodStart" to closure.periodStart,
                "periodEnd" to closure.periodEnd,
                "totalSales" to closure.totalSales,
                "grossProfit" to closure.grossProfit,
                "chargedOrders" to closure.chargedOrders,
                "cancelledOrders" to closure.cancelledOrders,
                "productsSold" to closure.productsSold,
                "cashSales" to closure.cashSales,
                "yapePlinSales" to closure.yapePlinSales,
                "cardSales" to closure.cardSales,
                "bestSellingProduct" to closure.bestSellingProduct,
                "createdBy" to closure.createdBy,
                "createdAt" to closure.createdAt
            )
        )
    }

    private fun DataSnapshot.toClosure(): CashClosureEntity? = runCatching {
        val id = key ?: return null
        CashClosureEntity(
            id = id,
            periodStart = (child("periodStart").value as? Number)?.toLong() ?: 0L,
            periodEnd = (child("periodEnd").value as? Number)?.toLong() ?: 0L,
            totalSales = (child("totalSales").value as? Number)?.toDouble() ?: 0.0,
            grossProfit = (child("grossProfit").value as? Number)?.toDouble() ?: 0.0,
            chargedOrders = (child("chargedOrders").value as? Number)?.toInt() ?: 0,
            cancelledOrders = (child("cancelledOrders").value as? Number)?.toInt() ?: 0,
            productsSold = (child("productsSold").value as? Number)?.toInt() ?: 0,
            cashSales = (child("cashSales").value as? Number)?.toDouble() ?: 0.0,
            yapePlinSales = (child("yapePlinSales").value as? Number)?.toDouble() ?: 0.0,
            cardSales = (child("cardSales").value as? Number)?.toDouble() ?: 0.0,
            bestSellingProduct = child("bestSellingProduct").value as? String ?: "Sin ventas",
            createdBy = child("createdBy").value as? String ?: "",
            createdAt = (child("createdAt").value as? Number)?.toLong() ?: 0L
        )
    }.getOrNull()
}
