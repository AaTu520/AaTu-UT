# AaTu_UT

冠军等级与 AaTu_EXUp 的装备强化附属，提供装备拆解、属性提取与突破。

## 成品

Minecraft 1.21.1。

- NeoForge [aatu_ut-1.0.0.jar](artifacts/neoforge/aatu_ut-1.0.0.jar)

[装备与强化说明](GUIDE.md)

## 构建

将 [AaTu-EXUp](https://github.com/AaTu520/AaTu-EXUp) 克隆到同一父目录，保留文件夹名 `AaTu-EXUp`。

将冠军等级 1.0.0 的编译依赖放入 `libs/champions_level-1.0.0.jar`。

使用 JDK 21，在仓库根目录执行 `.\gradlew.bat assemble -x test`。
