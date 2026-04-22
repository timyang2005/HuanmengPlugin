package com.huanmeng.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.ui.components.SettingsClickableEntry
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi

/**
 * 幻梦轻小说 LNR 插件主类
 *
 * 对应书源：幻梦轻小说7.0 API版
 * 书源站点：https://www.huanmengacg.com
 * API密码：huanmengapi
 */
@Suppress("unused")
@Plugin(
    version = BuildConfig.VERSION_CODE,
    name = "幻梦轻小说",
    versionName = BuildConfig.VERSION_NAME,
    author = "huanmengacg",
    description = "幻梦轻小说全网最齐全的轻小说网，访问速度优质，网页内置插图，拥有海量的轻小说书籍。\n密码：huanmengapi\nTG交流群：https://t.me/huanmengnovel",
    updateUrl = "https://github.com/dmzz-yyhyy/LightNovelReader-PluginRepository/blob/main/data/com.huanmeng.plugin/",
    apiVersion = 2
)
class HuanmengPlugin(
    val userDataRepositoryApi: UserDataRepositoryApi
) : LightNovelReaderPlugin {

    override fun onLoad() {
        // 插件加载完成
    }

    @Composable
    override fun PageContent(paddingValues: PaddingValues) {
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsClickableEntry(
                modifier = Modifier.background(colorScheme.surfaceContainer),
                title = "关于幻梦轻小说",
                description = "全网最齐全的轻小说网，海量书籍，网页内置插图",
                onClick = {}
            )
            SettingsClickableEntry(
                modifier = Modifier.background(colorScheme.surfaceContainer),
                title = "TG交流群",
                description = "https://t.me/huanmengnovel",
                onClick = {}
            )
            SettingsClickableEntry(
                modifier = Modifier.background(colorScheme.surfaceContainer),
                title = "官网",
                description = "https://www.huanmengacg.com",
                onClick = {}
            )
        }
    }
}

class PluginDiscoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {}
}
