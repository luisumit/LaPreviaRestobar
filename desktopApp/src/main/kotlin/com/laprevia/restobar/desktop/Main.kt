package com.laprevia.restobar.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.data.repository.GitLiveOrderRepository
import com.laprevia.restobar.data.repository.GitLiveProductRepository
import com.laprevia.restobar.data.repository.GitLiveTableRepository
import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.domain.service.SalesCalculator
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

// ==================== Paleta "Noche de Previa" (misma que Android) ====================
private val NightBackground = Color(0xFF12121A)
private val NightSurface = Color(0xFF1E1E28)
private val AmberPrimary = Color(0xFFFFB300)
private val CoralSecondary = Color(0xFFFF6E40)
private val SuccessGreen = Color(0xFF66BB6A)
private val WarningOrange = Color(0xFFFFB74D)
private val SmokeWhite = Color(0xFFF5F5F5)

// ==================== Arranque ====================

fun main() {
    initFirebaseDesktop()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 1100.dp, height = 720.dp),
            title = "La Previa Restobar — Panel de Escritorio"
        ) {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AmberPrimary,
                    secondary = CoralSecondary,
                    background = NightBackground,
                    surface = NightSurface,
                    onBackground = SmokeWhite,
                    onSurface = SmokeWhite
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = NightBackground) {
                    App()
                }
            }
        }
    }
}

/**
 * En escritorio no existe google-services.json: se inicializa GitLive a mano con
 * los mismos datos del proyecto Firebase que usa la app Android.
 */
private fun initFirebaseDesktop() {
    FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
        val storage = mutableMapOf<String, String>()
        override fun store(key: String, value: String) { storage[key] = value }
        override fun retrieve(key: String): String? = storage[key]
        override fun clear(key: String) { storage.remove(key) }
        override fun log(msg: String) = println("[Firebase] $msg")
    })
    Firebase.initialize(
        context = android.app.Application(),
        options = FirebaseOptions(
            applicationId = "1:383569219396:android:58c48b61e8b7005f799111",
            apiKey = "AIzaSyD_nXUvuPfTeEmUYk6n_FhucVfuYhAntx0",
            databaseUrl = "https://laprevia-restobar-default-rtdb.firebaseio.com",
            projectId = "laprevia-restobar"
        )
    )
    println("✅ Firebase (GitLive/JVM) inicializado para escritorio")
}

// ==================== App ====================

@Composable
private fun App() {
    // Nota JVM: firebase-java-sdk no implementa user.email (TODO() interno),
    // asi que guardamos el email escrito en el login en vez de pedirlo al SDK.
    var user by remember { mutableStateOf<FirebaseUser?>(null) }
    var loggedEmail by remember { mutableStateOf("") }
    if (user == null) {
        LoginView(onLoggedIn = { u, email -> user = u; loggedEmail = email })
    } else {
        PanelView(userEmail = loggedEmail, onLogout = {
            user = null
        })
    }
}

// ==================== Login ====================

