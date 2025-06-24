package com.example.sniffledns

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sniffledns.data.AppInfo
import com.example.sniffledns.ui.theme.SniffleDNSTheme
import com.example.sniffledns.vpn.SniffleVpnService

class MainActivity : ComponentActivity() {

    private val VPN_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val button = Button(this).apply {
            text = "Start SniffleDNS"
            setOnClickListener { startVpn() }
        }
        setContentView(button)
        setContent {
            MainScreen(context = this)
        }

    }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startService(Intent(this, SniffleVpnService::class.java))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun getInstalledApps(context: Context): List<PackageInfo?> {
        val pm = context.packageManager
        return pm.getInstalledPackages(PackageManager.GET_META_DATA)
            .filter {
                pm.getLaunchIntentForPackage(it.packageName) != null
            }
    }

    fun Drawable.toImageBitmap(): ImageBitmap {
        val bitmap = Bitmap.createBitmap(
            intrinsicWidth.takeIf { it > 0 } ?: 1,
            intrinsicHeight.takeIf { it > 0 } ?: 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap.asImageBitmap()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SniffleDNSTheme {
        Greeting("Android")
    }
}

@Composable
fun MainScreen(context: Context) {
    val pm = context.packageManager
    val apps = remember {
        mutableStateListOf<AppInfo>().apply {
            addAll(
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }.map {
                    AppInfo(
                        name = pm.getApplicationLabel(it).toString(),
                        packageName = it.packageName,
                        icon = pm.getApplicationIcon(it)
                    )
                })
        }
    }
    val selectedPackages = apps.filter { it.isBlocked }.map { it.packageName }

    val intent = Intent(context, SniffleVpnService::class.java).apply {
        putStringArrayListExtra("allowedApps", ArrayList(selectedPackages))
    }
    context.startService(intent)

    AppListScreen(apps) { toggledApp ->
        Log.d("SniffleDNS", "${toggledApp.packageName} isBlocked=${toggledApp.isBlocked}")
        // save this to memory/state/dataStore
    }
}

@Composable
fun AppRow(app: AppInfo, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
//        Image(
//            bitmap = app.icon.toImageBitmap(),
//            contentDescription = app.name,
//            modifier = Modifier.size(40.dp)
//        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = app.name, modifier = Modifier.weight(1f), fontSize = 16.sp
        )

        Switch(
            checked = app.isBlocked, onCheckedChange = { onToggle(it) })
    }
}

@Composable
fun AppListScreen(apps: List<AppInfo>, onToggleApp: (AppInfo) -> Unit) {
    LazyColumn {
        items(apps.size) { index ->
            val app = apps[index]
            AppRow(app = app) { isChecked ->
                app.isBlocked = isChecked
                onToggleApp(app)
            }
        }
    }
}
