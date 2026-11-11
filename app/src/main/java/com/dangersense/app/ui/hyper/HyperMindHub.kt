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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HyperOS.SquircleLg)
            .background(HyperOS.Surface)
            .border(1.dp, OneUI.GraphiteGreenLine.copy(alpha = 0.45f), HyperOS.SquircleLg)
            .padding(22.dp)
    ) {
        Text(
            "Microfone",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = OneUI.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        DecibelMeter(db = db)
    }
}
