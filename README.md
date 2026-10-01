# 羊了个羊/sheep - J2ME 三消小游戏

一个使用 Java ME / J2ME（MIDP + CLDC）编写的三消小游戏，入口为 `cn.sheep.sheep`，主逻辑在 `cn.sheep.Game`。

玩家点击或选择最上层未被遮挡的方块，将其放入底部槽位；三个相同方块会自动消除。槽位满 7 个且无法三消时失败，清空所有方块后过关。

> 本项目仅供学习、交流与 J2ME 技术研究使用。请勿用于商业用途。

## 截图

> 建议放 2~4 张截图或 GIF。

![游戏截图](docs/screenshot1.png)
![游戏截图](docs/screenshot2.png)

## 功能特性

- J2ME MIDlet + Canvas 实现
- 两个关卡
- 三消槽位玩法
- 自动排序、三个相同自动消除
- 支持按键操作和触摸操作
- 暂停菜单：继续、重开、帮助、关于、退出
- 背景音乐播放
- 简单粒子消除效果
- 被遮挡方块不可点击

## 玩法说明

1. 点击或选中最上层、未被遮挡的方块。
2. 方块会进入底部槽位。
3. 槽位中三个相同方块会自动消除。
4. 槽位最多 7 个。
5. 如果槽位满 7 个且无法三消，则游戏失败。
6. 清空场上所有方块后过关。

## 操作方式

### 触摸屏

- 点击方块：放入槽位
- 点击底部“菜单”：暂停
- 点击底部“移出”：撤销/移出槽位方块

### 按键

- 方向键：移动光标
- 确认键 / 5 / 8：选择方块
- `*` 键：撤销 / 移出
- 左软键 / 右软键：暂停菜单

具体按键可能因模拟器或真机不同而有所差异。

## 目录结构

```text
sheep/
├── src/
│   └── cn/
│       └── sheep/
│           ├── sheep.java          # MIDlet 入口
│           ├── Game.java           # 游戏主逻辑、绘制、输入、关卡
│           ├── Tile.java           # 方块数据
│           ├── Particle.java       # 粒子数据
│           └── ParticleTile.java   # 粒子组
├── res/                            # 图片、音频资源
│   ├── block_lawn.png
│   ├── block_bg.png
│   ├── slot_bg.png
│   ├── block_bg_dark_1.png
│   ├── block_bg_dark_2.png
│   ├── block_bg_dark_3.png
│   ├── block_1.png ... block_16.png
│   ├── block_1_dark_1.png ... block_16_dark_3.png
│   ├── lawn_1.wav
│   └── lawn_2.wav
├── README.md
└── LICENSE
```

> 注意：代码中资源路径是 `/block_lawn.png`、`/lawn_1.wav` 这种根路径形式。打包进 JAR 后，资源需要位于 JAR 根目录，或者在构建时把 `res/` 映射到根目录。

## 环境要求

- Java ME / J2ME
- CLDC 1.1
- MIDP 2.0
- 支持 `javax.microedition.media`
- 支持 PNG 图片和 WAV 音频
- 推荐使用：
  - Sun Java Wireless Toolkit
  - Java ME SDK
  - Eclipse ME
  - KEmulator
  - J2ME Loader
  - 支持 J2ME 的真机

## 构建与运行

### 使用 J2ME Wireless Toolkit / Java ME SDK

1. 新建一个 MIDlet 项目。
2. MIDlet 入口类填写：

```text
cn.sheep.sheep
```

3. 将 `src/cn/sheep/` 下的 Java 文件加入项目。
4. 将 `res/` 中的资源放到项目资源目录。
5. 确保打包后资源位于 JAR 根目录。
6. 构建生成 `.jar` 和 `.jad`。
7. 使用模拟器或真机运行。

### 使用 KEmulator / J2ME Loader

1. 先构建出 `.jar` 文件。
2. 打开 KEmulator 或 J2ME Loader。
3. 加载 `.jar` 或 `.jad`。
4. 开始游戏。

## 代码结构说明

- `sheep.java`：MIDlet 入口，负责启动 `Game`。
- `Game.java`：核心类，包含绘制、输入、关卡初始化、槽位逻辑、消除逻辑、暂停菜单、帮助和关于。
- `Tile.java`：单个方块的数据结构。
- `Particle.java`：消除粒子。
- `ParticleTile.java`：一组粒子效果。

## 已知问题

- 资源加载失败时部分异常被静默忽略，资源缺失可能表现为黑屏或方块不显示。
- 音效播放依赖设备/模拟器实现，部分环境可能无法播放。
- 坐标和尺寸部分硬编码，不同分辨率设备上可能布局异常。
- 当前只有两个关卡。
- 代码包含反编译风格写法，后续可继续重构。
- 没有存档、排行榜、设置等功能。

## 开源协议

本项目代码使用 `[MIT / Apache-2.0 / GPL-3.0 / 其他]` 协议开源。

> 注意：如果图片、音频、原始代码来自第三方或原版游戏，这些资源不一定能随代码一起使用同一协议。请确认你拥有相应版权，或在 README 中单独注明来源和授权。

建议：

- 代码：`[你的许可证]`
- 图片/音频素材：`[来源于网络]`

## 致谢

- 感谢 J2ME / MIDP 社区
  - 厂商：`[xmwold.com]`
- 感谢所有测试和反馈的朋友

## 免责声明

本项目仅供学习、研究和交流使用，不保证稳定性与完整性。  
若本项目代码、图片、音频或其他内容侵犯了你的权益，请联系 `[你的邮箱]`，我会尽快处理。  
请勿将本项目用于商业用途或任何违法用途。

## 贡献

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 新建分支：`git checkout -b feature/xxx`
3. 提交修改：`git commit -m "Add xxx"`
4. 推送分支：`git push origin feature/xxx`
5. 提交 Pull Request

## 联系方式

- 作者：`[mmcnb]`
- GitHub：`[mmcnb]`
- 邮箱：`[3475272270]`
- 项目地址：`[sheep]`

## 更新日志

### v1.0.0

- 完成 J2ME 版三消基础玩法
- 支持两个关卡
- 支持暂停、帮助、关于
- 支持触摸和按键操作
