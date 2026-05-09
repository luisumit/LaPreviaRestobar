// UserRole.kt - VERSIÓN SEALED CLASS CORREGIDA
package com.laprevia.restobar.data.model

sealed class UserRole {
    object MESERO : UserRole()
    object COCINERO : UserRole()
    object ADMIN : UserRole()

    // Propiedad name para compatibilidad
    val name: String
        get() = when (this) {
            is MESERO -> "MESERO"
            is COCINERO -> "COCINERO"
            is ADMIN -> "ADMIN"
        }

    override fun toString(): String = name

    companion object {
        fun fromString(value: String): UserRole {
            return when (value.uppercase()) {
                "MESERO" -> MESERO
                "COCINERO" -> COCINERO
                "ADMIN" -> ADMIN
                else -> MESERO // Valor por defecto
            }
        }
    }
}
