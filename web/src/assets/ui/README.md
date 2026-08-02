# UI 图片资源目录

此目录用于存放前端 UI 图片资源（Logo、图标、背景图、插画等外部图片文件）。

> 规划说明：当前界面使用 Element Plus 矢量图标（`@element-plus/icons-vue`）。
> 后续版本将逐步用**外部图片**替换矢量图 UI，届时把图片文件统一放在本目录中管理。

## 使用方式

图片文件（`png` / `jpg` / `jpeg` / `svg` / `webp` / `gif`）直接放在本目录下，
或按用途分子目录存放：

```
src/assets/ui/
├── logos/          # Logo
├── icons/          # 自定义图标
├── backgrounds/    # 背景图
└── illustrations/  # 插画
```

在代码中通过 `@` 别名引用：

```ts
import uiLogo from '@/assets/ui/logos/logo.png'
```

```html
<img :src="uiLogo" alt="Logo" />
```

## 注意

- 用户上传的文件请走后端存储，不要放入此目录。
- 图片提交前建议压缩（单张 < 200KB），避免增大仓库体积。
