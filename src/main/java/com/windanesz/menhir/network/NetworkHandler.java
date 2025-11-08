package com.windanesz.menhir.network;

import com.windanesz.menhir.Menhir;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {
	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Menhir.MODID);

	public static void registerMessages() {
		INSTANCE.registerMessage(PacketActivateBirthsignPower.Handler.class, PacketActivateBirthsignPower.class, 0, Side.SERVER);
		INSTANCE.registerMessage(PacketStopChanneling.Handler.class, PacketStopChanneling.class, 1, Side.SERVER);
		INSTANCE.registerMessage(PacketPlaceBlock.Handler.class, PacketPlaceBlock.class, 2, Side.SERVER);
		INSTANCE.registerMessage(PacketSyncBirthsignData.Handler.class, PacketSyncBirthsignData.class, 3, Side.CLIENT);
		INSTANCE.registerMessage(PacketSetBirthsign.Handler.class, PacketSetBirthsign.class, 4, Side.SERVER);
		INSTANCE.registerMessage(PacketOpenBirthsignSelectionGUI.Handler.class, PacketOpenBirthsignSelectionGUI.class, 5, Side.CLIENT);
	}

	public static void sendToServer(Object message) {
		INSTANCE.sendToServer((net.minecraftforge.fml.common.network.simpleimpl.IMessage) message);
	}

	public static void sendToAllClients(Object message) {
		INSTANCE.sendToAll((net.minecraftforge.fml.common.network.simpleimpl.IMessage) message);
	}
} 