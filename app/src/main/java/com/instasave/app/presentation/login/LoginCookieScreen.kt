package com.instasave.app.presentation.login

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instasave.app.domain.model.SessionState
import com.instasave.app.presentation.theme.AccentError
import com.instasave.app.presentation.theme.AccentSuccess
import com.instasave.app.presentation.theme.HairlineBorder
import com.instasave.app.presentation.theme.PitchBlack
import com.instasave.app.presentation.theme.SurfaceDark
import com.instasave.app.presentation.theme.TextMuted
import com.instasave.app.presentation.theme.TextPrimary
import com.instasave.app.presentation.theme.TextSecondary
import com.instasave.app.presentation.theme.Typography

@Composable
fun LoginCookieScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    var isWebViewVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(16.dp)
    ) {
        Text(text = "Session Sync", style = Typography.headlineLarge)
        Text(
            text = "Log into Instagram to download Stories, Highlights & Private posts you follow",
            style = Typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (val state = sessionState) {
            is SessionState.SignedIn -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active", tint = AccentSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Active Session", style = Typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (!state.handle.isNullEmpty()) "Signed in as @${state.handle}" else "Instagram session active in local encrypted store",
                            style = Typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = { viewModel.onSignOut() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentError)
                        ) {
                            Text(text = "Sign Out & Erase Session", style = Typography.titleMedium)
                        }
                    }
                }
            }
            SessionState.SignedOut -> {
                if (!isWebViewVisible) {
                    // Consent Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Security", tint = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Private Account & Stories Access", style = Typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Authentication runs in a sandboxed WebView directly on official instagram.com. Session keys are encrypted locally using Android KeyStore. Zero credentials leave your device.",
                                style = Typography.bodyMedium,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { isWebViewVisible = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TextPrimary,
                                    contentColor = PitchBlack
                                )
                            ) {
                                Text(text = "Open Instagram Login", style = Typography.titleMedium)
                            }
                        }
                    }
                } else {
                    // Sandboxed WebView for instagram.com authentication
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            val cookieManager = CookieManager.getInstance()
                                            val cookiesStr = cookieManager.getCookie("https://www.instagram.com") ?: ""

                                            if (cookiesStr.contains("sessionid=")) {
                                                val cookieMap = mutableMapOf<String, String>()
                                                cookiesStr.split(";").forEach { part ->
                                                    val kv = part.trim().split("=")
                                                    if (kv.size == 2) {
                                                        cookieMap[kv[0]] = kv[1]
                                                    }
                                                }
                                                viewModel.onCookiesCaptured(cookieMap, handle = null)
                                                onLoginSuccess()
                                            }
                                        }
                                    }
                                    loadUrl("https://www.instagram.com/accounts/login/")
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
