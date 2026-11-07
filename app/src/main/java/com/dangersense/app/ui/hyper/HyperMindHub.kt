package com.dangersense.app.ui.hyper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangersense.app.ui.theme.OneUI

@Composable
fun HyperMindHub(
    db: Float,
    bpm: Int,
    screenMinutes: Int,
    localSource: String,
    localAction: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HyperOS.SquircleLg)
            .background(HyperOS.Surface)
            .border(1.dp, OneUI.GraphiteGreenLine.copy(alpha = 0.55f), HyperOS.SquircleLg)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HyperMind",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OneUI.TextPrimary
            )
            Text(
                localSource,
                fontSize = 12.sp,
                color = OneUI.TextSecondary
            )
        }

        Spacer(Modifier.height(16.dp))

        DecibelMeter(db = db)

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusCapsule(
                "bpm",
                if (bpm > 0) "$bpm" else "—",
                accent = HyperOS.VitalGreen
            )
            StatusCapsule(
                "tela",
                "$screenMinutes min",
                accent = HyperOS.AlertOrange
            )
        }

        if (localAction.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Ação preventiva: $localAction",
                fontSize = 13.sp,
                color = OneUI.Aurora,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OneUI.GraphiteGreen)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
