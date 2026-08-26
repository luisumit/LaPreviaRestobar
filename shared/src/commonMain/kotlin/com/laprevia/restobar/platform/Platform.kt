package com.laprevia.restobar.platform

/**
 * APIs de plataforma que en Android/Desktop vienen de la JVM (`System`, `UUID`) pero que
 * en código común (multiplataforma) deben declararse con `expect` y resolverse por target.
 */
expect fun currentTimeMillis(): Long

expect fun randomUuid(): String
