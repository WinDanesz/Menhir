package com.windanesz.menhir.ability;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

public abstract class AbstractActiveAbility implements IBirthsignActiveAbility {
    protected ResourceLocation icon;
    protected String unlocalizedName;
    protected String parentName;
    protected String groupName;

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return icon;
    }

    @Override
    public String getUnlocalizedName() {
        return unlocalizedName != null ? unlocalizedName : "";
    }

    public void setIcon(ResourceLocation icon) {
        this.icon = icon;
    }

    public void setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
    }

    @Override
    public void setParentName(String name) {
        this.parentName = name;
    }

    @Override
    public String getParentName() {
        return parentName != null ? parentName : "";
    }

    @Override
    public void setGroupName(String name) {
        this.groupName = name;
    }

    @Override
    public String getGroupName() {
        return groupName != null ? groupName : "";
    }

    @Override
    public void configure(Map<String, Object> params) {
        String iconPath = ParameterUtils.getStringParameter(params, "icon", "");
        if (!iconPath.isEmpty()) {
            this.icon = new ResourceLocation(iconPath);
        }

        String name = ParameterUtils.getStringParameter(params, "name", "");
        if (!name.isEmpty()) {
            this.unlocalizedName = name;
        }

        String group = ParameterUtils.getStringParameter(params, "ability_name", "");
        if (!group.isEmpty()) {
            this.groupName = group;
        }
    }
}
