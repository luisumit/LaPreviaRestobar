package com.laprevia.restobar.domain

object ProductValidation {
    private val decimalPattern = Regex("(?:\\d+(?:\\.\\d*)?|\\.\\d+)")

    fun parseDecimal(input: String): Double? {
        val normalized = input.trim().replace(',', '.')
        if (!decimalPattern.matches(normalized)) return null
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    }

    fun filterNumericInput(input: String): String = buildString {
        var decimalSeparatorAdded = false
        input.forEach { character ->
            when {
                character.isDigit() -> append(character)
                (character == '.' || character == ',') && !decimalSeparatorAdded -> {
                    append('.')
                    decimalSeparatorAdded = true
                }
            }
        }
    }
}
