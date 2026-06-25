package com.spondon.app.feature.donor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Crimson = Color(0xFFC0102A)
val CrimsonDeep = Color(0xFF8C0A1F)
val CrimsonSoft = Color(0xFFE8536B)
val Ink = Color(0xFF221416)
val Gold = Color(0xFFC9962E)
val GoldLight = Color(0xFFF0CD7A)
val GoldDark = Color(0xFF9C711E)
val Paper = Color(0xFFFFFDFB)

@Composable
fun DonationCertificate(
    modifier: Modifier = Modifier,
    recipientName: String = "Olivia Wilson",
    donationDate: String = "20 April 2026",
    hospitalName: String = "Dhaka Medical College Hospital",
    signatoryName: String = "Mark Johnson"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .shadow(elevation = 16.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(Paper)
            .clipToBounds()
    ) {
        // Decorative background elements
        CertificateBackgroundDecorations()

        // Borders
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .border(2.dp, Crimson)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .border(1.dp, Crimson)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp, horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Brand Logo & Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.spondon.app.R.drawable.logo),
                    contentDescription = "Spondon Logo",
                    modifier = Modifier.size(24.dp, 28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SPONDON",
                        color = CrimsonDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 4.sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = CrimsonSoft,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "স্পন্দন",
                        color = CrimsonSoft,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 2.dp) // Slight nudge for Bengali font optical alignment
                    )
                }
            }

            // Title
            Text(
                text = "CERTIFICATE",
                color = Crimson,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                letterSpacing = 3.sp,
                lineHeight = 36.sp
            )
            Text(
                text = "OF BLOOD DONATION",
                color = Ink.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "This certificate is presented to",
                color = Ink.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Recipient Name
            Text(
                text = recipientName,
                color = CrimsonDeep,
                fontFamily = FontFamily.Cursive,
                fontSize = 54.sp,
                lineHeight = 54.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .background(Color.Gray.copy(alpha = 0.4f))
                    .padding(bottom = 12.dp)
            )

            // Recognition Text
            Spacer(modifier = Modifier.height(12.dp))
            val recognitionText = buildAnnotatedString {
                append("in recognition of your generous contribution as a blood donor on ")
                withStyle(SpanStyle(color = Crimson, fontWeight = FontWeight.SemiBold)) {
                    append(donationDate)
                }
                append(", at ")
                withStyle(SpanStyle(color = Crimson, fontWeight = FontWeight.SemiBold)) {
                    append(hospitalName)
                }
                append(".\nYour selfless act has helped save lives and brought hope to those in need.\nThank you for being a ")
                withStyle(SpanStyle(color = Crimson, fontWeight = FontWeight.SemiBold)) {
                    append("true hero")
                }
                append(".")
            }
            Text(
                text = recognitionText,
                color = Ink,
                fontSize = 10.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Center Medal (above the date/authority text)
                CertificateMedal(
                    modifier = Modifier
                        .size(60.dp, 80.dp)
                        .offset(y = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left Foot Block
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text(
                            text = donationDate,
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val y = size.height + 4.dp.toPx()
                                    drawLine(
                                        Color.Gray.copy(alpha = 0.4f),
                                        Offset(0f, y),
                                        Offset(size.width * 1.5f, y),
                                        strokeWidth
                                    )
                                }
                                .padding(bottom = 6.dp)
                        )
                        Text(text = "Date of Donation", color = Ink.copy(alpha = 0.65f), fontSize = 9.sp)
                    }

                    // Empty spacer for the middle gap under the medal
                    Spacer(modifier = Modifier.weight(0.5f))

                    // Right Foot Block
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val y = size.height + 4.dp.toPx()
                                    drawLine(
                                        Color.Gray.copy(alpha = 0.4f),
                                        Offset(-size.width * 0.5f, y),
                                        Offset(size.width, y),
                                        strokeWidth
                                    )
                                }
                                .padding(bottom = 6.dp)
                        ) {
                            Text(text = "Spondon App", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(text = "Every Drop Counts", color = CrimsonSoft, fontSize = 8.sp, fontStyle = FontStyle.Italic)
                        }
                        Text(text = signatoryName, color = Crimson, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CertificateBackgroundDecorations() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val ecgPath = Path().apply {
            moveTo(0f, 70f)
            lineTo(40f, 70f)
            lineTo(55f, 70f)
            lineTo(65f, 20f)
            lineTo(75f, 120f)
            lineTo(85f, 70f)
            lineTo(100f, 70f)
            lineTo(600f, 70f)
        }

        // Top ECG strip
        drawPath(
            path = ecgPath,
            color = Crimson.copy(alpha = 0.16f),
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Repeat top strip horizontally
        translate(left = 600f) {
            drawPath(
                path = ecgPath,
                color = Crimson.copy(alpha = 0.16f),
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Bottom ECG strip
        val bottomEcgPath = Path().apply {
            moveTo(0f, 70f)
            lineTo(60f, 70f)
            lineTo(72f, 70f)
            lineTo(82f, 15f)
            lineTo(94f, 125f)
            lineTo(104f, 70f)
            lineTo(130f, 70f)
            lineTo(600f, 70f)
        }

        translate(top = size.height - 140f) {
            drawPath(
                path = bottomEcgPath,
                color = Crimson.copy(alpha = 0.16f),
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            translate(left = 600f) {
                drawPath(
                    path = bottomEcgPath,
                    color = Crimson.copy(alpha = 0.16f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }

    // Soft blur drops
    val dropPath = Path().apply {
        moveTo(50f, 0f)
        cubicTo(50f, 0f, 95f, 60f, 95f, 85f)
        cubicTo(95f, 105f, 75f, 120f, 50f, 120f)
        cubicTo(25f, 120f, 5f, 105f, 5f, 85f)
        cubicTo(5f, 60f, 50f, 0f, 50f, 0f)
        close()
    }

    Canvas(modifier = Modifier.fillMaxSize().blur(8.dp, BlurredEdgeTreatment.Unbounded)) {
        // Top left
        translate(left = -20f, top = -30f) {
            withTransform({ rotate(-12f, Offset(50f, 60f)) }) {
                drawPath(dropPath, Crimson.copy(alpha = 0.1f))
            }
        }
        // Bottom right
        translate(left = size.width - 100f, top = size.height - 100f) {
            withTransform({ rotate(168f, Offset(50f, 60f)) }) {
                drawPath(dropPath, Crimson.copy(alpha = 0.1f))
            }
        }
        // Top right small
        translate(left = size.width - 120f, top = 40f) {
            withTransform({
                scale(0.6f, 0.6f, Offset(50f, 60f))
                rotate(10f, Offset(50f, 60f))
            }) {
                drawPath(dropPath, Crimson.copy(alpha = 0.08f))
            }
        }
        // Bottom left small
        translate(left = 80f, top = size.height - 140f) {
            withTransform({
                scale(0.5f, 0.5f, Offset(50f, 60f))
                rotate(-15f, Offset(50f, 60f))
            }) {
                drawPath(dropPath, Crimson.copy(alpha = 0.08f))
            }
        }
    }
}

@Composable
fun CertificateLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val dropScaleX = size.width / 60f
        val dropScaleY = size.height / 70f
        
        withTransform({
            scale(dropScaleX, dropScaleY, Offset.Zero)
        }) {
            val bgPath = Path().apply {
                moveTo(30f, 2f)
                cubicTo(30f, 2f, 56f, 38f, 56f, 52f)
                cubicTo(56f, 63.6f, 44.1f, 70f, 30f, 70f)
                cubicTo(15.9f, 70f, 4f, 63.6f, 4f, 52f)
                cubicTo(4f, 38f, 30f, 2f, 30f, 2f)
                close()
            }
            drawPath(
                path = bgPath,
                brush = Brush.linearGradient(
                    0.0f to CrimsonSoft,
                    0.55f to Crimson,
                    1.0f to Color(0xFF7A0A1C),
                    start = Offset(0f, 0f),
                    end = Offset(60f, 70f)
                )
            )

            val sCutoutPath = Path().apply {
                moveTo(37f, 26f)
                cubicTo(33f, 23f, 25f, 23f, 22f, 27f)
                cubicTo(19.5f, 30.4f, 21.5f, 32.6f, 26f, 33.6f)
                cubicTo(31f, 34.7f, 36.5f, 35.6f, 35f, 41f)
                cubicTo(33.7f, 45.7f, 25.5f, 46.4f, 21f, 42.5f)
            }
            drawPath(
                path = sCutoutPath,
                color = Paper,
                style = Stroke(width = 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun CertificateMedal(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 100f
        val scaleY = size.height / 130f

        withTransform({
            scale(scaleX, scaleY, Offset.Zero)
        }) {
            // Polygon
            val polyPath = Path().apply {
                moveTo(50f, 2f)
                lineTo(61f, 18f)
                lineTo(80f, 12f)
                lineTo(79f, 32f)
                lineTo(98f, 40f)
                lineTo(83f, 54f)
                lineTo(92f, 73f)
                lineTo(72f, 70f)
                lineTo(68f, 90f)
                lineTo(50f, 79f)
                lineTo(32f, 90f)
                lineTo(28f, 70f)
                lineTo(8f, 73f)
                lineTo(17f, 54f)
                lineTo(2f, 40f)
                lineTo(21f, 32f)
                lineTo(20f, 12f)
                lineTo(39f, 18f)
                close()
            }
            drawPath(
                path = polyPath,
                brush = Brush.linearGradient(
                    0.0f to GoldLight,
                    0.5f to Gold,
                    1.0f to GoldDark,
                    start = Offset(0f, 0f),
                    end = Offset(100f, 100f)
                )
            )

            // Inner circles
            drawCircle(color = GoldLight, radius = 24f, center = Offset(50f, 42f))
            drawCircle(
                color = GoldDark,
                radius = 24f,
                center = Offset(50f, 42f),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = GoldDark,
                radius = 17f,
                center = Offset(50f, 42f),
                style = Stroke(width = 1.5f)
            )

            // Bottom ribbon
            val ribbonPath = Path().apply {
                moveTo(38f, 60f)
                lineTo(30f, 118f)
                lineTo(50f, 100f)
                lineTo(70f, 118f)
                lineTo(62f, 60f)
                close()
            }
            drawPath(path = ribbonPath, color = CrimsonDeep)
        }
    }
}
