package io.github.jaswantk917.chitthi.ui.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.jaswantk917.chitthi.data.SmsStore
import io.github.jaswantk917.chitthi.model.Box as MessageBox
import io.github.jaswantk917.chitthi.model.Message
import io.github.jaswantk917.chitthi.model.SenderInfo
import io.github.jaswantk917.chitthi.model.Tag
import io.github.jaswantk917.chitthi.model.Verdict
import io.github.jaswantk917.chitthi.notify.Clipboard
import io.github.jaswantk917.chitthi.ui.components.Avatar
import io.github.jaswantk917.chitthi.ui.components.TagRow
import io.github.jaswantk917.chitthi.ui.util.Haptic
import io.github.jaswantk917.chitthi.ui.util.clockTime
import io.github.jaswantk917.chitthi.ui.util.dayLabel
import io.github.jaswantk917.chitthi.ui.util.edgeBlur
import io.github.jaswantk917.chitthi.ui.util.pressScale
import io.github.jaswantk917.chitthi.ui.util.rememberHaptics
import io.github.jaswantk917.chitthi.ui.util.sameDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ThreadActions {
    fun back()
    fun send(text: String)
    fun retry(message: Message)
    /** false when not allowed (not the default SMS app) */
    suspend fun delete(message: Message): Boolean
    fun call()
    fun more()
    suspend fun smartReplies(chat: List<Pair<Boolean, String>>): List<String>
}

private sealed interface Item {
    val key: String
    data class Day(val label: String, override val key: String) : Item
    data class Bubble(val message: Message, val verdict: Verdict, val joinsAbove: Boolean, val joinsBelow: Boolean) : Item {
        override val key: String get() = message.key
    }
}

private const val GROUP_GAP_MS = 3 * 60_000L

private fun rows(messages: List<Message>, verdict: (Message) -> Verdict): List<Item> {
    val out = ArrayList<Item>(messages.size + 8)
    messages.forEachIndexed { i, m ->
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        if (prev == null || !sameDay(prev.date, m.date)) out += Item.Day(dayLabel(m.date), "day-${m.key}")
        fun joins(other: Message?) = other != null && other.incoming == m.incoming &&
            kotlin.math.abs(other.date - m.date) < GROUP_GAP_MS && sameDay(other.date, m.date)
        out += Item.Bubble(m, verdict(m), joins(prev), joins(next))
    }
    return out.asReversed()
}

