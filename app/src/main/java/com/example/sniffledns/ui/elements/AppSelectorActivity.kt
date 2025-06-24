package com.example.sniffledns.ui.elements

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.sniffledns.data.AppInfo

class AppSelectorActivity {

    fun getInstalledApps(context: Context): List<ApplicationInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                pm.getLaunchIntentForPackage(it.packageName) != null
            }
    }








}


