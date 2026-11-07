package io.github.jaswantk917.chitthi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.jaswantk917.chitthi.model.SenderInfo
import kotlin.math.abs

fun logoUrl(domain: String) =
    "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$domain&size=128"

private val avatarShapes = listOf(
    MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided, MaterialShapes.Flower, MaterialShapes.Pentagon,
    MaterialShapes.Gem, MaterialShapes.SoftBurst, MaterialShapes.Cookie7Sided,
)

/** A contact's photo, a brand's logo on a white disc, or an initial on a playful shape. */
@Composable
fun Avatar(sender: SenderInfo, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    var photoFailed by remember(sender.photo) { mutableStateOf(false) }
    var logoFailed by remember(sender.logoDomain) { mutableStateOf(false) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            sender.photo != null && !photoFailed -> AsyncImage(
                model = sender.photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { photoFailed = true },
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            sender.logoDomain != null && !logoFailed -> Box(
                Modifier.fillMaxSize().clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = logoUrl(sender.logoDomain),
                    contentDescription = null,
                    onError = { logoFailed = true },
                    modifier = Modifier.fillMaxSize().padding(size * 0.2f).clip(MaterialTheme.shapes.small),
                )
            }
            else -> Monogram(sender, size)
        }
    }
}

@Composable
private fun Monogram(sender: SenderInfo, size: Dp) {
    val hash = abs(sender.key.hashCode())
    val scheme = MaterialTheme.colorScheme
    val (container, content) = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        scheme.surfaceContainerHighest to scheme.onSurfaceVariant,
    )[hash % 4]
    Box(
        Modifier.fillMaxSize().clip(avatarShapes[hash % avatarShapes.size].toShape()).background(container),
        contentAlignment = Alignment.Center,
    ) {
        val initial = sender.title.firstOrNull { it.isLetter() }
        when {
            sender.key.startsWith("t:") -> Icon(Icons.Rounded.Groups, null, tint = content, modifier = Modifier.size(size * 0.5f))
            initial == null -> Icon(Icons.Rounded.Person, null, tint = content, modifier = Modifier.size(size * 0.5f))
            else -> Text(
                initial.uppercase(),
                color = content,
                style = MaterialTheme.typography.titleLargeEmphasized,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}
