package com.example.cursorpad.ui.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.cursorpad.ui.theme.CursorPadTheme

data class FAQItem(
    val question: String,
    val answer: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    val faqs = listOf(
        FAQItem(
            question = "Why does CursorPad sometimes stop working in the background?",
            answer = {
                Text(
                    text = "Some Android manufacturers (Xiaomi/MIUI, in particular) aggressively kill background services to save battery. If CursorPad stops working in the background, try these steps:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                NumberedStep(
                    "1",
                    "Disable battery restrictions. Settings > Apps > CursorPad > Battery saver > \" No Restrictions \"."
                )

                Spacer(modifier = Modifier.height(12.dp))

                NumberedStep(
                    "2",
                    "Lock the app in recents. Settings > Security > Boost Speed > Lock apps > Enable CursorPad."
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Learn more on dontkillmyapp.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://dontkillmyapp.com/xiaomi".toUri()
                            )
                            context.startActivity(intent)
                        })
                )

            }
        ),
        FAQItem(
            question = "Why can't I enable Accessibility?"
        ) {
            Text(
                text = "Since CursorPad is a sideloaded app, Android restricts access to the Accessibility setting by default. You must first allow restricted settings from the App Info page. Press the Accessibility Permission on the Home screen for a step-by-step walkthrough.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FAQ") },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            faqs.forEachIndexed { index, item ->
                FAQCard(
                    faq = item,
                    isExpanded = expandedIndex == index,
                    onToggle = {
                        expandedIndex = if (expandedIndex == index) null else index
                    }
                )

                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun FAQCard(
    faq: FAQItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth(
            )
            .animateContentSize()
    ) {
        Column(
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    faq.answer()
                }
            }
        }
    }
}

@Composable
fun NumberedStep(
    number: String,
    text: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun Preview() {
    CursorPadTheme() {
        FAQScreen(onBackPressed = {})
    }
}