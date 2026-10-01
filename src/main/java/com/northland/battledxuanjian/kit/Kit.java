package com.northland.battledxuanjian.kit;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

/** 单个阵营的装备包数据。 */
public final class Kit {

    private List<ItemStack> contents;
    private ItemStack[] armor;
    private ItemStack offhand;

    public Kit() {
        this.contents = new ArrayList<>();
        this.armor = new ItemStack[4];
        this.offhand = null;
    }

    public Kit(List<ItemStack> contents, ItemStack[] armor, ItemStack offhand) {
        this.contents = contents == null ? new ArrayList<>() : new ArrayList<>(contents);
        this.armor = armor == null ? new ItemStack[4] : armor.clone();
        this.offhand = offhand;
    }

    public List<ItemStack> contents() {
        return contents;
    }

    public ItemStack[] armor() {
        return armor;
    }

    public ItemStack offhand() {
        return offhand;
    }

    public int contentCount() {
        int count = 0;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public int armorCount() {
        int count = 0;
        for (ItemStack item : armor) {
            if (item != null && !item.getType().isAir()) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public int offhandCount() {
        return offhand != null && !offhand.getType().isAir() ? offhand.getAmount() : 0;
    }

    public boolean isEmpty() {
        return contentCount() == 0 && armorCount() == 0 && offhandCount() == 0;
    }
}
