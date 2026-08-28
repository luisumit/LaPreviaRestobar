package com.laprevia.restobar.presentation.viewmodel

import android.content.Context
import android.content.ContentValues
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.laprevia.restobar.data.local.entity.AppErrorLogEntity
import com.laprevia.restobar.data.local.entity.AuditLogEntity
import com.laprevia.restobar.data.local.entity.CashClosureEntity
import com.laprevia.restobar.data.local.datastore.PreferencesManager
import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.domain.model.DailySalesPoint
import com.laprevia.restobar.domain.model.ProductSalesPoint
import com.laprevia.restobar.domain.service.SalesCalculator
import com.laprevia.restobar.data.local.sync.SyncManager
import com.laprevia.restobar.data.mapper.toDomain
import com.laprevia.restobar.data.mapper.toEntity
import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

fun startOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

fun endOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = startOfDay(timestamp)
        add(Calendar.DAY_OF_MONTH, 1)
    }
    return calendar.timeInMillis - 1
}

fun hourOfDay(timestamp: Long): Int {
    return Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
}

data class AdminUiState(
    val products: List<Product> = emptyList(),
    val orderHistory: List<Order> = emptyList(),
    val categories: List<String> = emptyList(),
    val tables: List<Table> = emptyList(),
    val dashboardMetrics: AdminDashboardMetrics = AdminDashboardMetrics(),
    val reportFilter: AdminReportFilter = AdminReportFilter.DAY,
    val customReportStart: Long = startOfDay(System.currentTimeMillis()),
    val customReportEnd: Long = endOfDay(System.currentTimeMillis()),
    val report: SalesReport = SalesReport(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val warning: String? = null,
    val selectedProduct: Product? = null,
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val cashClosures: List<CashClosureEntity> = emptyList(),
    val errorLogs: List<AppErrorLogEntity> = emptyList(),
    val whatsappNumber: String = "",
    val showRestoreDialog: Boolean = false,
    val restoreJson: String = "",
    val showProductForm: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isOffline: Boolean = false,
    val pendingSyncCount: Int = 0,
    val occupiedTables: Int = 0,
    val activeOrdersCount: Int = 0,
    val criticalStockCount: Int = 0
)

const val PUBLIC_MENU_URL = "https://laprevia-restobar.web.app"

enum class AdminReportFilter(val label: String) {
    DAY("Dia"),
    WEEK("Sem."),
    MONTH("Mes"),
    YEAR("Año"),
    CUSTOM("Rango")
}

data class AdminDashboardMetrics(
    val salesToday: Double = 0.0,
    val salesYear: Double = 0.0,
    val bestSellingProduct: String = "Sin ventas",
    val bestSellingQuantity: Int = 0,
    val activeProducts: Int = 0,
    val activeOrders: Int = 0,
    val lowStockProducts: Int = 0,
    val outOfStockProducts: Int = 0,
    val lowStockProductNames: List<String> = emptyList(),
    val outOfStockProductNames: List<String> = emptyList(),
    val occupiedTables: Int = 0,
    val totalTables: Int = 0
)

data class SalesReport(
    val title: String = "Ventas del dia",
    val totalSales: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalOrders: Int = 0,
    val chargedOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val productsSold: Int = 0,
    val cashSales: Double = 0.0,
    val yapePlinSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val bestSellingProduct: String = "Sin ventas",
    val bestSellingQuantity: Int = 0,
    val periodStart: Long = 0L,
    val periodEnd: Long = 0L,
    val dailySales: List<DailySalesPoint> = emptyList(),
    val topProducts: List<ProductSalesPoint> = emptyList(),
    val salesByHour: List<DailySalesPoint> = emptyList()
)

class AdminViewModel constructor(
    private val firebaseProductRepository: FirebaseProductRepository,
    private val firebaseOrderRepository: FirebaseOrderRepository,
    private val firebaseTableRepository: FirebaseTableRepository,
    private val db: AppDatabase,
    private val syncManager: SyncManager,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    private fun checkInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isInternetAvailable.value = true
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isOffline = false,
                        warning = "Internet disponible - Sincronizando..."
                    )
                    loadProductsFromFirebase()
                    syncPendingProducts()
                    kotlinx.coroutines.delay(2000)
                    _uiState.value = _uiState.value.copy(warning = null)
                    if (_uiState.value.pendingSyncCount == 0) {
                        showMessage("Sincronizacion completada", isSuccess = true)
                    }
                }
            }

            override fun onLost(network: Network) {
                _isInternetAvailable.value = false
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isOffline = true,
                        warning = "SIN INTERNET - Los cambios se guardaran localmente",
                        error = null,
                        success = null
                    )
                }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (_isInternetAvailable.value != hasInternet) {
                    _isInternetAvailable.value = hasInternet
                    if (hasInternet) {
                        viewModelScope.launch {
                            loadProductsFromFirebase()
                            syncPendingProducts()
                        }
                    }
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        _isInternetAvailable.value = checkInternet()
    }

    init {
        startNetworkMonitoring()

        viewModelScope.launch {
            loadProductsFromRoom()
            if (checkInternet()) {
                loadProductsFromFirebase()
                showMessage("Conectado - Los datos se sincronizan automaticamente", isSuccess = true)
            } else {
                showMessage("SIN INTERNET - Los cambios se guardaran localmente", isWarning = true)
            }
            checkPendingSync()
            listenToProductChanges()
            observeDashboardData()
            observeControlLogs()
        }
    }

    private fun observeControlLogs() {
        viewModelScope.launch {
            db.auditLogDao().getLatestFlow().collect { logs ->
                _uiState.value = _uiState.value.copy(auditLogs = logs)
            }
        }
        viewModelScope.launch {
            db.cashClosureDao().getLatestFlow().collect { closures ->
                _uiState.value = _uiState.value.copy(cashClosures = closures)
            }
        }
        viewModelScope.launch {
            db.appErrorLogDao().getLatestFlow().collect { errors ->
                _uiState.value = _uiState.value.copy(errorLogs = errors)
            }
        }
        viewModelScope.launch {
            preferencesManager.whatsappNumber.collect { number ->
                _uiState.value = _uiState.value.copy(whatsappNumber = number)
            }
        }
    }

    fun saveWhatsappNumber(number: String) {
        viewModelScope.launch { preferencesManager.saveWhatsappNumber(number.trim()) }
    }

    /** Arma el resumen del dia/periodo para enviar por WhatsApp. */
    fun buildDailySummaryText(): String {
        val report = _uiState.value.report
        val top = report.topProducts.take(3)
            .mapIndexed { i, p -> "${i + 1}. ${p.name} (${p.quantity})" }
            .joinToString("\n")
            .ifBlank { "Sin ventas" }
        return buildString {
            appendLine("*LA PREVIA RESTOBAR*")
            appendLine("Resumen: ${report.title}")
            appendLine("${reportPeriodTitle(report)}")
            appendLine("")
            appendLine("Total vendido: S/ ${money(report.totalSales)}")
            appendLine("Ganancia estimada: S/ ${money(report.grossProfit)}")
            appendLine("Pedidos cobrados: ${report.chargedOrders}")
            appendLine("Pedidos cancelados: ${report.cancelledOrders}")
            appendLine("Productos vendidos: ${report.productsSold}")
            appendLine("")
            appendLine("*Metodos de pago:*")
            appendLine("Efectivo: S/ ${money(report.cashSales)}")
            appendLine("Yape/Plin: S/ ${money(report.yapePlinSales)}")
            appendLine("Tarjeta: S/ ${money(report.cardSales)}")
            appendLine("")
            appendLine("*Top productos:*")
            appendLine(top)
        }
    }

    private fun listenToProductChanges() {
        viewModelScope.launch {
            try {
                firebaseProductRepository.listenToProductChanges().collect { updatedProduct ->
                    val existing = db.productDao().getById(updatedProduct.id)
                    if (existing != null && existing.stock != updatedProduct.stock) {
                        db.productDao().insert(updatedProduct.toEntity().copy(syncStatus = "SYNCED"))
                        loadProductsFromRoom()
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.d("Admin: Error escuchando productos: ${e.message}")
            }
        }
    }

    private fun observeDashboardData() {
        viewModelScope.launch {
            combine(
                firebaseTableRepository.getTables(),
                firebaseOrderRepository.getOrdersRealTime()
            ) { tables, orders ->
                val activeOrders = normalizeActiveOrders(orders)
                normalizeTables(tables, activeOrders) to activeOrders
            }.collect { (tables, activeOrders) ->
                activeOrders.forEach { order ->
                    db.orderDao().insert(order.toEntity().copy(syncStatus = "SYNCED"))
                }
                val allOrders = loadOrdersSafely().mergeById(activeOrders)
                val dashboardMetrics = buildDashboardMetrics(_uiState.value.products, allOrders, tables)
                val report = buildSalesReport(
                    orders = allOrders,
                    filter = _uiState.value.reportFilter,
                    products = _uiState.value.products,
                    customStart = _uiState.value.customReportStart,
                    customEnd = _uiState.value.customReportEnd
                )

                _uiState.value = _uiState.value.copy(
                    tables = tables,
                    dashboardMetrics = dashboardMetrics,
                    report = report,
                    orderHistory = buildOrderHistory(allOrders, report.periodStart, report.periodEnd),
                    occupiedTables = dashboardMetrics.occupiedTables,
                    activeOrdersCount = dashboardMetrics.activeOrders
                )
            }
        }

        viewModelScope.launch {
            db.orderDao().getAllFlow().collect { orders ->
                val activeStatuses = listOf("PENDING", "ENVIADO", "ACEPTADO", "EN_PREPARACION", "LISTO", "ENTREGADO")
                val activeCount = orders.count { it.status in activeStatuses }
                val criticalStock = _uiState.value.products.count { it.trackInventory && it.stock <= it.minStock }

                _uiState.value = _uiState.value.copy(
                    activeOrdersCount = activeCount,
                    criticalStockCount = criticalStock
                )
            }
        }
    }

    private fun showMessage(message: String, isError: Boolean = false, isSuccess: Boolean = false, isWarning: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            error = if (isError) message else null,
            success = if (isSuccess) message else null,
            warning = if (isWarning) message else null
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            clearError()
            clearSuccess()
            clearWarning()
        }
    }

    private suspend fun loadProductsFromRoom() {
        try {
            val uniqueProducts = db.productDao().getAll()
                .map { it.toDomain() }
                .distinctBy { it.id }
                .sortedBy { it.name }
            val orders = loadOrdersSafely()
            val tables = loadTablesSafely()
            val report = buildSalesReport(
                orders = orders,
                filter = _uiState.value.reportFilter,
                products = uniqueProducts,
                customStart = _uiState.value.customReportStart,
                customEnd = _uiState.value.customReportEnd
            )

            _uiState.value = _uiState.value.copy(
                products = uniqueProducts,
                categories = uniqueProducts.mapNotNull { it.category }.distinct().sorted(),
                tables = tables,
                dashboardMetrics = buildDashboardMetrics(uniqueProducts, orders, tables),
                report = report,
                orderHistory = buildOrderHistory(orders, report.periodStart, report.periodEnd),
                isLoading = false,
                isOffline = !_isInternetAvailable.value
            )
        } catch (e: Exception) {
            timber.log.Timber.d("Admin: Error cargando desde Room: ${e.message}")
        }
    }

    private fun loadProductsFromFirebase() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                firebaseProductRepository.getProductsRealTime().collect { firebaseProducts ->
                    val remoteIds = firebaseProducts.map { it.id }
                    if (remoteIds.isNotEmpty()) {
                        db.productDao().deleteSyncedProductsNotIn(remoteIds)
                    }

                    firebaseProducts.forEach { product ->
                        val existing = db.productDao().getById(product.id)
                        if (existing == null || existing.stock != product.stock) {
                            db.productDao().insert(product.toEntity().copy(syncStatus = "SYNCED"))
                        }
                    }

                    val pendingProducts = db.productDao().getPending().map { it.toDomain() }
                    val pendingIds = pendingProducts.map { it.id }.toSet()
                    val syncedProducts = firebaseProducts.filter { it.id !in pendingIds }
                    val uniqueProducts = (syncedProducts + pendingProducts).distinctBy { it.id }.sortedBy { it.name }
                    val orders = loadOrdersSafely()
                    val tables = loadTablesSafely()
                    val report = buildSalesReport(
                        orders = orders,
                        filter = _uiState.value.reportFilter,
                        products = uniqueProducts,
                        customStart = _uiState.value.customReportStart,
                        customEnd = _uiState.value.customReportEnd
                    )

                    _uiState.value = _uiState.value.copy(
                        products = uniqueProducts,
                        categories = uniqueProducts.mapNotNull { it.category }.distinct().sorted(),
                        tables = tables,
                        dashboardMetrics = buildDashboardMetrics(uniqueProducts, orders, tables),
                        report = report,
                        orderHistory = buildOrderHistory(orders, report.periodStart, report.periodEnd),
                        isLoading = false,
                        isOffline = !_isInternetAvailable.value,
                        pendingSyncCount = pendingProducts.size
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, isOffline = true)
                showMessage("Error de conexion: ${e.message}", isError = true)
            }
        }
    }

    fun selectReportFilter(filter: AdminReportFilter) {
        viewModelScope.launch {
            val report = buildSalesReport(
                orders = loadOrdersSafely(),
                filter = filter,
                products = _uiState.value.products,
                customStart = _uiState.value.customReportStart,
                customEnd = _uiState.value.customReportEnd
            )
            _uiState.value = _uiState.value.copy(
                reportFilter = filter,
                report = report,
                orderHistory = buildOrderHistory(loadOrdersSafely(), report.periodStart, report.periodEnd)
            )
        }
    }

    fun selectCustomReportRange(start: Long, end: Long) {
        viewModelScope.launch {
            val normalizedStart = startOfDay(start)
            val normalizedEnd = endOfDay(end)
            val report = buildSalesReport(
                orders = loadOrdersSafely(),
                filter = AdminReportFilter.CUSTOM,
                products = _uiState.value.products,
                customStart = normalizedStart,
                customEnd = normalizedEnd
            )
            _uiState.value = _uiState.value.copy(
                reportFilter = AdminReportFilter.CUSTOM,
                customReportStart = normalizedStart,
                customReportEnd = normalizedEnd,
                report = report,
                orderHistory = buildOrderHistory(loadOrdersSafely(), report.periodStart, report.periodEnd)
            )
        }
    }

    fun exportReportToPdf() {
        viewModelScope.launch {
            try {
                val fileName = createPdfReport(_uiState.value.report)
                showMessage("PDF guardado en Descargas/LaPreviaReportes: $fileName", isSuccess = true)
            } catch (e: Exception) {
                showMessage("Error exportando PDF: ${e.message}", isError = true)
            }
        }
    }

    fun exportReportToExcel() {
        viewModelScope.launch {
            try {
                val fileName = createCsvReport(_uiState.value.report)
                showMessage("Excel guardado en Descargas/LaPreviaReportes: $fileName", isSuccess = true)
            } catch (e: Exception) {
                showMessage("Error exportando Excel: ${e.message}", isError = true)
            }
        }
    }

    fun exportBackupToJson() {
        viewModelScope.launch {
            try {
                val fileName = createJsonBackup(
                    products = _uiState.value.products,
                    orders = loadOrdersSafely(),
                    inventory = loadInventorySafely(),
                    tables = loadTablesSafely()
                )
                showMessage("Backup JSON guardado en Descargas/LaPreviaReportes: $fileName", isSuccess = true)
            } catch (e: Exception) {
                showMessage("Error exportando backup JSON: ${e.message}", isError = true)
            }
        }
    }

    fun exportBackupToExcel() {
        viewModelScope.launch {
            try {
                val fileName = createBackupCsv(
                    products = _uiState.value.products,
                    orders = loadOrdersSafely(),
                    inventory = loadInventorySafely(),
                    tables = loadTablesSafely()
                )
                showMessage("Backup Excel guardado en Descargas/LaPreviaReportes: $fileName", isSuccess = true)
            } catch (e: Exception) {
                showMessage("Error exportando backup Excel: ${e.message}", isError = true)
            }
        }
    }

    fun exportAuditToExcel() {
        viewModelScope.launch {
            try {
                val fileName = createAuditCsv(db.auditLogDao().getAll())
                showMessage("Auditoria guardada en Descargas/LaPreviaReportes: $fileName", isSuccess = true)
            } catch (e: Exception) {
                logError("Admin.exportAuditToExcel", e.message.orEmpty(), e.stackTraceToString())
                showMessage("Error exportando auditoria: ${e.message}", isError = true)
            }
        }
    }

    fun closeCashRegister() {
        viewModelScope.launch {
            try {
                val report = _uiState.value.report
                val closure = CashClosureEntity(
                    id = UUID.randomUUID().toString(),
                    periodStart = report.periodStart,
                    periodEnd = report.periodEnd,
                    totalSales = report.totalSales,
                    grossProfit = report.grossProfit,
                    chargedOrders = report.chargedOrders,
                    cancelledOrders = report.cancelledOrders,
                    productsSold = report.productsSold,
                    cashSales = report.cashSales,
                    yapePlinSales = report.yapePlinSales,
                    cardSales = report.cardSales,
                    bestSellingProduct = "${report.bestSellingProduct} (${report.bestSellingQuantity})",
                    createdBy = "Administrador"
                )
                db.cashClosureDao().insert(closure)
                // Ademas del guardado local, sube el cierre a Firebase para que
                // tambien se vea desde el panel de escritorio (fire-and-forget).
                launch {
                    runCatching {
                        com.laprevia.restobar.data.repository.GitLiveCashClosureRepository()
                            .saveClosure(closure)
                    }
                }
                logAudit(
                    action = "CIERRE_CAJA",
                    targetType = "REPORT",
                    targetId = closure.id,
                    detail = "Total S/ ${money(report.totalSales)}, ganancia S/ ${money(report.grossProfit)}"
                )
                showMessage("Cierre de caja guardado", isSuccess = true)
            } catch (e: Exception) {
                logError("Admin.closeCashRegister", e.message.orEmpty(), e.stackTraceToString())
                showMessage("Error cerrando caja: ${e.message}", isError = true)
            }
        }
    }

    fun showRestoreBackupDialog() {
        _uiState.value = _uiState.value.copy(showRestoreDialog = true, restoreJson = "")
    }

    fun hideRestoreBackupDialog() {
        _uiState.value = _uiState.value.copy(showRestoreDialog = false, restoreJson = "")
    }

    fun updateRestoreJson(value: String) {
        _uiState.value = _uiState.value.copy(restoreJson = value)
    }

    fun restoreBackupFromJson() {
        viewModelScope.launch {
            try {
                val json = _uiState.value.restoreJson.trim()
                if (json.isBlank()) {
                    showMessage("Pega el contenido JSON del backup", isError = true)
                    return@launch
                }
                val root = JsonParser.parseString(json).asJsonObject
                val gson = Gson()
                val productType = object : TypeToken<List<Product>>() {}.type
                val orderType = object : TypeToken<List<Order>>() {}.type
                val products: List<Product> = gson.fromJson(root.getAsJsonArray("products"), productType) ?: emptyList()
                val orders: List<Order> = gson.fromJson(root.getAsJsonArray("orders"), orderType) ?: emptyList()

                products.forEach { db.productDao().insert(it.toEntity().copy(syncStatus = "PENDING")) }
                orders.forEach { db.orderDao().insert(it.toEntity().copy(syncStatus = "PENDING")) }
                logAudit(
                    action = "BACKUP_RESTAURADO",
                    targetType = "BACKUP",
                    targetId = "JSON",
                    detail = "Productos ${products.size}, pedidos ${orders.size}"
                )
                hideRestoreBackupDialog()
                refreshProducts()
                showMessage("Backup restaurado localmente. Sincroniza para subirlo a Firebase.", isSuccess = true)
            } catch (e: Exception) {
                logError("Admin.restoreBackupFromJson", e.message.orEmpty(), e.stackTraceToString())
                showMessage("Backup invalido: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun logAudit(action: String, targetType: String, targetId: String, detail: String) {
        db.auditLogDao().insert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = action,
                actorRole = "ADMIN",
                actorName = "Administrador",
                targetType = targetType,
                targetId = targetId,
                detail = detail
            )
        )
    }

    private suspend fun logError(source: String, message: String, detail: String) {
        db.appErrorLogDao().insert(
            AppErrorLogEntity(
                id = UUID.randomUUID().toString(),
                source = source,
                message = message,
                detail = detail
            )
        )
        // Ademas del log local, lo reporta a Crashlytics para verlo remotamente.
        runCatching {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                setCustomKey("source", source)
                log("$source: $message")
                recordException(Exception("$source: $message | $detail"))
            }
        }
    }

    private suspend fun loadOrdersSafely(): List<Order> {
        return db.orderDao().getAll().mapNotNull { entity ->
            runCatching { entity.toDomain() }.getOrNull()
        }
    }

    private suspend fun loadTablesSafely(): List<Table> {
        val tables = runCatching {
            firebaseTableRepository.getTables().first()
        }.getOrElse {
            db.tableDao().getAll().mapNotNull { entity ->
                runCatching { entity.toDomain() }.getOrNull()
            }
        }
        return normalizeTables(tables, normalizeActiveOrders(loadOrdersSafely()))
    }

    private suspend fun loadInventorySafely(): List<Inventory> {
        return db.inventoryDao().getAll().map { it.toDomain() }
    }

    private fun buildDashboardMetrics(products: List<Product>, orders: List<Order>, tables: List<Table>): AdminDashboardMetrics {
        val todayReport = buildSalesReport(orders, AdminReportFilter.DAY, products)
        val yearReport = buildSalesReport(orders, AdminReportFilter.YEAR, products)
        val activeOrders = normalizeActiveOrders(orders)
        val topProductToday = topProductFromOpenSales(orders)
        val validTables = tables.filter { it.id in 1..8 && it.number in 1..8 }
        val lowStockProducts = products
            .filter { it.trackInventory && it.stock > 0.0 && it.stock <= it.minStock }
            .sortedBy { it.name }
        val outOfStockProducts = products
            .filter { it.trackInventory && it.stock <= 0.0 }
            .sortedBy { it.name }

        return AdminDashboardMetrics(
            salesToday = todayReport.totalSales,
            salesYear = yearReport.totalSales,
            bestSellingProduct = topProductToday?.first ?: "Sin ventas",
            bestSellingQuantity = topProductToday?.second ?: 0,
            activeProducts = products.count { it.isActive },
            activeOrders = activeOrders.size,
            lowStockProducts = lowStockProducts.size,
            outOfStockProducts = outOfStockProducts.size,
            lowStockProductNames = lowStockProducts.map { "${it.name} (${formatStock(it.stock)})" },
            outOfStockProductNames = outOfStockProducts.map { it.name },
            occupiedTables = activeOrders.map { it.tableId }.distinct().count(),
            totalTables = validTables.size
        )
    }

    private fun buildSalesReport(
        orders: List<Order>,
        filter: AdminReportFilter,
        products: List<Product>,
        customStart: Long = startOfDay(System.currentTimeMillis()),
        customEnd: Long = endOfDay(System.currentTimeMillis())
    ): SalesReport {
        val (start, end) = periodBounds(filter, customStart, customEnd)
        val filteredOrders = orders.filter { it.createdAt in start..end }
        val chargedOrders = filteredOrders.filter { it.status == OrderStatus.COMPLETED }
        val soldItems = chargedOrders.flatMap { it.items }
        val totalSales = chargedOrders.sumOf { SalesCalculator.orderTotal(it) }
        val cashSales = SalesCalculator.paymentTotal(chargedOrders, PaymentMethod.CASH)
        val yapePlinSales = SalesCalculator.paymentTotal(chargedOrders, PaymentMethod.YAPE_PLIN)
        val cardSales = SalesCalculator.paymentTotal(chargedOrders, PaymentMethod.CARD)
        val grossProfit = SalesCalculator.grossProfit(chargedOrders, products)
        val bestSeller = soldItems
            .groupBy { it.productName.ifBlank { "Producto sin nombre" } }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .maxByOrNull { it.value }

        val topProducts = SalesCalculator.topProducts(chargedOrders, 5)

        val dailySales = buildSalesSeries(chargedOrders, start, end, filter)

        val salesByHour = SalesCalculator.salesByHour(chargedOrders)

        return SalesReport(
            title = when (filter) {
                AdminReportFilter.DAY -> "Ventas del dia"
                AdminReportFilter.WEEK -> "Ventas de la semana"
                AdminReportFilter.MONTH -> "Ventas del mes"
                AdminReportFilter.YEAR -> "Ventas del año"
                AdminReportFilter.CUSTOM -> "Ventas por rango"
            },
            totalSales = totalSales,
            grossProfit = grossProfit,
            totalOrders = filteredOrders.size,
            chargedOrders = chargedOrders.size,
            cancelledOrders = filteredOrders.count { it.status == OrderStatus.CANCELLED },
            productsSold = soldItems.sumOf { it.quantity },
            cashSales = cashSales,
            yapePlinSales = yapePlinSales,
            cardSales = cardSales,
            bestSellingProduct = bestSeller?.key ?: "Sin ventas",
            bestSellingQuantity = bestSeller?.value ?: 0,
            periodStart = start,
            periodEnd = end,
            dailySales = dailySales,
            topProducts = topProducts,
            salesByHour = salesByHour
        )
    }

    /**
     * Agrupa las ventas cobradas del periodo en "buckets" temporales para dibujar
     * la tendencia: bloques de 2h en el dia, dias en semana/mes/rango y meses en el año.
     */
    private fun buildSalesSeries(
        chargedOrders: List<Order>,
        start: Long,
        end: Long,
        filter: AdminReportFilter
    ): List<DailySalesPoint> {
        val monthsShort = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val points = mutableListOf<DailySalesPoint>()
        val cal = Calendar.getInstance()

        fun sumBetween(from: Long, to: Long): Double =
            chargedOrders.filter { it.createdAt in from..to }.sumOf { orderTotal(it) }

        when (filter) {
            AdminReportFilter.DAY -> {
                var hour = 0
                while (hour < 24) {
                    cal.timeInMillis = start
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val from = cal.timeInMillis
                    val to = from + 2 * 60 * 60 * 1000 - 1
                    points.add(DailySalesPoint("${hour}h", sumBetween(from, minOf(to, end))))
                    hour += 2
                }
            }
            AdminReportFilter.YEAR -> {
                for (month in 0 until 12) {
                    cal.timeInMillis = start
                    cal.set(Calendar.MONTH, month)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val from = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    val to = cal.timeInMillis - 1
                    points.add(DailySalesPoint(monthsShort[month], sumBetween(from, minOf(to, end))))
                }
            }
            else -> {
                var dayStart = startOfDay(start)
                var guard = 0
                while (dayStart <= end && guard < 62) {
                    val dayEnd = endOfDay(dayStart)
                    cal.timeInMillis = dayStart
                    points.add(DailySalesPoint("${cal.get(Calendar.DAY_OF_MONTH)}", sumBetween(dayStart, minOf(dayEnd, end))))
                    cal.timeInMillis = dayStart
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    dayStart = cal.timeInMillis
                    guard++
                }
            }
        }
        return points
    }

    private fun buildOrderHistory(orders: List<Order>, start: Long, end: Long): List<Order> {
        return orders
            .filter { it.createdAt in start..end }
            .filter { it.status == OrderStatus.COMPLETED || it.status == OrderStatus.CANCELLED }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
    }

    private fun periodBounds(filter: AdminReportFilter, customStart: Long, customEnd: Long): Pair<Long, Long> {
        if (filter == AdminReportFilter.CUSTOM) {
            return minOf(customStart, customEnd) to maxOf(customStart, customEnd)
        }

        val calendar = Calendar.getInstance()
        when (filter) {
            AdminReportFilter.DAY -> Unit
            AdminReportFilter.WEEK -> calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            AdminReportFilter.MONTH -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            AdminReportFilter.YEAR -> calendar.set(Calendar.DAY_OF_YEAR, 1)
            AdminReportFilter.CUSTOM -> Unit
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        when (filter) {
            AdminReportFilter.DAY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            AdminReportFilter.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            AdminReportFilter.MONTH -> calendar.add(Calendar.MONTH, 1)
            AdminReportFilter.YEAR -> calendar.add(Calendar.YEAR, 1)
            AdminReportFilter.CUSTOM -> Unit
        }
        return start to (calendar.timeInMillis - 1)
    }

    private fun orderTotal(order: Order): Double {
        return order.total.takeIf { it > 0.0 } ?: order.items.sumOf { item ->
            item.subtotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity)
        }
    }

    private fun calculateGrossProfit(orders: List<Order>, products: List<Product>): Double {
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

    private fun formatStock(stock: Double): String {
        return if (stock % 1.0 == 0.0) stock.toInt().toString() else "%.2f".format(Locale.US, stock)
    }

    private fun normalizeActiveOrders(orders: List<Order>): List<Order> {
        val activeStatuses = setOf(
            OrderStatus.PENDING,
            OrderStatus.ENVIADO,
            OrderStatus.ACEPTADO,
            OrderStatus.EN_PREPARACION,
            OrderStatus.LISTO,
            OrderStatus.ENTREGADO
        )
        return orders
            .map { order ->
                if (order.tableId == 0 && order.tableNumber in 1..8) {
                    order.copy(tableId = order.tableNumber)
                } else {
                    order
                }
            }
            .filter { it.status in activeStatuses && it.tableId in 1..8 }
            .distinctBy { it.id }
    }

    private fun normalizeTables(tables: List<Table>, activeOrders: List<Order>): List<Table> {
        val activeByTable = activeOrders.associateBy { it.tableId }
        return tables
            .filter { it.id in 1..8 && it.number in 1..8 }
            .distinctBy { it.id }
            .sortedBy { it.number }
            .map { table ->
                val activeOrder = activeByTable[table.id]
                when {
                    activeOrder != null -> table.copy(status = TableStatus.OCUPADA, currentOrderId = activeOrder.id)
                    table.status == TableStatus.OCUPADA -> table.copy(status = TableStatus.LIBRE, currentOrderId = null)
                    else -> table
                }
            }
    }

    private fun topProductFromOpenSales(orders: List<Order>): Pair<String, Int>? {
        val (start, end) = periodBounds(
            AdminReportFilter.DAY,
            startOfDay(System.currentTimeMillis()),
            endOfDay(System.currentTimeMillis())
        )
        return orders
            .filter { it.createdAt in start..end && it.status != OrderStatus.CANCELLED }
            .flatMap { it.items }
            .groupBy { it.productName.ifBlank { "Producto sin nombre" } }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }
    }

    private fun List<Order>.mergeById(other: List<Order>): List<Order> = (this + other).distinctBy { it.id }

    private fun reportsDirectory(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LaPreviaReportes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun createCsvReport(report: SalesReport): String {
        val fileName = "reporte_${timestamp()}.csv"
        val periodTitle = reportPeriodTitle(report)
        val csv = buildString {
            appendLine("REPORTE DE VENTAS - LA PREVIA")
            appendLine("Titulo,${report.title}")
            appendLine(periodTitle)
            appendLine("Total vendido,S/ ${money(report.totalSales)}")
            appendLine("Ganancia estimada,S/ ${money(report.grossProfit)}")
            appendLine("Cantidad de pedidos,${report.totalOrders}")
            appendLine("Pedidos cobrados,${report.chargedOrders}")
            appendLine("Pedidos cancelados,${report.cancelledOrders}")
            appendLine("Productos vendidos,${report.productsSold}")
            appendLine("Ventas efectivo,S/ ${money(report.cashSales)}")
            appendLine("Ventas Yape/Plin,S/ ${money(report.yapePlinSales)}")
            appendLine("Ventas tarjeta,S/ ${money(report.cardSales)}")
            appendLine("Producto mas vendido,${report.bestSellingProduct} (${report.bestSellingQuantity})")
        }
        writeReportFile(
            fileName = fileName,
            mimeType = "text/csv",
            bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csv.toByteArray()
        )
        return fileName
    }

    private fun createAuditCsv(logs: List<AuditLogEntity>): String {
        val fileName = "auditoria_${timestamp()}.csv"
        val csv = buildString {
            appendLine("AUDITORIA - LA PREVIA RESTOBAR")
            appendLine("Generado,${formatDate(System.currentTimeMillis())}")
            appendLine()
            appendLine("Fecha,Rol,Usuario,Accion,Tipo,ID,Detalle")
            logs.forEach { log ->
                appendLine("${csv(formatDate(log.createdAt))},${csv(log.actorRole)},${csv(log.actorName)},${csv(log.action)},${csv(log.targetType)},${csv(log.targetId)},${csv(log.detail)}")
            }
        }
        writeReportFile(
            fileName = fileName,
            mimeType = "text/csv",
            bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csv.toByteArray()
        )
        return fileName
    }

    private fun createPdfReport(report: SalesReport): String {
        val fileName = "reporte_${timestamp()}.pdf"
        val document = PdfDocument()
        val periodTitle = reportPeriodTitle(report)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            color = AndroidColor.rgb(26, 26, 46)
            textSize = 24f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = AndroidColor.rgb(30, 30, 30)
            textSize = 15f
        }

        var y = 72f
        canvas.drawText("LA PREVIA RESTOBAR", 48f, y, titlePaint)
        y += 34f
        canvas.drawText(report.title, 48f, y, textPaint)
        y += 26f
        canvas.drawText(periodTitle, 48f, y, textPaint)
        y += 42f

        listOf(
            "Total vendido: S/ ${money(report.totalSales)}",
            "Ganancia estimada: S/ ${money(report.grossProfit)}",
            "Cantidad de pedidos: ${report.totalOrders}",
            "Pedidos cobrados: ${report.chargedOrders}",
            "Pedidos cancelados: ${report.cancelledOrders}",
            "Productos vendidos: ${report.productsSold}",
            "Producto mas vendido: ${report.bestSellingProduct} (${report.bestSellingQuantity})"
        ).forEach { line ->
            canvas.drawText(line, 48f, y, textPaint)
            y += 28f
        }

        document.finishPage(page)
        writeReportFile(fileName = fileName, mimeType = "application/pdf") { output ->
            document.writeTo(output)
        }
        document.close()
        return fileName
    }

    private fun createJsonBackup(
        products: List<Product>,
        orders: List<Order>,
        inventory: List<Inventory>,
        tables: List<Table>
    ): String {
        val fileName = "backup_laprevia_${timestamp()}.json"
        val json = buildString {
            appendLine("{")
            appendLine("  \"generatedAt\": \"${escapeJson(formatDate(System.currentTimeMillis()))}\",")
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
                append("\"total\":${orderTotal(order)},")
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
        writeReportFile(fileName, "application/json", json.toByteArray())
        return fileName
    }

    private fun createBackupCsv(
        products: List<Product>,
        orders: List<Order>,
        inventory: List<Inventory>,
        tables: List<Table>
    ): String {
        val fileName = "backup_laprevia_${timestamp()}.csv"
        val csv = buildString {
            appendLine("BACKUP LA PREVIA RESTOBAR")
            appendLine("Generado,${formatDate(System.currentTimeMillis())}")
            appendLine()
            appendLine("[PRODUCTOS]")
            appendLine("ID,Nombre,Categoria,Precio venta,Costo,Stock,Stock minimo,Activo")
            products.forEach { product ->
                appendLine("${csv(product.id)},${csv(product.name)},${csv(product.category)},${product.salePrice ?: 0.0},${product.costPrice ?: 0.0},${product.stock},${product.minStock},${product.isActive}")
            }
            appendLine()
            appendLine("[PEDIDOS]")
            appendLine("ID,Mesa,Estado,Total,Fecha,Mesero,Notas")
            orders.forEach { order ->
                appendLine("${csv(order.id)},${order.tableNumber},${order.status.name},${orderTotal(order)},${csv(formatDate(order.createdAt))},${csv(order.waiterName.orEmpty())},${csv(order.notes.orEmpty())}")
            }
            appendLine()
            appendLine("[INVENTARIO]")
            appendLine("Producto ID,Producto,Stock actual,Unidad,Stock minimo,Categoria")
            inventory.forEach { item ->
                appendLine("${csv(item.productId)},${csv(item.productName)},${item.currentStock},${csv(item.unitOfMeasure)},${item.minimumStock},${csv(item.category.orEmpty())}")
            }
            appendLine()
            appendLine("[MESAS]")
            appendLine("ID,Numero,Estado,Pedido actual,Capacidad")
            tables.forEach { table ->
                appendLine("${table.id},${table.number},${table.status.name},${csv(table.currentOrderId.orEmpty())},${table.capacity}")
            }
        }
        writeReportFile(
            fileName = fileName,
            mimeType = "text/csv",
            bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csv.toByteArray()
        )
        return fileName
    }

    private fun writeReportFile(fileName: String, mimeType: String, bytes: ByteArray) {
        writeReportFile(fileName, mimeType) { output -> output.write(bytes) }
    }

    private fun writeReportFile(fileName: String, mimeType: String, write: (java.io.OutputStream) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/LaPreviaReportes")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("No se pudo crear el archivo en Descargas")
            resolver.openOutputStream(uri)?.use(write)
                ?: error("No se pudo abrir el archivo en Descargas")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            val file = File(reportsDirectory(), fileName)
            FileOutputStream(file).use(write)
        }
    }

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    private fun formatDate(value: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(value)
    private fun formatDateOnly(value: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(value)
    private fun reportPeriodTitle(report: SalesReport): String = "Reporte del ${formatDateOnly(report.periodStart)} al ${formatDateOnly(report.periodEnd)}"
    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun checkPendingSync() {
        viewModelScope.launch {
            try {
                val pendingCount = db.productDao().getPending().size
                _uiState.value = _uiState.value.copy(pendingSyncCount = pendingCount)
                if (pendingCount > 0 && _isInternetAvailable.value) {
                    syncPendingProducts()
                } else if (pendingCount > 0 && !_isInternetAvailable.value) {
                    showMessage("$pendingCount producto(s) pendiente(s) de sincronizar", isWarning = true)
                }
            } catch (e: Exception) {
                timber.log.Timber.d("Admin: Error verificando pendientes: ${e.message}")
            }
        }
    }

    private fun syncPendingProducts() {
        viewModelScope.launch {
            try {
                showMessage("Sincronizando productos pendientes...", isWarning = true)
                syncManager.syncProducts()
                val pendingCount = db.productDao().getPending().size
                _uiState.value = _uiState.value.copy(pendingSyncCount = pendingCount)
                if (pendingCount == 0) {
                    showMessage("Sincronizacion completada", isSuccess = true)
                } else {
                    showMessage("Quedan $pendingCount producto(s) pendiente(s)", isWarning = true)
                }
                loadProductsFromRoom()
                if (_isInternetAvailable.value) loadProductsFromFirebase()
            } catch (e: Exception) {
                showMessage("Error al sincronizar: ${e.message}", isError = true)
            }
        }
    }

    fun checkLowStockImmediately() {
        viewModelScope.launch {
            try {
                val trackedProducts = db.productDao().getAll().filter { it.trackInventory }
                val outOfStock = trackedProducts.filter { it.stock == 0.0 }
                val lowStock = trackedProducts.filter { it.stock > 0 && it.stock <= it.minStock }
                if (outOfStock.isNotEmpty() || lowStock.isNotEmpty()) {
                    val message = when {
                        outOfStock.isNotEmpty() && lowStock.isNotEmpty() ->
                            "${outOfStock.size} agotados | ${lowStock.size} stock bajo"
                        outOfStock.isNotEmpty() -> "${outOfStock.size} producto(s) agotados"
                        else -> "${lowStock.size} producto(s) con stock bajo"
                    }
                    _uiState.value = _uiState.value.copy(warning = message)
                }
            } catch (e: Exception) {
                timber.log.Timber.d("Admin: Error verificando stock bajo: ${e.message}")
            }
        }
    }

    fun showProductForm(product: Product? = null) {
        _uiState.value = _uiState.value.copy(
            showProductForm = true,
            selectedProduct = product,
            error = null,
            success = null,
            warning = null
        )
    }

    fun hideProductForm() {
        _uiState.value = _uiState.value.copy(showProductForm = false, selectedProduct = null)
    }

    fun showDeleteDialog(product: Product) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true, selectedProduct = product)
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false, selectedProduct = null)
    }

    fun createProduct(product: Product) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val finalProduct = if (product.id.isEmpty()) product.copy(id = UUID.randomUUID().toString()) else product
                if (db.productDao().getById(finalProduct.id) != null) {
                    showMessage("Ya existe un producto con ese ID", isError = true)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                db.productDao().insert(finalProduct.toEntity().copy(syncStatus = "PENDING"))
                if (_isInternetAvailable.value) {
                    try {
                        firebaseProductRepository.createProduct(finalProduct)
                        db.productDao().updateStatus(finalProduct.id, "SYNCED")
                        showMessage("Producto '${finalProduct.name}' creado y sincronizado", isSuccess = true)
                    } catch (e: Exception) {
                        showMessage("Producto guardado localmente. Se sincronizara despues", isWarning = true)
                    }
                } else {
                    showMessage("SIN INTERNET - Producto guardado localmente", isWarning = true)
                }

                refreshProducts()
                logAudit(
                    action = "PRODUCTO_CREADO",
                    targetType = "PRODUCT",
                    targetId = finalProduct.id,
                    detail = "Producto ${finalProduct.name}, stock ${finalProduct.stock}"
                )
                hideProductForm()
                checkLowStockImmediately()
            } catch (e: Exception) {
                showMessage("Error al crear producto: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val updatedEntity = product.toEntity().copy(
                    syncStatus = "PENDING",
                    version = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )
                db.productDao().insert(updatedEntity)

                if (_isInternetAvailable.value) {
                    try {
                        firebaseProductRepository.updateProduct(product)
                        db.productDao().updateStatus(product.id, "SYNCED")
                        showMessage("Producto '${product.name}' actualizado y sincronizado", isSuccess = true)
                    } catch (e: Exception) {
                        showMessage("Producto actualizado localmente. Se sincronizara despues", isWarning = true)
                    }
                } else {
                    showMessage("SIN INTERNET - Producto actualizado localmente", isWarning = true)
                }

                refreshProducts()
                logAudit(
                    action = "PRODUCTO_ACTUALIZADO",
                    targetType = "PRODUCT",
                    targetId = product.id,
                    detail = "Producto ${product.name}, stock ${product.stock}, costo ${product.costPrice ?: 0.0}"
                )
                hideProductForm()
                checkLowStockImmediately()
            } catch (e: Exception) {
                showMessage("Error al actualizar producto: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteProduct() {
        val product = _uiState.value.selectedProduct
        if (product == null) {
            showMessage("No se selecciono ningun producto para eliminar", isError = true)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                db.productDao().deleteProduct(product.id)

                if (_isInternetAvailable.value) {
                    try {
                        firebaseProductRepository.deleteProduct(product.id)
                        showMessage("Producto '${product.name}' eliminado de la nube", isSuccess = true)
                    } catch (e: Exception) {
                        showMessage("Producto eliminado localmente. Se eliminara de la nube despues", isWarning = true)
                    }
                } else {
                    showMessage("SIN INTERNET - Producto eliminado localmente", isWarning = true)
                }

                refreshProducts()
                logAudit(
                    action = "PRODUCTO_ELIMINADO",
                    targetType = "PRODUCT",
                    targetId = product.id,
                    detail = "Producto ${product.name}"
                )
                hideDeleteDialog()
                checkLowStockImmediately()
            } catch (e: Exception) {
                showMessage("Error al eliminar producto: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                if (_isInternetAvailable.value) {
                    syncManager.syncProducts()
                    syncManager.downloadProducts()
                    refreshProducts()
                    val pendingCount = db.productDao().getPending().size
                    if (pendingCount == 0) {
                        showMessage("Sincronizacion completada", isSuccess = true)
                    } else {
                        showMessage("Sincronizacion parcial. Quedan $pendingCount producto(s)", isWarning = true)
                    }
                } else {
                    showMessage("Sin conexion a internet. Los cambios se guardaran localmente", isError = true)
                }
            } catch (e: Exception) {
                showMessage("Error en sincronizacion: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            loadProductsFromRoom()
            if (_isInternetAvailable.value) loadProductsFromFirebase()
            checkPendingSync()
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearSuccess() { _uiState.value = _uiState.value.copy(success = null) }
    fun clearWarning() { _uiState.value = _uiState.value.copy(warning = null) }

    val hasPendingSync: Boolean get() = _uiState.value.pendingSyncCount > 0
    val connectionStatusText: String get() =
        if (!_isInternetAvailable.value) "SIN INTERNET - Modo offline"
        else if (hasPendingSync) "${_uiState.value.pendingSyncCount} pendiente(s)"
        else "Conectado - Todo sincronizado"
}
