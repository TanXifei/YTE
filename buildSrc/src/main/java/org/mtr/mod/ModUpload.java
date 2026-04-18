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
		String changelog = "## **更新日志**\n" +
				"\n" +
				"> **\uD83E\uDDE8 新春快乐，万事如意！**\n" +
				"> 本 Mod 依赖 **Minecraft Transit Railway 4.0.0 及以上**\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **新增方块**\n" +
				"\n" +
				"#### **通力 (KONE) 系列**\n" +
				"* 通力 KDS330 系列呼梯面板（类型 1，外挂式，带屏幕，**支持触摸款式**）\n" +
				"* 通力 KDS330 系列呼梯面板（类型 1，外挂式，无屏幕，**支持触摸款式**）\n" +
				"* 通力 KDS330 系列候梯楼层显示器（类型 1，外挂式，占一格/两格）\n" +
				"* 通力 KDS360 系列呼梯面板（类型 1，外挂式，带屏幕）\n" +
				"* 通力 KDS360 系列呼梯面板（类型 1，外挂式，带屏幕，**屏幕上置款式**）\n" +
				"* 通力 KDS360 系列呼梯面板（类型 1，外挂式，无屏幕）\n" +
				"* 通力 KDS220 系列呼梯面板（类型 1，外挂式，带屏幕）\n" +
				"* 通力 KDS220 系列呼梯面板（类型 1，外挂式，无屏幕）\n" +
				"* 通力 KDS220 系列候梯楼层显示器（类型 1，外挂式，占一格/两格）\n" +
				"\n" +
				"#### **奥的斯 (Otis) 系列**\n" +
				"* 奥的斯 HF023 呼梯面板\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **新增功能**\n" +
				"* **材质升级**：为 `mitsubishi_nexway_button_1` 添加了 **PBR 材质**支持。\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **新增翻译**\n" +
				"* 补全 `en_us` 和 `zh_hk` 的翻译。\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **优化内容**\n" +
				"* **字体渲染系统优化**：\n" +
				"    * **防止“砍头”**：增大了字体渲染缓冲值，防止溢出导致的字体显示不全。\n" +
				"    * **排版微调**：将 `TextView` 的字符间距设置方法修改为浮点型，提升显示精度。\n" +
				"    * **重构逻辑**：重构了自定义字体纹理创建系统，并限制了最大生成资源数量。\n" +
				"* **渲染表现改进**：\n" +
				"    * 优化了日立点阵显示器、三菱显示器的渲染逻辑。\n" +
				"    * 改进了三菱点阵外呼面板的显示方法。\n" +
				"    * 优化自定义字体缓存，当电梯楼层切换时，显示屏不再闪烁。\n" +
				"* **系统与资源重构**：\n" +
				"    * 重构动态纹理与字体资源处理系统。\n" +
				"    * 移除 `ThyssenKrupp TE-GL1` 呼梯面板的相关遗留文件。\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **修复内容**\n" +
				"* **显示修复**：修复了日立 VIB-221 点阵款式中箭头位置偏移的问题。\n" +
				"* **逻辑修正**：修正了奥的斯 Series 3 ELD 的显示布局。\n" +
				"\n" +
				"---\n" +
				"---\n" +
				"\n" +
				"## **Update Log**\n" +
				"\n" +
				"> **\uD83E\uDDE8 Happy Chinese New Year and Best Wishes!**\n" +
				"> This Mod requires **Minecraft Transit Railway 4.0.0 or above.**\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **New Blocks**\n" +
				"\n" +
				"#### **KONE Series**\n" +
				"* KONE KDS330 Series Call Panels (Type 1, Surface Mounted, with Screen, **Touch Model supported**)\n" +
				"* KONE KDS330 Series Call Panels (Type 1, Surface Mounted, without Screen, **Touch Model supported**)\n" +
				"* KONE KDS330 Series Hall Lanterns (Type 1, Surface Mounted, 1-block/2-block wide)\n" +
				"* KONE KDS360 Series Call Panels (Type 1, Surface Mounted, with Screen)\n" +
				"* KONE KDS360 Series Call Panels (Type 1, Surface Mounted, with Screen, **Top-mounted Screen model**)\n" +
				"* KONE KDS360 Series Call Panels (Type 1, Surface Mounted, without Screen)\n" +
				"* KONE KDS220 Series Call Panels (Type 1, Surface Mounted, with Screen)\n" +
				"* KONE KDS220 Series Call Panels (Type 1, Surface Mounted, without Screen)\n" +
				"* KONE KDS220 Series Hall Lanterns (Type 1, Surface Mounted, 1-block/2-block wide)\n" +
				"\n" +
				"#### **Otis Series**\n" +
				"* Otis HF023 Call Panel\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **New Features**\n" +
				"* **Texture Upgrade**: Added **PBR texture** support for `mitsubishi_nexway_button_1`.\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **New Translations**\n" +
				"* Completed translations for `en_us` and `zh_hk`.\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **Optimizations**\n" +
				"* **Font Rendering System Optimization**:\n" +
				"    * **Overflow Prevention**: Increased font rendering buffer values to prevent text clipping caused by overflow.\n" +
				"    * **Layout Fine-tuning**: Changed the `TextView` character spacing method to float for improved display precision.\n" +
				"    * **Logic Refactor**: Refactored the custom font texture creation system and limited the maximum number of generated resources.\n" +
				"* **Rendering Performance Improvements**:\n" +
				"    * Optimized rendering logic for Hitachi dot-matrix and Mitsubishi displays.\n" +
				"    * Improved the display method for Mitsubishi dot-matrix call panels.\n" +
				"    * Optimized custom font caching; displays no longer flicker when elevator floors change.\n" +
				"* **System & Resource Refactoring**:\n" +
				"    * Refactored dynamic texture and font resource handling systems.\n" +
				"    * Removed legacy files related to the `ThyssenKrupp TE-GL1` call panel.\n" +
				"\n" +
				"---\n" +
				"\n" +
				"### **Bug Fixes**\n" +
				"* **Display Fixes**: Fixed arrow position offset in the Hitachi VIB-221 dot-matrix model.\n" +
				"* **Logic Correction**: Corrected the display layout for the Otis Series 3 ELD.";





		if (args.length == 2) {
			for (final String minecraftVersion : MINECRAFT_VERSIONS) {
				for (final ModLoader modLoader : ModLoader.values()) {
					final String modVersion = String.format("%s-%s+%s", modLoader.name, args[0], minecraftVersion);
					final String modVersionUpperCase = String.format("%s-%s+%s", modLoader.name.toUpperCase(Locale.ENGLISH), args[0], minecraftVersion);
					final String fileName = String.format("Yunzhu-Transit-Extension-%s.jar", modVersion);
					final Path filePath = Paths.get("build/release").resolve(fileName);



					// Modrinth
					/*final Map<String, DependencyType> dependenciesModrinth = new HashMap<String, DependencyType>();
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
					));*/

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
