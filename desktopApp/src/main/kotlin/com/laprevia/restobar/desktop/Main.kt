package com.laprevia.restobar.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.Table
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
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ==================== Arranque ====================

fun main() {
    initFirebaseDesktop()
    application {
        // 1280x800 ideal, acotado a la pantalla real (laptops de caja 1366x768)
        val screen = java.awt.Toolkit.getDefaultToolkit().screenSize
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                width = minOf(1280, screen.width - 40).dp,
                height = minOf(800, screen.height - 80).dp
            ),
            title = "La Previa Restobar — Panel de Escritorio"
        ) {
            LpTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Lp.Bg) {
                    App()
                }
            }
        }
    }
}

/**
 * Almacenamiento persistente del token de sesion (recordar sesion entre reinicios).
 * Vive en ~/.laprevia-desktop-auth.properties.
 */
object AuthStorage {
    private val file = java.io.File(System.getProperty("user.home"), ".laprevia-desktop-auth.properties")
    private val props = java.util.Properties().apply {
        runCatching { if (file.exists()) file.inputStream().use { load(it) } }
    }

    fun store(key: String, value: String) { props.setProperty(key, value); persist() }
    fun retrieve(key: String): String? = props.getProperty(key)
    fun clear(key: String) { props.remove(key); persist() }
    fun clearAll() { props.clear(); persist() }
    private fun persist() {
        runCatching { file.outputStream().use { props.store(it, "La Previa - sesion del panel") } }
    }
}

/**
 * En escritorio no existe google-services.json: se inicializa GitLive a mano con
 * los mismos datos del proyecto Firebase que usa la app Android.
 */
