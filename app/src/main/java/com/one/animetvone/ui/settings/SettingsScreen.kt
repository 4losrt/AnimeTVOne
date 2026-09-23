package com.one.animetvone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.one.animetvone.data.source.AnimeListDiskCache
import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.data.source.HistoryManager
import com.one.animetvone.ui.theme.TvAccentCyan
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvPureWhite
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import com.one.animetvone.data.repository.AnimeRepositoryImpl
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val firstAction = remember { FocusRequester() }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { firstAction.requestFocus() }
    Column(
        modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(TvBackgroundTop, TvBackgroundBottom))).padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Settings, "設定", tint = TvAccentCyan)
            Spacer(Modifier.width(14.dp))
            Text("設定", color = TvPureWhite, style = MaterialTheme.typography.headlineMedium)
        }
        Text("資料與播放紀錄", color = TvAccentCyan, fontSize = 17.sp)
        Text("動畫列表最後更新：" + AnimeListDiskCache.lastUpdatedMillis().let {
            if (it > 0) DateFormat.getDateTimeInstance().format(Date(it)) else "尚未更新"
        }, color = TvPureWhite.copy(alpha = .65f), fontSize = 14.sp)
        SettingButton("重新整理動畫資料", "從來源取得最新列表，保留追番與播放紀錄", Icons.Rounded.Settings, Modifier.focusRequester(firstAction)) {
            lifecycleOwner.lifecycleScope.launch {
                AnimeListDiskCache.clearCache()
                AnimeRepositoryProvider.instance.warmUp()
                status = "動畫資料已嘗試更新；離線時會保留目前畫面"
            }
        }
        SettingButton("重設封面快取", "清除已保存的封面連結，下次載入重新搜尋", Icons.Rounded.Image) {
            confirmation = "封面"
        }
        SettingButton("清除播放歷史", "永久刪除所有續播進度", Icons.Rounded.DeleteSweep) {
            confirmation = "歷史"
        }
        Spacer(Modifier.height(12.dp))
        status?.let { Text(it, color = TvAccentCyan, fontSize = 15.sp) }
        Text("AnimeTV  •  Fire TV Edition", color = TvPureWhite.copy(alpha = .45f), fontSize = 14.sp)
    }
    confirmation?.let { target ->
        AlertDialog(onDismissRequest = { confirmation = null }, confirmButton = {
            TextButton(onClick = {
                if (target == "歷史") HistoryManager.clearHistory(context)
                else {
                    CoverImageFetcher.clearAllCache(context)
                    (AnimeRepositoryProvider.instance as? AnimeRepositoryImpl)?.invalidateCovers()
                }
                status = if (target == "歷史") "播放歷史已清除" else "封面快取已重設"
                confirmation = null
            }) { Text("確認清除") }
        }, dismissButton = {
            TextButton(onClick = { confirmation = null }) { Text("取消") }
        }, title = { Text("清除${target}資料？") }, text = {
            Text(if (target == "歷史") "續播進度將無法復原。" else "封面會在下次顯示時重新下載。")
        })
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(72.dp),
        colors = ButtonDefaults.colors(
            containerColor = TvBackgroundTop,
            contentColor = TvPureWhite,
            focusedContainerColor = TvAccentCyan,
            focusedContentColor = TvBackgroundTop
        ),
        border = ButtonDefaults.border(
            border = Border(border = androidx.compose.foundation.BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(12.dp)),
            focusedBorder = Border(border = androidx.compose.foundation.BorderStroke(2.dp, TvAccentCyan), shape = RoundedCornerShape(12.dp))
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title)
            Spacer(Modifier.width(16.dp))
            Column { Text(title, fontSize = 18.sp); Text(subtitle, fontSize = 13.sp) }
        }
    }
}
