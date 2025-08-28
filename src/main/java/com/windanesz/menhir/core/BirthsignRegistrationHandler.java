package com.windanesz.menhir.core;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.block.BlockMenhirStone;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.List;

@Mod.EventBusSubscriber
public final class BirthsignRegistrationHandler {

	public static final Block MENHIR_STONE = new BlockMenhirStone().setRegistryName("menhir:menhir_stone");

	@SubscribeEvent
	public static void registerBirthsigns(RegistryEvent.Register<Birthsign> event) {

		IForgeRegistry<Birthsign> registry = event.getRegistry();
		Birthsign.registry = registry;


		List<Birthsign> birthsignToRegister = BirthsignDataLoader.loadBirthsignData();

		for (Birthsign birthsign : birthsignToRegister) {
			birthsign.setRegistryName(Menhir.MODID, birthsign.name);
			registry.register(birthsign);
		}
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		event.getRegistry().register(MENHIR_STONE);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		final ItemBlock itemBlock = new BlockMenhirStone.ItemBlockMenhirStone(MENHIR_STONE);
		itemBlock.setRegistryName(MENHIR_STONE.getRegistryName());
		event.getRegistry().register(itemBlock);
	}
} 