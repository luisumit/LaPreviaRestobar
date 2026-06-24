package com.laprevia.restobar.presentation.screens.admin

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.laprevia.restobar.presentation.notifications.AdminStockScheduler
import com.laprevia.restobar.presentation.theme.SuccessGreen
import com.laprevia.restobar.presentation.theme.WarningOrange
import com.laprevia.restobar.presentation.viewmodel.AdminDashboardMetrics
import com.laprevia.restobar.presentation.viewmodel.AdminReportFilter
import com.laprevia.restobar.presentation.viewmodel.AdminViewModel
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel
import com.laprevia.restobar.presentation.viewmodel.PUBLIC_MENU_URL
import com.laprevia.restobar.presentation.viewmodel.SalesReport
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

private enum class ProductQuickFilter(val label: String) {
    ALL("Todos"),
    ACTIVE("Activos"),
    OUT_OF_STOCK("Agotados"),
    LOW_STOCK("Stock bajo")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current
    val isTablet = isTabletScreen()
    var selectedAdminTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        AdminStockScheduler.schedulePeriodicCheck(context)
    }

    LaunchedEffect(uiState.success, uiState.warning, uiState.error) {
        if (uiState.success != null || uiState.warning != null || uiState.error != null) {
            delay(3000)
            if (uiState.success != null) viewModel.clearSuccess()
            if (uiState.warning != null) viewModel.clearWarning()
            if (uiState.error != null) viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isTablet) 12.dp else 8.dp,
                        shape = RoundedCornerShape(bottomStart = if (isTablet) 32.dp else 24.dp, bottomEnd = if (isTablet) 32.dp else 24.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = if (isTablet) 16.dp else 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "LA PREVIA RESTOBAR",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Panel administrativo",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(
                            onClick = { AdminStockScheduler.triggerImmediateCheck(context) },
                            modifier = Modifier
                                .size(if (isTablet) 48.dp else 44.dp)
                                .clip(CircleShape)
                                .background(WarningOrange)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Probar notificacion", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(if (isTablet) 8.dp else 4.dp))

                        IconButton(
                            onClick = {
                                onLogout()
                            },
                            modifier = Modifier
                                .size(if (isTablet) 48.dp else 44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Cerrar sesion", tint = MaterialTheme.colorScheme.onSecondary)
                        }
                    }

                    ConnectionStatusBanner(
                        isOffline = uiState.isOffline,
                        pendingSyncCount = uiState.pendingSyncCount,
                        connectionStatusText = viewModel.connectionStatusText,
                        onManualSync = { viewModel.manualSync() }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedAdminTab == 2) {
                FloatingActionButton(
                    onClick = { viewModel.showProductForm() },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.shadow(if (isTablet) 12.dp else 8.dp, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            MessageBanner(
                error = uiState.error,
                warning = uiState.warning,
                success = uiState.success,
                onClearError = { viewModel.clearError() },
                onClearWarning = { viewModel.clearWarning() },
                onClearSuccess = { viewModel.clearSuccess() }
            )

            AdminSectionTabs(
                selectedTab = selectedAdminTab,
                onTabSelected = { selectedAdminTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTablet) 24.dp else 16.dp)
                    .padding(top = if (isTablet) 20.dp else 16.dp)
            )

            when (selectedAdminTab) {
                0 -> AdminDashboardSection(uiState.dashboardMetrics, isTablet, Modifier.fillMaxSize())
                1 -> SalesReportSection(
                    report = uiState.report,
                    selectedFilter = uiState.reportFilter,
                    customStart = uiState.customReportStart,
                    customEnd = uiState.customReportEnd,
                    onFilterSelected = { viewModel.selectReportFilter(it) },
                    onCustomRangeSelected = { start, end -> viewModel.selectCustomReportRange(start, end) },
                    onExportPdf = { viewModel.exportReportToPdf() },
                    onExportExcel = { viewModel.exportReportToExcel() },
                    isTablet = isTablet,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> ProductsAdminSection(
                    products = uiState.products,
                    categoriesCount = uiState.categories.size,
                    isTablet = isTablet,
                    connectionStatusText = viewModel.connectionStatusText,
                    isOffline = uiState.isOffline,
                    pendingSyncCount = uiState.pendingSyncCount,
                    onEdit = { viewModel.showProductForm(it) },
                    onDelete = { viewModel.showDeleteDialog(it) },
                    modifier = Modifier.fillMaxSize()
                )
                else -> MenuQrSection(isTablet = isTablet, modifier = Modifier.fillMaxSize())
            }
        }
    }

    if (uiState.showProductForm) {
        ProductFormDialog(
            product = uiState.selectedProduct,
            categories = uiState.categories,
            onSave = { product ->
                if (uiState.selectedProduct == null) viewModel.createProduct(product) else viewModel.updateProduct(product)
            },
            onDismiss = { viewModel.hideProductForm() }
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("Eliminar producto") },
            text = { Text("Estas seguro de que quieres eliminar \"${uiState.selectedProduct?.name}\"?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProduct() }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AdminSectionTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTab,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    ) {
        AdminTab(selected = selectedTab == 0, label = "Panel", icon = Icons.Default.Dashboard) { onTabSelected(0) }
        AdminTab(selected = selectedTab == 1, label = "Reportes", icon = Icons.Default.Assessment) { onTabSelected(1) }
        AdminTab(selected = selectedTab == 2, label = "Productos", icon = Icons.Default.Inventory) { onTabSelected(2) }
        AdminTab(selected = selectedTab == 3, label = "QR", icon = Icons.Default.QrCode2) { onTabSelected(3) }
    }
}

@Composable
fun AdminTab(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
        },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        selectedContentColor = MaterialTheme.colorScheme.onSurface,
        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    )
}

@Composable
fun AdminDashboardSection(
    metrics: AdminDashboardMetrics,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
    ) {
        item {
            DashboardHeaderCard("Dashboard", "Ventas del dia, productos activos y alertas operativas", Icons.Default.Dashboard, isTablet)
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Ventas del dia", "S/ ${formatMoney(metrics.salesToday)}", Icons.Default.Payments, listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
                DashboardMetricCard("Ventas del año", "S/ ${formatMoney(metrics.salesYear)}", Icons.Default.Assessment, listOf(SuccessGreen, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
            }
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Pedidos activos", metrics.activeOrders.toString(), Icons.Default.RestaurantMenu, listOf(SuccessGreen, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
                DashboardMetricCard("Productos activos", metrics.activeProducts.toString(), Icons.Default.Inventory, listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary), Modifier.weight(1f))
            }
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Producto top", metrics.bestSellingQuantity.toString(), Icons.Default.Star, listOf(WarningOrange, MaterialTheme.colorScheme.secondary), Modifier.weight(1f), metrics.bestSellingProduct)
                DashboardMetricCard("Mesas ocupadas", "${metrics.occupiedTables}/${metrics.totalTables}", Icons.Default.EventSeat, listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
            }
        }
        item {
            StockAlertCard(metrics = metrics, isTablet = isTablet)
        }
        item {
            DashboardInfoCard(
                title = "Inventario",
                icon = Icons.Default.Inventory2,
                rows = listOf(
                    "Productos con stock bajo" to metrics.lowStockProducts.toString(),
                    "Productos agotados" to metrics.outOfStockProducts.toString()
                ),
                isTablet = isTablet
            )
        }
    }
}