@Composable
private fun LoginView(onLoggedIn: (FirebaseUser, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("LA PREVIA", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = SmokeWhite)
        Text("RESTOBAR — Panel de Escritorio", color = AmberPrimary, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            modifier = Modifier.width(360.dp)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Contrasena") }, singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.width(360.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                loading = true; error = null
                scope.launch {
                    try {
                        val result = Firebase.auth.signInWithEmailAndPassword(email.trim(), password)
                        result.user?.let { onLoggedIn(it, email.trim()) }
                            ?: run { error = "Error de autenticacion" }
                    } catch (e: Exception) {
                        error = e.message ?: "Error desconocido"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.width(360.dp).height(46.dp)
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Iniciar Sesion")
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = CoralSecondary, fontSize = 13.sp)
        }
    }
}

// ==================== Panel principal ====================

private enum class Section(val label: String) { MESAS("Mesas"), PEDIDOS("Pedidos"), COCINA("Cocina"), PRODUCTOS("Productos"), REPORTE("Reporte") }

@Composable
private fun PanelView(userEmail: String, onLogout: () -> Unit) {
    var section by remember { mutableStateOf(Section.MESAS) }
    val tableRepo = remember { GitLiveTableRepository() }
    val orderRepo = remember { GitLiveOrderRepository() }
    val productRepo = remember { GitLiveProductRepository() }

    val tables by tableRepo.getTables().collectAsState(initial = emptyList())
    val orders by orderRepo.getOrders().collectAsState(initial = emptyList())
    val products by productRepo.getAllProducts().collectAsState(initial = emptyList())

    // CAJA: pedido seleccionado para cobrar desde la PC
    var orderToCobrar by remember { mutableStateOf<Order?>(null) }
    var showPrinterSettings by remember { mutableStateOf(false) }
    if (showPrinterSettings) {
        PrinterSettingsDialog(onClose = { showPrinterSettings = false })
    }
    orderToCobrar?.let { order ->
        CobrarDialog(
            order = order,
            orderRepo = orderRepo,
            tableRepo = tableRepo,
            onClose = { orderToCobrar = null }
        )
    }

    // Campanita de pedido nuevo: suena cuando aparece un pedido activo
    // creado despues de abrir el panel (no suena por el historial).
    val panelStart = remember { System.currentTimeMillis() }
    val seenOrderIds = remember { mutableSetOf<String>() }
    LaunchedEffect(orders) {
        orders.forEach { order ->
            val isNew = seenOrderIds.add(order.id)
            if (isNew && order.createdAt >= panelStart && order.status in ACTIVE_STATUSES) {
                OrderSound.play()
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(containerColor = NightSurface) {
            Spacer(Modifier.height(12.dp))
            NavigationRailItem(
                selected = section == Section.MESAS,
                onClick = { section = Section.MESAS },
                icon = { Icon(Icons.Default.TableRestaurant, contentDescription = null) },
                label = { Text("Mesas") }
            )
            NavigationRailItem(
                selected = section == Section.PEDIDOS,
                onClick = { section = Section.PEDIDOS },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                label = { Text("Pedidos") }
            )
            NavigationRailItem(
                selected = section == Section.COCINA,
                onClick = { section = Section.COCINA },
                icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                label = { Text("Cocina") }
            )
            NavigationRailItem(
                selected = section == Section.PRODUCTOS,
                onClick = { section = Section.PRODUCTOS },
                icon = { Icon(Icons.Default.LocalBar, contentDescription = null) },
                label = { Text("Productos") }
            )
            NavigationRailItem(
                selected = section == Section.REPORTE,
                onClick = { section = Section.REPORTE },
                icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                label = { Text("Reporte") }
            )
            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                selected = false,
                onClick = { showPrinterSettings = true },
                icon = { Icon(Icons.Default.Print, contentDescription = "Impresora") },
                label = { Text("Impresora", fontSize = 10.sp) }
            )
            TextButton(onClick = onLogout) { Text("Salir", color = CoralSecondary, fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
        }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                "${section.label}  ·  $userEmail",
                color = SmokeWhite.copy(alpha = 0.6f), fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            when (section) {
                Section.MESAS -> MesasView(tables, orders)
                Section.PEDIDOS -> PedidosView(orders, onCobrar = { orderToCobrar = it })
                Section.COCINA -> KdsView(orders, orderRepo)
                Section.PRODUCTOS -> ProductosView(products, productRepo)
                Section.REPORTE -> ReporteView(orders, userEmail)
            }
        }
    }
}

// ==================== Mesas ====================

private fun tableLabel(n: Int) = "M" + n.toString().padStart(2, '0')

// Misma regla que la app Android (normalizeTables): una mesa esta OCUPADA solo si
// tiene un pedido ACTIVO; un status OCUPADA viejo en Firebase sin pedido se muestra LIBRE.
private val ACTIVE_STATUSES = setOf(
    OrderStatus.PENDING, OrderStatus.ENVIADO, OrderStatus.ACEPTADO,
    OrderStatus.EN_PREPARACION, OrderStatus.LISTO, OrderStatus.ENTREGADO
)

@Composable
private fun MesasView(tables: List<Table>, orders: List<Order>) {
    val activeByTable = orders
        .map { if (it.tableId == 0 && it.tableNumber in 1..8) it.copy(tableId = it.tableNumber) else it }
        .filter { it.status in ACTIVE_STATUSES && it.tableId in 1..8 }
        .distinctBy { it.id }
        .associateBy { it.tableId }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tables.filter { it.number in 1..8 }.sortedBy { it.number }) { table ->
            val activeOrder = activeByTable[table.id]
            // Igual que el celular: solo un pedido activo marca la mesa como ocupada.
            val occupied = activeOrder != null
            Card(
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(10.dp).background(
                                if (occupied) WarningOrange else SuccessGreen,
                                shape = RoundedCornerShape(50)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(tableLabel(table.number), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SmokeWhite)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (occupied) "OCUPADA" else "LIBRE",
                        color = if (occupied) WarningOrange else SuccessGreen,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text("Capacidad: ${table.capacity}", color = SmokeWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                    activeOrder?.let {
                        Text(
                            "Pedido: ${Money(SalesCalculator.orderTotal(it)).formatted()}",
                            color = AmberPrimary, fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== Pedidos ====================

@Composable
private fun PedidosView(orders: List<Order>, onCobrar: (Order) -> Unit) {
    val active = orders
        .filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
        .sortedByDescending { it.createdAt }

    if (active.isEmpty()) {
        Text("No hay pedidos activos ahora mismo.", color = SmokeWhite.copy(alpha = 0.7f))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(active) { order ->
            Card(colors = CardDefaults.cardColors(containerColor = NightSurface), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${tableLabel(order.tableNumber)}  ·  ${order.status.name}",
                            fontWeight = FontWeight.Bold, color = AmberPrimary
                        )
                        Text(
                            Money(SalesCalculator.orderTotal(order)).formatted(),
                            fontWeight = FontWeight.Bold, color = SmokeWhite
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    order.items.forEach { item ->
                        Text("  ${item.quantity} x ${item.productName}", color = SmokeWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    if (!order.notes.isNullOrBlank()) {
                        Text("  Nota: ${order.notes}", color = WarningOrange, fontSize = 12.sp)
                    }
                    if (order.canBeCharged()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onCobrar(order) },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                            ) { Text("COBRAR", fontWeight = FontWeight.Bold) }
                            OutlinedButton(onClick = {
                                printOnDesktop(
                                    com.laprevia.restobar.data.printer.ReceiptFormatter().kitchenComanda(order)
                                )
                            }) { Text("Comanda") }
                        }
                    }
                }
            }
        }
    }
}
