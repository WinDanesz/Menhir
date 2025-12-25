package com.windanesz.menhir;

import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.command.CommandAddBirthsignCharges;
import com.windanesz.menhir.command.CommandGetBirthsign;
import com.windanesz.menhir.command.CommandPlaceMenhirStone;
import com.windanesz.menhir.command.CommandSetBirthsign;
import com.windanesz.menhir.core.AltarRegistry;
import com.windanesz.menhir.integration.antiqueatlas.MenhirAntiqueAtlasIntegration;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import com.windanesz.menhir.tileentity.TileEntityMenhirStone;
import com.windanesz.menhir.worldgen.AltarWorldGenerator;
import com.windanesz.menhir.worldgen.WorldGenMenhirStone;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

import java.util.Random;

@Mod(modid = Menhir.MODID, name = Menhir.NAME, version = "1.1.0", acceptedMinecraftVersions = "1.12.2", dependencies = Menhir.DEPENDENCIES)
public class Menhir {

	public static final String MODID = "menhir";
	public static final String NAME = "Menhir";
	public static final String DEPENDENCIES = "after:ebwizardry@[4.3,4.4);" + "after:wizardryutils@[1.2.2,);";

	public static final Random rand = new Random();

	/**
	 * Static instance of the {@link Settings} object for Menhir.
	 */
	public static Settings settings = new Settings();
	public static Logger logger;
	// The instance of wizardry that Forge uses.
	@Mod.Instance(Menhir.MODID)
	public static Menhir instance;
	// Location of the proxy code, used by Forge.
	@SidedProxy(clientSide = "com.windanesz.menhir.client.ClientProxy", serverSide = "com.windanesz.menhir.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		settings = new Settings();

		Birthsign.createRegistry();

		BirthsignDataProvider.register();

		// Register tile entities
		net.minecraft.tileentity.TileEntity.register("menhir_stone", TileEntityMenhirStone.class);
		net.minecraft.tileentity.TileEntity.register("menhir_altar", TileEntityAltar.class);

		// Register altar effect handlers
		com.windanesz.menhir.core.AltarEffectHandlerRegistry.registerHandler(new com.windanesz.menhir.altar.handler.TeleportTwinHandler());
		com.windanesz.menhir.core.AltarEffectHandlerRegistry.registerHandler(new com.windanesz.menhir.altar.handler.PrayerHandler());
		com.windanesz.menhir.core.AltarEffectHandlerRegistry.registerHandler(new com.windanesz.menhir.altar.handler.TeleportRecallHandler());
		com.windanesz.menhir.core.AltarEffectHandlerRegistry.registerHandler(new com.windanesz.menhir.altar.handler.TemporaryItemHandler());
		logger.info("Registered altar effect handlers");

		// Load altar definitions from config
		AltarRegistry.loadAltarDefinitions(event.getModConfigurationDirectory());
		logger.info("Loaded altar definitions");

		proxy.registerRenderers();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {

		MinecraftForge.EVENT_BUS.register(instance); // Since there's already an instance we might as well use it
		MinecraftForge.EVENT_BUS.register(new com.windanesz.menhir.eventhandler.ChannelingManager());
		MinecraftForge.EVENT_BUS.register(new com.windanesz.menhir.event.GuardianTracker());
		// Network must be registered on both sides
		com.windanesz.menhir.network.NetworkHandler.registerMessages();
		proxy.registerParticles();
		proxy.init();

		// Initialize Antique Atlas integration
		MenhirAntiqueAtlasIntegration.init();

		// Register world generation for menhir stones
		GameRegistry.registerWorldGenerator(new WorldGenMenhirStone(), 100); // Priority 100 (medium priority)
		logger.info("Registered menhir stone world generation");

		// Register world generation for altars
		GameRegistry.registerWorldGenerator(new AltarWorldGenerator(), 101); // Priority 101 (slightly after menhir stones)
		logger.info("Registered altar world generation");
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {

		proxy.initialiseLayers();
		proxy.checkTranslationKeys();
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandGetBirthsign());
		event.registerServerCommand(new CommandAddBirthsignCharges());
		event.registerServerCommand(new CommandSetBirthsign());
		event.registerServerCommand(new CommandPlaceMenhirStone());
	}
}
