# RegionStory

RegionStory 是一个面向 Minecraft 1.21.11 的 Fabric 区域剧情对话模组。
区域和对话由数据包 JSON 定义，客户端面板、选项、提示和图标主体使用自定义 GUI Shader 绘制。

## 环境

- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.19.3 或更高
- Fabric API 0.141.6+1.21.11 或兼容版本
- 不依赖 Cloth Config、NanoVG 或其他 UI 库

## 构建

Windows：

```powershell
./gradlew.bat build
```

Linux/macOS：

```bash
./gradlew build
```

产物位于 `build/libs/regionstory-<version>.jar`。

## 安装

将 Fabric API 和构建出的 RegionStory JAR 放入实例的 `mods` 文件夹，然后启动 Minecraft 1.21.11。
客户端首次启动会生成 `config/regionstory.json`。

## 使用流程

1. 进入示例区域。
2. 屏幕中心偏右下方显示区域提示。
3. 按默认 `F` 键，或点击提示条。
4. 进入对话后，点击对话框或按任意键推进无选项台词。
5. 有选项时点击选项条；选项会先播放快速高亮动画，再向服务端发送选择。
6. `Esc` 向服务端请求结束当前对话。

默认示例区域：

- `starfall_circle`：圆形区域，关联 `starfall_intro`。
- `windmill_box`：长方体区域，关联 `windmill_story`。

示例文件位于：

```text
src/main/resources/data/regionstory/regions/
src/main/resources/data/regionstory/dialogues/
```

## 区域 JSON

路径：`data/<namespace>/regions/<file>.json`

圆形区域：

```json
{
  "id": "starfall_circle",
  "dimension": "minecraft:overworld",
  "type": "sphere",
  "center": [0, 64, 0],
  "radius": 8,
  "prompt": "按 F 与派蒙对话",
  "icon": "regionstory:icon/star",
  "dialogue": "starfall_intro",
  "priority": 10
}
```

长方体区域：

```json
{
  "id": "windmill_box",
  "dimension": "minecraft:overworld",
  "type": "box",
  "min": [20, 60, 20],
  "max": [30, 75, 30],
  "prompt": "按 F 调查风车",
  "icon": "regionstory:icon/map",
  "dialogue": "windmill_story",
  "priority": 5
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `id` | 区域唯一 ID |
| `dimension` | 维度 ID |
| `type` | `sphere` 或 `box` |
| `center`、`radius` | 圆形区域中心和半径 |
| `min`、`max` | 长方体两角坐标 |
| `prompt` | 提示条文字，可包含或省略 `F`；界面会把按键单独绘制在提示框左侧 |
| `icon` | 内置图标名，或外部 PNG 资源 ID；外部资源可写 `namespace:textures/gui/name.png`，也可省略 `textures/` 和 `.png` |
| `dialogue` | 对话 ID |
| `priority` | 重叠区域的优先级，数值越大越优先 |

## 对话 JSON

路径：`data/<namespace>/dialogues/<file>.json`

```json
{
  "id": "starfall_intro",
  "start": "start",
  "entries": [
    {
      "id": "start",
      "speaker": "派蒙",
      "speakerTitle": "旅行伙伴",
      "text": "前面就是星落湖了，要不要先去看看？",
      "options": [
        {
          "text": "当然，出发吧。",
          "next": "lake",
          "commands": [],
          "icon": "compass"
        },
        {
          "text": "我们先休息一下。",
          "next": "rest",
          "commands": [],
          "icon": "dialogue"
        }
      ]
    },
    {
      "id": "lake",
      "speaker": "旅行者",
      "speakerTitle": "旅人",
      "text": "湖面比想象中还要平静。",
      "next": "finish",
      "commands": []
    },
    {
      "id": "finish",
      "speaker": "派蒙",
      "speakerTitle": "旅行伙伴",
      "text": "那就这样说定了。",
      "endDialog": true,
      "commands": []
    }
  ]
}
```

`commands` 由服务端以玩家身份执行，可以写带或不带 `/` 的命令。
`next` 为空时结束当前对话，条目或选项设置 `endDialog: true` 也会结束当前对话。
选项的 `icon` 可使用内置别名（如 `compass`、`map`、`exit`），或使用外部 PNG 资源 ID。

## 命令和按键

- `/regionstory reload`：重新加载区域和对话数据，需要 OP 2。
- 默认交互键：`F`，可以在 Minecraft 控制设置中修改。
- 无选项台词：点击对话区域或按任意键推进。
- `Esc`：请求关闭当前对话。

## 镜头配置

配置文件：`config/regionstory.json`

```json
{
  "enterDuration": 12,
  "exitDuration": 12,
  "thirdPersonDistance": 4.5,
  "heightOffset": 0.6,
  "yawOffset": 18.0,
  "pitchOffset": 8.0
}
```

正值 `yawOffset` 表示玩家右后方视角。对话期间会锁定游戏鼠标、隐藏原版 HUD，并保持场景清晰，不添加背景模糊。

## UI 渲染结构

核心面板位于：

```text
src/main/resources/assets/regionstory/shaders/core/
```

- `regionstory_panel.fsh`：开放渐隐面板、胶囊和底部通栏。
- `regionstory_symbol.fsh`：菱形、箭头、聊天气泡、星形、指南针、地图标记等。
- `RegionStoryPanelRenderState`：提交面板四边形和局部坐标。
- `RegionStorySymbolRenderState`：提交无纹理符号四边形。

选项框和提示框的右侧没有闭合圆角，而是通过 `smoothstep` 生成渐变透明的未封口边缘。
核心形状不再依赖 PNG；外部数据包仍可以通过图标资源 ID 指定 PNG 图标。
旧的 `dialogue_left/middle/right.png` 不参与渲染，不需要再制作或放大 UI 贴图。

## 字体

对话、选项和区域提示使用：

```text
src/main/resources/assets/regionstory/font/dialogue.ttf
src/main/resources/assets/regionstory/font/dialogue.json
```

替换 TTF 后重新构建即可。字体由 Minecraft 原生 `TextRenderer` 绘制，面板和图标仍由自定义 Shader 绘制。
如果需要外部图标，将 PNG 放在对应资源包的 `assets/<namespace>/textures/gui/`，然后在 JSON 中填写 `namespace:textures/gui/name.png`。

## 热重载和错误处理

数据包 JSON 在服务端重载时进行 ID、区域坐标、对话跳转和区域-对话引用校验。
单个错误文件会写入日志并跳过，不会让服务器因为一个坏文件崩溃。
客户端 Shader 注册失败时会记录错误并停用 RegionStory UI，避免破坏 Minecraft 主渲染流程。

## 许可证

MIT License，见 [LICENSE](LICENSE)。
