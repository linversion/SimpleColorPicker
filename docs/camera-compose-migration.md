# camera-compose 迁移调研

把预览从 `AndroidView` + `PreviewView` 互操作迁到 `androidx.camera:camera-compose`（CameraX 1.6.2）的调研结论与取舍记录。

## 库的形态

CameraX 1.5.0 起提供 `androidx.camera:camera-compose`，传递依赖
`androidx.camera.viewfinder:viewfinder-compose` / `viewfinder-core`（均 1.6.2 同版本）。

1.6.2 稳定版里只有一个低层构件：

```kotlin
// androidx.camera.compose
@Composable
fun CameraXViewfinder(
    surfaceRequest: SurfaceRequest,          // 非空！初始没有请求时不能组合
    modifier: Modifier = Modifier,
    implementationMode: ImplementationMode,  // Performance/兼容模式自动回退
    coordinateTransformer: MutableCoordinateTransformer,
    alignment: Alignment,
    contentScale: ContentScale,
)
```

- 渲染路径：`Preview` use case 的 `SurfaceRequest` 直接喂给 `CameraXViewfinder`，
  相机帧直达 Compose 布局，没有 View 层中转
- **没有**现成的高层 `CameraPreview`（带 tap-to-focus、生命周期绑定那种）。
  早期 alpha（`androidx.camera.view.compose.CameraPreview` + `onPreviewView` 回调）
  在正式版前被整个替换掉了，旧博客/旧 AI 记忆不可信；官方模式（Android Developers
  博客、jetpack-camera-app 参考应用）都是在 `CameraXViewfinder` 之上自己组合
- API 面无法从文档确认时（本机访问 developer.android.com 超时），直接解包
  Gradle 缓存里的 AAR 用 `javap` 看签名，是最准的方式；`surfaceRequest`
  非空就是编译器抓出来的，字节码 `javap` 不显示可空性

## 关键取舍

### FILL_CENTER 的等价表达

```kotlin
CameraXViewfinder(
    surfaceRequest = ...,
    alignment = Alignment.Center,
    contentScale = ContentScale.Crop,   // == PreviewView.FILL_CENTER
)
```

### 准星坐标映射：直接删掉

旧实现：`PreviewView.outputTransform` → `CoordinateTransform` →
analysis buffer 坐标，把视图中心映射到取样点。

新实现直接取 `image.width / 2, image.height / 2`。依据：取景是居中裁剪
（FILL_CENTER / Crop），缩放、旋转、镜像都围绕中心进行，视图中心在仿射变换下
恒等于缓冲区中心。旧代码在 transform 不可用时的 fallback 恰好就是这个逻辑，
迁移后它是主路径。副作用是若以后要支持非中心取样，需用
`MutableCoordinateTransformer`（viewfinder 版的 outputTransform）重建映射链。

### 中心测光对焦

`PreviewView.meteringPointFactory` 没有了。改用归一化中心点：

```kotlin
SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f)
```

归一化中心与分辨率、旋转无关，对「只对准星对焦」这一需求足够。

### 依赖

`camera-view` 整个移除（`PreviewView`、`CoordinateTransform` 不再被引用），
`camera-compose` 补上；`camera-lifecycle`、`camera-core` 不变。

## 已知行为差异

- `SurfaceRequest` 到达前 viewfinder 不组合，首帧前是空白（PreviewView
  时代是黑帧），视觉差异极小
- 分辨率切换时 `setSurfaceProvider` 会发新 `SurfaceRequest`，用
  `MutableStateFlow<SurfaceRequest?>` 承接、`collectAsState` 驱动重组即可，
  旧 request 由 viewfinder 内部完成释放

## 验证记录

- dev/product debug + product release（R8）构建通过
- 真机：相机打开、取景渲染、准星取色值与画面吻合、无 crash

参考：[Getting Started with CameraX in Jetpack Compose](https://medium.com/androiddevelopers/getting-started-with-camerax-in-jetpack-compose-781c722ca0c4)、
[google/jetpack-camera-app](https://github.com/google/jetpack-camera-app)、
CameraX release notes
