package com.laprevia.restobar.platform

import java.util.UUID

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun randomUuid(): String = UUID.randomUUID().toString()

actual fun hourOfTimestamp(timestamp: Long): Int =
    java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        .get(java.util.Calendar.HOUR_OF_DAY)
