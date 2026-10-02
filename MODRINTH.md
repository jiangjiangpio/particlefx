## 简短摘要

粒界 ParticleFX：面向 Minecraft 1.21.11 Fabric 服务器的原版粒子效果引擎，可选中英双语客户端编辑器。

## 详细正文

# 粒界 ParticleFX

**用一个名字管理整套粒子效果。** 在服务器安装粒界后，管理员可以创建、保存、分层编辑并播放粒子效果；普通 Java 玩家不必安装客户端 Mod，也能看到通过原版粒子包发送的效果。

```mcfunction
/particlefx play magic_circle
/particlefx group play new_year_fireworks
/particlefx attach fire_aura @p
/particlefx stop magic_circle
```

### 主要功能

- 可复用的粒子效果预设，以及一个按时间线编排的 `new_year_fireworks` 粒子组：16 枚烟花依次升空、在约 70 格高空错峰爆炸，整场约 2 分钟，最后显示“新年快乐”粒子字样且只播放一次。
- 多图层效果、15 种服务端形状、位置/旋转/缩放、播放模式与动画通道。
- 播放、预览、暂停、恢复、停止、实体跟随、指定观众和 JSON 持久化。
- 可选客户端编辑器：自动识别 Minecraft 语言，中文环境使用中文、其它语言使用英文，也可手动覆盖；支持粒子与形状搜索、几何及本地粒子预览，并可在粒子组页面播放和停止服务器粒子组。
- 简单、高级、专家三级创作工具：XY 手绘画笔、参数方程、曲线/曲面与公式模板。
- 服务端权限校验、效果大小与粒子发包预算。

公式在**客户端**生成最多 256 个静态形状点，保存后由服务器作为原版粒子播放；服务器不运行用户公式，观看效果的玩家不需要粒界客户端 Mod。专家级的噪声、傅里叶级数和场效果模板是形状创作示例，并非运行时物理场或微分方程求解器。

### 安装与兼容

- **游戏版本：** Minecraft Java Edition 1.21.11
- **加载器：** Fabric Loader 0.19.3
- **运行环境：** Java 21；需要适配 1.21.11 的 Fabric API
- **服务端：** 安装粒界 JAR 与 Fabric API，即可使用命令和 JSON 定义
- **客户端：** 安装同一个 JAR 可额外使用编辑器；普通玩家不必安装
- **Mod Menu：** 可选，提供编辑器入口
- **Geyser / Bedrock：** 粒子通过原版 Java 协议发送，由 Geyser 转换；不同 Bedrock 版本的粒子观感请在实际环境中验证

管理员还可以把当前维度的方块绑定到单个效果或粒子组，例如 `/particlefx redstone add ~ ~ ~ group new_year_fireworks`。服务端按红石上升沿触发一次，并将绑定保存到 `config/particlefx/triggers.json`。

高空烟花预设默认可见距离为 128 格，爆炸使用末地烛、荧光、灵魂火焰和电火花等粒子增强夜间辨识度。光影包仍可能改变原版粒子的亮度，不能保证所有光影下自发光；本 Mod 不会为烟花自动放置光源方块。

安装后，管理员在游戏中执行 `/particlefx play magic_circle` 即可体验第一个预设。若安装了客户端 Mod，可按 **P** 或输入 `/particlefx-editor` 打开编辑器。首次启动会在服务器 `config/particlefx/` 中创建配置与预设效果文件。
本项目采用CC0许可证。