@Composable
fun StockAlertCard(metrics: AdminDashboardMetrics, isTablet: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 22.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Productos con alerta de stock",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium
                )
            }
            StockAlertList(
                title = "Stock bajo (${metrics.lowStockProducts})",
                items = metrics.lowStockProductNames,
                emptyText = "No hay productos con stock bajo"
            )
            StockAlertList(
                title = "Agotados (${metrics.outOfStockProducts})",
                items = metrics.outOfStockProductNames,
                emptyText = "No hay productos agotados"
            )
        }
    }
}

@Composable
fun StockAlertList(title: String, items: List<String>, emptyText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        if (items.isEmpty()) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        } else {
            items.forEach { productName ->
                Text("• $productName", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SalesReportSection(
    report: SalesReport,
    selectedFilter: AdminReportFilter,
    customStart: Long,
    customEnd: Long,
    onFilterSelected: (AdminReportFilter) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
    ) {
        item {
            DashboardHeaderCard("Reporte y caja", "Filtra ventas, revisa cierre del turno y exporta el resumen", Icons.Default.PointOfSale, isTablet)
        }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AdminReportFilter.values().forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        shape = SegmentedButtonDefaults.itemShape(index, AdminReportFilter.values().size)
                    ) {
                        Text(filter.label)
                    }
                }
            }
        }
        item {
            ReportRangeSelector(
                start = customStart,
                end = customEnd,
                onRangeSelected = onCustomRangeSelected,
                isTablet = isTablet
            )
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Total vendido", "S/ ${formatMoney(report.totalSales)}", Icons.Default.Payments, listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
                DashboardMetricCard("Ganancia", "S/ ${formatMoney(report.grossProfit)}", Icons.Default.PointOfSale, listOf(SuccessGreen, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
            }
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Pedidos", report.totalOrders.toString(), Icons.Default.ReceiptLong, listOf(SuccessGreen, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
                DashboardMetricCard("Productos", report.productsSold.toString(), Icons.Default.Inventory, listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary), Modifier.weight(1f))
            }
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Pedidos cobrados", report.chargedOrders.toString(), Icons.Default.CheckCircle, listOf(SuccessGreen, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
                DashboardMetricCard("Pedidos cancelados", report.cancelledOrders.toString(), Icons.Default.Error, listOf(WarningOrange, MaterialTheme.colorScheme.secondary), Modifier.weight(1f))
            }
        }
        item {
            DashboardInfoCard(
                title = "Cierre del turno",
                icon = Icons.Default.Summarize,
                rows = listOf(
                    "Producto mas vendido" to "${report.bestSellingProduct} (${report.bestSellingQuantity})"
                ),
                isTablet = isTablet
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)) {
                Button(onClick = onExportPdf, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF")
                }
                Button(onClick = onExportExcel, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excel")
                }
            }
        }
    }
}

