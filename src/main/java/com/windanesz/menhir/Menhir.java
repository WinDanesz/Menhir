package com.windanesz.menhir;

import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.command.CommandAddBirthsignCharges;
import com.windanesz.menhir.command.CommandGetBirthsign;
import com.windanesz.menhir.command.CommandPlaceMenhirStone;
import com.windanesz.menhir.command.CommandSetBirthsign;
import com.windanesz.menhir.integration.antiqueatlas.MenhirAntiqueAtlasIntegration;
import com.windanesz.menhir.tileentity.TileEntityMenhirStone;
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

@Mod(modid = Menhir.MODID, name = Menhir.NAME, version = "@VERSION@", acceptedMinecraftVersions = "1.12.2", dependencies = Menhir.DEPENDENCIES)
public class Menhir {

	public static final String MODID = "menhir";
	public static final String NAME = "Menhir";
	public static final String DEPENDENCIES = "after:ebwizardry@[@WIZARDRY_VERSION@,4.4);" + "after:wizardryutils@[1.2.2,);";

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

		proxy.registerRenderers();
		//	proxy.registerExtraHandbookContent();

	}

	@EventHandler
	public void init(FMLInitializationEvent event) {

		MinecraftForge.EVENT_BUS.register(instance); // Since there's already an instance we might as well use it
		MinecraftForge.EVENT_BUS.register(new com.windanesz.menhir.eventhandler.ChannelingManager());
		// Network must be registered on both sides
		com.windanesz.menhir.network.NetworkHandler.registerMessages();
//		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandlerAS());
		proxy.registerParticles();
		proxy.init();

		// Initialize Antique Atlas integration
		MenhirAntiqueAtlasIntegration.init();

		// Register world generation for menhir stones
		GameRegistry.registerWorldGenerator(new WorldGenMenhirStone(), 100); // Priority 100 (medium priority)
		logger.info("Registered menhir stone world generation");
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
