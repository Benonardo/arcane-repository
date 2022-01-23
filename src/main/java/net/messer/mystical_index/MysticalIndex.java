package net.messer.mystical_index;

import net.fabricmc.api.ModInitializer;
import net.messer.config.ModConfigs;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.ModBlockEntities;
import net.messer.mystical_index.item.ModItems;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MysticalIndex implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("mystical_index");
	public static final String MOD_ID = "mystical_index";

	@Override
	public void onInitialize() {
		ModConfigs.registerConfigs();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		ModBlockEntities.registerBlockEntities();
	}
}
