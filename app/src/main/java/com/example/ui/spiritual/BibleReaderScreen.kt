package com.example.ui.spiritual

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.AppLanguage
import com.example.core.localization.LanguageManager
import com.example.data.local.entity.BibleVerseEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.model.UserRole
import com.example.data.repository.BibleRepository
import com.example.data.repository.UploadedBibleEdition
import com.example.ui.engagement.UploadBibleDatabaseDialog
import com.example.ui.engagement.UploadedBibleDocumentReaderDialog
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver

@Composable
fun BibleReaderScreen(
    bibleRepository: BibleRepository,
    session: SessionEntity,
    modifier: Modifier = Modifier
) {
    val activeLang = LanguageManager.currentLanguage.value
    val appStrings = LanguageManager.strings(activeLang)

    var searchQuery by remember { mutableStateOf("") }
    var filterByActiveLanguage by remember { mutableStateOf(true) }
    var selectedTranslation by remember { mutableStateOf("ALL") }
    var selectedTheme by remember { mutableStateOf("ALL") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var openedEdition by remember { mutableStateOf<UploadedBibleEdition?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bibleRepository) {
        bibleRepository.ensureSeeded()
    }

    // Auto-sync Bible Explorer whenever the user toggles language in the TopAppBar
    LaunchedEffect(activeLang) {
        filterByActiveLanguage = true
        selectedTranslation = "ALL"
    }

    val allVerses by bibleRepository.getAllVersesFlow().collectAsState(initial = emptyList())
    val uploadedEditions by bibleRepository.getUploadedEditionsFlow().collectAsState()

    val languageFilteredBase = remember(allVerses, activeLang, filterByActiveLanguage) {
        if (!filterByActiveLanguage) {
            allVerses
        } else {
            val matching = allVerses.filter { it.language == activeLang }
            if (matching.isNotEmpty()) matching else allVerses
        }
    }

    val availableTranslations = remember(languageFilteredBase) {
        listOf("ALL") + languageFilteredBase.map { it.translation.uppercase() }.distinct().sorted()
    }

    val availableThemes = remember(languageFilteredBase) {
        listOf("ALL") + languageFilteredBase.map { it.theme }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredVerses = remember(languageFilteredBase, searchQuery, selectedTranslation, selectedTheme) {
        languageFilteredBase.filter { verse ->
            val matchesTranslation = selectedTranslation == "ALL" ||
                verse.translation.equals(selectedTranslation, ignoreCase = true)
            val matchesTheme = selectedTheme == "ALL" ||
                verse.theme.equals(selectedTheme, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                verse.reference.contains(q, ignoreCase = true) ||
                verse.verseText.contains(q, ignoreCase = true) ||
                verse.book.contains(q, ignoreCase = true) ||
                verse.theme.contains(q, ignoreCase = true)
            matchesTranslation && matchesTheme && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("bible_reader_screen")
    ) {
        // Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GlobeBlack),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(FlameGoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = FlameGoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "The Gideonites - Youth Alive • ${appStrings.tabBibleExplorer}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${filteredVerses.size} Scriptures • ${uploadedEditions.size} PDF/EPUB Bibles",
                            style = MaterialTheme.typography.labelSmall,
                            color = FlameGoldAccent
                        )
                    }
                }

                if (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE) {
                    OutlinedButton(
                        onClick = { showUploadDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FlameGoldAccent),
                        modifier = Modifier.testTag("btn_upload_bible_spiritual")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = FlameGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload PDF/EPUB", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        statusBanner?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (uploadedEditions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "BACKEND PDF & EPUB REFERENCE BIBLES (TAP TO OPEN):",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uploadedEditions, key = { it.id }) { edition ->
                    Card(
                        onClick = { openedEdition = edition },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, FlameGoldAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Surface(
                                color = FlameGoldAccent,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = edition.fileFormat,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = GlobeBlack,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = edition.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${edition.translation} • ${edition.pageOrChapterCount} Sections",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(appStrings.bibleSearchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bible_reader_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Language Auto-Sync, Translation & Theme Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = filterByActiveLanguage,
                    onClick = { filterByActiveLanguage = !filterByActiveLanguage },
                    label = {
                        Text(
                            text = if (filterByActiveLanguage) appStrings.bibleAutoSyncedLabel else appStrings.bibleAllLanguagesLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("chip_bible_language_sync")
                )
            }
            items(availableTranslations) { trans ->
                FilterChip(
                    selected = selectedTranslation == trans,
                    onClick = { selectedTranslation = trans },
                    label = { Text(if (trans == "ALL") "All Versions" else trans, fontSize = 11.sp) }
                )
            }
            items(availableThemes) { theme ->
                FilterChip(
                    selected = selectedTheme == theme,
                    onClick = { selectedTheme = theme },
                    label = { Text(if (theme == "ALL") "All Themes" else theme, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredVerses, key = { it.id }) { verse ->
                BibleVerseReaderCard(verse = verse)
            }
        }
    }

    if (showUploadDialog) {
        UploadBibleDatabaseDialog(
            bibleRepository = bibleRepository,
            onDismiss = { showUploadDialog = false },
            onImportSuccess = { count ->
                statusBanner = "Uploaded & synced $count scriptures to the Bible backend!"
                showUploadDialog = false
            }
        )
    }

    openedEdition?.let { edition ->
        UploadedBibleDocumentReaderDialog(
            edition = edition,
            onDismiss = { openedEdition = null }
        )
    }
}

@Composable
private fun BibleVerseReaderCard(
    verse: BibleVerseEntity
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = FlameRedPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = verse.reference,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = FlameRedPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = FlameGoldAccent,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = verse.translation,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlobeBlack,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                if (verse.theme.isNotBlank()) {
                    Text(
                        text = verse.theme,
                        style = MaterialTheme.typography.labelSmall,
                        color = ShieldSilver
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "“${verse.verseText}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