@Composable
fun ThreadScreen(
    sender: SenderInfo,
    messages: List<Message>,
    verdict: (Message) -> Verdict,
    initialText: String,
    actions: ThreadActions,
) {
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val density = LocalDensity.current
    var topHeight by remember { mutableStateOf(72.dp) }
    var bottomHeight by remember { mutableStateOf(88.dp) }
    val initialKeys = remember(sender.key) { messages.map { it.key }.toHashSet() }
    val rows = remember(messages) { rows(messages, verdict) }

    val newest = messages.lastOrNull()
    LaunchedEffect(newest?.key) {
        if (newest != null && newest.key !in initialKeys) {
            if (newest.incoming) haptics.play(Haptic.Arrive)
            list.animateScrollToItem(0)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            state = list,
            reverseLayout = true,
            contentPadding = PaddingValues(top = topHeight + 8.dp, bottom = bottomHeight + 8.dp, start = 12.dp, end = 12.dp),
            modifier = Modifier.fillMaxSize().edgeBlur(top = topHeight, bottom = bottomHeight),
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is Item.Day -> DayChip(row.label, Modifier.animateItem())
                    is Item.Bubble -> Bubble(
                        row = row,
                        fresh = row.message.key !in initialKeys,
                        onCopied = { what -> scope.launch { snackbar.showSnackbar("Copied $what") } },
                        actions = actions,
                        onDeleteRefused = { scope.launch { snackbar.showSnackbar("Make Chitthi your SMS app to delete") } },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        TopBar(
            sender = sender,
            actions = actions,
            modifier = Modifier.align(Alignment.TopCenter).onSizeChanged { topHeight = with(density) { it.height.toDp() } },
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { bottomHeight = with(density) { it.height.toDp() } },
        ) {
            SnackbarHost(snackbar)
            if (sender.canReply) {
                Composer(sender, messages, initialText, actions)
            } else {
                NoReplies(sender)
            }
        }
    }
}

@Composable
private fun TopBar(sender: SenderInfo, actions: ThreadActions, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val surface = MaterialTheme.colorScheme.surface
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to surface, 0.75f to surface.copy(alpha = 0.92f), 1f to surface.copy(alpha = 0f)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { haptics.play(Haptic.Tap); actions.back() },
            shapes = IconButtonDefaults.shapes(MaterialShapes.Cookie9Sided.toShape(), MaterialShapes.Cookie4Sided.toShape()),
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Spacer(Modifier.width(8.dp))
        Avatar(sender, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sender.title, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            sender.subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (sender.key.startsWith("p:")) {
            IconButton(onClick = { haptics.play(Haptic.Tap); actions.call() }) { Icon(Icons.Rounded.Call, "Call") }
        }
        IconButton(onClick = { haptics.play(Haptic.Tap); actions.more() }) { Icon(Icons.Rounded.MoreVert, "More") }
    }
}

@Composable
private fun DayChip(label: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

private val urlPattern = Regex("(?i)\\b((?:https?://)?(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/[^\\s]*)?)")

@Composable
private fun Bubble(
    row: Item.Bubble,
    fresh: Boolean,
    onCopied: (String) -> Unit,
    actions: ThreadActions,
    onDeleteRefused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = row.message
    val mine = !m.incoming
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val big = 22.dp
    val small = 6.dp
    val shape = if (mine) {
        RoundedCornerShape(big, if (row.joinsAbove) small else big, if (row.joinsBelow) small else big, big)
    } else {
        RoundedCornerShape(if (row.joinsAbove) small else big, big, big, if (row.joinsBelow) small else big)
    }
    val container = when {
        m.box == MessageBox.Failed -> scheme.errorContainer
        mine -> scheme.primary
        else -> scheme.surfaceContainerHigh
    }
    val content = when {
        m.box == MessageBox.Failed -> scheme.onErrorContainer
        mine -> scheme.onPrimary
        else -> scheme.onSurface
    }
    val entrance = remember(m.key) { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(m.key) { entrance.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier
            .fillMaxWidth()
            .padding(top = if (row.joinsAbove) 2.dp else 8.dp)
            .graphicsLayer {
                val p = entrance.value
                alpha = p.coerceIn(0f, 1f)
                scaleX = 0.7f + 0.3f * p
                scaleY = 0.7f + 0.3f * p
                transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f)
            },
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box {
            Column(
                Modifier
                    .widthIn(max = 300.dp)
                    .pressScale(interaction, 0.97f)
                    .clip(shape)
                    .background(container)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { if (m.box == MessageBox.Failed) actions.retry(m) },
                        onLongClick = { haptics.play(Haptic.Hold); menu = true },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (m.isMms && m.hasImage) MmsImages(m)
                if (m.body.isNotBlank()) {
                    Text(
                        annotated(m.body, row.verdict.otp, content, scheme.tertiaryContainer, scheme.onTertiaryContainer) { code ->
                            Clipboard.copyOtp(context, code)
                            haptics.play(Haptic.Copy)
                            onCopied(code)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = content,
                    )
                }
                val money = row.verdict.tags.filter { it is Tag.Debit || it is Tag.Credit || it is Tag.Bill || it is Tag.Delivery || it is Tag.Travel }
                if (!mine && money.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    TagRow(money)
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(20.dp)) {
                DropdownMenuItem(
                    text = { Text("Copy text") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                    onClick = {
                        menu = false
                        context.getSystemService(android.content.ClipboardManager::class.java)
                            .setPrimaryClip(android.content.ClipData.newPlainText("Message", m.body))
                        haptics.play(Haptic.Copy)
                        onCopied("message")
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                    onClick = {
                        menu = false
                        scope.launch {
                            if (actions.delete(m)) haptics.play(Haptic.Delete) else { haptics.play(Haptic.Error); onDeleteRefused() }
                        }
                    },
                )
            }
        }
        if (!row.joinsBelow) {
            val status = when (m.box) {
                MessageBox.Sending -> "Sending…"
                MessageBox.Failed -> "Not sent · tap to retry"
                MessageBox.Sent -> "Sent"
                else -> null
            }
            Text(
                listOfNotNull(clockTime(m.date), status).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = if (m.box == MessageBox.Failed) scheme.error else scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MmsImages(m: Message) {
    val context = LocalContext.current
    val parts by produceState(emptyList<io.github.jaswantk917.chitthi.model.MmsPart>(), m.key) {
        value = withContext(Dispatchers.IO) { SmsStore.mmsParts(context, m.id) }
    }
    parts.filter { it.contentType.startsWith("image/") }.forEach { part ->
        AsyncImage(
            model = part.uri,
            contentDescription = "Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.padding(bottom = 6.dp).widthIn(max = 272.dp).heightIn(max = 320.dp).clip(RoundedCornerShape(16.dp)),
        )
    }
}

private fun annotated(
    body: String,
    otp: String?,
    linkColor: androidx.compose.ui.graphics.Color,
    otpBackground: androidx.compose.ui.graphics.Color,
    otpColor: androidx.compose.ui.graphics.Color,
    onOtp: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    data class Span(val range: IntRange, val link: LinkAnnotation)
    val spans = ArrayList<Span>()
    urlPattern.findAll(body).forEach { match ->
        val raw = match.value.trimEnd('.', ',', ')')
        val url = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
        spans += Span(
            match.range.first until match.range.first + raw.length,
            LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))),
        )
    }
    if (otp != null) {
        val at = Regex("(?<!\\d)" + Regex.escape(otp) + "(?!\\d)").find(body)
        if (at != null && spans.none { it.range.first <= at.range.last && at.range.first <= it.range.last }) {
            spans += Span(
                at.range,
                LinkAnnotation.Clickable(
                    "otp",
                    TextLinkStyles(SpanStyle(background = otpBackground, color = otpColor, fontWeight = FontWeight.Black)),
                ) { onOtp(otp) },
            )
        }
    }
    var cursor = 0
    spans.sortedBy { it.range.first }.forEach { span ->
        if (span.range.first < cursor) return@forEach
        append(body.substring(cursor, span.range.first))
        withLink(span.link) { append(body.substring(span.range.first, span.range.last + 1)) }
        cursor = span.range.last + 1
    }
    append(body.substring(cursor))
}

@Composable
private fun Composer(sender: SenderInfo, messages: List<Message>, initialText: String, actions: ThreadActions) {
    val haptics = rememberHaptics()
    var text by rememberSaveable(sender.key) { mutableStateOf(initialText) }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    val last = messages.lastOrNull()
    LaunchedEffect(last?.key) {
        suggestions = emptyList()
        if (last != null && last.incoming && last.body.isNotBlank()) {
            suggestions = actions.smartReplies(messages.takeLast(8).map { !it.incoming to it.body })
        }
    }
    val surface = MaterialTheme.colorScheme.surface
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to surface.copy(alpha = 0f), 0.35f to surface.copy(alpha = 0.92f), 1f to surface))
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
    ) {
        AnimatedVisibility(
            suggestions.isNotEmpty() && text.isEmpty(),
            enter = expandVertically(spring(dampingRatio = 0.7f)) + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AutoAwesome, "Suggested by Gemini Nano", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                suggestions.forEachIndexed { i, suggestion ->
                    var shown by remember(suggestion) { mutableStateOf(false) }
                    LaunchedEffect(suggestion) { kotlinx.coroutines.delay(i * 70L); shown = true }
                    val s by animateFloatAsState(if (shown) 1f else 0.6f, spring(dampingRatio = 0.5f), label = "chip")
                    SuggestionChip(
                        onClick = { haptics.play(Haptic.Tap); text = suggestion },
                        label = { Text(suggestion) },
                        shape = CircleShape,
                        modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s; alpha = ((s - 0.6f) / 0.4f).coerceIn(0f, 1f) },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(28.dp),
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            )
            Spacer(Modifier.width(8.dp))
            val canSend = text.isNotBlank()
            val turn by animateFloatAsState(if (canSend) 0f else -40f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "sendTurn")
            val grow by animateFloatAsState(if (canSend) 1f else 0.86f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "sendGrow")
            FilledIconButton(
                onClick = {
                    haptics.play(Haptic.Send)
                    actions.send(text.trim())
                    text = ""
                },
                enabled = canSend,
                shapes = IconButtonDefaults.shapes(MaterialShapes.Cookie9Sided.toShape(), MaterialShapes.Cookie4Sided.toShape()),
                modifier = Modifier.size(56.dp).graphicsLayer { scaleX = grow; scaleY = grow; rotationZ = turn },
            ) { Icon(Icons.AutoMirrored.Rounded.Send, "Send") }
        }
    }
}

@Composable
private fun NoReplies(sender: SenderInfo) {
    Box(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("${sender.title} doesn't take replies", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
