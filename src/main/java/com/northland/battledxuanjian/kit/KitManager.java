package com.northland.battledxuanjian.kit;

import com.northland.battledxuanjian.BattledXuanjianPlugin;
import com.northland.battledxuanjian.game.Side;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.block.ShulkerBox;

/**
 * 装备包管理（需求 §9）。
 *
 * <p>管理员用 {@code /bx kithand <阵营>} 手持潜影盒设置装备包：盒内物品作为物品栏内容，
 * 管理员当前护甲与副手作为装备包的护甲/副手。</p>
 */
public final class KitManager {

    private static final int INVENTORY_SIZE = 36;

    private final BattledXuanjianPlugin plugin;
    private final Map<Side, Kit> kits = new EnumMap<>(Side.class);
    private final NamespacedKey kitKey;

    public KitManager(BattledXuanjianPlugin plugin) {
        this.plugin = plugin;
        this.kitKey = new NamespacedKey(plugin, "kit_side");
    }

    private File file() {
        return new File(plugin.getDataFolder(), plugin.config().itemsFile);
    }

    /** 从 items.yml 读取装备包。 */
    public void load() {
        kits.clear();
        File file = file();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Side side : Side.values()) {
            String path = "kits." + side.id();
            if (!yaml.isConfigurationSection(path) && yaml.get(path + ".contents") == null) {
                continue;
            }
            List<ItemStack> contents = readItemList(yaml.getList(path + ".contents"));
            ItemStack[] armor = new ItemStack[4];
            List<ItemStack> armorList = readItemList(yaml.getList(path + ".armor"));
            for (int i = 0; i < Math.min(4, armorList.size()); i++) {
                armor[i] = armorList.get(i);
            }
            ItemStack offhand = yaml.getItemStack(path + ".offhand");
            kits.put(side, new Kit(contents, armor, offhand));
        }
    }

    private static List<ItemStack> readItemList(List<?> raw) {
        List<ItemStack> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (Object element : raw) {
            result.add(element instanceof ItemStack stack ? stack : null);
        }
        return result;
    }

    /** 保存到 items.yml。 */
    public void save() {
        File file = file();
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<Side, Kit> entry : kits.entrySet()) {
            String path = "kits." + entry.getKey().id();
            yaml.set(path + ".contents", entry.getValue().contents());
            yaml.set(path + ".armor", Arrays.asList(entry.getValue().armor()));
            yaml.set(path + ".offhand", entry.getValue().offhand());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录: " + parent.getAbsolutePath());
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存装备包失败: " + exception.getMessage());
        }
    }

    public Kit kitOf(Side side) {
        return kits.get(side);
    }

    public boolean hasKit(Side side) {
        Kit kit = kits.get(side);
        return kit != null && !kit.isEmpty();
    }

    public void clearAll() {
        kits.clear();
        save();
    }

    /**
     * 从玩家手中潜影盒读取装备包。
     *
     * @return 成功返回 {@code true}
     */
    public boolean captureFromMainHand(Player player, Side side) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        List<ItemStack> contents = readShulkerContents(hand);
        if (contents == null) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        Kit kit = new Kit(contents, inventory.getArmorContents(), inventory.getItemInOffHand());
        kits.put(side, kit);
        save();
        return true;
    }

    /** 读取潜影盒内容；非潜影盒返回 {@code null}。 */
    private List<ItemStack> readShulkerContents(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.getType().name().endsWith("SHULKER_BOX")) {
            return null;
        }
        // Paper 26.x：优先读取 CONTAINER 数据组件
        try {
            ItemContainerContents data = stack.getData(DataComponentTypes.CONTAINER);
            if (data != null) {
                return new ArrayList<>(data.contents());
            }
        } catch (Throwable ignored) {
            // 回退到方块状态读取
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            return new ArrayList<>(Arrays.asList(shulkerBox.getInventory().getContents()));
        }
        return null;
    }

    /** 清空玩家背包、护甲与副手。 */
    public void clearInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
        player.updateInventory();
    }

    /** 发放阵营装备包（自动清空原背包）。 */
    public void give(Player player, Side side) {
        PlayerInventory inventory = player.getInventory();
        clearInventory(player);
        Kit kit = kits.get(side);
        if (kit == null || kit.isEmpty()) {
            return;
        }
        ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        List<ItemStack> stored = kit.contents();
        for (int i = 0; i < Math.min(INVENTORY_SIZE, stored.size()); i++) {
            contents[i] = tag(stored.get(i), side);
        }
        inventory.setContents(contents);

        ItemStack[] armor = new ItemStack[4];
        ItemStack[] storedArmor = kit.armor();
        for (int i = 0; i < 4 && i < storedArmor.length; i++) {
            armor[i] = tag(storedArmor[i], side);
        }
        inventory.setArmorContents(armor);
        inventory.setItemInOffHand(tag(kit.offhand(), side));
        player.updateInventory();
    }

    /** 给物品打上阵营标记。 */
    public ItemStack tag(ItemStack stack, Side side) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        ItemStack copy = stack.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, side.id());
            copy.setItemMeta(meta);
        }
        return copy;
    }

    /** 读取物品的阵营标记。 */
    public Side kitSide(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        if (stack.getType() == Material.AIR) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
        return Side.byId(raw);
    }
}
