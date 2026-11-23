package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AnnouncementPriority
import com.example.data.model.UserRole
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.KingdomEmerald
import com.example.ui.theme.ShieldSilver

@Composable
fun ChurchEmblemHeader(
    modifier: Modifier = Modifier,
    sizeDp: Int = 64,
    showSubtext: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_church_logo),
                contentDescription = "The Gideonites - The Youth Alive Fellowship Logo",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size((sizeDp - 8).dp)
            )
        }
        if (showSubtext) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "THE GIDEONITES - THE YOUTH ALIVE FELLOWSHIP",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.0.sp,
                    color = FlameRedPrimary
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kingdom Giants • Youth Platform",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (baseColor, label) = when (role) {
        UserRole.ADMINISTRATOR -> FlameRedPrimary to role.displayName
        UserRole.YOUTH_EXECUTIVE -> FlameGoldAccent to role.displayName
        UserRole.YOUTH_MEMBER -> KingdomEmerald to role.displayName
        UserRole.GUEST -> ShieldSilver to role.displayName
    }

    Surface(
        color = baseColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, baseColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(baseColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = baseColor
                )
            )
        }
    }
}

@Composable
fun PriorityBadge(
    priority: AnnouncementPriority,
    modifier: Modifier = Modifier
) {
    val (baseColor, label) = when (priority) {
        AnnouncementPriority.URGENT -> FlameRedPrimary to "URGENT"
        AnnouncementPriority.IMPORTANT -> FlameGoldAccent to "IMPORTANT"
        AnnouncementPriority.NORMAL -> ShieldSilver to "INFO"
    }

    Surface(
        color = baseColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, baseColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            if (priority == AnnouncementPriority.URGENT) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = baseColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = baseColor
                )
            )
        }
    }
}

/**
 * Modern, clean, and inspiring empty state component for a fresh application.
 * Designed with a soft halo, crisp iconography, inviting copy, and optional primary call to action.
 */
@Composable
fun ModernEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    accentColor: Color = FlameRedPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        // Soft Halo Icon Container
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.10f))
                .border(1.5.dp, accentColor.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Reads, downscales, and JPEG-compresses an image from a Uri into a compact `data:image/jpeg;base64,...`
 * string so that profile photos and forum post attachments synchronize seamlessly across all devices via Cloud Firestore,
 * while also saving a local file fallback.
 */
private fun encodeImageUriToSyncableDataUri(
    context: android.content.Context,
    sourceUri: android.net.Uri,
    maxDimensionPx: Int,
    initialQuality: Int,
    maxBase64Length: Int,
    subDir: String,
    filePrefix: String
): String {
    return try {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: return sourceUri.toString()
        if (bytes.isEmpty()) return sourceUri.toString()

        // Also save local file copy
        val dir = java.io.File(context.filesDir, subDir).apply { mkdirs() }
        val destFile = java.io.File(dir, "${filePrefix}_${System.currentTimeMillis()}.jpg")

        val boundsOpts = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDimensionPx * 2 ||
            boundsOpts.outHeight / sampleSize > maxDimensionPx * 2
        ) {
            sampleSize *= 2
        }

        val decodeOpts = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val rawBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: return sourceUri.toString()

        val width = rawBitmap.width.coerceAtLeast(1)
        val height = rawBitmap.height.coerceAtLeast(1)
        val scale = minOf(
            maxDimensionPx.toFloat() / width.toFloat(),
            maxDimensionPx.toFloat() / height.toFloat(),
            1f
        )
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        val scaledBitmap = if (targetW != width || targetH != height) {
            android.graphics.Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
        } else {
            rawBitmap
        }

        var quality = initialQuality
        var compressedBytes: ByteArray
        do {
            val baos = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
            compressedBytes = baos.toByteArray()
            quality -= 12
        } while (compressedBytes.size * 4 / 3 > maxBase64Length && quality >= 28)

        try {
            java.io.FileOutputStream(destFile).use { it.write(compressedBytes) }
        } catch (_: Exception) {
        }

        val base64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    } catch (e: Exception) {
        sourceUri.toString()
    }
}

/**
 * Converts a local `file://` or `content://` URI string into a syncable `data:image/jpeg;base64,...` URI
 * if the file exists on this device so existing local photos also sync to other devices.
 */
