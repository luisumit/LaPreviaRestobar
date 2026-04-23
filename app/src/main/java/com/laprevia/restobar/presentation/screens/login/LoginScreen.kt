package com.laprevia.restobar.presentation.screens.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laprevia.restobar.data.model.UserRole
import com.laprevia.restobar.presentation.theme.PrimaryRed
import com.laprevia.restobar.presentation.theme.SecondaryAmber
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel
import com.laprevia.restobar.presentation.viewmodel.LoginUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onRoleSelected: (UserRole) -> Unit,
    onAuthenticationSuccess: (UserRole) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAnimation by remember { mutableStateOf(false) }

    // ✅ ELIMINADO: lastProcessedState - ya no es necesario
    // var lastProcessedState by remember { mutableStateOf<LoginUiState?>(null) }

    LaunchedEffect(Unit) {
        delay(500)
        showAnimation = true
    }

    // ✅ LAUNCHED EFFECT SIMPLIFICADO: Solo para logs, NO para navegación
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Authenticated -> {
                val role = (uiState as LoginUiState.Authenticated).role
                println("🔍 LoginScreen: Usuario autenticado como $role")
                // ✅ NO navegar aquí - la navegación se maneja en AppNavigation
            }
            is LoginUiState.RoleSelected -> {
                val role = (uiState as LoginUiState.RoleSelected).role
                println("🔍 LoginScreen: Rol seleccionado $role")
                // ✅ NO navegar aquí - la navegación se maneja en AppNavigation
            }
            else -> {
                // No hacer nada para otros estados
            }
        }
    }

    // Fondo con gradiente animado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
    ) {
        BackgroundElements()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            when (uiState) {
                is LoginUiState.Loading -> {
                    LoadingContent()
                }
                is LoginUiState.NoRoleSelected -> {
                    AnimatedRoleSelectionContent(
                        showAnimation = showAnimation,
                        onRoleSelected = { role ->
                            if (!isLoading) {
                                // ✅ SOLO llamar al ViewModel, NO navegar
                                viewModel.selectRole(role)
                            }
                        },
                        isLoading = isLoading
                    )
                }
                is LoginUiState.RoleSelected -> {
                    val role = (uiState as LoginUiState.RoleSelected).role
                    EmailLoginForm(
                        role = role,
                        viewModel = viewModel,
                        onBack = {
                            if (!isLoading) {
                                viewModel.navigateBack()
                            }
                        },
                        onAuthenticationSuccess = {
                            // ✅ VACÍO - la navegación se maneja en AppNavigation
                        }
                    )
                }
                is LoginUiState.Authenticated -> {
                    // ✅ CONTENIDO MEJORADO: Sin mensaje de "Redirigiendo..."
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Iniciando sesión...",
                            color = Color.White
                        )
                    }
                }
                is LoginUiState.Error -> {
                    ErrorContent(
                        errorMessage = (uiState as LoginUiState.Error).message,
                        onRetry = { viewModel.clearError() },
                        onBack = {
                            viewModel.clearError()
                            viewModel.navigateBack()
                        }
                    )
                }
            }
        }

        // Indicador de loading global
        if (isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PrimaryRed,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Botón para volver
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryRed.copy(alpha = 0.2f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                color = PrimaryRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Error de Autenticación",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Reintentar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = "Volver al inicio",
                color = Color.White
            )
        }
    }
}

