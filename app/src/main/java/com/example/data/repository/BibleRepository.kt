package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.core.localization.AppLanguage
import com.example.data.local.dao.BibleDao
import com.example.data.local.entity.BibleVerseEntity
import com.example.data.remote.CloudDailyScripture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

interface BibleRepository {
    fun getAllVersesFlow(): Flow<List<BibleVerseEntity>>
    fun getUploadedEditionsFlow(): StateFlow<List<UploadedBibleEdition>>
    fun searchVersesFlow(query: String, translation: String? = null): Flow<List<BibleVerseEntity>>
    suspend fun searchVerses(query: String, translation: String? = null): List<BibleVerseEntity>
    fun getVersesByBookAndChapterFlow(book: String, chapter: Int, translation: String? = null): Flow<List<BibleVerseEntity>>
    fun getDistinctBooksFlow(): Flow<List<String>>
    fun getDistinctTranslationsFlow(): Flow<List<String>>
    fun getDistinctThemesFlow(): Flow<List<String>>
    suspend fun getVerseByReference(reference: String, translation: String? = null): BibleVerseEntity?
    suspend fun getVerseCount(): Int
    suspend fun insertVerse(verse: BibleVerseEntity): Long
    suspend fun insertVerses(verses: List<BibleVerseEntity>)
    suspend fun importFromJson(jsonString: String): Result<Int>
    suspend fun importFromDelimitedText(text: String, defaultTranslation: String = "KJV"): Result<Int>
    suspend fun uploadBibleDocument(
        context: Context,
        uri: Uri,
        customTitle: String? = null,
        translation: String = "KJV",
        uploadedBy: String = "Admin"
    ): Result<ExtractedBibleUploadResult>
    suspend fun loadExpandedReferencePack(
        translation: String = "KJV",
        uploadedBy: String = "Admin"
    ): Result<Int>
    suspend fun deleteUploadedEdition(editionId: String): Result<Unit>
    suspend fun syncCloudBibleRecordsToLocalDb(cloudScriptures: List<CloudDailyScripture>)
    suspend fun ensureSeeded()
    suspend fun clearDatabase()
}

