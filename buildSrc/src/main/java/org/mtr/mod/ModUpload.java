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
		String changelog = "更新日志\n" +
				"本 Mod 依赖 Minecraft Transit Railway 4.0.0 及以上（暂不支持 4.1.0）\n" +
				"\n" +
				"优化内容\n" +
				"- 优化日立 B89 呼梯面板屏幕显示：移除楼层数字的滚动动画。\n" +
				"- 优化东芝楼层显示器（类型 2）：缩小显示字体尺寸，视觉效果更协调。\n" +
				"\n" +
				"修复内容\n" +
				"- 修复上海三菱 Lehy3 呼梯面板（类型 1）楼层显示文字未能居中的问题。\n" +
				"- 修复奥的斯 Series 1 到站灯（类型 2，横向，带屏幕）在占一格/两格状态下无法正常使用的问题（需将已放置的方块拆除并重新放置方可生效）。\n" +
				"- 修复奥的斯 Gen 3 呼梯面板上箭头尺寸与翻转方向不正确的问题。\n" +
				"- 修复批量楼层连接器与批量楼层设置器在操作后提示的已完成数量计数不准确的问题。\n" +
				"- 修复所有占两格宽度的部件，其右半部分无法被连接工具识别的问题。\n" +
				"- 修复日立 GHI-675 楼层显示器屏幕宽度与边距异常。\n" +
				"- 修复三菱 NexWay 系列楼层显示器（类型 1、类型 2）在不该出现粒子的情况下错误生成粒子的现象。\n" +
				"- 修复三菱信兴广场呼梯面板的按钮点击判定范围不正确。\n" +
				"- 修复迅达 M 系列圆形触摸呼梯面板的按钮点击判定范围不正确。\n" +
				"- 修复一些与 Mixin 有关的模组冲突问题。\n" +
				"\n" +
				"Changelog\n" +
				"This Mod requires Minecraft Transit Railway 4.0.0 or above (4.1.0 is not supported yet).\n" +
				"\n" +
				"Optimizations\n" +
				"- Optimized Hitachi B89 call panel screen: disabled the floor number scrolling animation.\n" +
				"- Optimized Toshiba Screen (Type 2): reduced the display font size for better visual proportion.\n" +
				"\n" +
				"Bug Fixes\n" +
				"- Fixed the floor number text not being centered on the Shanghai Mitsubishi Lehy3 Call Panel (Type 1).\n" +
				"- Fixed the OTIS Series 1 Hall Lantern (Type 2, Horizontal, With Screen) not functioning correctly in both 1-block and 2-block variants (existing blocks must be broken and placed again).\n" +
				"- Fixed incorrect arrow size and flip direction on the OTIS Gen 3 Call Panel.\n" +
				"- Fixed inaccurate completion count in status messages when using the Group Lift Buttons Connector and Group Floor Auto Setter.\n" +
				"- Fixed the right half of any 2-block-wide component being unrecognized by linking tools.\n" +
				"- Fixed the display width and margin issue on the Hitachi GHI-675 Screen.\n" +
				"- Fixed incorrect particle effects on Mitsubishi NexWay Screens (Type 1 and Type 2).\n" +
				"- Fixed the button hitbox on the Mitsubishi Shun Hing Square Call Panel.\n" +
				"- Fixed the button hitbox on the Schindler M Series Round Touch Button Call Panel.\n" +
				"- Fixed some mod compatibility issues related to Mixin.";





		if (args.length == 2) {
			for (final String minecraftVersion : MINECRAFT_VERSIONS) {
				for (final ModLoader modLoader : ModLoader.values()) {
					final String modVersion = String.format("%s-%s+%s", modLoader.name, args[0], minecraftVersion);
					final String modVersionUpperCase = String.format("%s-%s+%s", modLoader.name.toUpperCase(Locale.ENGLISH), args[0], minecraftVersion);
					final String fileName = String.format("Yunzhu-Transit-Extension-%s.jar", modVersion);
					final Path filePath = Paths.get("build/release").resolve(fileName);



					 //Modrinth
					final Map<String, DependencyType> dependenciesModrinth = new HashMap<String, DependencyType>();
					dependenciesModrinth.put("XKPAmI6u", DependencyType.REQUIRED);
					do {
					} while (!new ModId("nqMdKn6A", ModProvider.MODRINTH).uploadFile(
							modVersionUpperCase,
							modVersionUpperCase,
							changelog,
							dependenciesModrinth,
							ReleaseStatus.BETA,
							Collections.singleton(minecraftVersion),
							Collections.singleton(modLoader),
							false,
							Files.newInputStream(filePath),
							fileName,
							args[1]
					));

					// CurseForge
//					final Map<String, DependencyType> dependenciesCurseForge = new HashMap<>();
//					dependenciesCurseForge.put("minecraft-transit-railway", DependencyType.REQUIRED);//mtr依赖
//					do {
//					} while (!new ModId("1421375", ModProvider.CURSE_FORGE).uploadFile(
//							"",
//							modVersionUpperCase,
//							changelog,
//							dependenciesCurseForge,
//							ReleaseStatus.BETA,
//							Collections.singleton(minecraftVersion),
//							Collections.singleton(modLoader),
//							false,
//							Files.newInputStream(filePath),
//							fileName,
//							args[1]
//					));
				}
			}
		}
	}
}
