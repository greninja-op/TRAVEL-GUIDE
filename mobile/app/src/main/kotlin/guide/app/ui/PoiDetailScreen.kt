package guide.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import guide.app.R
import guide.app.data.PackLoader
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.Pressable
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Lines

/**
 * Luxury Place Story Screen — inspired by Alpine Lakeview Zurich & Conde Nast Traveler.
 *
 * Full-bleed hero photography with a transparent status bar, floating circular
 * navigation pills, overlapping rounded card sheet with rating, audio story status,
 * amenity tags, serif place prose, and pinned bottom action dock.
 */
@Composable
fun PoiDetailScreen(
    card: PackLoader.PoiCard,
    hoursText: String?,
    openNow: Boolean,
    language: guide.app.data.AppLanguage = guide.app.data.AppLanguage.ENGLISH,
    event: guide.app.extras.LocalEvent? = null,
    initialNote: String? = null,
    initialPhotoUri: String? = null,
    onSaveNote: (text: String, photoUri: String?) -> Unit = { _, _ -> },
    onDeleteNote: () -> Unit = {},
    onAddNote: () -> Unit = {},
    onBack: () -> Unit = {},
    onStartAudio: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var saved by remember { mutableStateOf(false) }
    var noteText by remember(initialNote) { mutableStateOf(initialNote.orEmpty()) }
    var attachedPhotoUri by remember(initialPhotoUri) { mutableStateOf(initialPhotoUri) }
    var isSavedFeedback by remember { mutableStateOf(false) }
    var qaQuestion by remember { mutableStateOf("") }
    var isAskingQa by remember { mutableStateOf(false) }
    var qaAnswer by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            attachedPhotoUri = uri.toString()
            onSaveNote(noteText, uri.toString())
            isSavedFeedback = true
        }
    }

    val photoBitmap: Bitmap? = remember(attachedPhotoUri) {
        attachedPhotoUri?.let { uriStr ->
            runCatching {
                val uri = Uri.parse(uriStr)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            }.getOrNull()
        }
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val localized = remember(card.id, language) {
        guide.app.data.AppStrings.getLocalizedPoi(card.id, language)
    }
    val displayName = localized?.name ?: card.name
    val displaySummary = localized?.summary ?: card.summary
    val displaySecret = localized?.secret ?: card.funFacts.firstOrNull()
    val displayHistory = localized?.history ?: card.history

    Box(modifier = Modifier.fillMaxSize().background(GuideTokens.Surface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp + navInset),
        ) {
            // ---- 1. Full-Bleed Hero Image Banner ----------------------------
            item(key = "hero-image") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp),
                ) {
                    Image(
                        painter = painterResource(id = PoiImageResolver.getDrawableForPoi(card.id)),
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Top gradient for status bar and button contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x77000000),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    // Bottom gradient for smooth transition
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x33000000),
                                    ),
                                ),
                            ),
                    )
                }
            }

            // ---- 2. Luxury Overlapping Card Body ----------------------------
            item(key = "content-card") {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = GuideTokens.Surface,
                    modifier = Modifier
                        .offset(y = (-24).dp)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = GuideTokens.Space.screenPad, vertical = GuideTokens.Space.base),
                    ) {
                        // Place Title
                        Text(
                            text = displayName,
                            style = GuideTokens.Heading.copy(fontWeight = FontWeight.Bold),
                            color = GuideTokens.Text,
                            maxLines = Lines.Title,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(GuideTokens.Space.xs))

                        // Category & Status Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm),
                        ) {
                            DetailPill(
                                label = layerWord(card.layer, language),
                                icon = layerIcon(card.layer),
                            )
                            HoursTag(hoursText = hoursText, openNow = openNow, language = language)
                        }

                        Spacer(Modifier.height(GuideTokens.Space.base))

                        // Essence Summary
                        Text(
                            text = displaySummary,
                            style = GuideTokens.Body.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = GuideTokens.Text,
                        )

                        // Live Happening Today Event Banner
                        if (event != null) {
                            Spacer(Modifier.height(GuideTokens.Space.base))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2E2005) else Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(GuideTokens.Space.base),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = GuideIcons.Calendar,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(GuideTokens.Space.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = guide.app.data.AppStrings.happeningToday(language).uppercase(),
                                            style = GuideTokens.Label.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                            ),
                                            color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFB45309),
                                        )
                                        Spacer(Modifier.height(GuideTokens.Space.xs))
                                        Text(
                                            text = event.title,
                                            style = GuideTokens.Title.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                            color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFEF3C7) else Color(0xFF78350F),
                                        )
                                        if (!event.note.isNullOrBlank()) {
                                            Spacer(Modifier.height(GuideTokens.Space.xs))
                                            Text(
                                                text = event.note,
                                                style = GuideTokens.Body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                                                color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFDE68A) else Color(0xFF92400E),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Highlighted Secret / Surprise Callout Card
                        if (displaySecret != null) {
                            Spacer(Modifier.height(GuideTokens.Space.base))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = GuideTokens.PrimaryWash,
                                border = BorderStroke(1.dp, GuideTokens.Border),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(GuideTokens.Space.base),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = GuideTokens.Primary,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = GuideIcons.Sparkle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(GuideTokens.Space.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = guide.app.data.AppStrings.hiddenSecret(language),
                                            style = GuideTokens.Caption.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                            ),
                                            color = GuideTokens.Primary,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = displaySecret,
                                            style = GuideTokens.Body.copy(
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 22.sp,
                                            ),
                                            color = GuideTokens.Text,
                                        )
                                    }
                                }
                            }
                        }

                        // The Living Story Section
                        Spacer(Modifier.height(GuideTokens.Space.lg))
                        SectionHeader(guide.app.data.AppStrings.livingStory(language))
                        Spacer(Modifier.height(GuideTokens.Space.xs))
                        Text(
                            text = displayHistory,
                            style = GuideTokens.Story,
                            color = GuideTokens.Text,
                            lineHeight = 26.sp,
                        )

                        // What to Spot in Person (The Field Checklist)
                        if (card.seeList.isNotEmpty()) {
                            Spacer(Modifier.height(GuideTokens.Space.lg))
                            SectionHeader(guide.app.data.AppStrings.whatToSpot(language))
                            Spacer(Modifier.height(GuideTokens.Space.xs))
                            GuideCard {
                                card.seeList.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        NumberBadge(number = index + 1)
                                        Spacer(Modifier.width(GuideTokens.Space.md))
                                        Text(
                                            text = item,
                                            style = GuideTokens.Body,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (index != card.seeList.lastIndex) {
                                        GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.sm))
                                    }
                                }
                            }
                        }

                        // Additional Lore & Curiosities
                        if (card.funFacts.size > 1) {
                            Spacer(Modifier.height(GuideTokens.Space.lg))
                            SectionHeader(guide.app.data.AppStrings.heritageLore(language))
                            Spacer(Modifier.height(GuideTokens.Space.xs))
                            GuideCard {
                                card.funFacts.drop(1).forEachIndexed { index, fact ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Icon(
                                            imageVector = GuideIcons.Compass,
                                            contentDescription = null,
                                            tint = GuideTokens.Highlight,
                                            modifier = Modifier.padding(top = 2.dp).size(16.dp),
                                        )
                                        Spacer(Modifier.width(GuideTokens.Space.md))
                                        Text(
                                            text = fact,
                                            style = GuideTokens.Body,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (index != card.funFacts.size - 2) {
                                        GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.sm))
                                    }
                                }
                            }
                        }

                        // Hands-free Earbud Cue Banner
                        Spacer(Modifier.height(GuideTokens.Space.lg))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = GuideTokens.Surface2,
                            border = BorderStroke(1.dp, GuideTokens.Border),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(GuideTokens.Space.base),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = GuideIcons.Headphones,
                                    contentDescription = null,
                                    tint = GuideTokens.Primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(GuideTokens.Space.md))
                                Column {
                                    Text(
                                        text = guide.app.data.AppStrings.spontaneousGuideTitle(language),
                                        style = GuideTokens.Label,
                                        color = GuideTokens.Text,
                                    )
                                    Text(
                                        text = guide.app.data.AppStrings.spontaneousGuideSubtitle(language),
                                        style = GuideTokens.Caption,
                                        color = GuideTokens.Text2,
                                    )
                                }
                            }
                        }

                        // ---- Interactive AI Tour Guide Q&A ------------------
                        Spacer(Modifier.height(GuideTokens.Space.lg))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GuideTokens.Surface,
                            border = BorderStroke(1.dp, GuideTokens.Primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(GuideTokens.Space.base)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = GuideTokens.PrimaryWash,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = GuideIcons.Sparkles,
                                                contentDescription = null,
                                                tint = GuideTokens.Primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(GuideTokens.Space.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = guide.app.data.AppStrings.askGuideHeader(language),
                                            style = GuideTokens.Label,
                                            color = GuideTokens.Text,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = guide.app.data.AppStrings.askGuideSubtitle(language),
                                            style = GuideTokens.Caption,
                                            color = GuideTokens.Text2,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(GuideTokens.Space.md))

                                // Quick question chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    val quickQuestions = listOf(
                                        guide.app.data.AppStrings.askGuideQuickHistory(language),
                                        guide.app.data.AppStrings.askGuideQuickSecrets(language),
                                        guide.app.data.AppStrings.askGuideQuickArchitecture(language),
                                    )
                                    quickQuestions.forEach { prompt ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = GuideTokens.Surface2,
                                            border = BorderStroke(1.dp, GuideTokens.Border),
                                            onClick = {
                                                qaQuestion = prompt
                                                isAskingQa = true
                                                coroutineScope.launch {
                                                    val answer = guide.app.voice.SpontaneousGuideAiEngine.askQuestion(
                                                        context = context,
                                                        card = card,
                                                        question = prompt,
                                                        language = language,
                                                        activeEvent = event?.title,
                                                    )
                                                    qaAnswer = answer
                                                    isAskingQa = false
                                                    guide.app.location.GuideService.speak(context, answer)
                                                }
                                            },
                                        ) {
                                            Text(
                                                text = prompt,
                                                style = GuideTokens.Caption,
                                                color = GuideTokens.Text,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(GuideTokens.Space.md))

                                // Question text input and send button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = qaQuestion,
                                        onValueChange = { qaQuestion = it },
                                        placeholder = {
                                            Text(
                                                text = guide.app.data.AppStrings.askGuidePlaceholder(language),
                                                style = GuideTokens.Caption,
                                                color = GuideTokens.Text2,
                                            )
                                        },
                                        textStyle = GuideTokens.Body.copy(color = GuideTokens.Text),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GuideTokens.Primary,
                                            unfocusedBorderColor = GuideTokens.Border,
                                            focusedContainerColor = GuideTokens.Surface2,
                                            unfocusedContainerColor = GuideTokens.Surface2,
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (qaQuestion.isNotBlank() && !isAskingQa) GuideTokens.Primary else GuideTokens.Border,
                                        modifier = Modifier.size(48.dp),
                                        onClick = {
                                            if (qaQuestion.isNotBlank() && !isAskingQa) {
                                                val q = qaQuestion.trim()
                                                isAskingQa = true
                                                coroutineScope.launch {
                                                    val answer = guide.app.voice.SpontaneousGuideAiEngine.askQuestion(
                                                        context = context,
                                                        card = card,
                                                        question = q,
                                                        language = language,
                                                        activeEvent = event?.title,
                                                    )
                                                    qaAnswer = answer
                                                    isAskingQa = false
                                                    guide.app.location.GuideService.speak(context, answer)
                                                }
                                            }
                                        },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = GuideIcons.Send,
                                                contentDescription = "Send",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }

                                if (isAskingQa) {
                                    Spacer(Modifier.height(GuideTokens.Space.sm))
                                    Text(
                                        text = guide.app.data.AppStrings.askGuideAnswering(language),
                                        style = GuideTokens.Caption,
                                        color = GuideTokens.Primary,
                                    )
                                }

                                // Spoken AI response bubble
                                qaAnswer?.let { answer ->
                                    Spacer(Modifier.height(GuideTokens.Space.md))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = GuideTokens.Surface2,
                                        border = BorderStroke(1.dp, GuideTokens.Border),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                StatusTag(
                                                    text = guide.app.data.AppStrings.guideAnswerSpoken(language),
                                                    color = GuideTokens.Primary,
                                                )
                                                Surface(
                                                    shape = CircleShape,
                                                    color = GuideTokens.PrimaryWash,
                                                    modifier = Modifier.size(28.dp),
                                                    onClick = {
                                                        guide.app.location.GuideService.speak(context, answer)
                                                    },
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = GuideIcons.Speak,
                                                            contentDescription = "Replay Voice",
                                                            tint = GuideTokens.Primary,
                                                            modifier = Modifier.size(14.dp),
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "\"$answer\"",
                                                style = GuideTokens.Body,
                                                color = GuideTokens.Text,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ---- Personal Notes & Photos Section -----------------
                        Spacer(Modifier.height(GuideTokens.Space.lg))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SectionHeader(guide.app.data.AppStrings.myNotesHeader(language))
                            if (isSavedFeedback) {
                                StatusTag(
                                    text = guide.app.data.AppStrings.noteSavedBadge(language),
                                    color = GuideTokens.Success,
                                    icon = GuideIcons.Check,
                                )
                            }
                        }
                        Spacer(Modifier.height(GuideTokens.Space.xs))
                        GuideCard {
                            Column {
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = {
                                        noteText = it
                                        isSavedFeedback = false
                                    },
                                    placeholder = {
                                        Text(
                                            text = guide.app.data.AppStrings.notesPlaceholder(language),
                                            style = GuideTokens.Chrome,
                                            color = GuideTokens.Text2,
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = GuideTokens.Surface2,
                                        unfocusedContainerColor = GuideTokens.Surface2,
                                        focusedBorderColor = GuideTokens.Primary,
                                        unfocusedBorderColor = GuideTokens.Border,
                                        focusedTextColor = GuideTokens.Text,
                                        unfocusedTextColor = GuideTokens.Text,
                                    ),
                                    textStyle = GuideTokens.Body,
                                )

                                if (photoBitmap != null) {
                                    Spacer(Modifier.height(GuideTokens.Space.sm))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                    ) {
                                        Image(
                                            bitmap = photoBitmap.asImageBitmap(),
                                            contentDescription = "Personal note photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xCC000000),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(32.dp),
                                            onClick = {
                                                attachedPhotoUri = null
                                                onSaveNote(noteText, null)
                                            },
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = GuideIcons.X,
                                                    contentDescription = guide.app.data.AppStrings.removePhotoBtn(language),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(GuideTokens.Space.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    GuideButton(
                                        text = guide.app.data.AppStrings.attachPhotoBtn(language),
                                        icon = GuideIcons.Camera,
                                        variant = GuideButtonVariant.Tonal,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                            )
                                        },
                                    )
                                    GuideButton(
                                        text = guide.app.data.AppStrings.saveNoteBtn(language),
                                        icon = GuideIcons.Check,
                                        variant = GuideButtonVariant.Primary,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onSaveNote(noteText, attachedPhotoUri)
                                            isSavedFeedback = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- 3. Floating Top Navigation Bar (Over the Hero Image) ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = statusInset + GuideTokens.Space.sm,
                    start = GuideTokens.Space.screenPad,
                    end = GuideTokens.Space.screenPad,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circular Back Button
            Surface(
                shape = CircleShape,
                color = GuideTokens.Surface.copy(alpha = 0.90f),
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp),
                onClick = onBack,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GuideIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = GuideTokens.Text,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Circular Bookmark Button
            Surface(
                shape = CircleShape,
                color = GuideTokens.Surface.copy(alpha = 0.90f),
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp),
                onClick = {
                    saved = !saved
                    onAddNote()
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GuideIcons.Heart,
                        contentDescription = "Save place",
                        tint = if (saved) GuideTokens.Primary else GuideTokens.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // ---- 4. Pinned Bottom Action Dock (Alpine Lakeview Zurich CTA) ------
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = GuideTokens.Surface,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, GuideTokens.Border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = GuideTokens.Space.base, vertical = GuideTokens.Space.md),
            ) {
                // Wide Sunset Coral CTA Button
                GuideButton(
                    text = guide.app.data.AppStrings.startAudioStory(language),
                    onClick = onStartAudio,
                    variant = GuideButtonVariant.Primary,
                    icon = GuideIcons.Speak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailPill(label: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(GuideTokens.ChipRadius),
        color = GuideTokens.Surface2,
        border = BorderStroke(1.dp, GuideTokens.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GuideTokens.Primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = GuideTokens.Caption.copy(fontWeight = FontWeight.Medium),
                color = GuideTokens.Text,
            )
        }
    }
}

@Composable
private fun NumberBadge(number: Int) {
    Surface(
        shape = CircleShape,
        color = GuideTokens.PrimaryWash,
        border = BorderStroke(1.dp, GuideTokens.Primary.copy(alpha = 0.3f)),
        modifier = Modifier.size(24.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = GuideTokens.Caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                color = GuideTokens.Primary,
            )
        }
    }
}

/** Open / closed tag. */
@Composable
private fun HoursTag(hoursText: String?, openNow: Boolean, language: guide.app.data.AppLanguage) {
    if (hoursText != null) {
        val word = if (openNow) guide.app.data.AppStrings.openNow(language, hoursText) else guide.app.data.AppStrings.closedNow(language)
        val color = if (openNow) GuideTokens.Success else GuideTokens.Danger
        StatusTag(text = word, color = color, icon = GuideIcons.Clock)
    } else {
        StatusTag(text = guide.app.data.AppStrings.openAlways(language), color = GuideTokens.Success, icon = GuideIcons.Clock)
    }
}

private fun layerWord(layer: String, language: guide.app.data.AppLanguage): String = when (layer) {
    "food" -> guide.app.data.AppStrings.chipFood(language)
    "stay" -> guide.app.data.AppStrings.chipStays(language)
    else -> guide.app.data.AppStrings.chipHeritage(language)
}

private fun layerIcon(layer: String): ImageVector = when (layer) {
    "food" -> GuideIcons.Food
    "stay" -> GuideIcons.Bed
    else -> GuideIcons.Heritage
}
