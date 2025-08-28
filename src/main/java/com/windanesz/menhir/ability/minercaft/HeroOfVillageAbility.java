package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HeroOfVillageAbility implements IBirthsignActiveAbility {

	// List of common items that villagers might give
	private static final Item[] VILLAGER_ITEMS = {
			Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT,
			Items.BREAD, Items.COOKIE, Items.CAKE, Items.APPLE,
			Items.CARROT, Items.POTATO, Items.WHEAT, Items.SUGAR,
			Items.PAPER, Items.BOOK, Items.ENCHANTED_BOOK, Items.EXPERIENCE_BOTTLE
	};
	private final int duration;

	public HeroOfVillageAbility(int duration) {
		this.duration = duration;
	}

	public static HeroOfVillageAbility create(Map<String, Object> params, String birthsignName) {
		int duration = 30; // Default 30 seconds

		if (params.containsKey("duration")) {
			Object durationObj = params.get("duration");
			if (durationObj instanceof Number) {
				duration = ((Number) durationObj).intValue();
			}
		}

		return new HeroOfVillageAbility(duration);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		World world = player.world;

		// Find nearby villagers
		AxisAlignedBB searchBox = new AxisAlignedBB(
				player.posX - 16.0, player.posY - 8.0, player.posZ - 16.0,
				player.posX + 16.0, player.posY + 8.0, player.posZ + 16.0
		);

		List<EntityVillager> nearbyVillagers = world.getEntitiesWithinAABB(EntityVillager.class, searchBox);

		if (nearbyVillagers.isEmpty()) {
			player.sendMessage(new TextComponentString(TextFormatting.YELLOW +
					"No villagers nearby to charm!"));
			return false;
		}

		// Make villagers throw items at the player
		Random rand = world.rand;
		int itemsThrown = 0;

		for (EntityVillager villager : nearbyVillagers) {
			if (villager.isEntityAlive() && !villager.isChild()) {
				// Random chance for each villager to throw an item
				if (rand.nextFloat() < 0.7f) { // 70% chance
					ItemStack itemStack = null;

					// Try to get an offered trade item from the villager
					MerchantRecipeList offers = villager.getRecipes(null);
					if (offers != null && !offers.isEmpty()) {
						// Pick a random trade offer
						MerchantRecipe offer = offers.get(rand.nextInt(offers.size()));
						ItemStack offerStack = offer.getItemToSell();
						if (!offerStack.isEmpty()) {
							// Copy the stack and randomize the count
							int maxCount = Math.min(offerStack.getMaxStackSize(), 3);
							int count = 1 + rand.nextInt(maxCount);
							itemStack = offerStack.copy();
							itemStack.setCount(count);
						}
					}

					// If no trade item, fall back to random villager item
					if (itemStack == null) {
						Item randomItem = VILLAGER_ITEMS[rand.nextInt(VILLAGER_ITEMS.length)];
						itemStack = new ItemStack(randomItem, 1 + rand.nextInt(3)); // 1-3 items
					}

					// Drop the item near the villager
					villager.entityDropItem(itemStack, 0.5f);
					itemsThrown++;
				}
			}
		}

		if (itemsThrown > 0) {
			player.sendMessage(new TextComponentString(TextFormatting.GREEN +
					"Villagers are showering you with gifts!"));
		}
		return true;

	}

	public int getDuration() {
		return duration;
	}
}
