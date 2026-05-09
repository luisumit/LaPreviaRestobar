// AppNavigation.kt - VERSIÓN CON AUTENTICACIÓN FIREBASE
package com.laprevia.restobar.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.laprevia.restobar.data.model.UserRole
import com.laprevia.restobar.presentation.screens.admin.AdminMainScreen
import com.laprevia.restobar.presentation.screens.chef.ChefMainScreen
import com.laprevia.restobar.presentation.screens.chef.InventoryScreen as ChefInventoryScreen
import com.laprevia.restobar.presentation.screens.chef.OrdersScreen as ChefOrdersScreen
import com.laprevia.restobar.presentation.screens.login.LoginScreen
import com.laprevia.restobar.presentation.screens.waiter.InventoryScreen as WaiterInventoryScreen
import com.laprevia.restobar.presentation.screens.waiter.OrdersScreen as WaiterOrdersScreen
import com.laprevia.restobar.presentation.screens.waiter.ProductsScreen
import com.laprevia.restobar.presentation.screens.waiter.TablesScreen
import com.laprevia.restobar.presentation.screens.waiter.WaiterMainScreen
import com.laprevia.restobar.presentation.screens.waiter.TableDetailsScreen
import com.laprevia.restobar.presentation.viewmodel.AdminViewModel
import com.laprevia.restobar.presentation.viewmodel.ChefViewModel
import com.laprevia.restobar.presentation.viewmodel.InventoryViewModel
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()

    // ✅ CORRECCIÓN 1: Usar collectAsState() en lugar de .value directamente
    val currentUser by loginViewModel.currentUser.collectAsState()
    val userRole by loginViewModel.userRole.collectAsState()

    // Estados para controlar autenticación
    var isAuthenticated by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Observar cambios en la autenticación
    LaunchedEffect(currentUser, userRole) {
        if (currentUser != null && userRole != null) {
            // Usuario autenticado con rol
            isAuthenticated = true
            isLoading = false

            // Navegar automáticamente según el rol
            when (userRole) {
                UserRole.MESERO -> {
                    if (navController.currentDestination?.route != "waiter_main") {
                        navController.navigate("waiter_main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                UserRole.COCINERO -> {
                    if (navController.currentDestination?.route != "chef_main") {
                        navController.navigate("chef_main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                UserRole.ADMIN -> {
                    if (navController.currentDestination?.route != "admin_main") {
                        navController.navigate("admin_main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                else -> {
                    // Rol no reconocido
                    isAuthenticated = false
                }
            }
        } else if (currentUser == null) {
            // No hay usuario autenticado
            isAuthenticated = false
            isLoading = false

            // Asegurarse de estar en login
            if (navController.currentDestination?.route != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Determinar start destination
    val startDestination = if (isAuthenticated && userRole != null) {
        when (userRole) {
            UserRole.MESERO -> "waiter_main"
            UserRole.COCINERO -> "chef_main"
            UserRole.ADMIN -> "admin_main"
            else -> "login"
        }
    } else {
        "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ✅ PANTALLA DE LOGIN
        composable("login") {
            LoginScreen(
                viewModel = loginViewModel,
                onRoleSelected = { role ->
                    // Para modo rápido sin autenticación (mantener compatibilidad)
                    when (role) {
                        UserRole.MESERO -> navController.navigate("waiter_main") {
                            popUpTo("login") { inclusive = true }
                        }
                        UserRole.COCINERO -> navController.navigate("chef_main") {
                            popUpTo("login") { inclusive = true }
                        }
                        UserRole.ADMIN -> navController.navigate("admin_main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onAuthenticationSuccess = { role ->
                    // Para autenticación Firebase exitosa
                    isAuthenticated = true
                    when (role) {
                        UserRole.MESERO -> navController.navigate("waiter_main") {
                            popUpTo("login") { inclusive = true }
                        }
                        UserRole.COCINERO -> navController.navigate("chef_main") {
                            popUpTo("login") { inclusive = true }
                        }
                        UserRole.ADMIN -> navController.navigate("admin_main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ✅ RUTAS PROTEGIDAS DEL MESERO
        composable("waiter_main") {
            if (isAuthenticated && userRole == UserRole.MESERO) {
                val waiterViewModel: WaiterViewModel = hiltViewModel()
                // ✅ CORRECCIÓN 2: Verificar si WaiterMainScreen acepta onLogout
                // Si no lo acepta, crear una versión que sí lo haga o manejar logout de otra forma
                WaiterMainScreen(
                    navController = navController,
                    viewModel = waiterViewModel,
                    // Si WaiterMainScreen no tiene onLogout, quita este parámetro o actualiza la pantalla
                    onLogout = {
                        loginViewModel.signOut()
                        isAuthenticated = false
                        navController.navigate("login") {
                            popUpTo("waiter_main") { inclusive = true }
                        }
                    }
                )
            } else {
                // Redirigir al login si no está autorizado
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("waiter_main") { inclusive = true }
                    }
                }
            }
        }

        composable("tables") {
            if (isAuthenticated && userRole == UserRole.MESERO) {
                val waiterViewModel: WaiterViewModel = hiltViewModel()
                TablesScreen(
                    navController = navController,
                    viewModel = waiterViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("tables") { inclusive = true }
                    }
                }
            }
        }

        composable("table_details/{tableId}") { backStackEntry ->
            if (isAuthenticated && userRole == UserRole.MESERO) {
                val waiterViewModel: WaiterViewModel = hiltViewModel()
                val tableId = backStackEntry.arguments?.getString("tableId")
                TableDetailsScreen(
                    navController = navController,
                    tableId = tableId,
                    viewModel = waiterViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("table_details/{tableId}") { inclusive = true }
                    }
                }
            }
        }

        composable("orders") {
            if (isAuthenticated && userRole == UserRole.MESERO) {
                val waiterViewModel: WaiterViewModel = hiltViewModel()
                WaiterOrdersScreen(
                    navController = navController,
                    viewModel = waiterViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("orders") { inclusive = true }
                    }
                }
            }
        }

        composable("products") {
            if (isAuthenticated && userRole == UserRole.MESERO) {
                val waiterViewModel: WaiterViewModel = hiltViewModel()
                ProductsScreen(
                    navController = navController,
                    viewModel = waiterViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("products") { inclusive = true }
                    }
                }
            }
        }

        composable("inventory") {
            if (isAuthenticated && userRole == UserRole.MESERO) {
                val waiterViewModel: WaiterViewModel = hiltViewModel()
                WaiterInventoryScreen(
                    navController = navController,
                    viewModel = waiterViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("inventory") { inclusive = true }
                    }
                }
            }
        }

        // ✅ RUTAS PROTEGIDAS DEL CHEF
        // ✅ RUTAS PROTEGIDAS DEL CHEF - VERSIÓN CORREGIDA
        composable("chef_main") {
            if (isAuthenticated && userRole == UserRole.COCINERO) {
                ChefMainScreen(
                    onBack = {
                        navController.popBackStack() // ✅ SOLO PARA RETROCESO NORMAL
                    },
                    onLogout = { // ✅ AGREGAR ESTE PARÁMETRO
                        timber.log.Timber.d("🚪 AppNavigation: Cocinero cerrando sesión")
                        loginViewModel.signOut()
                        isAuthenticated = false
                        navController.navigate("login") {
                            popUpTo("chef_main") { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("chef_main") { inclusive = true }
                    }
                }
            }
        }

        // ✅ RUTAS PROTEGIDAS DEL ADMIN - SOLO ESTA PARTE CAMBIÉ
        composable("admin_main") {
            if (isAuthenticated && userRole == UserRole.ADMIN) {
                val adminViewModel: AdminViewModel = hiltViewModel()
                AdminMainScreen(
                    viewModel = adminViewModel,
                    loginViewModel = loginViewModel, // ✅ AGREGAR ESTO
                    onBack = {
                        navController.popBackStack() // ✅ SOLO PARA RETROCESO NORMAL
                    },
                    onLogout = { // ✅ AGREGAR ESTE PARÁMETRO
                        loginViewModel.signOut()
                        isAuthenticated = false
                        navController.navigate("login") {
                            popUpTo("admin_main") { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("admin_main") { inclusive = true }
                    }
                }
            }
        }
    }
}