private fun initFirebaseDesktop() {
    FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
        // Respaldado en disco: la sesion sobrevive reinicios del panel.
        override fun store(key: String, value: String) = AuthStorage.store(key, value)
        override fun retrieve(key: String): String? = AuthStorage.retrieve(key)
        override fun clear(key: String) = AuthStorage.clear(key)
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
    val scope = rememberCoroutineScope()
    // Sesion recordada: el token vive en AuthStorage (disco), asi que si hubo
    // login antes, Firebase.auth.currentUser ya viene restaurado al abrir.
    // Nota JVM: firebase-java-sdk no implementa user.email (TODO() interno),
    // asi que el email mostrado se guarda aparte en DesktopPrefs.
    var user by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var loggedEmail by remember { mutableStateOf(DesktopPrefs.lastEmail) }
    if (user == null) {
        LoginView(onLoggedIn = { u, email ->
            user = u
            loggedEmail = email
            DesktopPrefs.lastEmail = email
        })
    } else {
        PanelView(userEmail = loggedEmail, onLogout = {
            scope.launch { runCatching { Firebase.auth.signOut() } }
            AuthStorage.clearAll()
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
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun doLogin() {
        if (loading || email.isBlank() || password.isBlank()) return
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
    }

    Row(Modifier.fillMaxSize()) {
        // -------- Panel de marca (izquierda) --------
        Box(
            Modifier.weight(0.55f).fillMaxHeight().drawBehind {
                drawRect(Lp.BgDeep)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Lp.Amber.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(-size.width * 0.10f, -size.height * 0.15f),
                        radius = size.width * 1.05f
                    ),
                    center = Offset(-size.width * 0.10f, -size.height * 0.15f),
                    radius = size.width * 1.05f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Lp.Coral.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 1.05f, size.height * 1.15f),
                        radius = size.width * 0.95f
                    ),
                    center = Offset(size.width * 1.05f, size.height * 1.15f),
                    radius = size.width * 0.95f
                )
            }
        ) {
            // Copa de coctel dibujada en linea, decorativa
            MartiniArt(
                Modifier.size(360.dp).align(Alignment.CenterEnd)
                    .offset(x = 80.dp, y = 60.dp).rotate(-8f)
            )
            Column(
                Modifier.fillMaxSize().padding(horizontal = 52.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo + wordmark
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    LpLogo(46)
                    Text(
                        "RESTOBAR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 5.sp, color = Lp.TextSoft
                    )
                }
                // Lockup central: LA solido + PREVIA en contorno ambar
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column {
                        Text(
                            "LA", fontFamily = BebasFamily, fontSize = 128.sp,
                            lineHeight = 113.sp, letterSpacing = 3.sp, color = Lp.Text
                        )
                        Text(
                            "PREVIA", fontFamily = BebasFamily, fontSize = 128.sp,
                            lineHeight = 113.sp, letterSpacing = 3.sp,
                            color = Lp.Amber.copy(alpha = 0.75f),
                            style = TextStyle(drawStyle = Stroke(width = 3f))
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.width(34.dp).height(2.dp).background(Lp.Amber))
                        Text(
                            "Mesas, cocina y caja — en una sola pantalla.",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextSoft
                        )
                    }
                }
                // Pie: sello del panel
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(1.dp).background(
                            Brush.horizontalGradient(listOf(Lp.Amber.copy(alpha = 0.35f), Color.Transparent))
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        FooterTag("PANEL DE ESCRITORIO"); FooterDot()
                        FooterTag("WINDOWS"); FooterDot()
                        FooterTag("V1.0")
                    }
                }
            }
        }

        // -------- Formulario (derecha) --------
        Column(
            Modifier.weight(0.45f).fillMaxHeight()
                .background(Lp.FormPanel)
                .padding(horizontal = 64.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Acotado a 400dp: a todo el ancho los campos pierden proporcion
            Column(Modifier.widthIn(max = 400.dp).fillMaxWidth()) {
                Text(
                    "BIENVENIDO DE VUELTA", fontFamily = BebasFamily,
                    fontSize = 38.sp, letterSpacing = 2.sp, color = Lp.Text
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Inicia sesión para abrir la caja del turno.",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextDim
                )
                Spacer(Modifier.height(32.dp))

                FieldLabel("EMAIL")
                OutlinedTextField(
                    value = email,
                    // Una sola linea, sin espacios, maximo 60 caracteres
                    onValueChange = { email = it.replace("\n", "").replace(" ", "").take(60) },
                    placeholder = { Text("tucorreo@restobar.com", color = Lp.TextMuted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Lp.TextDim, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(13.dp),
                    colors = lpFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("CONTRASEÑA")
                OutlinedTextField(
                    value = password,
                    // Una sola linea, maximo 40 caracteres
                    onValueChange = { password = it.replace("\n", "").take(40) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Lp.TextDim, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = Lp.TextDim
                            )
                        }
                    },
                    shape = RoundedCornerShape(13.dp),
                    colors = lpFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                val ready = !loading && email.isNotBlank() && password.isNotBlank()
                Box(
                    Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (ready) Brush.linearGradient(listOf(Lp.Amber, Lp.AmberDeep))
                            else Brush.linearGradient(listOf(Lp.Amber.copy(alpha = 0.35f), Lp.AmberDeep.copy(alpha = 0.35f)))
                        )
                        .lpHover(0.10f, enabled = ready)
                        .clickable(enabled = ready) { doLogin() },
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Lp.OnAccent)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "INICIAR SESIÓN", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.5.sp, color = Lp.OnAccent
                            )
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Lp.OnAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Lp.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Lp.Green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sesión recordada en este equipo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextDim)
                }
            }
        }
    }
}