class BibleRepositoryImpl(
    private val bibleDao: BibleDao,
    private val cloudRepository: GideonitesCloudRepository? = null,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null,
    private val appContext: Context? = null
) : BibleRepository {

    private val _uploadedEditions = MutableStateFlow<List<UploadedBibleEdition>>(emptyList())

    init {
        loadPersistedEditionsFromDisk()
    }

    override fun getAllVersesFlow(): Flow<List<BibleVerseEntity>> = bibleDao.getAllVersesFlow()

    override fun getUploadedEditionsFlow(): StateFlow<List<UploadedBibleEdition>> =
        _uploadedEditions.asStateFlow()

    override fun searchVersesFlow(query: String, translation: String?): Flow<List<BibleVerseEntity>> {
        val cleanTranslation = if (translation.isNullOrBlank() || translation.equals("All", ignoreCase = true)) null else translation
        return bibleDao.searchVersesFlow(query.trim(), cleanTranslation)
    }

    override suspend fun searchVerses(query: String, translation: String?): List<BibleVerseEntity> {
        val cleanTranslation = if (translation.isNullOrBlank() || translation.equals("All", ignoreCase = true)) null else translation
        return bibleDao.searchVerses(query.trim(), cleanTranslation)
    }

    override fun getVersesByBookAndChapterFlow(
        book: String,
        chapter: Int,
        translation: String?
    ): Flow<List<BibleVerseEntity>> {
        val cleanTranslation = if (translation.isNullOrBlank() || translation.equals("All", ignoreCase = true)) null else translation
        return bibleDao.getVersesByBookAndChapterFlow(book, chapter, cleanTranslation)
    }

    override fun getDistinctBooksFlow(): Flow<List<String>> = bibleDao.getDistinctBooksFlow()

    override fun getDistinctTranslationsFlow(): Flow<List<String>> = bibleDao.getDistinctTranslationsFlow()

    override fun getDistinctThemesFlow(): Flow<List<String>> = bibleDao.getDistinctThemesFlow()

    override suspend fun getVerseByReference(reference: String, translation: String?): BibleVerseEntity? {
        val cleanTranslation = if (translation.isNullOrBlank() || translation.equals("All", ignoreCase = true)) null else translation
        return bibleDao.getVerseByReference(reference.trim(), cleanTranslation)
    }

    override suspend fun getVerseCount(): Int = bibleDao.getVerseCount()

    override suspend fun insertVerse(verse: BibleVerseEntity): Long = bibleDao.insertVerse(verse)

    override suspend fun insertVerses(verses: List<BibleVerseEntity>) {
        insertUniqueVerses(verses)
    }

    private suspend fun insertUniqueVerses(verses: List<BibleVerseEntity>): Int {
        if (verses.isEmpty()) return 0
        val existingKeys = bibleDao.searchVerses("", null)
            .map { "${it.reference.lowercase().trim()}|${it.translation.lowercase().trim()}" }
            .toHashSet()
        val uniqueNew = verses
            .distinctBy { "${it.reference.lowercase().trim()}|${it.translation.lowercase().trim()}" }
            .filter { "${it.reference.lowercase().trim()}|${it.translation.lowercase().trim()}" !in existingKeys }
        if (uniqueNew.isNotEmpty()) {
            bibleDao.insertVerses(uniqueNew)
        }
        return uniqueNew.size.coerceAtLeast(verses.size)
    }

    override suspend fun importFromJson(jsonString: String): Result<Int> {
        return runCatching {
            val list = mutableListOf<BibleVerseEntity>()
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseVerseJsonObject(obj))
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val array = root.optJSONArray("verses") ?: root.optJSONArray("bible") ?: JSONArray().put(root)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseVerseJsonObject(obj))
                }
            }
            if (list.isNotEmpty()) {
                insertUniqueVerses(list)
                syncVersesBatchToCloud("json_${UUID.randomUUID().toString().replace("-", "").take(8)}", list, "Admin")
            }
            list.size
        }
    }

    private fun parseVerseJsonObject(obj: JSONObject): BibleVerseEntity {
        val book = obj.optString("book", "Genesis")
        val chapter = obj.optInt("chapter", 1)
        val verse = obj.optInt("verse", 1)
        val ref = obj.optString("reference", "$book $chapter:$verse")
        val text = obj.optString("verseText", obj.optString("text", ""))
        val translation = obj.optString("translation", "KJV")
        val langStr = obj.optString("language", "")
        val language = if (langStr.isNotBlank()) AppLanguage.fromCodeOrName(langStr) else inferLanguageFromTranslation(translation)
        val theme = obj.optString("theme", "Faith & Exploits")
        val testament = obj.optString("testament", if (isOldTestament(book)) "OLD" else "NEW")
        val bookNumber = obj.optInt("bookNumber", getBookNumber(book))

        return BibleVerseEntity(
            book = book,
            bookNumber = bookNumber,
            testament = testament,
            chapter = chapter,
            verse = verse,
            verseText = text,
            reference = ref,
            translation = translation,
            language = language,
            theme = theme
        )
    }

    override suspend fun importFromDelimitedText(text: String, defaultTranslation: String): Result<Int> {
        return runCatching {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
            val list = mutableListOf<BibleVerseEntity>()

            for (line in lines) {
                // Format: Reference | Verse Text | Translation | Theme
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 2) {
                    val ref = parts[0]
                    val verseText = parts[1]
                    val translation = if (parts.size > 2 && parts[2].isNotBlank()) parts[2] else defaultTranslation
                    val theme = if (parts.size > 3 && parts[3].isNotBlank()) parts[3] else "Scripture Knowledge"

                    val parsed = parseReference(ref)

                    list.add(
                        BibleVerseEntity(
                            book = parsed.book,
                            bookNumber = getBookNumber(parsed.book),
                            testament = if (isOldTestament(parsed.book)) "OLD" else "NEW",
                            chapter = parsed.chapter,
                            verse = parsed.verse,
                            verseText = verseText,
                            reference = ref,
                            translation = translation,
                            language = inferLanguageFromTranslation(translation),
                            theme = theme
                        )
                    )
                }
            }
            if (list.isEmpty()) {
                list.addAll(BibleDocumentExtractor.extractVersesFromPlainText(text, defaultTranslation))
            }
            if (list.isNotEmpty()) {
                insertUniqueVerses(list)
                syncVersesBatchToCloud("txt_${UUID.randomUUID().toString().replace("-", "").take(8)}", list, "Admin")
            }
            list.size
        }
    }

    override suspend fun uploadBibleDocument(
        context: Context,
        uri: Uri,
        customTitle: String?,
        translation: String,
        uploadedBy: String
    ): Result<ExtractedBibleUploadResult> {
        return runCatching {
            val extracted = BibleDocumentExtractor.extractBibleFromUri(
                context = context,
                uri = uri,
                customTitle = customTitle,
                translation = translation,
                uploadedBy = uploadedBy
            ).getOrThrow()

            if (extracted.extractedVerses.isNotEmpty()) {
                insertUniqueVerses(extracted.extractedVerses)
            }

            val updatedEditions = (listOf(extracted.edition) + _uploadedEditions.value.filter { it.id != extracted.edition.id })
                .sortedByDescending { it.uploadedAt }
            _uploadedEditions.value = updatedEditions
            persistEditionsToDisk(updatedEditions)

            try {
                syncEditionToCloud(extracted.edition)
                syncVersesBatchToCloud(extracted.edition.id, extracted.extractedVerses, uploadedBy)
            } catch (_: Exception) {
            }

            notificationService?.postNotification(
                id = "notif_bible_${extracted.edition.id}",
                title = "📖 Bible Reference Uploaded: ${extracted.edition.title}",
                message = "${extracted.edition.fileFormat} Bible (${extracted.edition.translation}) with ${extracted.extractedVerses.size} indexed scriptures is now available for Executives when setting games & devotionals.",
                type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
                authorName = uploadedBy,
                authorRole = "Admin"
            )

            extracted
        }
    }

    override suspend fun loadExpandedReferencePack(
        translation: String,
        uploadedBy: String
    ): Result<Int> {
        return runCatching {
            val verses = BibleDocumentExtractor.getExpanded66BookReferenceVerses(translation)
            insertUniqueVerses(verses)

            val editionId = "bdoc_ref_${translation.lowercase().replace(Regex("[^a-z0-9]"), "")}"
            val sections = verses.groupBy { it.theme }.map { (themeName, themeVerses) ->
                BibleDocumentSection(
                    title = "$translation Reference • $themeName (${themeVerses.size} Verses)",
                    book = themeVerses.first().book,
                    chapter = themeVerses.first().chapter,
                    content = themeVerses.joinToString("\n\n") { v -> "[${v.reference}] “${v.verseText}”" }
                )
            }
            val edition = UploadedBibleEdition(
                id = editionId,
                title = "Gideonites 66-Book Executive Reference Bible ($translation)",
                translation = translation,
                fileFormat = "EPUB",
                fileName = "Gideonites_Executive_Reference_$translation.epub",
                pageOrChapterCount = sections.size,
                extractedVersesCount = verses.size,
                uploadedBy = uploadedBy,
                uploadedAt = System.currentTimeMillis(),
                sections = sections
            )
            val updatedEditions = (listOf(edition) + _uploadedEditions.value.filter { it.id != edition.id })
                .sortedByDescending { it.uploadedAt }
            _uploadedEditions.value = updatedEditions
            persistEditionsToDisk(updatedEditions)

            try {
                syncEditionToCloud(edition)
                syncVersesBatchToCloud(edition.id, verses, uploadedBy)
            } catch (_: Exception) {
            }

            verses.size
        }
    }

    override suspend fun deleteUploadedEdition(editionId: String): Result<Unit> {
        return runCatching {
            val target = _uploadedEditions.value.firstOrNull { it.id == editionId }
            target?.localFilePath?.let { path ->
                runCatching { File(path).delete() }
            }
            val remaining = _uploadedEditions.value.filter { it.id != editionId }
            _uploadedEditions.value = remaining
            persistEditionsToDisk(remaining)
            try {
                cloudRepository?.deleteDailyScripture("bibledoc_$editionId")
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun syncEditionToCloud(edition: UploadedBibleEdition) {
        val cloud = cloudRepository ?: return
        val metaJson = JSONObject().apply {
            put("category", "BIBLE_DOCUMENT")
            put("editionId", edition.id)
            put("title", edition.title.take(160))
            put("translation", edition.translation.take(30))
            put("fileFormat", edition.fileFormat.take(15))
            put("fileName", edition.fileName.take(100))
            put("pageOrChapterCount", edition.pageOrChapterCount)
            put("extractedVersesCount", edition.extractedVersesCount)
            put("uploadedAt", edition.uploadedAt)
        }.toString().take(980)

        val sectionsJson = BibleDocumentExtractor.serializeSectionsToJson(edition.sections)
        val p1 = sectionsJson.take(3000).ifBlank { "[]" }
        val p2 = sectionsJson.drop(3000).take(3000).takeIf { it.isNotBlank() }
        val p3 = sectionsJson.drop(6000).take(3000).takeIf { it.isNotBlank() }
        val p4 = sectionsJson.drop(9000).take(3000).takeIf { it.isNotBlank() }
        val p5 = sectionsJson.drop(12000).take(2000).takeIf { it.isNotBlank() }
        val p6 = sectionsJson.drop(14000).take(2000).takeIf { it.isNotBlank() }

        cloud.publishDailyScripture(
            scriptureId = "bibledoc_${edition.id}",
            authorName = edition.uploadedBy.ifBlank { "Admin" },
            dateKey = "BIBLE_EDITION",
            referenceEn = "${edition.translation} • ${edition.fileFormat} Bible",
            verseTextEn = "${edition.title} (${edition.extractedVersesCount} verses, ${edition.pageOrChapterCount} sections)",
            verseTextYo = p5,
            verseTextHa = p6,
            reflectionEn = p1,
            reflectionYo = p2,
            reflectionHa = p3,
            reflectionIg = p4,
            prayerPromptEn = metaJson
        )
    }

    private suspend fun syncVersesBatchToCloud(
        batchKey: String,
        verses: List<BibleVerseEntity>,
        uploadedBy: String
    ) {
        val cloud = cloudRepository ?: return
        val cleanBatchKey = batchKey.replace(Regex("[^a-zA-Z0-9]"), "").take(18)
        verses.chunked(35).take(4).forEachIndexed { chunkIdx, chunk ->
            val arr = JSONArray()
            chunk.forEach { v ->
                arr.put(
                    JSONObject().apply {
                        put("r", v.reference.take(60))
                        put("t", v.verseText.take(280))
                        put("v", v.translation.take(25))
                        put("m", v.theme.take(40))
                    }
                )
            }
            val fullJson = arr.toString()
            val p1 = fullJson.take(3000).ifBlank { "[]" }
            val p2 = fullJson.drop(3000).take(3000).takeIf { it.isNotBlank() }
            val p3 = fullJson.drop(6000).take(3000).takeIf { it.isNotBlank() }
            val p4 = fullJson.drop(9000).take(3000).takeIf { it.isNotBlank() }
            val metaJson = JSONObject().apply {
                put("category", "BIBLE_VERSE_PACK")
                put("batchKey", cleanBatchKey)
                put("chunkIdx", chunkIdx)
            }.toString()

            cloud.publishDailyScripture(
                scriptureId = "biblepack_${cleanBatchKey}_$chunkIdx",
                authorName = uploadedBy.ifBlank { "Admin" },
                dateKey = "BIBLE_PACK",
                referenceEn = "Bible Pack $cleanBatchKey #$chunkIdx",
                verseTextEn = "Synced ${chunk.size} Bible scriptures",
                reflectionEn = p1,
                reflectionYo = p2,
                reflectionHa = p3,
                reflectionIg = p4,
                prayerPromptEn = metaJson
            )
        }
    }

    override suspend fun syncCloudBibleRecordsToLocalDb(cloudScriptures: List<CloudDailyScripture>) {
        val remoteEditions = mutableListOf<UploadedBibleEdition>()
        val remoteVerses = mutableListOf<BibleVerseEntity>()

        for (s in cloudScriptures) {
            val prompt = s.prayerPromptEn.trim()
            if (!prompt.startsWith("{")) continue
            val meta = runCatching { JSONObject(prompt) }.getOrNull() ?: continue
            val category = meta.optString("category")

            if (category.equals("BIBLE_DOCUMENT", ignoreCase = true) || s.id.startsWith("scripture_bibledoc_")) {
                val editionId = meta.optString("editionId").ifBlank { s.id.removePrefix("scripture_bibledoc_") }
                val localExisting = _uploadedEditions.value.firstOrNull { it.id == editionId }
                val sectionsJson = buildString {
                    append(s.reflectionEn)
                    if (!s.reflectionYo.isNullOrBlank()) append(s.reflectionYo)
                    if (!s.reflectionHa.isNullOrBlank()) append(s.reflectionHa)
                    if (!s.reflectionIg.isNullOrBlank()) append(s.reflectionIg)
                    if (!s.verseTextYo.isNullOrBlank()) append(s.verseTextYo)
                    if (!s.verseTextHa.isNullOrBlank()) append(s.verseTextHa)
                }
                val sections = BibleDocumentExtractor.deserializeSectionsFromJson(sectionsJson)
                    .ifEmpty { localExisting?.sections.orEmpty() }

                remoteEditions.add(
                    UploadedBibleEdition(
                        id = editionId,
                        title = meta.optString("title").ifBlank { localExisting?.title ?: "Uploaded Fellowship Bible" },
                        translation = meta.optString("translation").ifBlank { "KJV" },
                        fileFormat = meta.optString("fileFormat").ifBlank { "PDF" },
                        fileName = meta.optString("fileName").ifBlank { "Fellowship_Bible.pdf" },
                        localFilePath = localExisting?.localFilePath,
                        pageOrChapterCount = meta.optInt("pageOrChapterCount", sections.size.coerceAtLeast(1)),
                        extractedVersesCount = meta.optInt("extractedVersesCount", 0),
                        uploadedBy = s.authorName.ifBlank { "Admin" },
                        uploadedAt = meta.optLong("uploadedAt", s.createdAt?.toDate()?.time ?: System.currentTimeMillis()),
                        sections = sections
                    )
                )
            } else if (category.equals("BIBLE_VERSE_PACK", ignoreCase = true) || s.id.startsWith("scripture_biblepack_")) {
                val packJson = buildString {
                    append(s.reflectionEn)
                    if (!s.reflectionYo.isNullOrBlank()) append(s.reflectionYo)
                    if (!s.reflectionHa.isNullOrBlank()) append(s.reflectionHa)
                    if (!s.reflectionIg.isNullOrBlank()) append(s.reflectionIg)
                }
                runCatching {
                    val arr = JSONArray(packJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val ref = obj.optString("r").trim()
                        val verseText = obj.optString("t").trim()
                        val trans = obj.optString("v", "KJV").trim()
                        val theme = obj.optString("m", "Faith & Trust").trim()
                        if (ref.isNotBlank() && verseText.isNotBlank()) {
                            val parsed = parseReference(ref)
                            val bookName = if (parsed.book.equals("Psalm", ignoreCase = true)) "Psalms" else parsed.book
                            val bookNum = getBookNumber(bookName)
                            remoteVerses.add(
                                BibleVerseEntity(
                                    book = bookName,
                                    bookNumber = bookNum,
                                    testament = if (bookNum <= 39) "OLD" else "NEW",
                                    chapter = parsed.chapter,
                                    verse = parsed.verse,
                                    verseText = verseText,
                                    reference = ref,
                                    translation = trans,
                                    language = inferLanguageFromTranslation(trans),
                                    theme = theme
                                )
                            )
                        }
                    }
                }
            }
        }

        if (remoteVerses.isNotEmpty()) {
            insertUniqueVerses(remoteVerses)
        }

        if (remoteEditions.isNotEmpty()) {
            val mergedMap = LinkedHashMap<String, UploadedBibleEdition>()
            remoteEditions.forEach { mergedMap[it.id] = it }
            _uploadedEditions.value.forEach { local ->
                if (!mergedMap.containsKey(local.id)) {
                    mergedMap[local.id] = local
                }
            }
            val mergedList = mergedMap.values.sortedByDescending { it.uploadedAt }
            _uploadedEditions.value = mergedList
            persistEditionsToDisk(mergedList)
        }
    }

    private fun loadPersistedEditionsFromDisk() {
        val ctx = appContext ?: return
        try {
            val indexFile = File(File(ctx.filesDir, "bible_uploads"), "editions_index.json")
            if (!indexFile.exists()) return
            val raw = indexFile.readText()
            val arr = JSONArray(raw)
            val list = mutableListOf<UploadedBibleEdition>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val sections = BibleDocumentExtractor.deserializeSectionsFromJson(obj.optString("sectionsJson", "[]"))
                list.add(
                    UploadedBibleEdition(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        translation = obj.optString("translation", "KJV"),
                        fileFormat = obj.optString("fileFormat", "PDF"),
                        fileName = obj.optString("fileName", "Bible.pdf"),
                        localFilePath = obj.optString("localFilePath").takeIf { it.isNotBlank() },
                        pageOrChapterCount = obj.optInt("pageOrChapterCount", 1),
                        extractedVersesCount = obj.optInt("extractedVersesCount", 0),
                        uploadedBy = obj.optString("uploadedBy", "Admin"),
                        uploadedAt = obj.optLong("uploadedAt", System.currentTimeMillis()),
                        sections = sections
                    )
                )
            }
            if (list.isNotEmpty()) {
                _uploadedEditions.value = list
            }
        } catch (_: Exception) {
        }
    }

    private fun persistEditionsToDisk(editions: List<UploadedBibleEdition>) {
        val ctx = appContext ?: return
        try {
            val dir = File(ctx.filesDir, "bible_uploads").apply { mkdirs() }
            val indexFile = File(dir, "editions_index.json")
            val arr = JSONArray()
            editions.forEach { ed ->
                val obj = JSONObject().apply {
                    put("id", ed.id)
                    put("title", ed.title)
                    put("translation", ed.translation)
                    put("fileFormat", ed.fileFormat)
                    put("fileName", ed.fileName)
                    put("localFilePath", ed.localFilePath ?: "")
                    put("pageOrChapterCount", ed.pageOrChapterCount)
                    put("extractedVersesCount", ed.extractedVersesCount)
                    put("uploadedBy", ed.uploadedBy)
                    put("uploadedAt", ed.uploadedAt)
                    put("sectionsJson", BibleDocumentExtractor.serializeSectionsToJson(ed.sections))
                }
                arr.put(obj)
            }
            indexFile.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    override suspend fun ensureSeeded() {
        val baseline = getBaselineSeedVerses() + BibleDocumentExtractor.getExpanded66BookReferenceVerses("KJV")
        if (bibleDao.getVerseCount() < baseline.size) {
            val existingRefs = bibleDao.searchVerses("", null).map { "${it.reference}|${it.translation}" }.toSet()
            val missing = baseline.filter { "${it.reference}|${it.translation}" !in existingRefs }
            if (missing.isNotEmpty()) {
                bibleDao.insertVerses(missing)
            }
        }
        if (_uploadedEditions.value.isEmpty()) {
            val defaultVerses = BibleDocumentExtractor.getExpanded66BookReferenceVerses("KJV")
            val defaultSections = defaultVerses.groupBy { it.theme }.map { (themeName, themeVerses) ->
                BibleDocumentSection(
                    title = "KJV Executive Reference • $themeName",
                    book = themeVerses.first().book,
                    chapter = themeVerses.first().chapter,
                    content = themeVerses.joinToString("\n\n") { v -> "[${v.reference}] “${v.verseText}”" }
                )
            }
            _uploadedEditions.value = listOf(
                UploadedBibleEdition(
                    id = "bdoc_default_kjv",
                    title = "Gideonites KJV Executive Reference Bible",
                    translation = "KJV",
                    fileFormat = "EPUB",
                    fileName = "Gideonites_KJV_Reference_Bible.epub",
                    pageOrChapterCount = defaultSections.size,
                    extractedVersesCount = baseline.size,
                    uploadedBy = "Fellowship Secretariat",
                    uploadedAt = 1761350400000L,
                    sections = defaultSections
                )
            )
        }
    }

    override suspend fun clearDatabase() {
        bibleDao.clearAllVerses()
    }

    companion object {
        data class ParsedRef(val book: String, val chapter: Int, val verse: Int)

        fun parseReference(ref: String): ParsedRef {
            return try {
                val lastSpace = ref.lastIndexOf(' ')
                if (lastSpace > 0) {
                    val bookPart = ref.substring(0, lastSpace).trim()
                    val numPart = ref.substring(lastSpace + 1).trim()
                    val colonIdx = numPart.indexOf(':')
                    if (colonIdx > 0) {
                        val chapter = numPart.substring(0, colonIdx).toIntOrNull() ?: 1
                        val verse = numPart.substring(colonIdx + 1).takeWhile { it.isDigit() }.toIntOrNull() ?: 1
                        ParsedRef(bookPart, chapter, verse)
                    } else {
                        ParsedRef(bookPart, numPart.toIntOrNull() ?: 1, 1)
                    }
                } else {
                    ParsedRef(ref, 1, 1)
                }
            } catch (e: Exception) {
                ParsedRef(ref, 1, 1)
            }
        }

        fun getBookNumber(book: String): Int {
            val b = book.lowercase().trim()
            return when {
                b.contains("gen") -> 1
                b.contains("exo") -> 2
                b.contains("lev") -> 3
                b.contains("num") -> 4
                b.contains("deu") -> 5
                b.contains("josh") -> 6
                b.contains("judg") -> 7
                b.contains("ruth") -> 8
                b.contains("1 sam") -> 9
                b.contains("2 sam") -> 10
                b.contains("1 king") -> 11
                b.contains("2 king") -> 12
                b.contains("1 chron") -> 13
                b.contains("2 chron") -> 14
                b.contains("ezra") -> 15
                b.contains("neh") -> 16
                b.contains("esth") -> 17
                b.contains("job") -> 18
                b.contains("psalm") -> 19
                b.contains("prov") -> 20
                b.contains("eccl") -> 21
                b.contains("song") -> 22
                b.contains("isa") -> 23
                b.contains("jer") -> 24
                b.contains("lam") -> 25
                b.contains("ezek") -> 26
                b.contains("dan") -> 27
                b.contains("hos") -> 28
                b.contains("joel") -> 29
                b.contains("amos") -> 30
                b.contains("obad") -> 31
                b.contains("jonah") -> 32
                b.contains("mic") -> 33
                b.contains("nah") -> 34
                b.contains("hab") -> 35
                b.contains("zeph") -> 36
                b.contains("hag") -> 37
                b.contains("zech") -> 38
                b.contains("mal") -> 39
                b.contains("matt") -> 40
                b.contains("mark") -> 41
                b.contains("luke") -> 42
                b.contains("john") && !b.contains("1") && !b.contains("2") && !b.contains("3") -> 43
                b.contains("acts") -> 44
                b.contains("rom") -> 45
                b.contains("1 cor") -> 46
                b.contains("2 cor") -> 47
                b.contains("gal") -> 48
                b.contains("eph") -> 49
                b.contains("phil") -> 50
                b.contains("col") -> 51
                b.contains("1 thess") -> 52
                b.contains("2 thess") -> 53
                b.contains("1 tim") -> 54
                b.contains("2 tim") -> 55
                b.contains("tit") -> 56
                b.contains("philem") -> 57
                b.contains("heb") -> 58
                b.contains("jam") -> 59
                b.contains("1 pet") -> 60
                b.contains("2 pet") -> 61
                b.contains("1 john") -> 62
                b.contains("2 john") -> 63
                b.contains("3 john") -> 64
                b.contains("jude") -> 65
                b.contains("rev") -> 66
                else -> 40
            }
        }

        fun isOldTestament(book: String): Boolean = getBookNumber(book) <= 39

        fun getBaselineSeedVerses(): List<BibleVerseEntity> {
            return listOf(
                // Youth Alive / Gideonites Core Theme: Exploits & Strength
                BibleVerseEntity(
                    book = "Daniel",
                    bookNumber = 27,
                    testament = "OLD",
                    chapter = 11,
                    verse = 32,
                    verseText = "And such as do wickedly against the covenant shall he corrupt by flatteries: but the people that do know their God shall be strong, and do exploits.",
                    reference = "Daniel 11:32",
                    translation = "KJV",
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Philippians",
                    bookNumber = 50,
                    testament = "NEW",
                    chapter = 4,
                    verse = 13,
                    verseText = "I can do all things through Christ which strengtheneth me.",
                    reference = "Philippians 4:13",
                    translation = "KJV",
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Isaiah",
                    bookNumber = 23,
                    testament = "OLD",
                    chapter = 40,
                    verse = 31,
                    verseText = "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.",
                    reference = "Isaiah 40:31",
                    translation = "KJV",
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Joshua",
                    bookNumber = 6,
                    testament = "OLD",
                    chapter = 1,
                    verse = 9,
                    verseText = "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee whithersoever thou goest.",
                    reference = "Joshua 1:9",
                    translation = "KJV",
                    theme = "Courage & Boldness"
                ),
                BibleVerseEntity(
                    book = "Romans",
                    bookNumber = 45,
                    testament = "NEW",
                    chapter = 8,
                    verse = 31,
                    verseText = "What shall we then say to these things? If God be for us, who can be against us?",
                    reference = "Romans 8:31",
                    translation = "KJV",
                    theme = "Victory & Conquerors"
                ),
                BibleVerseEntity(
                    book = "Romans",
                    bookNumber = 45,
                    testament = "NEW",
                    chapter = 8,
                    verse = 37,
                    verseText = "Nay, in all these things we are more than conquerors through him that loved us.",
                    reference = "Romans 8:37",
                    translation = "KJV",
                    theme = "Victory & Conquerors"
                ),
                BibleVerseEntity(
                    book = "1 Timothy",
                    bookNumber = 54,
                    testament = "NEW",
                    chapter = 4,
                    verse = 12,
                    verseText = "Let no man despise thy youth; but be thou an example of the believers, in word, in conversation, in charity, in spirit, in faith, in purity.",
                    reference = "1 Timothy 4:12",
                    translation = "KJV",
                    theme = "Youth & Purpose"
                ),
                BibleVerseEntity(
                    book = "Jeremiah",
                    bookNumber = 24,
                    testament = "OLD",
                    chapter = 29,
                    verse = 11,
                    verseText = "For I know the thoughts that I think toward you, saith the LORD, thoughts of peace, and not of evil, to give you an expected end.",
                    reference = "Jeremiah 29:11",
                    translation = "KJV",
                    theme = "Youth & Purpose"
                ),
                BibleVerseEntity(
                    book = "Jeremiah",
                    bookNumber = 24,
                    testament = "OLD",
                    chapter = 1,
                    verse = 7,
                    verseText = "But the LORD said unto me, Say not, I am a child: for thou shalt go to all that I shall send thee, and whatsoever I command thee thou shalt speak.",
                    reference = "Jeremiah 1:7",
                    translation = "KJV",
                    theme = "Youth & Purpose"
                ),
                BibleVerseEntity(
                    book = "Proverbs",
                    bookNumber = 20,
                    testament = "OLD",
                    chapter = 3,
                    verse = 5,
                    verseText = "Trust in the LORD with all thine heart; and lean not unto thine own understanding.",
                    reference = "Proverbs 3:5",
                    translation = "KJV",
                    theme = "Wisdom & Guidance"
                ),
                BibleVerseEntity(
                    book = "Proverbs",
                    bookNumber = 20,
                    testament = "OLD",
                    chapter = 3,
                    verse = 6,
                    verseText = "In all thy ways acknowledge him, and he shall direct thy paths.",
                    reference = "Proverbs 3:6",
                    translation = "KJV",
                    theme = "Wisdom & Guidance"
                ),
                BibleVerseEntity(
                    book = "2 Timothy",
                    bookNumber = 55,
                    testament = "NEW",
                    chapter = 1,
                    verse = 7,
                    verseText = "For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.",
                    reference = "2 Timothy 1:7",
                    translation = "KJV",
                    theme = "Courage & Boldness"
                ),
                BibleVerseEntity(
                    book = "Ephesians",
                    bookNumber = 49,
                    testament = "NEW",
                    chapter = 6,
                    verse = 10,
                    verseText = "Finally, my brethren, be strong in the Lord, and in the power of his might.",
                    reference = "Ephesians 6:10",
                    translation = "KJV",
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Ephesians",
                    bookNumber = 49,
                    testament = "NEW",
                    chapter = 6,
                    verse = 11,
                    verseText = "Put on the whole armour of God, that ye may be able to stand against the wiles of the devil.",
                    reference = "Ephesians 6:11",
                    translation = "KJV",
                    theme = "Victory & Conquerors"
                ),
                BibleVerseEntity(
                    book = "Psalms",
                    bookNumber = 19,
                    testament = "OLD",
                    chapter = 23,
                    verse = 1,
                    verseText = "The LORD is my shepherd; I shall not want.",
                    reference = "Psalm 23:1",
                    translation = "KJV",
                    theme = "Faith & Trust"
                ),
                BibleVerseEntity(
                    book = "Psalms",
                    bookNumber = 19,
                    testament = "OLD",
                    chapter = 119,
                    verse = 105,
                    verseText = "Thy word is a lamp unto my feet, and a light unto my path.",
                    reference = "Psalm 119:105",
                    translation = "KJV",
                    theme = "Wisdom & Guidance"
                ),
                BibleVerseEntity(
                    book = "Psalms",
                    bookNumber = 19,
                    testament = "OLD",
                    chapter = 27,
                    verse = 1,
                    verseText = "The LORD is my light and my salvation; whom shall I fear? the LORD is the strength of my life; of whom shall I be afraid?",
                    reference = "Psalm 27:1",
                    translation = "KJV",
                    theme = "Courage & Boldness"
                ),
                BibleVerseEntity(
                    book = "Hebrews",
                    bookNumber = 58,
                    testament = "NEW",
                    chapter = 11,
                    verse = 1,
                    verseText = "Now faith is the substance of things hoped for, the evidence of things not seen.",
                    reference = "Hebrews 11:1",
                    translation = "KJV",
                    theme = "Faith & Trust"
                ),
                BibleVerseEntity(
                    book = "Hebrews",
                    bookNumber = 58,
                    testament = "NEW",
                    chapter = 11,
                    verse = 6,
                    verseText = "But without faith it is impossible to please him: for he that cometh to God must believe that he is, and that he is a rewarder of them that diligently seek him.",
                    reference = "Hebrews 11:6",
                    translation = "KJV",
                    theme = "Faith & Trust"
                ),
                BibleVerseEntity(
                    book = "John",
                    bookNumber = 43,
                    testament = "NEW",
                    chapter = 3,
                    verse = 16,
                    verseText = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
                    reference = "John 3:16",
                    translation = "KJV",
                    theme = "Salvation & Grace"
                ),
                BibleVerseEntity(
                    book = "Romans",
                    bookNumber = 45,
                    testament = "NEW",
                    chapter = 12,
                    verse = 2,
                    verseText = "And be not conformed to this world: but be ye transformed by the renewing of your mind, that ye may prove what is that good, and acceptable, and perfect, will of God.",
                    reference = "Romans 12:2",
                    translation = "KJV",
                    theme = "Youth & Purpose"
                ),
                BibleVerseEntity(
                    book = "Matthew",
                    bookNumber = 40,
                    testament = "NEW",
                    chapter = 28,
                    verse = 19,
                    verseText = "Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost:",
                    reference = "Matthew 28:19",
                    translation = "KJV",
                    theme = "Great Commission & Outreach"
                ),
                BibleVerseEntity(
                    book = "Matthew",
                    bookNumber = 40,
                    testament = "NEW",
                    chapter = 6,
                    verse = 33,
                    verseText = "But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you.",
                    reference = "Matthew 6:33",
                    translation = "KJV",
                    theme = "Kingdom Priority"
                ),
                BibleVerseEntity(
                    book = "Galatians",
                    bookNumber = 48,
                    testament = "NEW",
                    chapter = 2,
                    verse = 20,
                    verseText = "I am crucified with Christ: nevertheless I live; yet not I, but Christ liveth in me: and the life which I now live in the flesh I live by the faith of the Son of God, who loved me, and gave himself for me.",
                    reference = "Galatians 2:20",
                    translation = "KJV",
                    theme = "Faith & Trust"
                ),
                BibleVerseEntity(
                    book = "2 Corinthians",
                    bookNumber = 47,
                    testament = "NEW",
                    chapter = 5,
                    verse = 17,
                    verseText = "Therefore if any man be in Christ, he is a new creature: old things are passed away; behold, all things are become new.",
                    reference = "2 Corinthians 5:17",
                    translation = "KJV",
                    theme = "Salvation & Grace"
                ),
                BibleVerseEntity(
                    book = "Judges",
                    bookNumber = 7,
                    testament = "OLD",
                    chapter = 6,
                    verse = 12,
                    verseText = "And the angel of the LORD appeared unto him, and said unto him, The LORD is with thee, thou mighty man of valour.",
                    reference = "Judges 6:12",
                    translation = "KJV",
                    theme = "Gideonites Heritage"
                ),
                BibleVerseEntity(
                    book = "Judges",
                    bookNumber = 7,
                    testament = "OLD",
                    chapter = 7,
                    verse = 20,
                    verseText = "And the three companies blew the trumpets, and brake the pitchers, and held the lamps in their left hands, and the trumpets in their right hands to blow withal: and they cried, The sword of the LORD, and of Gideon.",
                    reference = "Judges 7:20",
                    translation = "KJV",
                    language = AppLanguage.ENGLISH,
                    theme = "Gideonites Heritage"
                ),
                // YORUBA (Bibeli Mimo)
                BibleVerseEntity(
                    book = "Danieli",
                    bookNumber = 27,
                    testament = "OLD",
                    chapter = 11,
                    verse = 32,
                    verseText = "Ṣùgbọ́n àwọn ènìyàn tí ó mọ Ọlọ́run wọn yóò lágbára, wọn yóò sì ṣe àwọn iṣẹ́ agbára.",
                    reference = "Danieli 11:32",
                    translation = "Bibeli Mimo (YO)",
                    language = AppLanguage.YORUBA,
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Filipi",
                    bookNumber = 50,
                    testament = "NEW",
                    chapter = 4,
                    verse = 13,
                    verseText = "Èmi le ṣe ohun gbogbo nínú Kristi ẹni tí ó ń fi agbára fún mi.",
                    reference = "Filipi 4:13",
                    translation = "Bibeli Mimo (YO)",
                    language = AppLanguage.YORUBA,
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Johanu",
                    bookNumber = 43,
                    testament = "NEW",
                    chapter = 3,
                    verse = 16,
                    verseText = "Nítorí Ọlọ́run fẹ́ aráyé tó bẹ́ẹ̀ gẹ́ẹ́, tí ó fi ọmọ rẹ̀ kan ṣoṣo fúnni, kí ẹnikẹ́ni tí ó bá gbà á gbọ́ má bàa ṣègbé, ṣùgbọ́n kí ó lè ní ìyè àìnípẹ̀kun.",
                    reference = "Johanu 3:16",
                    translation = "Bibeli Mimo (YO)",
                    language = AppLanguage.YORUBA,
                    theme = "Salvation & Grace"
                ),
                BibleVerseEntity(
                    book = "Onídàájọ́",
                    bookNumber = 7,
                    testament = "OLD",
                    chapter = 6,
                    verse = 12,
                    verseText = "Áńgẹ́lì OLÚWA sì farahàn án, ó sì wí fún un pé, OLÚWA wà pẹ̀lú rẹ, ìwọ akọni alágbára.",
                    reference = "Onídàájọ́ 6:12 (Judges 6:12)",
                    translation = "Bibeli Mimo (YO)",
                    language = AppLanguage.YORUBA,
                    theme = "Gideonites Heritage"
                ),
                BibleVerseEntity(
                    book = "Jóṣúà",
                    bookNumber = 6,
                    testament = "OLD",
                    chapter = 1,
                    verse = 9,
                    verseText = "Èmi kò ha ti pa á láṣẹ fún ọ? Jẹ́ alágbára kí o sì ní ìgboyà rere; má bẹ̀rù, bẹ́ẹ̀ ni kí o má ṣe fòyà: nítorí OLÚWA Ọlọ́run rẹ wà pẹ̀lú rẹ níbi gbogbo tí ìwọ bá ń lọ.",
                    reference = "Jóṣúà 1:9 (Joshua 1:9)",
                    translation = "Bibeli Mimo (YO)",
                    language = AppLanguage.YORUBA,
                    theme = "Courage & Boldness"
                ),
                BibleVerseEntity(
                    book = "1 Tímótì",
                    bookNumber = 54,
                    testament = "NEW",
                    chapter = 4,
                    verse = 12,
                    verseText = "Má ṣe jẹ́ kí ẹnikẹ́ni gan ìgbà èwe rẹ; ṣùgbọ́n jẹ́ àpẹẹrẹ fún àwọn onígbàgbọ́ nínú ọ̀rọ̀, nínú ìwà, nínú ìfẹ́, nínú ẹ̀mí, nínú ìgbàgbọ́, nínú ìwà mímọ́.",
                    reference = "1 Tímótì 4:12 (1 Timothy 4:12)",
                    translation = "Bibeli Mimo (YO)",
                    language = AppLanguage.YORUBA,
                    theme = "Youth & Purpose"
                ),
                // HAUSA (Littafi Mai Tsarki)
                BibleVerseEntity(
                    book = "Daniyel",
                    bookNumber = 27,
                    testament = "OLD",
                    chapter = 11,
                    verse = 32,
                    verseText = "Amma mutanen da suka san Allahnsu za su kasance da ƙarfi, su kuma yi manyan ayyuka.",
                    reference = "Daniyel 11:32",
                    translation = "Littafi Mai Tsarki (HA)",
                    language = AppLanguage.HAUSA,
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Filibiyawa",
                    bookNumber = 50,
                    testament = "NEW",
                    chapter = 4,
                    verse = 13,
                    verseText = "Zan iya yin komai ta wurin Almasihu wanda yake ƙarfafa ni.",
                    reference = "Filibiyawa 4:13",
                    translation = "Littafi Mai Tsarki (HA)",
                    language = AppLanguage.HAUSA,
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Yahaya",
                    bookNumber = 43,
                    testament = "NEW",
                    chapter = 3,
                    verse = 16,
                    verseText = "Domin Allah ya ƙaunaci duniya har ya ba da makaɗaicin Ɗansa, domin duk wanda ya gaskata da shi kada ya lalace, sai dai ya sami rai madawwami.",
                    reference = "Yahaya 3:16",
                    translation = "Littafi Mai Tsarki (HA)",
                    language = AppLanguage.HAUSA,
                    theme = "Salvation & Grace"
                ),
                BibleVerseEntity(
                    book = "Mahukunta",
                    bookNumber = 7,
                    testament = "OLD",
                    chapter = 6,
                    verse = 12,
                    verseText = "Mala'ikan Ubangiji kuwa ya bayyana gare shi, ya ce masa, Ubangiji yana tare da kai, ya kai jarumi mai ƙarfi.",
                    reference = "Mahukunta 6:12 (Judges 6:12)",
                    translation = "Littafi Mai Tsarki (HA)",
                    language = AppLanguage.HAUSA,
                    theme = "Gideonites Heritage"
                ),
                BibleVerseEntity(
                    book = "Yoshuwa",
                    bookNumber = 6,
                    testament = "OLD",
                    chapter = 1,
                    verse = 9,
                    verseText = "Ban umarce ka ba? Ka yi ƙarfi, ka yi ƙarfin hali; kada ka ji tsoro, kada ka razana: gama Ubangiji Allahnka yana tare da kai duk inda za ka.",
                    reference = "Yoshuwa 1:9 (Joshua 1:9)",
                    translation = "Littafi Mai Tsarki (HA)",
                    language = AppLanguage.HAUSA,
                    theme = "Courage & Boldness"
                ),
                BibleVerseEntity(
                    book = "1 Timoti",
                    bookNumber = 54,
                    testament = "NEW",
                    chapter = 4,
                    verse = 12,
                    verseText = "Kada kowa ya rena ƙuruciyarka, amma ka zama abin koyi ga masu ba da gaskiya a cikin magana, da hali, da ƙauna, da bangaskiya, da tsarki.",
                    reference = "1 Timoti 4:12 (1 Timothy 4:12)",
                    translation = "Littafi Mai Tsarki (HA)",
                    language = AppLanguage.HAUSA,
                    theme = "Youth & Purpose"
                ),
                // IGBO (Baịbụl Nsọ)
                BibleVerseEntity(
                    book = "Daniel",
                    bookNumber = 27,
                    testament = "OLD",
                    chapter = 11,
                    verse = 32,
                    verseText = "Ma ndị maara Chineke ha ga-esi ike, meekwa ihe dị ukwuu.",
                    reference = "Daniel 11:32",
                    translation = "Baịbụl Nsọ (IG)",
                    language = AppLanguage.IGBO,
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Ndị Filipaị",
                    bookNumber = 50,
                    testament = "NEW",
                    chapter = 4,
                    verse = 13,
                    verseText = "Enwere m ike ime ihe niile site n'aka Kraịst onye na-enye m ike.",
                    reference = "Ndị Filipaị 4:13",
                    translation = "Baịbụl Nsọ (IG)",
                    language = AppLanguage.IGBO,
                    theme = "Exploits & Strength"
                ),
                BibleVerseEntity(
                    book = "Jọn",
                    bookNumber = 43,
                    testament = "NEW",
                    chapter = 3,
                    verse = 16,
                    verseText = "N'ihi na Chineke hụrụ ụwa n'anya otu a, na ọ nyere Ọkpara ọ mụrụ naanị ya, ka onye ọ bụla nke kwere na ya ghara ịla n'iyi, kama ka o nwee ndụ ebighị ebi.",
                    reference = "Jọn 3:16",
                    translation = "Baịbụl Nsọ (IG)",
                    language = AppLanguage.IGBO,
                    theme = "Salvation & Grace"
                ),
                BibleVerseEntity(
                    book = "Ndị Ikpe",
                    bookNumber = 7,
                    testament = "OLD",
                    chapter = 6,
                    verse = 12,
                    verseText = "Mmụọ ozi Onyenwe anyị wee pụta ìhè n'ebe ọ nọ, sị ya, Onyenwe anyị nọnyeere gị, gị dike n'agha.",
                    reference = "Ndị Ikpe 6:12 (Judges 6:12)",
                    translation = "Baịbụl Nsọ (IG)",
                    language = AppLanguage.IGBO,
                    theme = "Gideonites Heritage"
                ),
                BibleVerseEntity(
                    book = "Jọshụa",
                    bookNumber = 6,
                    testament = "OLD",
                    chapter = 1,
                    verse = 9,
                    verseText = "Ọ bụghị mụ nyere gị iwu? Dị ike, nweekwa obi ike; atụla egwu, akpọkwala ụjọ: n'ihi na Onyenwe anyị Chineke gị nọnyeere gị n'ebe ọ bụla ị na-aga.",
                    reference = "Jọshụa 1:9 (Joshua 1:9)",
                    translation = "Baịbụl Nsọ (IG)",
                    language = AppLanguage.IGBO,
                    theme = "Courage & Boldness"
                ),
                BibleVerseEntity(
                    book = "1 Timoti",
                    bookNumber = 54,
                    testament = "NEW",
                    chapter = 4,
                    verse = 12,
                    verseText = "Ekwekwala ka onye ọ bụla leda okorobịa gị anya; kama bụrụ ihe nlereanya nye ndị kwere ekwe n'okwu, n'omume, n'ịhụnanya, n'okwukwe, na n'ịdị ọcha.",
                    reference = "1 Timoti 4:12 (1 Timothy 4:12)",
                    translation = "Baịbụl Nsọ (IG)",
                    language = AppLanguage.IGBO,
                    theme = "Youth & Purpose"
                )
            )
        }

        fun inferLanguageFromTranslation(translation: String): AppLanguage {
            val upper = translation.uppercase()
            return when {
                upper.contains("YORUBA") || upper.contains("BIBELI") || upper.contains("(YO)") || upper == "YO" -> AppLanguage.YORUBA
                upper.contains("HAUSA") || upper.contains("LITTAFI") || upper.contains("(HA)") || upper == "HA" -> AppLanguage.HAUSA
                upper.contains("IGBO") || upper.contains("BAỊBỤL") || upper.contains("BAIBUL") || upper.contains("(IG)") || upper == "IG" -> AppLanguage.IGBO
                else -> AppLanguage.ENGLISH
            }
        }
    }
}
