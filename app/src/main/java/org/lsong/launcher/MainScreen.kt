package org.lsong.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun MainScreen(appListManager: AppListManager) {
    val appList by appListManager.appList.collectAsState()

    LaunchedEffect(Unit) {
        appListManager.refreshAppList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f)),
    ) {
        ClockDisplay()
        AppList(appList)
    }
}

@Composable
fun ClockDisplay() {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance()
            currentTime = DateFormat.format("h:mm a", calendar).toString()
            currentDate = DateFormat.format("EEE MMM d", calendar).toString()
            delay(60000) // Update every minute
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp, top = 64.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = currentTime,
            color = Color.White,
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = currentDate,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun AppList(appList: List<AppInfo>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 64.dp),
    ) {
        items(appList, key = { it.packageName }) { app ->
            AppItem(app = app)
        }
    }
}

@Composable
fun AppItem(app: AppInfo) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.packageManager?.getLaunchIntentForPackage(app.packageName)?.let {
                    context.startActivity(it)
                }
            }
            .padding(32.dp, 8.dp)
        ,
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = "App Icon for ${app.name}",
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.name,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

data class AppInfo(
    val name: String,
    val icon: Drawable,
    val packageName: String,
)

class AppListManager(private val context: ComponentActivity) {
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList = _appList.asStateFlow()

    fun refreshAppList() {
        context.lifecycleScope.launch(Dispatchers.Default) {
            _appList.value = getInstalledApps()
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager ?: return emptyList()
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val flags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        return packageManager.queryIntentActivities(intent, flags)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo: ResolveInfo ->
                AppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.activityInfo.loadIcon(packageManager),
                    packageName = resolveInfo.activityInfo.packageName
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }
}
