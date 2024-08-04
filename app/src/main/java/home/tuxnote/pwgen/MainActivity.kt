package home.tuxnote.pwgen

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import home.tuxnote.pwgen.ui.theme.PwgenTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Base64

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            PwgenTheme {
                val scope = rememberCoroutineScope()
                val notifyMessage = remember { SnackbarHostState() }
                Scaffold(modifier = Modifier.fillMaxSize(),

                    snackbarHost = {
                        SnackbarHost(
                            hostState = notifyMessage,
                            modifier = Modifier.padding(WindowInsets.ime.asPaddingValues())
                        )
                    }
                ) { innerPadding ->
                    PwgenMain(modifier = Modifier.padding(innerPadding), scope, notifyMessage)
                }
            }
        }
    }
}


@Composable
fun PwgenMain(modifier: Modifier = Modifier, scope: CoroutineScope, snackbar: SnackbarHostState) {
    var passphrase by remember { mutableStateOf("") }
    val context = LocalContext.current
    val focusRequester = FocusRequester()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("PASSPHRASE") },
            singleLine = true,
            modifier = Modifier
                .padding(40.dp, 100.dp)
                .focusRequester(focusRequester)
        );
        OutlinedButton(onClick = {

            if (passphrase.isEmpty()) {
                scope.launch {
                    snackbar.showSnackbar("Passphrase is Empty!")
                }
            } else {
//                val enc = Base64.encodeToByteArray()
                val enc = generateString(passphrase)
                copyToClipboard(context, enc)
                scope.launch {
                    snackbar.showSnackbar(
                        "Passphrase copied to Clipboard!"
                    )
                }
            }
            passphrase = ""
        }) {
            Text(text = "Generate")
        }

    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private fun generateString(data: String): String {
    try {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-512")
        val hash: ByteArray = digest.digest(data.toByteArray())
        return printableHexString(hash).slice(0..11).toByteArray().toBase64()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

private fun generateChecksum(data: ByteArray): String {
    try {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-512")
        val hash: ByteArray = digest.digest(data)
        return printableHexString(hash)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ""
}

fun printableHexString(data: ByteArray): String {
    // Create Hex String
    val hexString: StringBuilder = StringBuilder()
    for (aMessageDigest: Byte in data) {
        var h: String = Integer.toHexString(0xFF and aMessageDigest.toInt())
        while (h.length < 2)
            h = "0$h"
        hexString.append(h)
    }
    return hexString.toString()
}

fun ByteArray.toBase64(): String =
    String(Base64.getEncoder().encode(this))

fun copyToClipboard(context: Context, text: String) {
    val clipboardManager =
        context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("", text).apply {

        if (Build.VERSION.SDK_INT >= 33) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        } else {
            description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }

    })
}