package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.update.AppUpdateChecker
import com.example.core.update.GitHubReleaseUpdate
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack

@Composable
fun AppUpdateDialog(
    release: GitHubReleaseUpdate,
    onDownloadClick: () -> Unit,
    onViewReleasePageClick: () -> Unit,
    onLaterClick: () -> Unit,
    onSkipVersionClick: () -> Unit
) {
    val sizeText = AppUpdateChecker.formatApkSize(release.apkSizeBytes)

    AlertDialog(
        onDismissRequest = onLaterClick,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = "New Update Available",
                    tint = FlameRedPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "App Update Available",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} → v${release.latestVersion}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = FlameRedPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FlameGoldAccent.copy(alpha = 0.22f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = release.releaseTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlobeBlack
                            )
                        )
                        val metaLine = buildString {
                            append("GitHub Release • ${release.tagName}")
                            if (sizeText.isNotBlank()) {
                                append(" • APK ($sizeText)")
                            }
                        }
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = GlobeBlack.copy(alpha = 0.78f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "What's New:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = release.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onViewReleasePageClick) {
                        Text("View on GitHub", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onSkipVersionClick) {
                        Text(
                            text = "Skip ${release.tagName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                modifier = Modifier.testTag("btn_download_github_update")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (release.apkDownloadUrl != null) "Download APK" else "Get Update",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLaterClick,
                modifier = Modifier.testTag("btn_later_github_update")
            ) {
                Text("Later")
            }
        }
    )
}
