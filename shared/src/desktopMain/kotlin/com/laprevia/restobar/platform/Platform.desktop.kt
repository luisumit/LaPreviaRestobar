package com.laprevia.restobar.platform

import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun randomUuid(): String = UUID.randomUUID().toString()
