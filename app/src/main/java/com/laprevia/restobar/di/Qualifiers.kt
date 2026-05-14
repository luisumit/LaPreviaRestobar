// app/src/main/java/com/laprevia/restobar/di/Qualifiers.kt
package com.laprevia.restobar.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProductsReference

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TablesReference

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OrdersReference

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InventoryReference


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebSocketUrl
