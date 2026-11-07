package io.github.jaswantk917.chitthi.ui.search

import io.github.jaswantk917.chitthi.ui.components.pressMorph
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.jaswantk917.chitthi.data.Contact
import io.github.jaswantk917.chitthi.model.Message
import io.github.jaswantk917.chitthi.model.SenderInfo
import io.github.jaswantk917.chitthi.ui.components.Avatar
import io.github.jaswantk917.chitthi.ui.util.Haptic
import io.github.jaswantk917.chitthi.ui.util.arrive
import io.github.jaswantk917.chitthi.ui.util.pressScale
import io.github.jaswantk917.chitthi.ui.util.rememberHaptics
import io.github.jaswantk917.chitthi.ui.util.shortTime
import kotlinx.coroutines.delay

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, placeholder: String, onBack: () -> Unit, keyboard: KeyboardType = KeyboardType.Text) {
    val haptics = rememberHaptics()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val press1 = remember { MutableInteractionSource() }
        FilledTonalIconButton(
            onClick = { haptics.play(Haptic.Tap); onBack() },
            shape = pressMorph(MaterialShapes.Cookie9Sided, MaterialShapes.Cookie4Sided, press1),
            interactionSource = press1,
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Spacer(Modifier.width(8.dp))
        TextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
    }
}

@Composable
private fun PickRow(sender: SenderInfo, title: AnnotatedString, line: AnnotatedString, trailing: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(sender, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun highlight(text: String, query: String): AnnotatedString {
    val color = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.primaryContainer
    return buildAnnotatedString {
        val at = if (query.isBlank()) -1 else text.indexOf(query, ignoreCase = true)
        if (at < 0) { append(text); return@buildAnnotatedString }
        // start a little before the match so it's visible in two lines
        val from = (at - 40).coerceAtLeast(0)
        if (from > 0) append("…")
        append(text.substring(from, at))
        withStyle(SpanStyle(color = color, background = bg, fontWeight = FontWeight.Bold)) { append(text.substring(at, at + query.length)) }
        append(text.substring(at + query.length))
    }
}

@Composable
fun SearchScreen(search: (String) -> List<Pair<Message, SenderInfo>>, onOpen: (SenderInfo) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Pair<Message, SenderInfo>>()) }
    LaunchedEffect(query) {
        delay(150)
        results = search(query.trim())
    }
    val seen = remember(results) { HashSet<Any>() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        SearchBar(query, { query = it }, "Search messages", onBack)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            if (query.isNotBlank() && results.isEmpty()) {
                item { Hint("Nothing matches “$query”") }
            }
            items(results, key = { it.first.key }) { (message, sender) ->
                PickRow(
                    sender = sender,
                    title = AnnotatedString(sender.title),
                    line = highlight(message.body, query.trim()),
                    trailing = shortTime(message.date),
                    onClick = { onOpen(sender) },
                    modifier = Modifier.animateItem().arrive(message.key, results.indexOfFirst { it.first.key == message.key }, seen),
                )
            }
        }
    }
}

@Composable
fun NewChatScreen(contacts: List<Contact>, senderFor: (String) -> SenderInfo, onPick: (SenderInfo) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val digits = query.filter { it.isDigit() || it == '+' }
    val looksLikeNumber = digits.count { it.isDigit() } >= 3 && query.none { it.isLetter() }
    val matches = remember(query, contacts) {
        val q = query.trim()
        if (q.isEmpty()) contacts
        else contacts.filter { it.name.contains(q, true) || it.number.filter(Char::isDigit).contains(q.filter(Char::isDigit).ifEmpty { "\u0000" }) }
    }
    val seen = remember { HashSet<Any>() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        SearchBar(query, { query = it }, "Name or number", onBack)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            if (looksLikeNumber) {
                item(key = "number") {
                    val sender = senderFor(digits)
                    val interaction = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .pressScale(interaction)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(interaction, indication = null) { onPick(sender) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Text("Message $digits", style = MaterialTheme.typography.titleMediumEmphasized, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            if (contacts.isEmpty()) item { Hint("No contacts to show") }
            items(matches, key = { it.number + it.name }) { contact ->
                val sender = senderFor(contact.number)
                PickRow(
                    sender = sender,
                    title = AnnotatedString(contact.name),
                    line = AnnotatedString(contact.number),
                    trailing = null,
                    onClick = { onPick(sender) },
                    modifier = Modifier.animateItem().arrive(contact.number + contact.name, matches.indexOf(contact), seen),
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
