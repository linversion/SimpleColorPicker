# 图片取色方案

## 点选（eyedropper）

旧逻辑不是成熟做法：触摸点直接 `getPixel`，图片只按垂直居中绘制、X 不做偏移，宽图/窄图时准星和像素会对不上；也没有孔径。

现在：

1. 图片在 Canvas 里水平+垂直居中，原点记在 `imageOffset`
2. 触摸坐标减去原点再取样
3. 半径 5px 圆形孔径，RGB 通道各自中值
4. 解码强制 `SOFTWARE` + `ARGB_8888`，避免 Hardware Bitmap 不能读像素

仍没做的：放大镜、缩放平移后的矩阵映射、线性 RGB 平均。够用，还不是 Photoshop 级。

## 色板

选图后后台提取主色。

```
原图（缩小到约 96×96 面积）
        |
        | Android Palette / median-cut，最多 16 个 swatch
        v
Lab ΔE < 14 合并相近色
        |
        v
最多 8 色，按像素占比排序
```

入口：顶栏 Palette 图标 → `ModalBottomSheet`。点击一行复制 `#RRGGBB`。

Palette 是 Android 上的常规做法（Material 取色、通知栏配色都用它）。它不是 k-means，是 median-cut 量化，对海报/插画稳，对渐变照片会合并成几块主色，这是预期。

## 文件

| 文件 | 职责 |
| --- | --- |
| `picker/PaletteExtractor.kt` | Palette + ΔE 合并、孔径中值 |
| `picker/ColorPickerController.kt` | 坐标映射与点选 |
| `picker/ImageColorPicker.kt` | 居中绘制 + 触摸 |
| `ui/widget/PaletteBottomSheet.kt` | Sheet UI 与复制 |
| `OpenImageViewModel.kt` | 色板状态 |
