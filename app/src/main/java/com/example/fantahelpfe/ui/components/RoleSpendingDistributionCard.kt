package com.example.fantahelpfe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun RoleSpendingDistributionCard(
    title: String = "📊 Role Spending Allocation",
    gkSpend: Int,
    defSpend: Int,
    midSpend: Int,
    attSpend: Int,
    totalSpend: Int,
    modifier: Modifier = Modifier
) {
    val effectiveTotal = totalSpend.coerceAtLeast(1)
    val gkRatio = (gkSpend.toFloat() / effectiveTotal).coerceIn(0f, 1f)
    val defRatio = (defSpend.toFloat() / effectiveTotal).coerceIn(0f, 1f)
    val midRatio = (midSpend.toFloat() / effectiveTotal).coerceIn(0f, 1f)
    val attRatio = (attSpend.toFloat() / effectiveTotal).coerceIn(0f, 1f)

    val gkPct = ((gkSpend.toDouble() / effectiveTotal) * 100).roundToInt()
    val defPct = ((defSpend.toDouble() / effectiveTotal) * 100).roundToInt()
    val midPct = ((midSpend.toDouble() / effectiveTotal) * 100).roundToInt()
    val attPct = ((attSpend.toDouble() / effectiveTotal) * 100).roundToInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Total: $totalSpend CR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Multi-segment horizontal proportional bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            ) {
                if (gkRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(gkRatio.coerceAtLeast(0.005f))
                            .fillMaxHeight()
                            .background(Color(0xFFFFB74D))
                    )
                }
                if (defRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(defRatio.coerceAtLeast(0.005f))
                            .fillMaxHeight()
                            .background(Color(0xFF64B5F6))
                    )
                }
                if (midRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(midRatio.coerceAtLeast(0.005f))
                            .fillMaxHeight()
                            .background(Color(0xFF81C784))
                    )
                }
                if (attRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(attRatio.coerceAtLeast(0.005f))
                            .fillMaxHeight()
                            .background(Color(0xE5E57373))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4 Role spending pills with spend and percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RoleSpendingPill("🧤 P", "$gkPct%", "$gkSpend CR", Color(0xFFFFB74D), Modifier.weight(1f))
                RoleSpendingPill("🛡️ D", "$defPct%", "$defSpend CR", Color(0xFF64B5F6), Modifier.weight(1f))
                RoleSpendingPill("🎯 C", "$midPct%", "$midSpend CR", Color(0xFF81C784), Modifier.weight(1f))
                RoleSpendingPill("🚀 A", "$attPct%", "$attSpend CR", Color(0xE5E57373), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoleSpendingPill(
    role: String,
    percentText: String,
    spendText: String,
    roleColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = roleColor.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    role,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = roleColor
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    percentText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                spendText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
