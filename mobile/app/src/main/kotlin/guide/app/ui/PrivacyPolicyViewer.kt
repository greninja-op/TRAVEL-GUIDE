package guide.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import guide.app.ui.components.GuideIcons
import guide.app.ui.theme.GuideTokens

/**
 * Native in-app HTML document viewer for the official Privacy Policy & Data Architecture.
 * Loads the local bundled HTML documentation from assets/docs/privacy_policy.html
 * ensuring offline access, zero tracking, and standard mobile document presentation.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyPolicyViewer(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GuideTokens.Bg,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = GuideIcons.ChevronLeft,
                            contentDescription = "Close",
                            tint = GuideTokens.Text,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Policy",
                            style = GuideTokens.Title,
                            fontWeight = FontWeight.Bold,
                            color = GuideTokens.Text,
                        )
                        Text(
                            text = "Official Architecture Documentation",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                }
            }

            // WebView content
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false // No JS needed for static legal doc
                            settings.allowFileAccess = true
                            settings.domStorageEnabled = false
                            setBackgroundColor(0) // Transparent to match theme
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url ?: return false
                                    // Open external links in system browser
                                    if (url.scheme == "http" || url.scheme == "https") {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, url))
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl("file:///android_asset/docs/privacy_policy.html")
                        }
                    },
                )
            }
        }
    }
}
