// presentation/screens/chef/QuickAction.kt
package com.laprevia.restobar.presentation.screens.chef.components

import androidx.compose.ui.graphics.Color
import com.laprevia.restobar.data.model.OrderStatus

data class QuickAction(
    val label: String,
    val status: OrderStatus,
    val color: Color
)