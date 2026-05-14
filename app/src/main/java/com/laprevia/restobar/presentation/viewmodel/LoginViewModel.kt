package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.laprevia.restobar.data.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class LoginUiState {
    object Loading : LoginUiState()
    object NoRoleSelected : LoginUiState()
    data class RoleSelected(val role: UserRole) : LoginUiState()
    data class Authenticated(val role: UserRole, val user: FirebaseUser) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()

    // ✅ NUEVO: Control para evitar múltiples navegaciones
    private var hasNavigated = false

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            hasNavigated = false // ✅ Resetear flag de navegación

            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                _currentUser.value = currentUser
                getUserRole(currentUser.uid) { role ->
                    _userRole.value = role
                    // ✅ SOLO actualizar estado, NO navegar aquí
                    _uiState.value = LoginUiState.Authenticated(role, currentUser)
                    _isLoading.value = false
                    timber.log.Timber.d("✅ ViewModel: Usuario ya autenticado - $role")
                }
            } else {
                delay(1000)
                _uiState.value = LoginUiState.NoRoleSelected
                _isLoading.value = false
                _currentUser.value = null
                _userRole.value = null
                timber.log.Timber.d("✅ ViewModel: No hay usuario autenticado")
            }
        }
    }

    // ✅ MÉTODO MEJORADO: Con protección contra múltiples llamadas
    fun selectRole(role: UserRole) {
        viewModelScope.launch {
            // Verificar más específicamente si ya estamos en el mismo rol
            val currentState = _uiState.value
            if (currentState is LoginUiState.RoleSelected && currentState.role == role) {
                timber.log.Timber.d("🔄 ViewModel: Ya está seleccionado el rol $role")
                return@launch
            }

            timber.log.Timber.d("🔄 ViewModel: Seleccionando rol $role")
            hasNavigated = false // ✅ Resetear flag
            _userRole.value = role
            _uiState.value = LoginUiState.RoleSelected(role)
        }
    }

    // ✅ MÉTODO MEJORADO: Con verificación de estado actual
    fun navigateBack() {
        viewModelScope.launch {
            if (_uiState.value is LoginUiState.NoRoleSelected) {
                timber.log.Timber.d("🔄 ViewModel: Ya estamos en estado inicial")
                return@launch
            }

            timber.log.Timber.d("🔄 ViewModel: Volviendo al inicio")
            hasNavigated = false // ✅ Resetear flag
            _userRole.value = null
            _uiState.value = LoginUiState.NoRoleSelected
        }
    }

    // 🔐 MÉTODO PARA LOGIN - MEJORADO (sin parpadeo)
    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: (UserRole, FirebaseUser) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        // ✅ MEJORADO: Verificar si ya estamos cargando
        if (_isLoading.value) {
            timber.log.Timber.d("🔄 ViewModel: Login ya en progreso, ignorando...")
            return
        }

        _isLoading.value = true
        hasNavigated = false // ✅ Resetear flag

        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 ViewModel: Iniciando autenticación para $email")
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {
                    getUserRole(user.uid) { role ->
                        // ✅ MEJORADO: Setear ambas propiedades ANTES de cambiar estado
                        _currentUser.value = user
                        _userRole.value = role

                        // ✅ SOLO actualizar estado una vez
                        _uiState.value = LoginUiState.Authenticated(role, user)
                        _isLoading.value = false

                        timber.log.Timber.d("✅ ViewModel: Autenticación exitosa - $role")
                        onSuccess(role, user)
                    }
                } else {
                    // ✅ MEJORADO: Solo cambiar estado si no estamos ya en error
                    if (_uiState.value !is LoginUiState.Error) {
                        _uiState.value = LoginUiState.Error("Error en la autenticación")
                    }
                    _isLoading.value = false
                    timber.log.Timber.d("❌ ViewModel: Error - usuario es null")
                    onError("Error en la autenticación")
                }
            } catch (e: Exception) {
                // ✅ MEJORADO: Solo cambiar estado si no estamos ya en error
                if (_uiState.value !is LoginUiState.Error) {
                    _uiState.value = LoginUiState.Error("Error: ${e.message ?: "Error desconocido"}")
                }
                _isLoading.value = false
                timber.log.Timber.d("❌ ViewModel: Error en autenticación: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // 🔐 MÉTODO PARA REGISTRO - MEJORADO (sin parpadeo)
    fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        role: UserRole,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // ✅ MEJORADO: Verificar si ya estamos cargando
        if (_isLoading.value) {
            timber.log.Timber.d("🔄 ViewModel: Registro ya en progreso, ignorando...")
            return
        }

        _isLoading.value = true
        hasNavigated = false // ✅ Resetear flag

        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 ViewModel: Creando usuario $email con rol $role")
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {
                    saveUserRole(user.uid, role, user.email ?: "")
                    // ✅ MEJORADO: Setear propiedades ANTES de cambiar estado
                    _currentUser.value = user
                    _userRole.value = role

                    // ✅ SOLO actualizar estado una vez
                    _uiState.value = LoginUiState.Authenticated(role, user)
                    _isLoading.value = false

                    timber.log.Timber.d("✅ ViewModel: Usuario creado exitosamente - $role")
                    onSuccess()
                } else {
                    if (_uiState.value !is LoginUiState.Error) {
                        _uiState.value = LoginUiState.Error("Error creando usuario")
                    }
                    _isLoading.value = false
                    timber.log.Timber.d("❌ ViewModel: Error - usuario es null al crear")
                    onError("Error creando usuario")
                }
            } catch (e: Exception) {
                if (_uiState.value !is LoginUiState.Error) {
                    _uiState.value = LoginUiState.Error("Error: ${e.message ?: "Error desconocido"}")
                }
                _isLoading.value = false
                timber.log.Timber.d("❌ ViewModel: Error creando usuario: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // 🔐 MÉTODO PARA CERRAR SESIÓN - MEJORADO
    fun signOut() {
        viewModelScope.launch {
            // ✅ MEJORADO: Verificar estado actual
            if (_uiState.value is LoginUiState.NoRoleSelected) {
                timber.log.Timber.d("🔄 ViewModel: Ya estamos desconectados")
                return@launch
            }

            timber.log.Timber.d("🔄 ViewModel: Cerrando sesión...")
            firebaseAuth.signOut()
            // ✅ ACTUALIZADO: Limpiar ambas propiedades
            _currentUser.value = null
            _userRole.value = null
            _uiState.value = LoginUiState.NoRoleSelected
            hasNavigated = false // ✅ Resetear flag
            timber.log.Timber.d("✅ ViewModel: Sesión cerrada - usuario y rol limpiados")
        }
    }

    // 📋 OBTENER ROL DEL USUARIO - MEJORADO
    private fun getUserRole(userId: String, onRoleRetrieved: (UserRole) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId).child("role")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roleString = snapshot.getValue(String::class.java) ?: "MESERO"
                val role = UserRole.fromString(roleString)
                timber.log.Timber.d("✅ ViewModel: Rol obtenido de Firebase: $role")
                onRoleRetrieved(role)
            }

            override fun onCancelled(error: DatabaseError) {
                timber.log.Timber.d("❌ ViewModel: Error obteniendo rol: ${error.message}")
                onRoleRetrieved(UserRole.MESERO)
            }
        })
    }

    // 💾 GUARDAR ROL DEL USUARIO - MEJORADO
    private fun saveUserRole(userId: String, role: UserRole, email: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)

        val userData = mapOf(
            "role" to role.name,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        userRef.setValue(userData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                timber.log.Timber.d("✅ ViewModel: Rol de usuario guardado en Firebase: ${role.name} para $email")
            } else {
                timber.log.Timber.d("❌ ViewModel: Error guardando rol: ${task.exception?.message}")
            }
        }
    }

    // 🔄 MÉTODO PARA REVISAR AUTENTICACIÓN - MEJORADO
    fun checkAuthentication() {
        viewModelScope.launch {
            if (_isLoading.value) {
                timber.log.Timber.d("🔄 ViewModel: Ya estamos verificando autenticación")
                return@launch
            }
            checkCurrentUser()
        }
    }

    // 🧹 LIMPIAR ERRORES - MEJORADO
    fun clearError() {
        viewModelScope.launch {
            if (_uiState.value is LoginUiState.Error) {
                timber.log.Timber.d("🔄 ViewModel: Limpiando error")
                _uiState.value = LoginUiState.NoRoleSelected
            }
        }
    }

    // ✅ NUEVO: Método para obtener información del usuario actual
    fun getCurrentUserInfo(): String {
        return "Usuario: ${_currentUser.value?.email ?: "No autenticado"}, Rol: ${_userRole.value ?: "No definido"}"
    }

    // ✅ NUEVO: Método para verificar si hay un usuario autenticado
    fun isUserAuthenticated(): Boolean {
        return _currentUser.value != null && _userRole.value != null
    }

    // ✅ NUEVO: Método para marcar que ya se navegó (usado por AppNavigation)
    fun markAsNavigated() {
        hasNavigated = true
        timber.log.Timber.d("📍 ViewModel: Navegación marcada como completada")
    }

    // ✅ NUEVO: Verificar si ya se navegó
    fun hasNavigationOccurred(): Boolean {
        return hasNavigated
    }
}
