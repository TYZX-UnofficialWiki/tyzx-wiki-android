# 天一维基 · 安卓客户端

江苏省天一中学非官方维基（[wiki.tyzx.org](https://wiki.tyzx.org/)）的第三方 Android 客户端。

界面追求两件事：**Material 3 Expressive** 的表达力，以及被操作时才出现的**液态玻璃**。

> **非官方声明**
> 本项目由学生自发开发，与江苏省天一中学官方、天一维基站点运营方均无隶属关系。
> 内容版权归各条目作者所有，本应用仅提供阅读与编辑入口。站点若要求下线，本项目立即停止分发。

## 当前状态

v0.1，开发中。**目前只做到"看形状"**，还不能正常使用。

| 模块 | 状态 |
|---|---|
| 首页瀑布流（两栏） | 静态数据，未接 API |
| 底栏液态玻璃 | 已完成，移植自 BiliTools |
| 深/浅主题 + Material You 动态取色 | 已完成 |
| 阅读页（原生渲染） | 未开始 |
| 搜索 / 分类 / 随机 | 未开始 |
| 登录、注册、编辑、上传、评论 | 未开始 |

## 下载

每次推送到 `main` 自动构建，产物永远覆盖在同一个地址：

**https://github.com/TYZX-UnofficialWiki/tyzx-wiki-android/releases/tag/latest**

直链：`releases/download/latest/app-debug.apk`

- 调试签名（`app/debug.p12`，口令 `tyzxdebug`），仅供测试
- 需要 Android 13（API 33）及以上
- 覆盖安装不会冲突，因为签名是固定的

## 构建

需要 JDK 17+ 与 Android SDK 37：

```bash
./gradlew assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

仓库自带 GitHub Actions（`.github/workflows/build.yml`），推送到 `main` 即出包并发布 Release。

## 技术栈

- Kotlin + Jetpack Compose（Material 3 1.5）
- 液态玻璃：`io.github.kyant0:backdrop` + 移植自 BiliTools 的交互层（AGSL 运行时着色器）
- 图片：Coil 3
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
