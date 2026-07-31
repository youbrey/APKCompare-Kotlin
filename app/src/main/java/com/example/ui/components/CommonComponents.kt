package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiffType
import com.example.data.model.RiskLevel
import com.example.ui.theme.*

@Composable
fun RiskBadge(riskLevel: RiskLevel, score: Int, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (riskLevel) {
        RiskLevel.LOW -> Triple(ProEmeraldLight, ProEmerald, "BERISIKO RENDAH")
        RiskLevel.MEDIUM -> Triple(ProAmberLight, ProAmber, "RISIKO SEDANG")
        RiskLevel.HIGH -> Triple(ProRoseLight, ProRose, "RISIKO TINGGI")
        RiskLevel.CRITICAL -> Triple(ProRoseLight, ProRose, "RISIKO KRITIS")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ($score/100)",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun DiffTypeBadge(diffType: DiffType) {
    val (bgColor, textColor, symbol) = when (diffType) {
        DiffType.ADDED -> Triple(ProEmeraldLight, ProEmerald, "+ DITAMBAHKAN")
        DiffType.REMOVED -> Triple(ProRoseLight, ProRose, "- DIHAPUS")
        DiffType.MODIFIED -> Triple(ProAmberLight, ProAmber, "~ DIMODIFIKASI")
        DiffType.UNCHANGED -> Triple(ProSlate200, ProSlate500, "=")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = symbol,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ProWhiteSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ProBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    color = ProSlate500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = ProBlue600,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = ProSlate500,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SecurityCategoryBadge(categoryName: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ProBlue100,
        border = androidx.compose.foundation.BorderStroke(1.dp, ProBlue600.copy(alpha = 0.3f))
    ) {
        Text(
            text = "🔒 $categoryName",
            color = ProBlue700,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

