package org.mtr.mod;

import com.jonafanho.apitools.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ModUpload {

	private static final String[] MINECRAFT_VERSIONS = {"1.16.5", "1.17.1", "1.18.2", "1.19.2", "1.19.4", "1.20.1", "1.20.4"};


	public static void main(String[] args) throws IOException {
		String changelog = "# 更新日志\n" +
				"\n" +
				"本 Mod 依赖 Minecraft Transit Railway 4.0.0 及以上\n" +
				"\n" +
				"## 新增方块\n" +
				"\n" +
				"### 奥的斯 (Otis) 系列\n" +
				"- 奥的斯 Series 1 候梯楼层显示器（类型 1，横向，占一格）\n" +
				"- 奥的斯 Series 1 候梯楼层显示器（类型 1，横向，占两格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 1，横向，占一格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 1，横向，占两格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 1，带屏幕，横向，占一格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 1，带屏幕，横向，占两格）\n" +
				"- 奥的斯 Series 1 候梯楼层显示器（类型 2，纵向，占一格）\n" +
				"- 奥的斯 Series 1 候梯楼层显示器（类型 2，纵向，占两格）\n" +
				"- 奥的斯 Series 1 候梯楼层显示器（类型 2，横向，占一格）\n" +
				"- 奥的斯 Series 1 候梯楼层显示器（类型 2，横向，占两格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，纵向，占一格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，纵向，占两格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，带屏幕，纵向，占一格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，带屏幕，纵向，占两格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，横向，占一格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，横向，占两格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，带屏幕，横向，占一格）\n" +
				"- 奥的斯 Series 1 到站灯（类型 2，带屏幕，横向，占两格）\n" +
				"\n" +
				"### 迅达 (Schindler) 系列\n" +
				"- 迅达 M 系列候梯楼层显示器（类型 5，占一格）\n" +
				"- 迅达 M 系列候梯楼层显示器（类型 5，占两格）\n" +
				"\n" +
				"## 新增功能\n" +
				"- 电梯门纹理升级：为日立 B85、通力 M 系列、三菱 NexWay、奥的斯 E411 US、迅达 QKS9 电梯门添加侧面及背面贴图，视觉效果更加完整。\n" +
				"- 按钮音效：为部分呼梯面板新增点击音效，提升交互反馈。\n" +
				"\n" +
				"## 新增翻译\n" +
				"- 补全了迅达 M 系列类型 5 楼层显示器的中英文翻译。\n" +
				"- 优化部分原有方块的描述文本，增加方向性标识。\n" +
				"\n" +
				"## 优化内容\n" +
				"- 贴图性能优化：自动将尺寸超过 256 像素的贴图缩放至 256 像素以内，减少资源占用。\n" +
				"- 日立系列显示优化：调整多款呼梯面板的字体与渲染逻辑。\n" +
				"- 蒂森克虏伯字体更新：替换为优化后的点阵字体文件。\n" +
				"\n" +
				"## 修复内容\n" +
				"- 修复 PAT RS01 指示牌退出游戏无法保存的问题，现在退出重进后设置内容不再丢失。\n" +
				"- 修复 YTE 连接器导致到站灯与按钮状态串扰的问题；重构按钮声音播放机制。\n" +
				"- 修复楼层自动设置器在特定场景下陷入死循环的问题。\n" +
				"- 修复迅达 M 系列楼层显示器（类型 5）的渲染异常。\n" +
				"- 修复日立 VIB-181A 等面板的字体错误。\n" +
				"\n" +
				"# Changelog\n" +
				"\n" +
				"This Mod requires Minecraft Transit Railway 4.0.0 or above.\n" +
				"\n" +
				"## New Blocks\n" +
				"\n" +
				"### Otis Series\n" +
				"- OTIS Series 1 Hall Floor Indicator (Type 1, Horizontal) Odd\n" +
				"- OTIS Series 1 Hall Floor Indicator (Type 1, Horizontal) Even\n" +
				"- OTIS Series 1 Hall Lantern (Type 1, Horizontal) Odd\n" +
				"- OTIS Series 1 Hall Lantern (Type 1, Horizontal) Even\n" +
				"- OTIS Series 1 Hall Lantern (Type 1, Horizontal, With Screen) Odd\n" +
				"- OTIS Series 1 Hall Lantern (Type 1, Horizontal, With Screen) Even\n" +
				"- OTIS Series 1 Hall Floor Indicator (Type 2, Vertical) Odd\n" +
				"- OTIS Series 1 Hall Floor Indicator (Type 2, Vertical) Even\n" +
				"- OTIS Series 1 Hall Floor Indicator (Type 2, Horizontal) Odd\n" +
				"- OTIS Series 1 Hall Floor Indicator (Type 2, Horizontal) Even\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Vertical) Odd\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Vertical) Even\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Vertical, With Screen) Odd\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Vertical, With Screen) Even\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Horizontal) Odd\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Horizontal) Even\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Horizontal, With Screen) Odd\n" +
				"- OTIS Series 1 Hall Lantern (Type 2, Horizontal, With Screen) Even\n" +
				"\n" +
				"### Schindler Series\n" +
				"- Schindler M-Series Hall Floor Indicator (Type 5) Odd\n" +
				"- Schindler M-Series Hall Floor Indicator (Type 5) Even\n" +
				"\n" +
				"## New Features\n" +
				"- Elevator Door Texture Upgrade: Added side and back textures for Hitachi B85, Kone M, Mitsubishi NexWay, Otis E411 US, and Schindler QKS9 doors.\n" +
				"- Button Sounds: Added click sound effects for certain call panels.\n" +
				"\n" +
				"## New Translations\n" +
				"- Improved descriptions for existing blocks by adding directional labels (e.g., Vertical, Horizontal).\n" +
				"\n" +
				"## Optimizations\n" +
				"- Texture Performance: Textures larger than 256px are now automatically scaled down to reduce resource usage.\n" +
				"- Hitachi Display Improvements: Adjusted fonts and rendering for multiple Hitachi panel models.\n" +
				"- Thyssenkrupp Font Update: Replaced with an optimized dot-matrix font file.\n" +
				"\n" +
				"## Bug Fixes\n" +
				"- Fixed PAT RS01 Railway Sign not saving its configuration after world reload.\n" +
				"- Fixed YTE Connector causing state crosstalk between hall lanterns and buttons; refactored sound playback mechanism.\n" +
				"- Fixed Floor Auto Setter occasionally entering an infinite loop.\n" +
				"- Fixed rendering issue with Schindler M-Series Screen (Type 5).\n" +
				"- Fixed font errors on certain Hitachi panels such as VIB-181A.";





		if (args.length == 2) {
			for (final String minecraftVersion : MINECRAFT_VERSIONS) {
				for (final ModLoader modLoader : ModLoader.values()) {
					final String modVersion = String.format("%s-%s+%s", modLoader.name, args[0], minecraftVersion);
					final String modVersionUpperCase = String.format("%s-%s+%s", modLoader.name.toUpperCase(Locale.ENGLISH), args[0], minecraftVersion);
					final String fileName = String.format("Yunzhu-Transit-Extension-%s.jar", modVersion);
					final Path filePath = Paths.get("build/release").resolve(fileName);



					// Modrinth
//					final Map<String, DependencyType> dependenciesModrinth = new HashMap<String, DependencyType>();
//					dependenciesModrinth.put("XKPAmI6u", DependencyType.REQUIRED);
//					do {
//					} while (!new ModId("nqMdKn6A", ModProvider.MODRINTH).uploadFile(
//							modVersionUpperCase,
//							modVersionUpperCase,
//							changelog,
//							dependenciesModrinth,
//							ReleaseStatus.BETA,
//							Collections.singleton(minecraftVersion),
//							Collections.singleton(modLoader),
//							false,
//							Files.newInputStream(filePath),
//							fileName,
//							args[1]
//					));

					// CurseForge
					final Map<String, DependencyType> dependenciesCurseForge = new HashMap<>();
					dependenciesCurseForge.put("minecraft-transit-railway", DependencyType.REQUIRED);//mtr依赖
					do {
					} while (!new ModId("1421375", ModProvider.CURSE_FORGE).uploadFile(
							"",
							modVersionUpperCase,
							changelog,
							dependenciesCurseForge,
							ReleaseStatus.BETA,
							Collections.singleton(minecraftVersion),
							Collections.singleton(modLoader),
							false,
							Files.newInputStream(filePath),
							fileName,
							args[1]
					));
				}
			}
		}
	}
}
