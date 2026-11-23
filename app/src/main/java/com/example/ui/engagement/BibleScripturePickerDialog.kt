package com.example.ui.engagement

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.BibleVerseEntity
import com.example.data.model.scrambleVerse
import com.example.data.repository.BibleDocumentExtractor
import com.example.data.repository.BibleRepository
import com.example.data.repository.UploadedBibleEdition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GideonNavy = Color(0xFF0F1E36)
private val GideonGold = Color(0xFFD4AF37)
private val GideonAccent = Color(0xFF2E7D32)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BibleScripturePickerDialog(
    bibleRepository: BibleRepository?,
    onDismiss: () -> Unit,
    onScriptureSelected: (BibleVerseEntity) -> Unit,
    initialTargetContext: String = "Scripture Scramble",
    initialTabIndex: Int = 0
) {
    val coroutineScope = rememberCoroutineScope()
    var activeTabIndex by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex) } // 0 = Scripture Verses, 1 = PDF / EPUB Bibles
    var searchQuery by remember { mutableStateOf("") }
    var selectedTranslation by remember { mutableStateOf("All") }
    var selectedTheme by remember { mutableStateOf("All") }
    var selectedBook by remember { mutableStateOf("All") }

    var showUploadDialog by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var totalVerseCount by remember { mutableIntStateOf(0) }
    var openedEdition by remember { mutableStateOf<UploadedBibleEdition?>(null) }

    LaunchedEffect(Unit) {
        bibleRepository?.ensureSeeded()
        totalVerseCount = bibleRepository?.getVerseCount() ?: 0
    }

    val allVerses by (bibleRepository?.getAllVersesFlow() ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val uploadedEditions by (bibleRepository?.getUploadedEditionsFlow()
        ?: remember { MutableStateFlow(emptyList<UploadedBibleEdition>()) }).collectAsState()

    LaunchedEffect(allVerses.size) {
        totalVerseCount = allVerses.size
    }

    val filteredVerses = remember(allVerses, searchQuery, selectedTranslation, selectedTheme, selectedBook) {
        allVerses.filter { verse ->
            val matchesQuery = searchQuery.isBlank() ||
                verse.reference.contains(searchQuery, ignoreCase = true) ||
                verse.verseText.contains(searchQuery, ignoreCase = true) ||
                verse.book.contains(searchQuery, ignoreCase = true) ||
                verse.theme.contains(searchQuery, ignoreCase = true)

            val matchesTranslation = selectedTranslation == "All" || verse.translation.equals(selectedTranslation, ignoreCase = true)
            val matchesTheme = selectedTheme == "All" || verse.theme.equals(selectedTheme, ignoreCase = true)
            val matchesBook = selectedBook == "All" || verse.book.equals(selectedBook, ignoreCase = true)

            matchesQuery && matchesTranslation && matchesTheme && matchesBook
        }
    }

    val availableBooks = remember(allVerses) {
        listOf("All") + allVerses.map { it.book }.distinct().sorted()
    }

    val availableThemes = listOf(
        "All",
        "Exploits & Strength",
        "Courage & Boldness",
        "Youth & Purpose",
        "Victory & Conquerors",
        "Wisdom & Guidance",
        "Faith & Trust",
        "Gideonites Heritage"
    )

    val availableTranslations = remember(allVerses) {
        val dynamic = allVerses.map { it.translation }.distinct()
        (listOf("All", "KJV", "NKJV", "NIV", "AMP") + dynamic).distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.93f)
                .testTag("dialog_bible_scripture_picker"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Gideonites Branding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GideonNavy,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = "Bible Database",
                                    tint = GideonGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Executive Bible Reference",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GideonGold
                                ) {
                                    Text(
                                        text = "$totalVerseCount Verses",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GideonNavy,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Reference scriptures or open PDF/EPUB Bibles for $initialTargetContext",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showUploadDialog = true },
                            modifier = Modifier.testTag("btn_open_upload_bible_dialog"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload PDF/EPUB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_bible_picker")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                importStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = GideonAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = GideonAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GideonAccent
                                )
                            }
                            IconButton(onClick = { importStatusMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Top Mode Selector: Search Scriptures vs Uploaded PDF / EPUB Bibles
                TabRow(
                    selectedTabIndex = activeTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeTabIndex == 0,
                        onClick = { activeTabIndex = 0 },
                        text = {
                            Text(
                                "📖 Scripture Verses ($totalVerseCount)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_picker_verses")
                    )
                    Tab(
                        selected = activeTabIndex == 1,
                        onClick = { activeTabIndex = 1 },
                        text = {
                            Text(
                                "📄 PDF / EPUB Bibles (${uploadedEditions.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_picker_pdf_epub")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeTabIndex == 0) {
                    // Search Bar with instant clear
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search scriptures, references, topics (e.g. exploits, Daniel, Romans)") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_bible_search"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Translations Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Version:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(availableTranslations) { trans ->
                                FilterChip(
                                    selected = selectedTranslation == trans,
                                    onClick = { selectedTranslation = trans },
                                    label = { Text(trans, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("chip_translation_$trans")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Themes Horizontal Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Theme:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(availableThemes) { theme ->
                                FilterChip(
                                    selected = selectedTheme == theme,
                                    onClick = { selectedTheme = theme },
                                    label = { Text(theme, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("chip_theme_${theme.take(6)}")
                                )
                            }
                        }
                    }

                    if (availableBooks.size > 2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Book:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(availableBooks) { book ->
                                    FilterChip(
                                        selected = selectedBook == book,
                                        onClick = { selectedBook = book },
                                        label = { Text(book, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("chip_book_$book")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Found ${filteredVerses.size} Scripture${if (filteredVerses.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = { activeTabIndex = 1 },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Open PDF / EPUB Reader →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredVerses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(54.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No scriptures match your current filter",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Search another keyword, reset filters, or tap 'Upload PDF/EPUB' to import a Bible.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedTranslation = "All"
                                        selectedTheme = "All"
                                        selectedBook = "All"
                                    }
                                ) {
                                    Text("Reset All Filters")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredVerses, key = { it.id }) { verse ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verse_item_${verse.reference.replace(" ", "_")}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = GideonNavy
                                                ) {
                                                    Text(
                                                        text = verse.reference,
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = GideonGold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = verse.translation,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                if (verse.theme.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                                    ) {
                                                        Text(
                                                            text = verse.theme,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    onScriptureSelected(verse)
                                                    onDismiss()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GideonAccent),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("btn_select_verse_${verse.reference.replace(" ", "_")}")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Select", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "“${verse.verseText}”",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontStyle = FontStyle.Italic,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        val scrambledSample = remember(verse.verseText) {
                                            scrambleVerse(verse.verseText).take(6)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Auto-Scramble Preview: ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            scrambledSample.forEach { tile ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = GideonGold.copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, GideonGold.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = tile,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (scrambleVerse(verse.verseText).size > 6) {
                                                Text("...", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tab 1: Uploaded PDF / EPUB Bible Editions (Backend Synced)
                    UploadedBibleEditionsTabContent(
                        uploadedEditions = uploadedEditions,
                        onOpenEdition = { openedEdition = it },
                        onUploadNew = { showUploadDialog = true },
                        onLoadExpandedPack = {
                            coroutineScope.launch {
                                bibleRepository?.loadExpandedReferencePack("KJV", "Fellowship Administrator")
                                    ?.onSuccess { count ->
                                        importStatusMessage = "Loaded & synced $count 66-Book Reference Scriptures to the backend!"
                                    }
                            }
                        },
                        onDeleteEdition = { editionId ->
                            coroutineScope.launch {
                                bibleRepository?.deleteUploadedEdition(editionId)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadBibleDatabaseDialog(
            bibleRepository = bibleRepository,
            onDismiss = { showUploadDialog = false },
            onImportSuccess = { count ->
                importStatusMessage = "Uploaded & synced $count scriptures to the backend!"
                showUploadDialog = false
            }
        )
    }

    openedEdition?.let { edition ->
        UploadedBibleDocumentReaderDialog(
            edition = edition,
            onDismiss = { openedEdition = null },
            onSelectScripture = { verse ->
                openedEdition = null
                onScriptureSelected(verse)
                onDismiss()
            }
        )
    }
}

@Composable
private fun UploadedBibleEditionsTabContent(
    uploadedEditions: List<UploadedBibleEdition>,
    onOpenEdition: (UploadedBibleEdition) -> Unit,
    onUploadNew: () -> Unit,
    onLoadExpandedPack: () -> Unit,
    onDeleteEdition: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GideonNavy),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Backend PDF & EPUB Bible Library",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = GideonGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Uploaded PDF and EPUB Bibles are synced to the backend so Executives can read chapters or pick scripture references when setting games.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onUploadNew,
                    colors = ButtonDefaults.buttonColors(containerColor = GideonGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GideonNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload PDF/EPUB", color = GideonNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uploadedEditions, key = { it.id }) { edition ->
                val isPdf = edition.fileFormat.equals("PDF", ignoreCase = true)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEdition(edition) }
                        .testTag("bible_edition_card_${edition.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPdf) Color(0xFFDC2626).copy(alpha = 0.15f) else GideonNavy,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.MenuBook,
                                        contentDescription = edition.fileFormat,
                                        tint = if (isPdf) Color(0xFFDC2626) else GideonGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = GideonGold,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = edition.fileFormat.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = GideonNavy,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = edition.translation,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = edition.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${edition.extractedVersesCount} Indexed Verses • ${edition.pageOrChapterCount} Sections • By ${edition.uploadedBy} (${dateFormat.format(Date(edition.uploadedAt))})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (edition.id != "bdoc_default_kjv") {
                                IconButton(onClick = { onDeleteEdition(edition.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Edition",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = { onOpenEdition(edition) },
                                colors = ButtonDefaults.buttonColors(containerColor = GideonAccent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Bible", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen PDF & EPUB Bible Document Reader & Scripture Reference Extractor.
 * Allows Executives and Admins to read uploaded PDF pages (via PdfRenderer) or EPUB/text chapters,
 * search inside the Bible document, and with 1 tap select any verse or passage when setting games.
 */
@Composable
fun UploadedBibleDocumentReaderDialog(
    edition: UploadedBibleEdition,
    onDismiss: () -> Unit,
    onSelectScripture: ((BibleVerseEntity) -> Unit)? = null
) {
    val localPdfFile = remember(edition.localFilePath, edition.fileFormat) {
        edition.localFilePath?.let { File(it) }?.takeIf {
            it.exists() && edition.fileFormat.equals("PDF", ignoreCase = true)
        }
    }
    var viewMode by remember(localPdfFile) {
        mutableIntStateOf(0) // 0 = Searchable Chapters & Verses, 1 = Rendered PDF Page View
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSectionIndex by remember { mutableIntStateOf(0) }
    var currentPdfPage by remember { mutableIntStateOf(0) }
    var pdfTotalPages by remember { mutableIntStateOf(edition.pageOrChapterCount.coerceAtLeast(1)) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(localPdfFile, currentPdfPage, viewMode) {
        if (localPdfFile != null && viewMode == 1) {
            withContext(Dispatchers.IO) {
                pdfTotalPages = BibleDocumentExtractor.countPdfPages(localPdfFile)
                renderedBitmap = BibleDocumentExtractor.renderPdfPageToBitmap(localPdfFile, currentPdfPage)
            }
        }
    }

    val filteredSections = remember(edition.sections, searchQuery) {
        if (searchQuery.isBlank()) {
            edition.sections
        } else {
            edition.sections.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true) ||
                    it.book.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val activeSection = filteredSections.getOrNull(selectedSectionIndex.coerceIn(0, (filteredSections.size - 1).coerceAtLeast(0)))
    val sectionVerses = remember(activeSection, edition.translation) {
        if (activeSection == null) emptyList()
        else BibleDocumentExtractor.extractVersesFromPlainText(
            text = activeSection.content,
            translation = edition.translation,
            defaultBook = activeSection.book,
            defaultChapter = activeSection.chapter
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("dialog_uploaded_bible_reader"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = GideonGold, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "${edition.fileFormat} • ${edition.translation}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = GideonNavy,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = edition.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Uploaded by ${edition.uploadedBy} • Tap any scripture below to reference it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Reader")
                    }
                }

                if (localPdfFile != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TabRow(selectedTabIndex = viewMode) {
                        Tab(
                            selected = viewMode == 0,
                            onClick = { viewMode = 0 },
                            text = { Text("Searchable Scripture Chapters", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = viewMode == 1,
                            onClick = { viewMode = 1 },
                            text = { Text("PDF Page Viewer ($pdfTotalPages Pages)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (viewMode == 1 && localPdfFile != null) {
                    // Native Android PdfRenderer Page Viewer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPdfPage > 0) currentPdfPage-- },
                            enabled = currentPdfPage > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }
                        Text(
                            text = "Page ${currentPdfPage + 1} of $pdfTotalPages",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        OutlinedButton(
                            onClick = { if (currentPdfPage < pdfTotalPages - 1) currentPdfPage++ },
                            enabled = currentPdfPage < pdfTotalPages - 1
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = renderedBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Bible Page ${currentPdfPage + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    // Searchable Chapters & Instant Scripture Reference Picker
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            selectedSectionIndex = 0
                        },
                        placeholder = { Text("Search inside ${edition.title} (e.g. Gideon, faith, Psalm 23)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredSections.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredSections.size) { idx ->
                                val sec = filteredSections[idx]
                                FilterChip(
                                    selected = selectedSectionIndex == idx,
                                    onClick = { selectedSectionIndex = idx },
                                    label = {
                                        Text(
                                            text = sec.title.take(32),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeSection == null) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No chapters match \"$searchQuery\".")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = activeSection.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = activeSection.content,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                                    )
                                }
                            }

                            if (onSelectScripture != null && sectionVerses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "TAP A VERSE FROM THIS CHAPTER TO USE IN GAME / QUIZ:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GideonAccent
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                sectionVerses.forEach { v ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, GideonAccent.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onSelectScripture(v) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${v.reference} (${v.translation})",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = GideonNavy
                                                )
                                                Text(
                                                    text = "“${v.verseText}”",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { onSelectScripture(v) },
                                                colors = ButtonDefaults.buttonColors(containerColor = GideonAccent),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Use Verse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated dialog for uploading a PDF (.pdf), EPUB (.epub), JSON (.json), or Text (.txt/.csv) Bible
 * to the local Room database AND syncing it to the Firestore backend so Executives on all devices
 * can access it whenever they need a scripture reference.
 */
@Composable
fun UploadBibleDatabaseDialog(
    bibleRepository: BibleRepository?,
    onDismiss: () -> Unit,
    onImportSuccess: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedFormatTab by remember { mutableIntStateOf(0) } // 0 = PDF / EPUB File, 1 = Line Format, 2 = JSON Format
    var rawInputText by remember { mutableStateOf("") }
    var customEditionTitle by remember { mutableStateOf("") }
    var defaultTranslation by remember { mutableStateOf("KJV") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val translationOptions = listOf("KJV", "NKJV", "NIV", "AMP", "Bibeli Mimo (YO)", "Littafi Mai Tsarki (HA)", "Baịbụl Nsọ (IG)")

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            val resolvedName = BibleDocumentExtractor.resolveFileName(context, uri)
            selectedFileName = resolvedName
            if (customEditionTitle.isBlank()) {
                customEditionTitle = resolvedName.substringBeforeLast('.')
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
            }
            errorMessage = null

            val lower = resolvedName.lowercase()
            if (lower.endsWith(".pdf") || lower.endsWith(".epub")) {
                selectedFormatTab = 0
            } else {
                coroutineScope.launch {
                    runCatching {
                        val content = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?.trim() ?: ""
                        if (content.isNotBlank() && !content.startsWith("%PDF") && !content.startsWith("PK")) {
                            rawInputText = content
                            selectedFormatTab = if (content.startsWith("[") || content.startsWith("{")) 2 else 1
                        }
                    }
                }
            }
        }
    }

    val sampleDelimited = """
Philippians 4:19 | But my God shall supply all your need according to his riches in glory by Christ Jesus. | KJV | Faith & Provision
Isaiah 54:17 | No weapon that is formed against thee shall prosper; and every tongue that shall rise against thee in judgment thou shalt condemn. | KJV | Victory & Conquerors
1 Corinthians 15:57 | But thanks be to God, which giveth us the victory through our Lord Jesus Christ. | KJV | Victory & Conquerors
1 John 4:4 | Ye are of God, little children, and have overcome them: because greater is he that is in you, than he that is in the world. | KJV | Exploits & Strength
Psalm 1:1-2 | Blessed is the man that walketh not in the counsel of the ungodly, nor standeth in the way of sinners. | KJV | Wisdom & Guidance
Zechariah 4:6 | Not by might, nor by power, but by my spirit, saith the LORD of hosts. | KJV | Exploits & Strength
    """.trimIndent()

    val sampleJson = """
[
  {
    "book": "Philippians",
    "chapter": 4,
    "verse": 19,
    "reference": "Philippians 4:19",
    "verseText": "But my God shall supply all your need according to his riches in glory by Christ Jesus.",
    "translation": "KJV",
    "theme": "Faith & Provision"
  },
  {
    "book": "Isaiah",
    "chapter": 54,
    "verse": 17,
    "reference": "Isaiah 54:17",
    "verseText": "No weapon that is formed against thee shall prosper; and every tongue that shall rise against thee in judgment thou shalt condemn.",
    "translation": "KJV",
    "theme": "Victory & Conquerors"
  }
]
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GideonGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Bible (PDF / EPUB / Backend)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Upload a PDF or EPUB version of the Bible to the backend so Executives can read and reference scriptures when setting games, quizzes, or devotionals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedFormatTab) {
                    Tab(
                        selected = selectedFormatTab == 0,
                        onClick = { selectedFormatTab = 0 },
                        text = { Text("PDF / EPUB", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedFormatTab == 1,
                        onClick = { selectedFormatTab = 1 },
                        text = { Text("Line / Text", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedFormatTab == 2,
                        onClick = { selectedFormatTab = 2 },
                        text = { Text("JSON", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Translation Selector Row
                Text(
                    text = "Bible Translation / Version:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(translationOptions) { trans ->
                        FilterChip(
                            selected = defaultTranslation == trans,
                            onClick = { defaultTranslation = trans },
                            label = { Text(trans, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedFormatTab == 0) {
                    OutlinedTextField(
                        value = customEditionTitle,
                        onValueChange = { customEditionTitle = it },
                        label = { Text("Bible Edition Title (e.g. KJV Reference Bible)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_pick_bible_file")
                    ) {
                        Icon(
                            imageVector = if (selectedFileName?.endsWith(".pdf", true) == true) Icons.Default.PictureAsPdf else Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = GideonGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedFileName?.let { "Selected: $it (Tap to Change)" }
                                ?: "Select PDF (.pdf) or EPUB (.epub) Bible from Device",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Quick Backend Seed Option:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Need an instant 66-book Executive Reference Bible synced to the backend right now?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    if (bibleRepository == null) return@OutlinedButton
                                    isImporting = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val res = bibleRepository.loadExpandedReferencePack(
                                            translation = defaultTranslation,
                                            uploadedBy = "Admin"
                                        )
                                        isImporting = false
                                        res.fold(
                                            onSuccess = { count -> onImportSuccess(count) },
                                            onFailure = { ex -> errorMessage = ex.message }
                                        )
                                    }
                                },
                                enabled = !isImporting,
                                modifier = Modifier.fillMaxWidth().testTag("btn_load_expanded_bible_pack")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync 66-Book Reference Bible ($defaultTranslation)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedFileName?.let { "Loaded: $it" } ?: "Load .txt / .csv / .json File",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedFormatTab == 1) "Reference | Verse Text | Version | Theme" else "JSON Array of Verses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = {
                                rawInputText = if (selectedFormatTab == 1) sampleDelimited else sampleJson
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fill Sample Data", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = rawInputText,
                        onValueChange = {
                            rawInputText = it
                            errorMessage = null
                        },
                        placeholder = {
                            Text(
                                if (selectedFormatTab == 1)
                                    "Paste lines: Reference | Verse Text | Translation | Theme"
                                else
                                    "Paste JSON array of verses..."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("input_bible_upload_content"),
                        maxLines = 10
                    )
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "⚠️ $err", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (isImporting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Extracting & syncing Bible to backend...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bibleRepository == null) {
                        errorMessage = "Bible repository is unavailable"
                        return@Button
                    }

                    if (selectedFormatTab == 0) {
                        val uri = selectedFileUri
                        if (uri == null) {
                            errorMessage = "Please select a .pdf or .epub Bible file from your device, or tap 'Sync 66-Book Reference Bible' above."
                            return@Button
                        }
                        isImporting = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = bibleRepository.uploadBibleDocument(
                                context = context,
                                uri = uri,
                                customTitle = customEditionTitle.takeIf { it.isNotBlank() },
                                translation = defaultTranslation,
                                uploadedBy = "Admin"
                            )
                            isImporting = false
                            result.fold(
                                onSuccess = { extracted ->
                                    onImportSuccess(extracted.extractedVerses.size.coerceAtLeast(extracted.edition.pageOrChapterCount))
                                },
                                onFailure = { ex ->
                                    errorMessage = "Upload failed: ${ex.message}"
                                }
                            )
                        }
                    } else {
                        if (rawInputText.isBlank()) {
                            errorMessage = "Please enter or paste scripture data to import"
                            return@Button
                        }
                        isImporting = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = if (selectedFormatTab == 1) {
                                bibleRepository.importFromDelimitedText(rawInputText, defaultTranslation)
                            } else {
                                bibleRepository.importFromJson(rawInputText)
                            }
                            isImporting = false
                            result.fold(
                                onSuccess = { count ->
                                    if (count > 0) {
                                        onImportSuccess(count)
                                    } else {
                                        errorMessage = "No valid verses parsed. Please check format."
                                    }
                                },
                                onFailure = { ex ->
                                    errorMessage = "Import failed: ${ex.message}"
                                }
                            )
                        }
                    }
                },
                enabled = !isImporting,
                modifier = Modifier.testTag("btn_confirm_upload_bible")
            ) {
                Text(if (selectedFormatTab == 0) "Upload PDF/EPUB to Backend" else "Import & Sync to Backend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
