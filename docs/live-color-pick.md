# 实时取色方案

记录当前相机预览取色的实现，方便以后改参数或换算法时对账。

## 目标

预览跟手，但顶栏颜色不因噪声、手抖、AE/AWB 微漂而每帧乱跳。对准星看到的颜色，而不是 analysis buffer 的几何中心。

## 管线

```
CameraXViewfinder (Alignment.Center + ContentScale.Crop，居中裁剪)
        |
        | 取景为居中裁剪，准星恒等于 buffer 中心，直接取中心
        v
ImageAnalysis RGBA 640x480 @ ~20Hz
        |
        | 圆形孔径 r=8，RGB 通道各自取中值
        v
sRGB -> Lab
        |
        | EMA α=0.28
        | ΔE76 < 1.6 则不刷新 UI
        v
Lab -> sRGB -> ColorState
        |
        | locked=true 时丢弃新样本
        v
顶栏 / Hex
```

预览实现见 `camera-compose-migration.md`。

## 模块

| 文件 | 职责 |
| --- | --- |
| `camera/CameraPreview.kt` | 绑定 Preview + Analysis，对准星做一次 AF/AE/AWB 测光 |
| `camera/ColorAnalyzer.kt` | 缓冲区中心取样、圆形孔径、中值 |
| `camera/ColorScience.kt` | sRGB ↔ CIE Lab（D65）、ΔE76 |
| `MainViewModel.kt` | Lab EMA、死区、锁定 |
| `MainActivity.kt` / `ColorResult.kt` | 点准星或锁图标冻结颜色 |

## 为什么这样选

**RGBA 而不是手转 YUV**  
CameraX `OUTPUT_IMAGE_FORMAT_RGBA_8888` 把格式转换交给框架，避开 BT.601/709 系数和 UV stride 踩坑。

**中心即准星**  
取景是居中裁剪（`ContentScale.Crop`），缩放、旋转、镜像都围绕中心，视图中心在仿射变换下恒等于 analysis buffer 中心，直接取中心即可。PreviewView 时代的 `outputTransform` 映射链已随 camera-compose 迁移删除；若以后支持非中心取样，需用 `MutableCoordinateTransformer` 重建。

**圆形孔径 + 中值**  
半径 8px（约 17×17 的圆）。中值抗高光、接缝、摩尔纹，比 RGB 均值稳。中值仍在 sRGB 上做，这是算力和鲁棒性的折中；色度平滑放到下一步的 Lab。

**Lab EMA + ΔE 死区**  
在 Lab 里做指数滑动平均（α=0.28），再用 CIE76。ΔE < 1.6 视为同一色，不推 UI。比 RGB 欧氏距离更接近观感。没用 CIEDE2000，成本更高，当前孔径精度也用不上。

**不锁死 3A**  
绑定后对准星 `startFocusAndMetering`，4 秒自动取消。全程 `CONTROL_AE_LOCK` / `AWB_LOCK` 会让转镜头后整幅曝光和白平衡错位，取色"稳但偏"。锁定对象是颜色样本，不是相机 3A。

**点按锁定**  
`ColorState.locked`。锁定后 `onSample` 直接 return，方便抄 Hex。再点准星或锁图标解锁。

## 关键参数

| 参数 | 值 | 位置 |
| --- | --- | --- |
| Analysis 分辨率 | 640×480，就近回退 | `CameraPreview` |
| 采样间隔 | 50ms | `ColorAnalyzer.analyzeIntervalMs` |
| 孔径半径 | 8px | `ColorAnalyzer.radiusPx` |
| EMA α | 0.28 | `MainViewModel.smooth` |
| ΔE 死区 | 1.6 | `MainViewModel.holdDeltaE` |
| 明暗阈值 | Lab L ≥ 55 | `MainViewModel.onSample` |
| 测光自动取消 | 4s | `CameraPreview` |

调手感优先改这张表，不要先动色空间。

## 明确没做的

- 可拖动准星（现在固定屏幕中心）
- 放大镜 / 冻结整帧再取
- 孔径内高斯加权或线性 RGB 平均
- CIEDE2000、命名色、色板历史
- 显示色域与相机色域标定

## 旧方案为什么弃掉

早期是 3 秒采 1 个 YUV 像素，直接 `emit`。单像素噪声、预览与 buffer 中心错位、手写 YUV、无时间滤波，叠在一起就是"颜色一直在变"。
