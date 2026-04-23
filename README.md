# NoSleep

NoSleep 是一个简单的 Android 网页常亮工具。它可以在内置 WebView 中打开用户输入的网页，并在指定时间内保持屏幕常亮，适合需要长时间查看网页看板、工作页面或本地服务页面的场景。

## 主要功能

- 自定义输入访问 URL。
- 在 App 内置 WebView 中打开网页。
- 支持常亮时长：30 分钟、1 小时、3 小时、6 小时、8 小时、12 小时。
- 到达常亮时长后自动撤销常亮控制，恢复系统息屏策略。
- 支持保存常用 URL 预设。
- 工作页启用安全窗口，减少系统截图和录屏风险。

## 使用方法

1. 安装 APK 并打开 App。
2. 在 `访问URL` 中输入你有权访问的网页地址，例如 `https://your-domain.example/path`。
3. 勾选 `使用确认`。
4. 选择需要的 `工作页常亮时长`。
5. 点击 `打开网页`。

如需下次快速打开同一地址，可以填写 `预设名称（可选）`，然后点击 `保存预设`。保存后的预设会显示在首页，点击 `应用` 可快速填回 URL 和常亮时长。

## 隐私与数据

NoSleep 不会上传 URL、网页内容或任何用户数据。访问 URL 默认不会自动保存，只有用户主动点击 `保存预设` 时，URL 才会保存在本机。

当前公开版不包含内置业务 URL、账号密码字段、隐藏采集逻辑或水印功能。

## 构建

推荐使用 Android Studio 自带 JDK：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
GRADLE_USER_HOME="$PWD/.gradle-home" \
./gradlew :app:assembleDebug --no-daemon
```

生成的 Debug APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```
## 注意事项
- 长时间常亮会增加耗电和发热，请按实际需要选择常亮时长。
- 使用者应确认自己有权访问输入的 URL，并自行承担访问内容相关责任。
- Android 厂商系统可能会对后台、WebView 或电源策略有差异，建议以真机测试结果为准。
- 本项目已基于 AICoding 完成开发，现已停止维护。