@Composable
private fun BackgroundElements() {
    // Burbujas decorativas
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(40.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    ) {
        // Burbuja 1
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryRed.copy(alpha = 0.3f),
                            PrimaryRed.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Burbuja 2
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 300.dp, y = 400.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SecondaryAmber.copy(alpha = 0.2f),
                            SecondaryAmber.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Burbuja 3
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = 250.dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF388E3C).copy(alpha = 0.15f),
                            Color(0xFF388E3C).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo animado
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryRed, SecondaryAmber)
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Cargando",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        CircularProgressIndicator(
            color = PrimaryRed,
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Iniciando La Previa",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Preparando tu experiencia...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AnimatedRoleSelectionContent(
    showAnimation: Boolean,
    onRoleSelected: (UserRole) -> Unit,
    isLoading: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedHeaderSection(showAnimation = showAnimation)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            AnimatedTitle(showAnimation = showAnimation)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            AnimatedRoleSelectionButton(
                showAnimation = showAnimation,
                role = UserRole.MESERO,
                description = "Gestionar mesas, tomar pedidos y atender clientes",
                icon = Icons.Default.WineBar,
                delay = 300,
                onClick = {
                    if (!isLoading) {
                        onRoleSelected(UserRole.MESERO)
                    }
                },
                isLoading = isLoading
            )
        }

        item {
            AnimatedRoleSelectionButton(
                showAnimation = showAnimation,
                role = UserRole.COCINERO,
                description = "Preparar pedidos, gestionar cocina y calidad",
                icon = Icons.Default.RestaurantMenu,
                delay = 500,
                onClick = {
                    if (!isLoading) {
                        onRoleSelected(UserRole.COCINERO)
                    }
                },
                isLoading = isLoading
            )
        }

        item {
            AnimatedRoleSelectionButton(
                showAnimation = showAnimation,
                role = UserRole.ADMIN,
                description = "Gestionar productos, precios, inventario y configuración",
                icon = Icons.Default.Settings,
                delay = 700,
                onClick = {
                    if (!isLoading) {
                        onRoleSelected(UserRole.ADMIN)
                    }
                },
                isLoading = isLoading
            )
        }

        // Nueva sección para login tradicional
        item {
            Spacer(modifier = Modifier.height(32.dp))
            TraditionalLoginOption(showAnimation = showAnimation)
        }
    }
}

@Composable
private fun TraditionalLoginOption(showAnimation: Boolean) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            delay(1000)
            alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 600))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alphaAnim.value),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿Ya tienes una cuenta?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Inicia sesión con email y contraseña",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AnimatedHeaderSection(showAnimation: Boolean) {
    val translationY = remember { Animatable(100f) }

    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .offset(y = translationY.value.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo con efecto glassmorphism
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    clip = false
                )
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.3f, size.height * 0.3f),
                                radius = size.minDimension * 0.8f
                            ),
                            blendMode = BlendMode.Overlay
                        )
                    }
                }
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryRed, SecondaryAmber),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Logo La Previa",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nombre del restaurante
        Text(
            text = "EL PATIO DE LA CASA",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "RESTAURANTE.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        // Línea decorativa
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PrimaryRed, SecondaryAmber)
                    )
                )
        )
    }
}

@Composable
private fun AnimatedTitle(showAnimation: Boolean) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            delay(200)
            alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 800))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alphaAnim.value),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿QUIÉN ERES?",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecciona tu rol para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedRoleSelectionButton(
    showAnimation: Boolean,
    role: UserRole,
    description: String,
    icon: ImageVector,
    delay: Long,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    val translationX = remember { Animatable(if (role == UserRole.MESERO) -100f else if (role == UserRole.COCINERO) 100f else -100f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            delay(delay)
            translationX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 600))
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = translationX.value.dp)
            .alpha(alphaAnim.value)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        enabled = !isLoading
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            // Efecto de borde gradiente
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryRed.copy(alpha = 0.3f),
                                SecondaryAmber.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Icono con fondo gradiente
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryRed, SecondaryAmber),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Texto
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = when (role) {
                            UserRole.MESERO -> "MESERO/A"
                            UserRole.COCINERO -> "COCINERO/A"
                            UserRole.ADMIN -> "ADMINISTRADOR/A"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }

                // Flecha indicadora
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Seleccionar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailLoginForm(
    role: UserRole,
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onAuthenticationSuccess: (UserRole) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Botón para volver
        IconButton(
            onClick = {
                if (!isLoading) {
                    onBack()
                }
            },
            modifier = Modifier.align(Alignment.Start),
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = if (isLoading) Color.White.copy(alpha = 0.5f) else Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Título
        Text(
            text = "Iniciar Sesión",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Como ${when (role) {
                UserRole.MESERO -> "Mesero/a"
                UserRole.COCINERO -> "Cocinero/a"
                UserRole.ADMIN -> "Administrador/a"
            }}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedBorderColor = PrimaryRed,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedLabelColor = Color.White.copy(alpha = 0.9f),
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Contraseña",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedBorderColor = PrimaryRed,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedLabelColor = Color.White.copy(alpha = 0.9f),
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botón de login
        Button(
            onClick = {
                if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                    viewModel.signInWithEmailAndPassword(
                        email = email,
                        password = password
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Iniciar Sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Texto informativo
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "O selecciona otro rol si no tienes cuenta",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}