package com.windanesz.menhir.client;

import com.windanesz.menhir.client.gui.GuiBirthsign;
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
	static boolean showBirthsignKeyPressed = false;

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

		// Show Birthsign GUI
		if (ClientProxy.KEY_SHOW_BIRTHSIGN != null && ClientProxy.KEY_SHOW_BIRTHSIGN.isPressed() && Minecraft.getMinecraft().inGameHasFocus) {
			if (!showBirthsignKeyPressed) {
				showBirthsignKeyPressed = true;
				Minecraft mc = Minecraft.getMinecraft();
				if (mc.player != null && mc.currentScreen == null) {
					mc.displayGuiScreen(new GuiBirthsign(mc.player));
				}
			}
		} else {
			showBirthsignKeyPressed = false;
		}
	}
}
