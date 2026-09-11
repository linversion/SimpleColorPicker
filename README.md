# SimpleColorPicker

相机实时取色 + 图片点选。对准星或点击图片得到 Hex，图片还可以抽一张主色色板。

[Google Play](https://play.google.com/store/apps/details?id=com.linversion.simplecolorpicker)

包名：`com.linversion.simplecolorpicker`  
`compileSdk` / `targetSdk` 36，`minSdk` 23，当前版本 `1.1.0`（versionCode 8）。

## 功能

- **相机取色**：准星固定屏幕中心。RGBA 分析帧、圆形孔径中值、Lab EMA、ΔE 死区。点准星或锁图标冻结当前色。
- **图片取色**：图居中显示，触摸坐标映射到 bitmap，5px 孔径中值。顶栏 Palette 图标弹出色板，点击复制 Hex。
- **换图**：顶栏图片按钮重新选图。

算法细节不在这里展开：

- [docs/live-color-pick.md](docs/live-color-pick.md) 相机管线
- [docs/image-color-pick.md](docs/image-color-pick.md) 图片点选和色板

## 技术栈

Kotlin、Jetpack Compose、CameraX 1.4、AndroidX Palette。

Product flavor：

| flavor | applicationId | 用途 |
| --- | --- | --- |
| `dev` | `com.linversion.simplecolorpicker.dev` | 调试 |
| `product` | `com.linversion.simplecolorpicker` | 上架 |

## 本地构建

JDK 17，AGP 8.5.2，Gradle 8.7。

```bash
./gradlew :app:assembleProductDebug
./gradlew :app:bundleProductRelease
```

Release 签名在 `app/keystore/sign.jks`，配置写在 `app/build.gradle`。

## 出 AAB

推 tag 会跑 `.github/workflows/build-aab.yml`，打 `productRelease` AAB 并挂到 GitHub Release。

```bash
git tag v1.1.0
git push origin v1.1.0
```

改代码后不要复用旧 tag，删掉重打，或升版本再打新 tag。