@Composable
fun ReportRangeSelector(
    start: Long,
    end: Long,
    onRangeSelected: (Long, Long) -> Unit,
    isTablet: Boolean
) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Rango por fechas",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showDatePicker(context, start) { selected ->
                            onRangeSelected(selected, end)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Desde: ${formatDateShort(start)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = {
                        showDatePicker(context, end) { selected ->
                            onRangeSelected(start, selected)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hasta: ${formatDateShort(end)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProductsAdminSection(
    products: List<com.laprevia.restobar.data.model.Product>,
    categoriesCount: Int,
    isTablet: Boolean,
    connectionStatusText: String,
    isOffline: Boolean,
    pendingSyncCount: Int,
    onEdit: (com.laprevia.restobar.data.model.Product) -> Unit,
    onDelete: (com.laprevia.restobar.data.model.Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedQuickFilter by rememberSaveable { mutableStateOf(ProductQuickFilter.ALL) }
    val filteredProducts = remember(products, searchText, selectedQuickFilter) {
        products
            .filter { product ->
                searchText.isBlank() || product.name.contains(searchText, ignoreCase = true)
            }
            .filter { product ->
                when (selectedQuickFilter) {
                    ProductQuickFilter.ALL -> true
                    ProductQuickFilter.ACTIVE -> product.isActive
                    ProductQuickFilter.OUT_OF_STOCK -> product.trackInventory && product.stock == 0.0
                    ProductQuickFilter.LOW_STOCK -> product.trackInventory && product.stock > 0.0 && product.stock <= product.minStock
                }
            }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
    ) {
        item {
            DashboardHeaderCard("Panel de administracion", connectionStatusText, Icons.Default.Inventory, isTablet)
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Total productos", products.size.toString(), Icons.Default.Inventory, listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
                DashboardMetricCard("Activos", products.count { it.isActive }.toString(), Icons.Default.CheckCircle, listOf(SuccessGreen, MaterialTheme.colorScheme.tertiary), Modifier.weight(1f))
            }
        }
        item {
            DashboardStatGrid(isTablet) {
                DashboardMetricCard("Con inventario", products.count { it.trackInventory }.toString(), Icons.Default.Inventory2, listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary), Modifier.weight(1f))
                DashboardMetricCard("Categorias", categoriesCount.toString(), Icons.Default.Assessment, listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primaryContainer), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(if (isTablet) 24.dp else 16.dp)) {
                    Text("Productos registrados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Buscar producto") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ProductQuickFilter.values().forEachIndexed { index, filter ->
                            SegmentedButton(
                                selected = selectedQuickFilter == filter,
                                onClick = { selectedQuickFilter = filter },
                                shape = SegmentedButtonDefaults.itemShape(index, ProductQuickFilter.values().size)
                            ) {
                                Text(filter.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${filteredProducts.size} de ${products.size} productos",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (products.isEmpty()) {
                        Text("No hay productos registrados. Presiona el boton + para agregar uno.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    } else if (filteredProducts.isEmpty()) {
                        Text("No hay productos con ese filtro.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)) {
                            filteredProducts.forEach { product ->
                                ProductAdminCard(product = product, onEdit = { onEdit(product) }, onDelete = { onDelete(product) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuQrSection(isTablet: Boolean, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
    ) {
        item {
            DashboardHeaderCard("QR general de carta", "Imprime este QR y usalo en todas las mesas", Icons.Default.QrCode2, isTablet)
        }
        item {
            MenuQrCard(menuUrl = PUBLIC_MENU_URL, isTablet = isTablet)
        }
    }
}

@Composable
fun MenuQrCard(menuUrl: String, isTablet: Boolean) {
    val qrBitmap = remember(menuUrl) { generateQrBitmap(menuUrl) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (isTablet) 24.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 14.dp)
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR general de carta",
                    modifier = Modifier.size(if (isTablet) 260.dp else 220.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(if (isTablet) 260.dp else 220.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(72.dp))
                }
            }
            Text("Carta digital del restaurante", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(menuUrl, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun generateQrBitmap(text: String, size: Int = 512): Bitmap? {
    return runCatching {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bitmap ->
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
    }.getOrNull()
}

@Composable
fun DashboardHeaderCard(title: String, subtitle: String, icon: ImageVector, isTablet: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(if (isTablet) 24.dp else 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(if (isTablet) 76.dp else 60.dp).clip(CircleShape).background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.surface))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(if (isTablet) 34.dp else 28.dp))
            }
            Spacer(modifier = Modifier.width(if (isTablet) 20.dp else 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DashboardStatGrid(isTablet: Boolean, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp), content = content)
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val isTablet = isTabletScreen()
    Card(modifier = modifier.height(if (isTablet) 122.dp else 108.dp), shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(gradientColors)).padding(if (isTablet) 18.dp else 14.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.32f), modifier = Modifier.align(Alignment.TopEnd).size(if (isTablet) 38.dp else 32.dp))
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge)
                Text(title, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun DashboardInfoCard(title: String, icon: ImageVector, rows: List<Pair<String, String>>, isTablet: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(if (isTablet) 22.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
            }
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), modifier = Modifier.weight(1f))
                    Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

fun formatDateShort(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(timestamp)
}

fun showDatePicker(context: Context, initialDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDate }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selected.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
fun isTabletScreen(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2 &&
        LocalConfiguration.current.screenWidthDp >= 600
}

@Composable
fun ConnectionStatusBanner(isOffline: Boolean, pendingSyncCount: Int, connectionStatusText: String, onManualSync: () -> Unit) {
    val backgroundColor = when {
        isOffline -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
        pendingSyncCount > 0 -> WarningOrange.copy(alpha = 0.9f)
        else -> SuccessGreen.copy(alpha = 0.9f)
    }
    val statusIcon = when {
        isOffline -> Icons.Default.WifiOff
        pendingSyncCount > 0 -> Icons.Default.Sync
        else -> Icons.Default.Wifi
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = backgroundColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(statusIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(connectionStatusText, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            if (pendingSyncCount > 0) {
                TextButton(onClick = onManualSync, colors = ButtonDefaults.textButtonColors(contentColor = Color.White), modifier = Modifier.height(28.dp)) {
                    Text("Sincronizar", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
            }
        }
    }
}

@Composable
fun MessageBanner(
    error: String?,
    warning: String?,
    success: String?,
    onClearError: () -> Unit,
    onClearWarning: () -> Unit,
    onClearSuccess: () -> Unit
) {
    if (error != null) {
        BannerRow(text = error, color = MaterialTheme.colorScheme.error.copy(alpha = 0.95f), icon = Icons.Default.Error, onClose = onClearError)
    }
    if (warning != null) {
        BannerRow(text = warning, color = WarningOrange.copy(alpha = 0.95f), icon = Icons.Default.Warning, onClose = onClearWarning)
    }
    if (success != null) {
        BannerRow(text = success, color = SuccessGreen.copy(alpha = 0.95f), icon = Icons.Default.CheckCircle, onClose = onClearSuccess)
    }
}

@Composable
fun BannerRow(text: String, color: Color, icon: ImageVector, onClose: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
