# 天一维基 · 安卓客户端

江苏省天一中学非官方维基（[wiki.tyzx.org](https://wiki.tyzx.org/)）的第三方 Android 客户端。

界面追求两件事：**Material 3 Expressive** 的表达力，以及被操作时才出现的**液态玻璃**。

> **非官方声明**
> 本项目由学生自发开发，与江苏省天一中学官方、天一维基站点运营方均无隶属关系。
> 内容版权归各条目作者所有，本应用仅提供阅读与编辑入口。站点若要求下线，本项目立即停止分发。

## 下载

每次推送到 `main` 自动构建，产物永远覆盖在同一个地址：

**https://github.com/TYZX-UnofficialWiki/tyzx-wiki-android/releases**

每次构建的版本号是 `0.3.<CI 构建序号>`（例如 `0.3.146`），tag 与之同名，取最新一版：

直链：`https://github.com/TYZX-UnofficialWiki/tyzx-wiki-android/releases/latest/download/app-debug.apk`

- 调试签名（`app/debug.p12`，口令 `tyzxdebug`），仅供测试
- 需要 Android 13（API 33）及以上
- 签名固定，覆盖安装不会冲突

## 功能

| 模块 | 状态 | 说明 |
|---|---|---|
| 首页 | 可用 | 随机条目双栏瀑布流 + 最近更新横向条 + 分类切换 |
| 搜索 | 可用 | 280ms 防抖，命中高亮，点开即读 |
| 分类 | 可用 | 按条目数排序（`acprop=size`），可下钻到条目列表 |
| 阅读页 | 可用 | Parsoid HTML + WebView 渲染，注入主题 CSS，站内链接走原生路由，外链按设置处理 |
| 登录 | 可用 | `action=login`，会话 cookie 保存在内存 |
| 注册 | 可用 | 支持 ConfirmEdit 图片验证码（解析 AuthManager 的 `requests` 字段） |
| 源码编辑 | 可用 | 等宽编辑器，摘要 + 监视开关，`baserevid` 冲突检测，未保存退出提醒 |
| 可视化编辑 | 可用 | Parsoid HTML 进 `contenteditable`，保存时经 `/transform/html/to/wikitext` 转回 wikitext |
| 图片上传 | 可用 | 相册选图 → multipart `action=upload` → 自动插入 `[[File:…]]` |
| 讨论页 | 可用 | 读 Talk 页，可发新话题（`section=new`） |
| 分享 | 可用 | 系统分享面板，带条目直链 |
| 深/浅主题 + Material You | 可用 | 默认品牌紫；可切跟随壁纸 |
| 阅读设置 | 可用 | 字号四档、只读模式、外链策略，持久化 |

### 已知限制

- 首页"兴趣流"没做：站点只有 15 个条目，按点击/相似度排意义不大，先用随机代替
- 阅读页是 WebView，不是 Compose 原生排版——这是权衡后的选择，MediaWiki 语法里有大量 HTML 标签，原生渲染兼容性差
- 没有离线缓存
- 只有中文站（`zh-wiki`），多语言未接
- 可视化编辑、图片上传、注册验证码三条链路**已在服务端验证接口，但尚未在真机上逐项点过**

## 界面

底栏平时是 Material 3 Expressive 的平涂指示器，按下才液化成带色散边缘的玻璃透镜，松手凝固回平涂。实现取自 BiliTools 的 `ui/liquidtabs`，见下方致谢。

## 构建

需要 JDK 17+ 与 Android SDK 37（Compose 1.12 的硬要求）：

```bash
./gradlew assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

仓库自带 GitHub Actions（`.github/workflows/build.yml`），推送到 `main` 即出包并发布 Release。

## 代码结构

```
app/src/main/java/org/tyzx/wiki/
├── MainActivity.kt          宿主：主题、路由、底栏、设置页
├── net/Wiki.kt              MediaWiki Action API + Parsoid REST 封装
├── ui/
│   ├── Home.kt              首页瀑布流
│   ├── Explore.kt           搜索 + 分类
│   ├── Reader.kt            阅读页（WebView）
│   ├── Editor.kt            源码编辑 + 图片上传
│   ├── VisualEditor.kt      可视化编辑
│   ├── Discussion.kt        讨论页
│   └── Auth.kt              登录 / 注册 / 验证码
└── glass/                   液态玻璃底栏（移植自 BiliTools）
```

## 技术栈

- Kotlin + Jetpack Compose（Material 3 1.5）
- 液态玻璃：`io.github.kyant0:backdrop` + 移植自 BiliTools 的交互层（AGSL 运行时着色器）
- 图片：Coil 3
- HTML 解析：jsoup（用于阅读页与讨论页的兜底处理）
- 站点接口：MediaWiki Action API + Parsoid REST（`/rest.php/v1/`）

## 开源许可

本项目以 **GNU General Public License v3.0** 发布，见 [LICENSE](LICENSE)。

之所以是 GPL 而不是 Apache：底栏的交互层移植自 BiliTools（GPL-3.0），传染性要求整仓同为 GPL。

### 致谢与来源

| 项目 | 许可 | 用途 |
|---|---|---|
| [BiliTools](https://github.com/happycola233/BiliTools) | GPL-3.0 | `app/src/main/java/org/tyzx/wiki/glass/liquidtabs/` 下的液态玻璃底栏交互层 |
| [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | Apache-2.0 | 玻璃的着色器与合成基础（BiliTools 亦基于此） |
| [Wikipedia Android](https://github.com/wikimedia/apps-android-wikipedia) | Apache-2.0 | 账号、验证码、编辑器等模块的实现参考 |
| [Ghost Downloader 3](https://github.com/XiaoYouChR/Ghost-Downloader-3) | GPL-3.0 | 视觉与动效参考（未使用其代码） |

站点图标 `tyzx-wiki-logo.png` 版权归天一维基所有，仅用于标识本客户端。
