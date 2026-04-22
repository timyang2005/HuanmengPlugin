# 幻梦轻小说 LNR 插件

将「幻梦轻小说7.0 API书源」移植为 [LightNovelReader](https://github.com/dmzz-yyhyy/LightNovelReader) 插件。

## 功能

- ✅ 搜索书籍（关键词）
- ✅ 书籍详情（封面、简介、标签、字数、完结状态）
- ✅ 章节目录（按卷分组）
- ✅ 章节正文（文字 + 插图）
- ✅ 探索页面（最新、连载中、已完结、分标签浏览）

## 数据源

- 站点：https://www.huanmengacg.com
- API 密码：`huanmengapi`

## 构建

```bash
./gradlew :plugin:assembleRelease
```

产物在 `plugin/build/outputs/apk/release/huanmeng-plugin-release.lnrp`

## API 版本

- LNR API Version: 2
- compileSdk: 36
- minSdk: 24