fun ensureSyncableImageUri(
    uriStr: String?,
    maxDimensionPx: Int = 640,
    maxBase64Length: Int = 220_000
): String? {
    if (uriStr.isNullOrBlank()) return null
    val trimmed = uriStr.trim()
    if (trimmed.startsWith("data:image/") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    if (trimmed.startsWith("file://")) {
        return try {
            val file = java.io.File(trimmed.removePrefix("file://"))
            if (!file.exists() || file.length() <= 0L) return trimmed
            val bytes = file.readBytes()
            val rawBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return trimmed
            val width = rawBitmap.width.coerceAtLeast(1)
            val height = rawBitmap.height.coerceAtLeast(1)
            val scale = minOf(
                maxDimensionPx.toFloat() / width.toFloat(),
                maxDimensionPx.toFloat() / height.toFloat(),
                1f
            )
            val scaled = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    rawBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }
            var quality = 72
            var compressed: ByteArray
            do {
                val baos = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
                compressed = baos.toByteArray()
                quality -= 12
            } while (compressed.size * 4 / 3 > maxBase64Length && quality >= 28)
            val base64 = android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (_: Exception) {
            trimmed
        }
    }
    return trimmed
}

/**
 * Resolves a stored URI string (`data:image/...;base64,...`, `file://...`, or `content://...` / `https://...`)
 * into a model object (`ByteArray`, `File`, or `Uri`) that Coil's `AsyncImage` can render on any device.
 */
fun resolveImageModelData(uriStr: String?): Any? {
    if (uriStr.isNullOrBlank()) return null
    val trimmed = uriStr.trim()
    return try {
        when {
            trimmed.startsWith("data:image/") && trimmed.contains("base64,") -> {
                val base64Part = trimmed.substringAfter("base64,")
                android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
            }
            trimmed.startsWith("file://") -> {
                java.io.File(trimmed.removePrefix("file://"))
            }
            else -> {
                android.net.Uri.parse(trimmed)
            }
        }
    } catch (_: Exception) {
        trimmed
    }
}

/**
 * Copies & compresses a selected photo URI from the Android Photo Picker into a syncable data URI
 * so the profile image persists across app restarts and syncs across all user devices via Firestore.
 */
fun persistProfilePhotoUri(context: android.content.Context, sourceUri: android.net.Uri): String {
    return encodeImageUriToSyncableDataUri(
        context = context,
        sourceUri = sourceUri,
        maxDimensionPx = 280,
        initialQuality = 76,
        maxBase64Length = 90_000,
        subDir = "profile_photos",
        filePrefix = "avatar"
    )
}

/**
 * Copies & compresses a selected forum post attachment photo from the Android Photo Picker into a
 * syncable data URI so attached images appear on all other devices via Firestore.
 */
fun persistPostAttachmentPhotoUri(context: android.content.Context, sourceUri: android.net.Uri): String {
    return encodeImageUriToSyncableDataUri(
        context = context,
        sourceUri = sourceUri,
        maxDimensionPx = 760,
        initialQuality = 74,
        maxBase64Length = 260_000,
        subDir = "post_attachments",
        filePrefix = "post_img"
    )
}

/**
 * Renders a circular user avatar using the attached profile photo URI (including cross-device base64 data URIs)
 * when available, or falls back to the user's initials / shield icon.
 */
@Composable
fun UserAvatar(
    photoUri: String?,
    fullName: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 72,
    backgroundColor: Color = FlameRedPrimary
) {
    val modelData = androidx.compose.runtime.remember(photoUri) {
        resolveImageModelData(photoUri)
    }

    val isSystemAdmin = fullName.trim().equals("Admin", ignoreCase = true) ||
        fullName.trim().equals("System Administrator", ignoreCase = true) ||
        fullName.trim().startsWith("System Admin", ignoreCase = true)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (modelData == null && isSystemAdmin) Color.White else backgroundColor)
            .border(2.dp, FlameGoldAccent.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (modelData != null) {
            coil.compose.AsyncImage(
                model = modelData,
                contentDescription = "$fullName profile picture",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
            )
        } else if (isSystemAdmin) {
            Image(
                painter = painterResource(id = R.drawable.ic_church_logo),
                contentDescription = "Official Church Logo",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size((sizeDp * 0.78f).dp)
            )
        } else {
            val initials = fullName
                .trim()
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }

            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = (sizeDp * 0.34f).sp
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((sizeDp * 0.5f).dp)
                )
            }
        }
    }
}

