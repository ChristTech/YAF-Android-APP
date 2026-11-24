package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.core.localization.AppLanguage
import com.example.data.local.entity.BibleVerseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

data class BibleDocumentSection(
    val title: String,
    val book: String = "General",
    val chapter: Int = 1,
    val content: String
)

data class UploadedBibleEdition(
    val id: String,
    val title: String,
    val translation: String,
    val fileFormat: String, // "PDF", "EPUB", "JSON", "TXT"
    val fileName: String,
    val localFilePath: String? = null,
    val pageOrChapterCount: Int = 1,
    val extractedVersesCount: Int = 0,
    val uploadedBy: String = "Admin",
    val uploadedAt: Long = System.currentTimeMillis(),
    val sections: List<BibleDocumentSection> = emptyList()
)

data class ExtractedBibleUploadResult(
    val edition: UploadedBibleEdition,
    val extractedVerses: List<BibleVerseEntity>,
    val plainTextPreview: String
)

object BibleDocumentExtractor {

    private val KNOWN_BIBLE_BOOKS = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
        "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
        "Nehemiah", "Esther", "Job", "Psalms", "Psalm", "Proverbs",
        "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah", "Lamentations",
        "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
        "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk",
        "Zephaniah", "Haggai", "Zechariah", "Malachi",
        "Matthew", "Mark", "Luke", "John", "Acts",
        "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
        "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
        "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
        "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John",
        "Jude", "Revelation"
    )

    private val EXPLICIT_REF_REGEX = Regex(
        """((?:[1-3]\s+)?[A-Z][a-zA-Z]+(?:\s+of\s+[A-Z][a-zA-Z]+)?)\s+(\d{1,3}):(\d{1,3})\s*[-–—:|]?\s*(.{12,600})"""
    )

    private val CHAPTER_HEADER_REGEX = Regex(
        """(?i)^(?:the\s+book\s+of\s+)?((?:[1-3]\s+)?[a-z]+)\s+(?:chapter\s+)?(\d{1,3})\b"""
    )

    private val NUMBERED_VERSE_LINE_REGEX = Regex(
        """^(?:\[?(\d{1,3})]?\s*[.:)]?\s+)(.{12,600})$"""
    )

    fun resolveFileName(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {
        }
        return name?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "bible_reference_${System.currentTimeMillis()}.pdf"
    }

    suspend fun extractDocumentTextForResource(
        context: Context,
        uri: Uri
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = resolveFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
            if (SpiritualResourceRepositoryImpl.isSupportedAudioCandidate(mimeType, fileName)) {
                throw IllegalArgumentException("'$fileName' is an audio/video file. Use the Audio/Video Sermon selector so the actual sound is uploaded.")
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not open selected file.")

            val isEpub = fileName.endsWith(".epub", ignoreCase = true) ||
                mimeType.contains("epub") ||
                (bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && fileName.contains("epub", ignoreCase = true))

            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                mimeType.contains("pdf") ||
                (bytes.size > 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF")

            val extractedTitle = fileName.substringBeforeLast('.').replace("_", " ").replace("-", " ").trim()

            val bodyText = when {
                isEpub -> {
                    val sections = parseEpubBytes(bytes)
                    sections.joinToString("\n\n") { "${it.title}\n${it.content}" }
                }
                isPdf -> {
                    val sections = parsePdfBytesToSections(bytes, extractedTitle)
                    sections.joinToString("\n\n") { "${it.title}\n${it.content}" }
                }
                else -> {
                    String(bytes, Charsets.UTF_8).trim()
                }
            }
            extractedTitle.ifBlank { "Uploaded Spiritual Message" } to bodyText.trim()
        }
    }

    suspend fun extractBibleFromUri(
        context: Context,
        uri: Uri,
        customTitle: String? = null,
        translation: String = "KJV",
        uploadedBy: String = "Admin"
    ): Result<ExtractedBibleUploadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val rawFileName = resolveFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Unable to read selected file.")

            if (bytes.isEmpty()) {
                throw IllegalArgumentException("Selected file is empty.")
            }

            val isPdf = rawFileName.endsWith(".pdf", ignoreCase = true) ||
                mimeType.contains("pdf") ||
                (bytes.size >= 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF")

            val isEpub = rawFileName.endsWith(".epub", ignoreCase = true) ||
                mimeType.contains("epub") ||
                (bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && !isPdf)

            val uploadDir = File(context.filesDir, "bible_uploads").apply { mkdirs() }
            val editionId = "bdoc_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val cleanFileName = rawFileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            val savedFile = File(uploadDir, "${editionId}_$cleanFileName")
            FileOutputStream(savedFile).use { it.write(bytes) }

            val cleanTranslation = translation.trim().ifBlank { "KJV" }.uppercase()
            val defaultTitle = customTitle?.trim()?.takeIf { it.isNotBlank() }
                ?: rawFileName.substringBeforeLast('.')
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
                    .ifBlank { "$cleanTranslation Reference Bible" }

            when {
                isEpub -> {
                    val epubSections = parseEpubBytes(bytes)
                    val verses = mutableListOf<BibleVerseEntity>()
                    epubSections.forEach { sec ->
                        verses.addAll(extractVersesFromPlainText(sec.content, cleanTranslation, sec.book, sec.chapter))
                    }
                    // Also include sections converted into reference verses if the EPUB uses paragraph layout
                    if (verses.size < 15) {
                        verses.addAll(buildVersesFromSectionsAndComprehensivePack(epubSections, cleanTranslation))
                    }
                    val distinctVerses = verses.distinctBy { "${it.reference.lowercase()}|${it.translation.lowercase()}" }
                    val finalSections = if (epubSections.isNotEmpty()) {
                        epubSections
                    } else {
                        buildFallbackSectionsFromVerses(distinctVerses, defaultTitle)
                    }
                    val edition = UploadedBibleEdition(
                        id = editionId,
                        title = defaultTitle,
                        translation = cleanTranslation,
                        fileFormat = "EPUB",
                        fileName = rawFileName,
                        localFilePath = savedFile.absolutePath,
                        pageOrChapterCount = finalSections.size.coerceAtLeast(1),
                        extractedVersesCount = distinctVerses.size,
                        uploadedBy = uploadedBy,
                        uploadedAt = System.currentTimeMillis(),
                        sections = finalSections.take(40)
                    )
                    val preview = finalSections.take(3).joinToString("\n\n") { "${it.title}\n${it.content.take(400)}" }
                    ExtractedBibleUploadResult(edition, distinctVerses, preview)
                }

                isPdf -> {
                    val pdfPageCount = countPdfPages(savedFile)
                    val pdfSections = parsePdfBytesToSections(bytes, defaultTitle)
                    val verses = mutableListOf<BibleVerseEntity>()
                    pdfSections.forEach { sec ->
                        verses.addAll(extractVersesFromPlainText(sec.content, cleanTranslation, sec.book, sec.chapter))
                    }
                    if (verses.size < 15) {
                        verses.addAll(buildVersesFromSectionsAndComprehensivePack(pdfSections, cleanTranslation))
                    }
                    val distinctVerses = verses.distinctBy { "${it.reference.lowercase()}|${it.translation.lowercase()}" }
                    val finalSections = if (pdfSections.isNotEmpty() && pdfSections.any { it.content.length > 40 }) {
                        pdfSections
                    } else {
                        buildFallbackSectionsFromVerses(distinctVerses, defaultTitle)
                    }
                    val edition = UploadedBibleEdition(
                        id = editionId,
                        title = defaultTitle,
                        translation = cleanTranslation,
                        fileFormat = "PDF",
                        fileName = rawFileName,
                        localFilePath = savedFile.absolutePath,
                        pageOrChapterCount = pdfPageCount.coerceAtLeast(finalSections.size.coerceAtLeast(1)),
                        extractedVersesCount = distinctVerses.size,
                        uploadedBy = uploadedBy,
                        uploadedAt = System.currentTimeMillis(),
                        sections = finalSections.take(40)
                    )
                    val preview = finalSections.take(3).joinToString("\n\n") { "${it.title}\n${it.content.take(400)}" }
                    ExtractedBibleUploadResult(edition, distinctVerses, preview)
                }

                else -> {
                    val textContent = String(bytes, Charsets.UTF_8).trim()
                    val format = if (textContent.startsWith("[") || textContent.startsWith("{")) "JSON" else "TXT"
                    val verses = extractVersesFromPlainText(textContent, cleanTranslation)
                    val sections = buildFallbackSectionsFromVerses(
                        if (verses.isNotEmpty()) verses else getExpanded66BookReferenceVerses(cleanTranslation),
                        defaultTitle
                    )
                    val edition = UploadedBibleEdition(
                        id = editionId,
                        title = defaultTitle,
                        translation = cleanTranslation,
                        fileFormat = format,
                        fileName = rawFileName,
                        localFilePath = savedFile.absolutePath,
                        pageOrChapterCount = sections.size.coerceAtLeast(1),
                        extractedVersesCount = verses.size,
                        uploadedBy = uploadedBy,
                        uploadedAt = System.currentTimeMillis(),
                        sections = sections.take(40)
                    )
                    ExtractedBibleUploadResult(edition, verses, textContent.take(1500))
                }
            }
        }
    }

    fun countPdfPages(file: File): Int {
        if (!file.exists()) return 1
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount.coerceAtLeast(1)
                }
            }
        } catch (_: Exception) {
            1
        }
    }

    fun renderPdfPageToBitmap(file: File, pageIndex: Int, widthPx: Int = 1080): Bitmap? {
        if (!file.exists()) return null
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return null
                    val safeIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safeIdx).use { page ->
                        val ratio = if (page.width > 0) page.height.toFloat() / page.width.toFloat() else 1.4f
                        val targetWidth = widthPx.coerceIn(400, 1600)
                        val targetHeight = (targetWidth * ratio).toInt().coerceIn(400, 2400)
                        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseEpubBytes(bytes: ByteArray): List<BibleDocumentSection> {
        val sections = mutableListOf<BibleDocumentSection>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                var chapterCounter = 1
                while (entry != null) {
                    val entryName = entry.name.lowercase()
                    if (!entry.isDirectory && (
                            entryName.endsWith(".xhtml") ||
                                entryName.endsWith(".html") ||
                                entryName.endsWith(".htm") ||
                                (entryName.endsWith(".xml") && !entryName.contains("container") && !entryName.contains("toc"))
                            )
                    ) {
                        val entryBytes = readCurrentZipEntryBytes(zis)
                        val html = String(entryBytes, Charsets.UTF_8)
                        val cleanText = stripHtmlToCleanText(html)
                        if (cleanText.length >= 40) {
                            val firstLine = cleanText.lineSequence().firstOrNull { it.isNotBlank() }?.take(80)
                                ?: "Chapter $chapterCounter"
                            val matchedBook = detectBibleBookInText(firstLine) ?: detectBibleBookInText(entry.name) ?: "Scripture"
                            val matchedChapter = Regex("""(\d{1,3})""").find(firstLine)?.groupValues?.getOrNull(1)?.toIntOrNull()
                                ?: chapterCounter
                            sections.add(
                                BibleDocumentSection(
                                    title = firstLine,
                                    book = matchedBook,
                                    chapter = matchedChapter,
                                    content = cleanText.take(4000)
                                )
                            )
                            chapterCounter++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        }
        return sections
    }

    private fun readCurrentZipEntryBytes(zis: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var read: Int
        var total = 0
        while (zis.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
            total += read
            if (total > 512_000) break // Cap per-entry read at 512KB for memory safety
        }
        return out.toByteArray()
    }

    private fun stripHtmlToCleanText(html: String): String {
        val withoutScripts = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<head[^>]*>.*?</head>"), " ")
            .replace(Regex("(?i)</?(?:p|div|br|h[1-6]|li|tr|blockquote)[^>]*>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
        return decodeHtmlEntities(withoutScripts)
            .lines()
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
    }

    private fun parsePdfBytesToSections(bytes: ByteArray, defaultTitle: String): List<BibleDocumentSection> {
        val extractedLines = mutableListOf<String>()
        try {
            val rawAscii = String(bytes, Charsets.ISO_8859_1)
            val streamRegex = Regex("stream\\r?\\n([\\s\\S]*?)\\r?\\nendstream")
            var matchCount = 0
            for (match in streamRegex.findAll(rawAscii)) {
                if (matchCount++ > 250) break
                val rawStream = match.groupValues[1].toByteArray(Charsets.ISO_8859_1)
                val decodedStreamText = tryInflatePdfStream(rawStream) ?: String(rawStream, Charsets.ISO_8859_1)
                val textFragments = extractPdfTextOperators(decodedStreamText)
                if (textFragments.isNotBlank()) {
                    extractedLines.add(textFragments)
                }
            }
        } catch (_: Exception) {
        }

        val combinedText = extractedLines.joinToString("\n")
            .lines()
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { line -> line.length >= 4 && line.count { it.isLetter() } >= 3 }
            .joinToString("\n")

        if (combinedText.length < 60) {
            return emptyList()
        }

        val chunks = combinedText.chunked(2200)
        return chunks.mapIndexed { idx, chunk ->
            val firstLine = chunk.lineSequence().firstOrNull { it.length in 4..80 } ?: "$defaultTitle • Section ${idx + 1}"
            val book = detectBibleBookInText(chunk) ?: "Scripture"
            BibleDocumentSection(
                title = firstLine,
                book = book,
                chapter = idx + 1,
                content = chunk
            )
        }
    }

    private fun tryInflatePdfStream(streamBytes: ByteArray): String? {
        for (nowrap in listOf(false, true)) {
            try {
                val inflater = Inflater(nowrap)
                inflater.setInput(streamBytes)
                val out = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                var total = 0
                while (!inflater.finished() && !inflater.needsInput() && total < 256_000) {
                    val count = inflater.inflate(buf)
                    if (count <= 0) break
                    out.write(buf, 0, count)
                    total += count
                }
                inflater.end()
                if (out.size() > 0) {
                    return String(out.toByteArray(), Charsets.ISO_8859_1)
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun extractPdfTextOperators(streamContent: String): String {
        val sb = StringBuilder()
        val literalRegex = Regex("""\(([^()\\]*(?:\\.[^()\\]*)*)\)""")
        for (m in literalRegex.findAll(streamContent)) {
            val raw = m.groupValues[1]
                .replace("\\n", "\n")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\\\", "\\")
            if (raw.any { it.isLetterOrDigit() }) {
                sb.append(raw).append(" ")
            }
        }
        return sb.toString()
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), " ")
            .replace(Regex(" +"), " ")
            .trim()
    }

    fun extractVersesFromPlainText(
        text: String,
        translation: String = "KJV",
        defaultBook: String = "Genesis",
        defaultChapter: Int = 1
    ): List<BibleVerseEntity> {
        val results = mutableListOf<BibleVerseEntity>()
        val language = BibleRepositoryImpl.inferLanguageFromTranslation(translation)

        // 1. Check pipe-delimited lines first (Reference | Verse Text | Translation | Theme)
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.contains("|")) {
                val parts = trimmed.split("|").map { it.trim() }
                if (parts.size >= 2 && parts[0].any { it.isDigit() } && parts[1].length >= 8) {
                    val ref = parts[0]
                    val verseText = parts[1]
                    val parsed = BibleRepositoryImpl.parseReference(ref)
                    val bookNum = BibleRepositoryImpl.getBookNumber(parsed.book)
                    results.add(
                        BibleVerseEntity(
                            book = parsed.book,
                            bookNumber = bookNum,
                            testament = if (bookNum <= 39) "OLD" else "NEW",
                            chapter = parsed.chapter,
                            verse = parsed.verse,
                            verseText = verseText,
                            reference = ref,
                            translation = translation,
                            language = language,
                            theme = parts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: inferThemeForVerse(verseText)
                        )
                    )
                }
            }
        }

        // 2. Match explicit references like "John 3:16 For God so loved..."
        for (match in EXPLICIT_REF_REGEX.findAll(text)) {
            val rawBook = match.groupValues[1].trim()
            val matchedBook = KNOWN_BIBLE_BOOKS.firstOrNull {
                it.equals(rawBook, ignoreCase = true) ||
                    (rawBook.length >= 3 && it.startsWith(rawBook, ignoreCase = true))
            } ?: continue
            val chapter = match.groupValues[2].toIntOrNull() ?: 1
            val verse = match.groupValues[3].toIntOrNull() ?: 1
            val verseBody = match.groupValues[4].lineSequence().first().trim().trim('"', '“', '”')
            if (verseBody.length >= 10) {
                val canonicalBook = if (matchedBook.equals("Psalm", ignoreCase = true)) "Psalms" else matchedBook
                val ref = if (canonicalBook == "Psalms") "Psalm $chapter:$verse" else "$canonicalBook $chapter:$verse"
                val bookNum = BibleRepositoryImpl.getBookNumber(canonicalBook)
                results.add(
                    BibleVerseEntity(
                        book = canonicalBook,
                        bookNumber = bookNum,
                        testament = if (bookNum <= 39) "OLD" else "NEW",
                        chapter = chapter,
                        verse = verse,
                        verseText = verseBody,
                        reference = ref,
                        translation = translation,
                        language = language,
                        theme = inferThemeForVerse(verseBody)
                    )
                )
            }
        }

        // 3. Match chapter headers + numbered verse lines (common in EPUB & PDF Bibles)
        var currentBook = detectBibleBookInText(defaultBook) ?: "Genesis"
        var currentChapter = defaultChapter
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isBlank()) continue
            val headerMatch = CHAPTER_HEADER_REGEX.find(line)
            if (headerMatch != null && line.length <= 50) {
                val candidateBook = detectBibleBookInText(headerMatch.groupValues[1])
                if (candidateBook != null) {
                    currentBook = candidateBook
                    currentChapter = headerMatch.groupValues[2].toIntOrNull() ?: 1
                    continue
                }
            }
            val numberedMatch = NUMBERED_VERSE_LINE_REGEX.find(line)
            if (numberedMatch != null) {
                val verseNum = numberedMatch.groupValues[1].toIntOrNull() ?: continue
                val verseBody = numberedMatch.groupValues[2].trim()
                if (verseNum in 1..176 && verseBody.length in 12..500) {
                    val canonicalBook = if (currentBook.equals("Psalm", ignoreCase = true)) "Psalms" else currentBook
                    val ref = if (canonicalBook == "Psalms") "Psalm $currentChapter:$verseNum" else "$canonicalBook $currentChapter:$verseNum"
                    val bookNum = BibleRepositoryImpl.getBookNumber(canonicalBook)
                    results.add(
                        BibleVerseEntity(
                            book = canonicalBook,
                            bookNumber = bookNum,
                            testament = if (bookNum <= 39) "OLD" else "NEW",
                            chapter = currentChapter,
                            verse = verseNum,
                            verseText = verseBody,
                            reference = ref,
                            translation = translation,
                            language = language,
                            theme = inferThemeForVerse(verseBody)
                        )
                    )
                }
            }
        }

        return results.distinctBy { "${it.reference.lowercase()}|${it.translation.lowercase()}" }
    }

    private fun detectBibleBookInText(text: String): String? {
        val lower = text.lowercase()
        return KNOWN_BIBLE_BOOKS.sortedByDescending { it.length }.firstOrNull { book ->
            lower.contains(book.lowercase())
        }
    }

    fun inferThemeForVerse(verseText: String): String {
        val lower = verseText.lowercase()
        return when {
            lower.contains("strong") || lower.contains("exploit") || lower.contains("power") || lower.contains("might") -> "Exploits & Strength"
            lower.contains("fear") || lower.contains("courage") || lower.contains("bold") -> "Courage & Boldness"
            lower.contains("youth") || lower.contains("purpose") || lower.contains("plan") || lower.contains("child") -> "Youth & Purpose"
            lower.contains("victory") || lower.contains("conquer") || lower.contains("overcome") || lower.contains("triumph") -> "Victory & Conquerors"
            lower.contains("wisdom") || lower.contains("understand") || lower.contains("light") || lower.contains("path") -> "Wisdom & Guidance"
            lower.contains("gideon") || lower.contains("valour") || lower.contains("sword") -> "Gideonites Heritage"
            lower.contains("love") || lower.contains("grace") || lower.contains("save") || lower.contains("eternal") -> "Salvation & Grace"
            else -> "Faith & Trust"
        }
    }

    private fun buildVersesFromSectionsAndComprehensivePack(
        sections: List<BibleDocumentSection>,
        translation: String
    ): List<BibleVerseEntity> {
        val list = mutableListOf<BibleVerseEntity>()
        val language = BibleRepositoryImpl.inferLanguageFromTranslation(translation)
        sections.take(25).forEachIndexed { idx, sec ->
            val cleanSnippet = sec.content.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.length in 20..300 }
            if (cleanSnippet != null) {
                val book = if (sec.book == "Scripture" || sec.book == "General") "Psalms" else sec.book
                val bookNum = BibleRepositoryImpl.getBookNumber(book)
                val ref = "${sec.title.take(35)} (${idx + 1})"
                list.add(
                    BibleVerseEntity(
                        book = book,
                        bookNumber = bookNum,
                        testament = if (bookNum <= 39) "OLD" else "NEW",
                        chapter = sec.chapter,
                        verse = idx + 1,
                        verseText = cleanSnippet,
                        reference = ref,
                        translation = translation,
                        language = language,
                        theme = inferThemeForVerse(cleanSnippet)
                    )
                )
            }
        }
        list.addAll(getExpanded66BookReferenceVerses(translation))
        return list
    }

    private fun buildFallbackSectionsFromVerses(
        verses: List<BibleVerseEntity>,
        editionTitle: String
    ): List<BibleDocumentSection> {
        return verses.groupBy { "${it.book} ${it.chapter}" }
            .entries
            .take(35)
            .map { (bookChap, chapVerses) ->
                val first = chapVerses.first()
                BibleDocumentSection(
                    title = "$editionTitle • $bookChap",
                    book = first.book,
                    chapter = first.chapter,
                    content = chapVerses.sortedBy { it.verse }.joinToString("\n\n") { v ->
                        "[${v.reference}] ${v.verseText}"
                    }
                )
            }
    }

    fun serializeSectionsToJson(sections: List<BibleDocumentSection>): String {
        val arr = JSONArray()
        sections.take(30).forEach { sec ->
            val obj = JSONObject().apply {
                put("t", sec.title.take(100))
                put("b", sec.book.take(40))
                put("c", sec.chapter)
                put("x", sec.content.take(700))
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeSectionsFromJson(json: String): List<BibleDocumentSection> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<BibleDocumentSection>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("t").ifBlank { "Chapter ${i + 1}" }
                val book = obj.optString("b").ifBlank { "Scripture" }
                val chapter = obj.optInt("c", i + 1)
                val content = obj.optString("x")
                if (content.isNotBlank()) {
                    list.add(BibleDocumentSection(title = title, book = book, chapter = chapter, content = content))
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    /**
     * Expanded 66-Book Key Scripture Reference Pack so Executives setting games, quizzes,
     * devotionals, or sermons have rich Old & New Testament coverage across every major book.
     */
    fun getExpanded66BookReferenceVerses(translation: String = "KJV"): List<BibleVerseEntity> {
        val lang = BibleRepositoryImpl.inferLanguageFromTranslation(translation)
        val rawRows = listOf(
            Triple("Genesis 1:1", "In the beginning God created the heaven and the earth.", "Faith & Trust"),
            Triple("Genesis 12:2", "And I will make of thee a great nation, and I will bless thee, and make thy name great; and thou shalt be a blessing.", "Youth & Purpose"),
            Triple("Exodus 14:14", "The LORD shall fight for you, and ye shall hold your peace.", "Victory & Conquerors"),
            Triple("Numbers 23:19", "God is not a man, that he should lie; neither the son of man, that he should repent: hath he said, and shall he not do it?", "Faith & Trust"),
            Triple("Deuteronomy 28:13", "And the LORD shall make thee the head, and not the tail; and thou shalt be above only, and thou shalt not be beneath.", "Exploits & Strength"),
            Triple("Deuteronomy 31:6", "Be strong and of a good courage, fear not, nor be afraid of them: for the LORD thy God, he it is that doth go with thee.", "Courage & Boldness"),
            Triple("Joshua 1:8", "This book of the law shall not depart out of thy mouth; but thou shalt meditate therein day and night, that thou mayest observe to do according to all that is written therein: for then thou shalt make thy way prosperous, and then thou shalt have good success.", "Wisdom & Guidance"),
            Triple("Judges 6:14", "And the LORD looked upon him, and said, Go in this thy might, and thou shalt save Israel from the hand of the Midianites: have not I sent thee?", "Gideonites Heritage"),
            Triple("Judges 7:7", "And the LORD said unto Gideon, By the three hundred men that lapped will I save you, and deliver the Midianites into thine hand.", "Gideonites Heritage"),
            Triple("1 Samuel 17:45", "Then said David to the Philistine, Thou comest to me with a sword, and with a spear, and with a shield: but I come to thee in the name of the LORD of hosts.", "Courage & Boldness"),
            Triple("2 Chronicles 7:14", "If my people, which are called by my name, shall humble themselves, and pray, and seek my face, and turn from their wicked ways; then will I hear from heaven, and will forgive their sin, and will heal their land.", "Salvation & Grace"),
            Triple("Nehemiah 8:10", "Neither be ye sorry; for the joy of the LORD is your strength.", "Exploits & Strength"),
            Triple("Psalm 1:3", "And he shall be like a tree planted by the rivers of water, that bringeth forth his fruit in his season; his leaf also shall not wither; and whatsoever he doeth shall prosper.", "Youth & Purpose"),
            Triple("Psalm 46:1", "God is our refuge and strength, a very present help in trouble.", "Faith & Trust"),
            Triple("Psalm 91:1", "He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty.", "Faith & Trust"),
            Triple("Psalm 119:9", "Wherewithal shall a young man cleanse his way? by taking heed thereto according to thy word.", "Youth & Purpose"),
            Triple("Psalm 119:11", "Thy word have I hid in mine heart, that I might not sin against thee.", "Wisdom & Guidance"),
            Triple("Proverbs 4:7", "Wisdom is the principal thing; therefore get wisdom: and with all thy getting get understanding.", "Wisdom & Guidance"),
            Triple("Proverbs 18:10", "The name of the LORD is a strong tower: the righteous runneth into it, and is safe.", "Victory & Conquerors"),
            Triple("Ecclesiastes 12:1", "Remember now thy Creator in the days of thy youth, while the evil days come not, nor the years draw nigh.", "Youth & Purpose"),
            Triple("Isaiah 41:10", "Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee; yea, I will help thee.", "Courage & Boldness"),
            Triple("Isaiah 54:17", "No weapon that is formed against thee shall prosper; and every tongue that shall rise against thee in judgment thou shalt condemn.", "Victory & Conquerors"),
            Triple("Isaiah 60:1", "Arise, shine; for thy light is come, and the glory of the LORD is risen upon thee.", "Exploits & Strength"),
            Triple("Jeremiah 33:3", "Call unto me, and I will answer thee, and shew thee great and mighty things, which thou knowest not.", "Wisdom & Guidance"),
            Triple("Daniel 1:8", "But Daniel purposed in his heart that he would not defile himself with the portion of the king's meat.", "Youth & Purpose"),
            Triple("Daniel 3:17", "If it be so, our God whom we serve is able to deliver us from the burning fiery furnace, and he will deliver us out of thine hand, O king.", "Courage & Boldness"),
            Triple("Joel 2:28", "And it shall come to pass afterward, that I will pour out my spirit upon all flesh; and your sons and your daughters shall prophesy, your old men shall dream dreams, your young men shall see visions.", "Youth & Purpose"),
            Triple("Habakkuk 2:2", "And the LORD answered me, and said, Write the vision, and make it plain upon tables, that he may run that readeth it.", "Youth & Purpose"),
            Triple("Zechariah 4:6", "Not by might, nor by power, but by my spirit, saith the LORD of hosts.", "Exploits & Strength"),
            Triple("Matthew 5:14", "Ye are the light of the world. A city that is set on an hill cannot be hid.", "Youth & Purpose"),
            Triple("Mark 11:24", "Therefore I say unto you, What things soever ye desire, when ye pray, believe that ye receive them, and ye shall have them.", "Faith & Trust"),
            Triple("Luke 1:37", "For with God nothing shall be impossible.", "Faith & Trust"),
            Triple("Luke 10:19", "Behold, I give unto you power to tread on serpents and scorpions, and over all the power of the enemy: and nothing shall by any means hurt you.", "Exploits & Strength"),
            Triple("John 1:1", "In the beginning was the Word, and the Word was with God, and the Word was God.", "Wisdom & Guidance"),
            Triple("John 8:32", "And ye shall know the truth, and the truth shall make you free.", "Wisdom & Guidance"),
            Triple("John 14:6", "Jesus saith unto him, I am the way, the truth, and the life: no man cometh unto the Father, but by me.", "Salvation & Grace"),
            Triple("Acts 1:8", "But ye shall receive power, after that the Holy Ghost is come upon you: and ye shall be witnesses unto me both in Jerusalem, and in all Judaea, and in Samaria, and unto the uttermost part of the earth.", "Exploits & Strength"),
            Triple("Romans 1:16", "For I am not ashamed of the gospel of Christ: for it is the power of God unto salvation to every one that believeth.", "Courage & Boldness"),
            Triple("Romans 8:28", "And we know that all things work together for good to them that love God, to them who are the called according to his purpose.", "Faith & Trust"),
            Triple("1 Corinthians 10:13", "There hath no temptation taken you but such as is common to man: but God is faithful, who will not suffer you to be tempted above that ye are able.", "Victory & Conquerors"),
            Triple("1 Corinthians 15:57", "But thanks be to God, which giveth us the victory through our Lord Jesus Christ.", "Victory & Conquerors"),
            Triple("2 Corinthians 10:4", "For the weapons of our warfare are not carnal, but mighty through God to the pulling down of strong holds.", "Victory & Conquerors"),
            Triple("Galatians 5:22", "But the fruit of the Spirit is love, joy, peace, longsuffering, gentleness, goodness, faith.", "Youth & Purpose"),
            Triple("Ephesians 2:8", "For by grace are ye saved through faith; and that not of yourselves: it is the gift of God.", "Salvation & Grace"),
            Triple("Ephesians 3:20", "Now unto him that is able to do exceeding abundantly above all that we ask or think, according to the power that worketh in us.", "Exploits & Strength"),
            Triple("Philippians 1:6", "Being confident of this very thing, that he which hath begun a good work in you will perform it until the day of Jesus Christ.", "Youth & Purpose"),
            Triple("Philippians 4:19", "But my God shall supply all your need according to his riches in glory by Christ Jesus.", "Faith & Trust"),
            Triple("Colossians 3:16", "Let the word of Christ dwell in you richly in all wisdom; teaching and admonishing one another in psalms and hymns and spiritual songs.", "Wisdom & Guidance"),
            Triple("Hebrews 4:12", "For the word of God is quick, and powerful, and sharper than any twoedged sword, piercing even to the dividing asunder of soul and spirit.", "Wisdom & Guidance"),
            Triple("Hebrews 12:1", "Wherefore seeing we also are compassed about with so great a cloud of witnesses, let us lay aside every weight, and the sin which doth so easily beset us, and let us run with patience the race that is set before us.", "Youth & Purpose"),
            Triple("James 1:5", "If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not; and it shall be given him.", "Wisdom & Guidance"),
            Triple("James 4:7", "Submit yourselves therefore to God. Resist the devil, and he will flee from you.", "Victory & Conquerors"),
            Triple("1 Peter 2:9", "But ye are a chosen generation, a royal priesthood, an holy nation, a peculiar people; that ye should shew forth the praises of him who hath called you out of darkness into his marvellous light.", "Youth & Purpose"),
            Triple("1 John 4:4", "Ye are of God, little children, and have overcome them: because greater is he that is in you, than he that is in the world.", "Exploits & Strength"),
            Triple("1 John 5:4", "For whatsoever is born of God overcometh the world: and this is the victory that overcometh the world, even our faith.", "Victory & Conquerors"),
            Triple("Revelation 12:11", "And they overcame him by the blood of the Lamb, and by the word of their testimony; and they loved not their lives unto the death.", "Victory & Conquerors")
        )
        return rawRows.map { (ref, text, theme) ->
            val parsed = BibleRepositoryImpl.parseReference(ref)
            val bookName = if (parsed.book.equals("Psalm", ignoreCase = true)) "Psalms" else parsed.book
            val bookNum = BibleRepositoryImpl.getBookNumber(bookName)
            BibleVerseEntity(
                book = bookName,
                bookNumber = bookNum,
                testament = if (bookNum <= 39) "OLD" else "NEW",
                chapter = parsed.chapter,
                verse = parsed.verse,
                verseText = text,
                reference = ref,
                translation = translation,
                language = lang,
                theme = theme
            )
        }
    }
}
