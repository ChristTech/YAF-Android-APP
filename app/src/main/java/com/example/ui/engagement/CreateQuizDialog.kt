package com.example.ui.engagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizCategory
import com.example.data.repository.BibleRepository
import com.example.ui.theme.FlameRedPrimary

data class MutableQuizQuestion(
    var question: String = "",
    var options: MutableList<String> = mutableListOf("", "", "", ""),
    var correctIndex: Int = 0,
    var scriptureRef: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateQuizDialog(
    onDismiss: () -> Unit,
    bibleRepository: BibleRepository? = null,
    onCreate: (
        title: String,
        description: String,
        category: QuizCategory,
        difficulty: String,
        timeLimitSeconds: Int,
        questions: List<QuizQuestion>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(QuizCategory.BIBLE_TRIVIA) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("Medium") }
    var difficultyExpanded by remember { mutableStateOf(false) }
    var timeLimit by remember { mutableStateOf("90") }
    var biblePickerTargetIndex by remember { mutableStateOf<Int?>(null) }
    var showAddQuestionFromBible by remember { mutableStateOf(false) }

    val questionList = remember {
        mutableStateListOf(
            MutableQuizQuestion(
                question = "What is the theme of Youth Alive Fellowship?",
                options = mutableListOf("Kingdom Giants", "Great Overcomers", "Faith Champions", "Praise Army"),
                correctIndex = 0,
                scriptureRef = "Daniel 11:32"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Scripture Trivia Quiz",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Admin • Trivia Publishing",
                    style = MaterialTheme.typography.labelSmall,
                    color = FlameRedPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quiz Title *") },
                    placeholder = { Text("e.g. Kingdom Giants Faith Quiz") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    placeholder = { Text("e.g. Test your mastery of God's Word") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quiz Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        QuizCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Difficulty and Time Limit
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = difficultyExpanded,
                        onExpandedChange = { difficultyExpanded = !difficultyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = difficulty,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Difficulty") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = difficultyExpanded,
                            onDismissRequest = { difficultyExpanded = false }
                        ) {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                DropdownMenuItem(
                                    text = { Text(diff) },
                                    onClick = {
                                        difficulty = diff
                                        difficultyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = timeLimit,
                        onValueChange = { timeLimit = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Time (sec)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Questions Section Header
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Questions (${questionList.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                questionList.add(
                                    MutableQuizQuestion(
                                        question = "",
                                        options = mutableListOf("", "", "", ""),
                                        correctIndex = 0,
                                        scriptureRef = ""
                                    )
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Question", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showAddQuestionFromBible = true }
                        ) {
                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("From Bible", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                questionList.forEachIndexed { qIndex, q ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Question #${qIndex + 1}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = FlameRedPrimary
                                )
                                if (questionList.size > 1) {
                                    IconButton(onClick = { questionList.removeAt(qIndex) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Question",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = q.question,
                                onValueChange = { updated ->
                                    questionList[qIndex] = q.copy(question = updated)
                                },
                                label = { Text("Question Prompt *") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = q.scriptureRef,
                                    onValueChange = { updated ->
                                        questionList[qIndex] = q.copy(scriptureRef = updated)
                                    },
                                    label = { Text("Scripture Reference") },
                                    placeholder = { Text("e.g. John 3:16") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { biblePickerTargetIndex = qIndex },
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bible DB", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Options (select the radio button for the correct answer):",
                                style = MaterialTheme.typography.labelSmall
                            )

                            q.options.forEachIndexed { optIndex, optText ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = q.correctIndex == optIndex,
                                        onClick = {
                                            questionList[qIndex] = q.copy(correctIndex = optIndex)
                                        }
                                    )
                                    OutlinedTextField(
                                        value = optText,
                                        onValueChange = { updated ->
                                            val newOpts = q.options.toMutableList()
                                            newOpts[optIndex] = updated
                                            questionList[qIndex] = q.copy(options = newOpts)
                                        },
                                        label = { Text("Option ${('A' + optIndex)}") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQuestions = questionList.filter { it.question.isNotBlank() }.mapIndexed { idx, q ->
                        QuizQuestion(
                            id = idx + 1,
                            question = q.question.trim(),
                            options = q.options.map { it.trim().ifEmpty { "Option" } },
                            correctAnswerIndex = q.correctIndex.coerceIn(0, 3),
                            scriptureRef = q.scriptureRef.trim()
                        )
                    }
                    val limit = timeLimit.toIntOrNull() ?: 90
                    onCreate(title, description, selectedCategory, difficulty, limit, finalQuestions)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Publish Quiz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (biblePickerTargetIndex != null) {
        val targetIdx = biblePickerTargetIndex!!
        BibleScripturePickerDialog(
            bibleRepository = bibleRepository,
            onDismiss = { biblePickerTargetIndex = null },
            onScriptureSelected = { selectedVerse ->
                if (targetIdx in questionList.indices) {
                    val currentQ = questionList[targetIdx]
                    val updatedPrompt = if (currentQ.question.isBlank()) {
                        "Which scripture proclaims: \"${selectedVerse.verseText.take(80)}...\"?"
                    } else {
                        currentQ.question
                    }
                    val updatedOpts = currentQ.options.toMutableList()
                    if (updatedOpts[0].isBlank()) {
                        updatedOpts[0] = selectedVerse.reference
                        if (updatedOpts[1].isBlank()) updatedOpts[1] = "Genesis 1:1"
                        if (updatedOpts[2].isBlank()) updatedOpts[2] = "Psalm 23:1"
                        if (updatedOpts[3].isBlank()) updatedOpts[3] = "Romans 8:28"
                    }
                    questionList[targetIdx] = currentQ.copy(
                        question = updatedPrompt,
                        scriptureRef = "${selectedVerse.reference} (${selectedVerse.translation})",
                        options = updatedOpts
                    )
                }
                biblePickerTargetIndex = null
            },
            initialTargetContext = "Solo Quiz Question"
        )
    }

    if (showAddQuestionFromBible) {
        BibleScripturePickerDialog(
            bibleRepository = bibleRepository,
            onDismiss = { showAddQuestionFromBible = false },
            onScriptureSelected = { selectedVerse ->
                questionList.add(
                    MutableQuizQuestion(
                        question = "Which scripture declares: \"${selectedVerse.verseText}\"?",
                        options = mutableListOf(
                            selectedVerse.reference,
                            "Proverbs 3:5",
                            "Isaiah 40:31",
                            "Joshua 1:8"
                        ),
                        correctIndex = 0,
                        scriptureRef = "${selectedVerse.reference} (${selectedVerse.translation})"
                    )
                )
                showAddQuestionFromBible = false
            },
            initialTargetContext = "Solo Scripture Quiz"
        )
    }
}
