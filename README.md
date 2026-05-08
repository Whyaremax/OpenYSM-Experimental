<div align="center">
  <img src="images/brand.png" alt="logo" width="300"/>
  <h1>OpenYSM</h1>
  <p>YSM开源替代品，基于2.6.5 forge</p>
</div>

## 说明

本仓库包含了 YesSteveModel (YSM) 2.6.5（2026年4月）版本的完整源代码。

包含1.20.1 Forge版本的全部源码。

**请注意：项目并非 Production Ready，可能存在命名语义错误，渲染错误等问题，如果您在使用过程中遇到了任何问题请打开 Issue 反馈，最好附带截图和可能的报错日志。**

## 当前进度（OpenYSM-Experimental）

这个分支目前重点不是做一个稳定发布版，而是把 OpenYSM 的加载、兼容、Java fallback 和 native 渲染实验整理成可测试的工作树。

当前已经接入或验证：

- Forge 1.20.1 / YSM 2.6.5 的 Java 侧模型加载、渲染、动画控制器、音频和纹理解码路径。
- 现代已加密 `.ysm` 模型加载，以及原版/旧版 YSM flat-folder 结构兼容：`info.json`、`main.json`、`arm.json`、`*.animation.json` 和纹理可以合成为当前 descriptor 结构。
- 旧版 `.ysm` 读取 fallback：优先走新版 `YsmCrypt.decryptYsmFile`，失败后尝试 legacy YSGP v1/v2 解包并交给 folder deserializer。
- legacy `extra.animation.json` 会补齐 `properties.extra_animation`，让动画轮盘能显示和触发旧模型的额外动画。
- 默认模型握手/同步路径已修正，避免客户端和服务器在默认模型处理上不一致。
- 可选 native renderer 已接入 Java/JNI render-first fast path：
  - 配置模式保留 `AUTO`、`OFF`、`RENDER_ONLY`、`FULL_EXPERIMENTAL`。
  - `AUTO` 仍默认走 Java renderer；要测试 native 需要选择 `RENDER_ONLY` 或使用 `-Dysm.nativeRenderer=render`。
  - `/openysm native status` 和 `/openysm native parity` 可用于查看 native 状态和做 Java/native 输出对比。
  - 当前 native 后端是 scalar CPU path，不包含 AVX2/SIMD。
  - Windows x64 已有实机渲染验证；CPU Scalar V2 的本地 harness 已通过，Windows 场景 FPS/回归测试仍需要继续跑。

仍然实验中：

- native renderer 永久保留 Java fallback；任何 native 库加载、自检、缓存、DirectBuffer 或兼容路径失败都应回退 Java。
- 旧版/未加密模型仍需要更多真实样本回归，特别是低版本二进制格式和边缘动画数据。
- AVX2/SIMD、GPU renderer、native parser/audio/crypto/zstd 加速暂时不是当前补丁目标。

## 为什么开源？

我们决定将新版 YSM 源码开源，主要基于以下几个原因：

### 1. 新版本的完全重置

新版 YSM 已经经过完全重新设计和开发，采用了全新的架构和加密方式。

我们认为这个全新的架构和加密方式很酷，因此发布了源代码供大家学习研究和使用。

### 2. 新版加密的现状

此前社区已经出现了一个破解 YSM 2.6.5 及以下版本加密的工具和方法，新版的加密机制实际上已经失去了保护作用。

同时，目前社区中的大部分新模型都已经公布了源文件或者被解密，新版加密已经毫无实际意义。

### 3. 支持开放的游戏氛围

我们注意到社区中没有开发者制作了去除加密功能的最新 YSM 版本，这表明了这是一个蓝海市场。

OpenYSM 开发组一直非常支持开放、自由的游戏开发氛围，我们希望通过开源新版源码，为其他开发者的二次开发和学习提供便利。

## TODO

- [x] Ogg Opus音频解码播放
- [x] Webp、Avif等纹理的解码
- [x] 符合YSM标准的服务器客户端通讯握手流程
- [x] 模型的读取与渲染
- [x] 子模型动画控制器
- [x] 与服务器通讯握手时默认模型未正确处理
- [x] 原版/旧版 flat-folder 模型结构兼容：`info.json`、`main.json`、`arm.json`、动画 JSON 和纹理
- [x] legacy `.ysm` fallback 读取路径已接入
- [x] legacy extra animation 会补齐动画轮盘数据
- [x] 可选 scalar native render-first fast path 已接入；Windows x64 已通过实机渲染验证，Java fallback 永久保留
- [x] CPU Scalar V2 native harness 已覆盖骨骼父子层级、part mask、36-byte packed 输出、glow light、projection culling、隐藏骨骼、动画变换和非均匀缩放法线
- [ ] CPU Scalar V2 在 Windows benchmark 场景中继续做 `/openysm native parity` 和 FPS 回归
- [ ] 更多低版本二进制模型/未加密模型真实样本兼容性测试
- [ ] AVX2/SIMD native renderer 优化（等 scalar path 足够稳定后再做）
- [ ] YSGPHeader生成

## 修改

我们相比已经发布的 YSM 版本做出了以下修改

- 使用 Java 重写了加载和渲染逻辑，现在可以脱离 Native 运行，例如在 MacOS，RISC-V 甚至手机上
- 支持现有的已加密的 YSM 模型
- 支持一部分原版/旧版 YSM 模型目录和旧 `.ysm` 包作为兼容输入
- 补齐旧模型 extra animation 到动画轮盘所需的 metadata
- 添加了可选 native scalar renderer，用于测试 Java/JNI 直写顶点缓冲的渲染加速；默认仍以 Java renderer 为稳定 fallback
- 添加了`/openysm cache dump`命令帮助你调试模型传输，导出服务器中的所有模型
- 添加了`/openysm native status`和`/openysm native parity`命令帮助你调试 native renderer 状态和 Java/native 输出一致性

## 开源协议

### 源代码协议

本项目的源代码采用 MIT License 开放，您可以自由地使用、修改和分发代码，仅需要保留原始的版权声明。

详细的许可证条款请参见 LICENSE 文件。

### 模型资源协议

仓库中自带的模型文件采用不同的协议：

- 默认模型: 采用 CC0 (Creative Commons Zero) 协议，完全开放，无任何使用限制
- 酒狐 (Wine Fox) 模型: 采用 CC BY-NC-SA 4.0 协议，允许非商业使用，需要署名，并且衍生作品需要采用相同协议

请在使用相应模型时严格遵守对应的协议要求。

## 使用建议

我们鼓励开发者基于此源码进行二次开发，创造出更加开放、易用的模型加载工具。
