package com.windanesz.menhir.ability;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupedActiveAbility extends AbstractActiveAbility {
    private final List<IBirthsignActiveAbility> children = new ArrayList<>();

    public GroupedActiveAbility(String groupName) {
        this.groupName = groupName;
        this.unlocalizedName = groupName;
    }

    public void addChild(IBirthsignActiveAbility ability) {
        children.add(ability);
        
        // If this group doesn't have an icon/name yet, adopt it from the first child
        if (children.size() == 1) {
            if (this.icon == null) this.icon = ability.getIcon();
            if (this.unlocalizedName == null || this.unlocalizedName.isEmpty()) this.unlocalizedName = ability.getUnlocalizedName();
            if (this.parentName == null || this.parentName.isEmpty()) this.parentName = ability.getParentName();
        }
    }

    public List<IBirthsignActiveAbility> getChildren() {
        return children;
    }

    @Override
    public boolean activate(EntityPlayer player, @Nullable Entity target) {
        boolean anySuccess = false;
        for (IBirthsignActiveAbility child : children) {
            if (child.activate(player, target)) {
                anySuccess = true;
            }
        }
        // Return true if at least one ability activated successfully to consume a charge
        return anySuccess;
    }

    @Override
    public void onChannelingComplete(EntityPlayer player) {
        for (IBirthsignActiveAbility child : children) {
            child.onChannelingComplete(player);
        }
    }
}
