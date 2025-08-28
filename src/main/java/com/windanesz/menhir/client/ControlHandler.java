package com.windanesz.menhir.client;

import com.windanesz.menhir.network.NetworkHandler;
import com.windanesz.menhir.network.PacketActivateBirthsignPower;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ControlHandler {

	static boolean birthsignPowerKeyPressed = false;

	@SubscribeEvent
	public static void onTickEvent(TickEvent.ClientTickEvent event) {
		// Birthsign Power Activation
		if (ClientProxy.KEY_ACTIVATE_POWER != null && ClientProxy.KEY_ACTIVATE_POWER.isKeyDown() && Minecraft.getMinecraft().inGameHasFocus) {
			if (!birthsignPowerKeyPressed) {
				birthsignPowerKeyPressed = true;
				// Send packet to server to activate birthsign power
				NetworkHandler.sendToServer(new PacketActivateBirthsignPower());
			}
		} else {
			if (birthsignPowerKeyPressed) {
				// Edge: key released — stop channeling on server
				NetworkHandler.sendToServer(new com.windanesz.menhir.network.PacketStopChanneling());
			}
			birthsignPowerKeyPressed = false;
		}
	}
}
