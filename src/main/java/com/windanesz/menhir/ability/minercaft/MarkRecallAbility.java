package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.eventhandler.ChannelingManager;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.Map;

public class MarkRecallAbility implements IBirthsignActiveAbility {

    private final int channelingTicks;
    private final double maxDistance;

    public MarkRecallAbility(int channelingTicks, double maxDistance) {
        this.channelingTicks = channelingTicks;
        this.maxDistance = maxDistance;
    }

    public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
        // Default to 60 ticks if not specified
        int chargeup = ParameterUtils.getIntParameter(params, "chargeup", 60);
        // Default max distance -1 (infinite)
        double maxDist = ParameterUtils.getDoubleParameter(params, "max_distance", -1.0);
        return new MarkRecallAbility(chargeup, maxDist);
    }

    @Override
    public boolean activate(EntityPlayer player, @Nullable Entity target) {
        if (player.world.isRemote) return false;

        // Check for sneaking -> Mark Location
        if (player.isSneaking()) {
            return markLocation(player);
        } else {
            // Not sneaking -> Start Channeling for Recall
            if (ChannelingManager.isPlayerChanneling(player)) {
                return false;
            }
            // Start channeling
            ChannelingManager.startChanneling(player, this, channelingTicks);
            // Return false so charge isn't consumed immediately
            return false;
        }
    }

    @Override
    public void onChannelingComplete(EntityPlayer player) {
        if (!player.world.isRemote) {
            boolean success = recallLocation(player);
            // Report success/failure to manager for charge consumption
            ChannelingManager.setLastExecutionResult(player, success);
        }
    }

    private boolean markLocation(EntityPlayer player) {
        IBirthsignData data = BirthsignDataProvider.get(player);
        if (data != null) {
            data.setInt("lodestone_x", (int) Math.floor(player.posX));
            data.setInt("lodestone_y", (int) Math.floor(player.posY));
            data.setInt("lodestone_z", (int) Math.floor(player.posZ));
            data.setInt("lodestone_dim", player.dimension);
            
            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Location marked by the Lodestone."));
            // Return false to NOT consume a charge
            return false;
        }
        return false;
    }

    private boolean recallLocation(EntityPlayer player) {
        IBirthsignData data = BirthsignDataProvider.get(player);
        if (data != null) {
            int dim = data.getInt("lodestone_dim");
            int x = data.getInt("lodestone_x");
            int y = data.getInt("lodestone_y");
            int z = data.getInt("lodestone_z");

            if (y <= 0 && x == 0 && z == 0) {
                 player.sendMessage(new TextComponentString(TextFormatting.RED + "No location marked!"));
                 return false;
            }

            if (player.dimension != dim) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "The Lodestone's pull cannot cross dimensions."));
                return false;
            }

            // Check distance
            if (maxDistance > 0) {
                double distSq = player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5);
                if (distSq > maxDistance * maxDistance) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "The Lodestone signal is too weak (Max range: " + (int)maxDistance + " blocks)."));
                    return false;
                }
            }

            if (player instanceof EntityPlayerMP) {
                EntityPlayerMP playerMP = (EntityPlayerMP) player;
                playerMP.connection.setPlayerLocation(x + 0.5, y + 0.1, z + 0.5, player.rotationYaw, player.rotationPitch);
                player.motionX = 0;
                player.motionY = 0;
                player.motionZ = 0;
                player.velocityChanged = true;
                player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Returned to the Lodestone."));
                
                player.world.playEvent(2003, player.getPosition(), 0);
                return true;
            }
        }
        return false;
    }
}