/** Copa de coctel en trazo fino (decorativa, como en el mockup). */
@Composable
private fun MartiniArt(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = 2.5f)
        val tint = Lp.Amber.copy(alpha = 0.22f)
        // Copa (triangulo)
        drawPath(
            Path().apply {
                moveTo(w * 0.12f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.10f)
                lineTo(w * 0.50f, h * 0.52f)
                close()
            },
            color = tint, style = stroke
        )
        // Tallo y base
        drawLine(tint, Offset(w * 0.50f, h * 0.52f), Offset(w * 0.50f, h * 0.84f), strokeWidth = 2.5f)
        drawLine(tint, Offset(w * 0.30f, h * 0.88f), Offset(w * 0.70f, h * 0.88f), strokeWidth = 2.5f)
        // Aceituna
        drawCircle(tint, radius = w * 0.055f, center = Offset(w * 0.64f, h * 0.20f), style = stroke)
    }
}

@Composable
private fun LpLogo(sizeDp: Int) {
    Box(
        Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp * 0.3).dp))
            .background(Brush.linearGradient(listOf(Lp.Amber, Lp.Coral))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "LP", fontFamily = BebasFamily, fontSize = (sizeDp * 0.48).sp,
            letterSpacing = 1.sp, color = Lp.OnAccent
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp, color = Lp.TextDim
    )
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun FooterTag(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Lp.TextMuted)
}

@Composable
private fun FooterDot() {
    Box(Modifier.size(4.dp).background(Lp.TextMuted, CircleShape))
}

// ==================== Panel principal ====================

private enum class Section(val label: String) { MESAS("Mesas"), PEDIDOS("Pedidos"), COCINA("Cocina"), PRODUCTOS("Productos"), REPORTE("Reporte") }

/** Pedidos activos por mesa (misma regla normalizeTables que la app Android). */
internal fun activeOrdersByTable(orders: List<Order>): Map<Int, Order> = orders
    .map { if (it.tableId == 0 && it.tableNumber in 1..8) it.copy(tableId = it.tableNumber) else it }
    .filter { it.status in ACTIVE_STATUSES && it.tableId in 1..8 }
    .distinctBy { it.id }
    .associateBy { it.tableId }

