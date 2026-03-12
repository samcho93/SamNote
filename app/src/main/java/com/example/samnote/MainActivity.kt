package com.example.samnote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.samnote.ui.components.SettingsSheet
import com.example.samnote.ui.editor.EditorScreen
import com.example.samnote.ui.theme.SamNoteTheme
import com.example.samnote.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {

    private var showSettings by mutableStateOf(false)

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
            }
            editorViewModel?.openFile(this, it)
        }
    }

    private val saveAsLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/*")
    ) { uri: Uri? ->
        uri?.let {
            editorViewModel?.saveFile(this, it)
            Toast.makeText(this, getString(R.string.file_saved), Toast.LENGTH_SHORT).show()
        }
    }

    private var editorViewModel: EditorViewModel? = null

    private var stateRestored = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: EditorViewModel = viewModel()
            editorViewModel = viewModel

            // 프로세스 재생성 시 상태 복원 (한 번만)
            if (!stateRestored) {
                stateRestored = true
                viewModel.restoreState(this@MainActivity)
            }

            SamNoteTheme(themeMode = viewModel.themeMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding()
                ) {
                    EditorScreen(
                        viewModel = viewModel,
                        onOpenFile = { openFile() },
                        onSaveFile = { saveFile(viewModel) },
                        onSaveAsFile = { saveAsFile(viewModel) },
                        onShowSettings = { showSettings = true }
                    )

                    if (showSettings) {
                        SettingsSheet(
                            onDismiss = { showSettings = false },
                            themeMode = viewModel.themeMode,
                            onThemeModeChange = { viewModel.setTheme(it) },
                            fontSize = viewModel.fontSize,
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            fontFamily = viewModel.fontFamily,
                            onFontFamilyChange = { viewModel.setFontFamily(it) },
                            textColor = viewModel.textColor,
                            onTextColorChange = { viewModel.setTextColor(it) },
                            bgColor = viewModel.editorBgColor,
                            onBgColorChange = { viewModel.setEditorBgColor(it) }
                        )
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        editorViewModel?.saveState(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                editorViewModel?.openFile(this, uri)
            }
        }
    }

    private fun openFile() {
        openFileLauncher.launch(arrayOf("*/*"))
    }

    private fun saveFile(viewModel: EditorViewModel) {
        val activeTab = viewModel.activeTab ?: return
        if (activeTab.uri != null) {
            viewModel.saveFile(this)
            Toast.makeText(this, getString(R.string.file_saved), Toast.LENGTH_SHORT).show()
        } else {
            saveAsFile(viewModel)
        }
    }

    private fun saveAsFile(viewModel: EditorViewModel) {
        val activeTab = viewModel.activeTab ?: return
        saveAsLauncher.launch(activeTab.fileName)
    }
}