private val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
private fun PanelView(userEmail: String, onLogout: () -> Unit) {
    var section by remember { mutableStateOf(Section.MESAS) }
    val tableRepo = remember { GitLiveTableRepository() }
    val orderRepo = remember { GitLiveOrderRepository() }
    val productRepo = remember { GitLiveProductRepository() }

    val tables by tableRepo.getTables().collectAsState(initial = emptyList())
    val orders by orderRepo.getOrders().collectAsState(initial = emptyList())
    val products by productRepo.getAllProducts().collectAsState(initial = emptyList())

    // Reloj compartido para cronometros (Mesas y Pedidos), refresco cada 30 s
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

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

    // Indicador de conexion: canal oficial de Firebase (.info/connected),
    // con 5 s de gracia para no parpadear durante la conexion inicial.
    val connected by remember {
        Firebase.database.reference(".info/connected").valueEvents
            .map { it.value as? Boolean ?: false }
    }.collectAsState(initial = true)
    var offline by remember { mutableStateOf(false) }
    LaunchedEffect(connected) {
        if (!connected) {
            delay(5000)
            offline = true
        } else {
            offline = false
        }
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

    // Agregados en vivo para header y badges (datos que ya estan en memoria)
    val activeCount = orders.count { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
    val kitchenCount = orders.count {
        it.status in setOf(
            OrderStatus.PENDING, OrderStatus.ENVIADO, OrderStatus.ACEPTADO,
            OrderStatus.EN_PREPARACION, OrderStatus.LISTO
        )
    }
    val occupiedCount = activeOrdersByTable(orders).size
    // coerce: al arrancar, orders puede llegar antes que tables (evita "-3 libres")
    val freeCount = (tables.count { it.number in 1..8 } - occupiedCount).coerceAtLeast(0)
    val todayStart = startOfToday()
    // Mismo criterio de fecha que el Reporte (createdAt): header y caja siempre cuadran
    val hoyOrders = orders.filter { it.status == OrderStatus.COMPLETED && it.createdAt >= todayStart }
    val hoyTotal = hoyOrders.sumOf { SalesCalculator.orderTotal(it) }

    Row(
        modifier = Modifier.fillMaxSize().drawBehind {
            drawRect(Lp.Bg)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Lp.Amber.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.92f, -size.height * 0.12f),
                    radius = size.width * 0.85f
                ),
                center = Offset(size.width * 0.92f, -size.height * 0.12f),
                radius = size.width * 0.85f
            )
        }
    ) {
        // -------- Barra lateral --------
        Column(
            Modifier.width(96.dp).fillMaxHeight().background(Lp.Sidebar).padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LpLogo(44)
            Box(Modifier.width(40.dp).height(1.dp).background(Lp.Divider))
            SidebarItem(Icons.Default.TableRestaurant, "Mesas", section == Section.MESAS) { section = Section.MESAS }
            SidebarItem(Icons.Default.ReceiptLong, "Pedidos", section == Section.PEDIDOS, badge = activeCount) { section = Section.PEDIDOS }
            SidebarItem(Icons.Default.Restaurant, "Cocina", section == Section.COCINA, badge = kitchenCount) { section = Section.COCINA }
            SidebarItem(Icons.Default.LocalBar, "Productos", section == Section.PRODUCTOS) { section = Section.PRODUCTOS }
            SidebarItem(Icons.Default.Assessment, "Reporte", section == Section.REPORTE) { section = Section.REPORTE }
            Spacer(Modifier.weight(1f))
            LpTooltip("Configurar impresora térmica") {
                SidebarItem(Icons.Default.Print, "Impresora", selected = false) { showPrinterSettings = true }
            }
            LpTooltip("Cerrar sesión") {
                SidebarItem(Icons.Default.Logout, "Salir", selected = false, tint = Lp.Coral, onClick = onLogout)
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Lp.CardBorder))

        // -------- Contenido --------
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    BebasTitle(section.label.uppercase(), 42)
                    Text(
                        when (section) {
                            Section.MESAS -> "$freeCount libres · $occupiedCount ocupadas"
                            Section.PEDIDOS -> "$activeCount activos · el más antiguo primero"
                            Section.COCINA -> "$kitchenCount en cola · el más antiguo primero"
                            Section.PRODUCTOS -> "${products.size} productos en el menú"
                            Section.REPORTE -> "ventas y cierres de caja"
                        },
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextDim
                    )
                }
                Spacer(Modifier.weight(1f))
                // Venta del dia siempre a la vista
                Row(
                    Modifier.background(Lp.Amber.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("HOY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Lp.TextDim)
                    Text(Money(hoyTotal).formatted(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Lp.Amber, style = TabularNumbers)
                    Text("· ${hoyOrders.size} cobrados", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextSoft)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(userEmail, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextFaint)
                }
                Spacer(Modifier.width(8.dp))
                ConnectionPill(offline)
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Lp.Divider.copy(alpha = 0.6f), Color.Transparent))
                )
            )
            Spacer(Modifier.height(20.dp))
            Crossfade(targetState = section, animationSpec = tween(180)) { s ->
                when (s) {
                    Section.MESAS -> MesasView(tables, orders, now, onCobrar = { orderToCobrar = it })
                    Section.PEDIDOS -> PedidosView(orders, now, onCobrar = { orderToCobrar = it })
                    Section.COCINA -> KdsView(orders, orderRepo)
                    Section.PRODUCTOS -> ProductosView(products, productRepo)
                    Section.REPORTE -> ReporteView(orders, userEmail)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LpTooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Box(
                Modifier.background(Color(0xFF22222E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextSoft)
            }
        },
        delayMillis = 500,
        content = content
    )
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tint: Color = if (selected) Lp.Amber else Lp.TextFaint,
    badge: Int = 0,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        when {
            selected -> Lp.Amber.copy(alpha = 0.12f)
            hovered -> Color.White.copy(alpha = 0.05f)
            else -> Color.Transparent
        },
        tween(120)
    )
    val railHeight by animateDpAsState(if (selected) 28.dp else 0.dp, tween(200))

    Box(Modifier.width(96.dp), contentAlignment = Alignment.Center) {
        // Rail activo pegado al borde izquierdo del sidebar
        Box(
            Modifier.align(Alignment.CenterStart).width(3.dp).height(railHeight)
                .background(Lp.Amber, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
        )
        Column(
            Modifier.width(72.dp).clip(RoundedCornerShape(14.dp))
                .background(bg)
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { onClick() }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
                if (badge > 0) {
                    Box(
                        Modifier.offset(x = 12.dp, y = (-5).dp).size(15.dp)
                            .background(Lp.Coral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badge > 9) "9+" else "$badge",
                            fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Lp.OnAccent
                        )
                    }
                }
            }
            Text(
                label, fontSize = 10.sp, color = tint,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ConnectionPill(offline: Boolean) {
    val color = if (offline) Lp.Red else Lp.Green
    // Punto estatico a proposito: una animacion infinita redibujaria el header
    // a cada frame durante toda la noche (el panel queda abierto 24/7).
    Row(
        Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(
            if (offline) "SIN CONEXIÓN — reconectando" else "EN LÍNEA",
            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = color
        )
    }
}

// ==================== Mesas ====================

internal fun tableLabel(n: Int) = "M" + n.toString().padStart(2, '0')

// Misma regla que la app Android (normalizeTables): una mesa esta OCUPADA solo si
// tiene un pedido ACTIVO; un status OCUPADA viejo en Firebase sin pedido se muestra LIBRE.
internal val ACTIVE_STATUSES = setOf(
    OrderStatus.PENDING, OrderStatus.ENVIADO, OrderStatus.ACEPTADO,
    OrderStatus.EN_PREPARACION, OrderStatus.LISTO, OrderStatus.ENTREGADO
)

@Composable
private fun MesasView(tables: List<Table>, orders: List<Order>, now: Long, onCobrar: (Order) -> Unit) {
    val activeByTable = activeOrdersByTable(orders)
    val visible = tables.filter { it.number in 1..8 }.sortedBy { it.number }
    val occupiedCount = visible.count { activeByTable[it.id] != null }
    val openTotal = activeByTable.values.sumOf { SalesCalculator.orderTotal(it) }

    Column {
        // Franja de resumen + mini-mapa del salon
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip("${visible.size - occupiedCount}", "LIBRES", Lp.Green)
                StatChip("$occupiedCount", "OCUPADAS", Lp.Warn)
                StatChip(Money(openTotal).formatted(), "ABIERTO EN MESAS", Lp.Amber)
            }
            Spacer(Modifier.weight(1f))
            // Mini-mapa: el estado del salon completo de un vistazo
            Row(
                Modifier.background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("SALÓN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Lp.TextMuted)
                Spacer(Modifier.width(2.dp))
                visible.forEach { table ->
                    val order = activeByTable[table.id]
                    val late = order != null && (now - order.createdAt) / 60000 >= 45
                    val color = when {
                        late -> Lp.Red
                        order != null -> Lp.Warn
                        else -> Lp.Green
                    }
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                            .background(if (order != null) color.copy(alpha = 0.16f) else Color.Transparent)
                            .border(1.dp, color.copy(alpha = if (order != null) 0.6f else 0.4f), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${table.number}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = color)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visible) { table ->
                val activeOrder = activeByTable[table.id]
                // Igual que el celular: solo un pedido activo marca la mesa como ocupada.
                if (activeOrder != null) {
                    OccupiedTableCard(activeOrder, now, onCobrar)
                } else {
                    FreeTableCard(table)
                }
            }
        }
    }
}

/** Mesa "apagada": numero en contorno + puntitos de asientos. */
@Composable
private fun FreeTableCard(table: Table) {
    Box(Modifier.lpCard(16.dp).lpHover(0.03f)) {
        Box(
            Modifier.align(Alignment.CenterStart).width(3.dp).height(28.dp)
                .background(Lp.Green.copy(alpha = 0.4f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
        )
        Column(Modifier.heightIn(min = 176.dp).fillMaxWidth().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    tableLabel(table.number), fontFamily = BebasFamily,
                    fontSize = 54.sp, lineHeight = 48.sp,
                    // Numero en contorno (mesa "apagada"), como el logo PREVIA
                    color = Color.White.copy(alpha = 0.35f),
                    style = TextStyle(drawStyle = Stroke(width = 1.5f))
                )
                StatusPill("LIBRE", Lp.Green)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(table.capacity.coerceIn(1, 8)) {
                    Box(Modifier.size(7.dp).background(Lp.Green.copy(alpha = 0.5f), CircleShape))
                }
                Spacer(Modifier.width(3.dp))
                Text("${table.capacity} asientos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextDim)
            }
            Spacer(Modifier.height(6.dp))
            Text("Sin pedido activo", color = Lp.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Mesa "encendida": anillo de tiempo, mozo, preview de platos, total y cobro. */
@Composable
private fun OccupiedTableCard(order: Order, now: Long, onCobrar: (Order) -> Unit) {
    val minutes = ((now - order.createdAt) / 60000).coerceAtLeast(0)
    val late = minutes >= 45
    val accent = if (late) Lp.Red else Lp.Warn
    Box(Modifier.lpCard(16.dp, borderTint = accent.copy(alpha = 0.4f)).lpHover(0.03f)) {
        Box(
            Modifier.align(Alignment.CenterStart).width(3.dp).height(28.dp)
                .background(accent, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
        )
        Column(Modifier.heightIn(min = 176.dp).fillMaxWidth().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    tableLabel(order.tableNumber), fontFamily = BebasFamily,
                    fontSize = 54.sp, lineHeight = 48.sp, color = Lp.Text
                )
                TimeRing(minutes)
            }
            Spacer(Modifier.height(6.dp))
            // Mozo con avatar de iniciales
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier.size(20.dp).background(Lp.Coral.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mozoInitials(order.waiterName), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Lp.Coral)
                }
                Text(
                    "${order.waiterName?.takeIf { it.isNotBlank() } ?: "Sin mozo"} · ${order.items.sumOf { it.quantity }} items",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextDim
                )
            }
            Spacer(Modifier.height(8.dp))
            // Preview de platos: primeros 2 + "N mas"
            order.items.take(2).forEach { item ->
                Text("${item.quantity}× ${item.productName}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextSoft, maxLines = 1)
            }
            if (order.items.size > 2) {
                Text("+${order.items.size - 2} más", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextMuted)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    Money(SalesCalculator.orderTotal(order)).formatted(),
                    fontFamily = BebasFamily, fontSize = 28.sp, color = Lp.Amber, style = TabularNumbers
                )
                if (order.canBeCharged()) {
                    Box(
                        Modifier.height(30.dp).clip(RoundedCornerShape(9.dp))
                            .background(Brush.linearGradient(listOf(Lp.Amber, Lp.AmberDeep)))
                            .lpHover(0.10f)
                            .clickable { onCobrar(order) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("COBRAR", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = Lp.OnAccent)
                    }
                } else {
                    StatusPill(kdsStatusLabel(order.status), kdsStatusColor(order.status))
                }
            }
        }
    }
}

/** Anillo de tiempo: se llena con los minutos (rojo al pasar 45'). */
@Composable
private fun TimeRing(minutes: Long) {
    val late = minutes >= 45
    val color = if (late) Lp.Red else Lp.Amber
    val fraction = (minutes.toFloat() / 60f).coerceIn(0.05f, 1f)
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(46.dp)) {
            val strokePx = 4.dp.toPx()
            val d = size.minDimension - strokePx
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = androidx.compose.ui.geometry.Size(d, d),
                style = Stroke(width = strokePx)
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                topLeft = topLeft, size = androidx.compose.ui.geometry.Size(d, d),
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Text(
            "$minutes'", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
            color = if (late) Lp.Red else Lp.Text, style = TabularNumbers
        )
    }
}

/** Iniciales del mozo para el avatar (max 2 letras). */
private fun mozoInitials(name: String?): String {
    val clean = name?.trim().orEmpty()
    if (clean.isBlank()) return "—"
    val parts = clean.split(" ").filter { it.isNotBlank() }
    return if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}".uppercase()
    else clean.take(2).uppercase()
}

// ==================== Pedidos ====================

@Composable
private fun PedidosView(orders: List<Order>, now: Long, onCobrar: (Order) -> Unit) {
    // FIFO: en caja se despacha lo mas viejo primero (mismo criterio que cocina)
    val active = orders
        .filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
        .sortedBy { it.createdAt }

    if (active.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Lp.TextMuted, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text("No hay pedidos activos ahora mismo.", color = Lp.TextDim, fontWeight = FontWeight.SemiBold)
        }
        return
    }

    Column {
        // Cuanta plata esta en la calle, siempre visible
        Row(Modifier.fillMaxWidth().padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${active.size} ACTIVOS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Lp.TextDim)
            Spacer(Modifier.weight(1f))
            Text("POR COBRAR: ", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Lp.TextDim)
            Text(
                Money(active.sumOf { SalesCalculator.orderTotal(it) }).formatted(),
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Lp.Amber, style = TabularNumbers
            )
        }
        Spacer(Modifier.height(12.dp))

        val gridState = rememberLazyGridState()
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(480.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize().padding(end = 14.dp)
            ) {
                items(active, key = { it.id }) { order ->
                    Column(Modifier.lpCard(16.dp).lpHover(0.03f).fillMaxWidth().padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    tableLabel(order.tableNumber), fontFamily = BebasFamily,
                                    fontSize = 26.sp, color = Lp.Text
                                )
                                StatusPill(kdsStatusLabel(order.status), kdsStatusColor(order.status))
                            }
                            Text(
                                Money(SalesCalculator.orderTotal(order)).formatted(),
                                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Lp.Amber, style = TabularNumbers
                            )
                        }
                        Text(
                            "${hourFormat.format(Date(order.createdAt))} · hace ${((now - order.createdAt) / 60000).coerceAtLeast(0)} min" +
                                " · ${order.waiterName?.takeIf { it.isNotBlank() } ?: "sin mozo"}",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextDim, style = TabularNumbers
                        )
                        Spacer(Modifier.height(8.dp))
                        order.items.forEach { item ->
                            Row {
                                Box(Modifier.width(34.dp)) {
                                    Text("${item.quantity} ×", color = Lp.TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, style = TabularNumbers)
                                }
                                Text(item.productName, color = Lp.TextSoft, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (!order.notes.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.background(Lp.Warn.copy(alpha = 0.12f), RoundedCornerShape(9.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text("Nota: ${order.notes}", color = Lp.Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (order.canBeCharged()) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier.height(42.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(Lp.Amber, Lp.AmberDeep)))
                                        .lpHover(0.10f)
                                        .clickable { onCobrar(order) }
                                        .padding(horizontal = 22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("COBRAR", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.5.sp, color = Lp.OnAccent)
                                }
                                OutlinedButton(
                                    onClick = {
                                        printOnDesktop(
                                            com.laprevia.restobar.data.printer.ReceiptFormatter().kitchenComanda(order)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Lp.FieldBorder),
                                    modifier = Modifier.height(42.dp).pointerHoverIcon(PointerIcon.Hand)
                                ) { Text("Comanda", color = Lp.TextSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(gridState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}
