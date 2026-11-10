package ru.artem.beforethefall.equipment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.List;

public final class SurvivalCraftingManager {
    private final BeforeTheFallPlugin plugin;
    public SurvivalCraftingManager(BeforeTheFallPlugin plugin) { this.plugin = plugin; }

    public void register() {
        var mask = new ShapedRecipe(new NamespacedKey(plugin, "craft_gas_mask"), plugin.getGasMaskManager().createMask());
        mask.shape("LGL", "IRI", " L ");
        mask.setIngredient('L', Material.LEATHER);
        mask.setIngredient('G', Material.GLASS_PANE);
        mask.setIngredient('I', Material.IRON_INGOT);
        mask.setIngredient('R', Material.IRON_BARS);
        add("craft_gas_mask", mask);

        // Correct orientation: redstone on the sides, iron in the centre.
        var filter = new ShapedRecipe(new NamespacedKey(plugin, "craft_filter"), plugin.getGasMaskManager().createFilter());
        filter.shape("PCP", "RIR", "PCP");
        filter.setIngredient('P', Material.PAPER);
        filter.setIngredient('C', Material.CHARCOAL);
        filter.setIngredient('I', Material.IRON_INGOT);
        filter.setIngredient('R', Material.REDSTONE);
        add("craft_filter", filter);

        ItemStack charge = new ItemStack(Material.TNT);
        ItemMeta chargeMeta = charge.getItemMeta();
        chargeMeta.setDisplayName(ChatColor.DARK_RED + "Ядерный заряд");
        chargeMeta.setCustomModelData(3);
        chargeMeta.setLore(List.of(ChatColor.GRAY + "Поставь как блок TNT", ChatColor.GRAY + "Подожги огнивом или активируй редстоун"));
        chargeMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "atomic_charge"), PersistentDataType.BYTE, (byte) 1);
        charge.setItemMeta(chargeMeta);
        var nuke = new ShapedRecipe(new NamespacedKey(plugin, "craft_nuclear_charge"), charge);
        nuke.shape("URU", "TCT", "URU");
        nuke.setIngredient('U', Material.GOLD_INGOT);
        nuke.setIngredient('R', Material.REDSTONE_BLOCK);
        nuke.setIngredient('T', Material.TNT);
        nuke.setIngredient('C', Material.CLOCK);
        add("craft_nuclear_charge", nuke);

        ItemStack core = new ItemStack(Material.LODESTONE);
        ItemMeta coreMeta = core.getItemMeta();
        coreMeta.setDisplayName(ChatColor.DARK_AQUA + "Магнетит — ядро бункера");
        coreMeta.setCustomModelData(4);
        coreMeta.setLore(List.of(ChatColor.GRAY + "Крафтовое ядро системы защиты", ChatColor.YELLOW + "Обычный магнетит не активирует бункер"));
        coreMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "atomic_bunker_core"), PersistentDataType.BYTE, (byte) 1);
        core.setItemMeta(coreMeta);
        var bunker = new ShapedRecipe(new NamespacedKey(plugin, "craft_bunker_core"), core);
        bunker.shape("OIO", "IRI", "OIO");
        bunker.setIngredient('O', Material.OBSIDIAN);
        bunker.setIngredient('I', Material.IRON_BLOCK);
        bunker.setIngredient('R', Material.REDSTONE_BLOCK);
        add("craft_bunker_core", bunker);

        var suit = plugin.getProtectiveSuitManager();
        var helmet = new ShapedRecipe(new NamespacedKey(plugin, "craft_radiation_helmet"), suit.createPiece(Material.LEATHER_HELMET, "шлем"));
        helmet.shape("LYL", "L L", "   "); helmet.setIngredient('L', Material.LEATHER); helmet.setIngredient('Y', Material.YELLOW_DYE); add("craft_radiation_helmet", helmet);
        var chest = new ShapedRecipe(new NamespacedKey(plugin, "craft_radiation_chestplate"), suit.createPiece(Material.LEATHER_CHESTPLATE, "нагрудник"));
        chest.shape("L L", "LYL", "L L"); chest.setIngredient('L', Material.LEATHER); chest.setIngredient('Y', Material.YELLOW_DYE); add("craft_radiation_chestplate", chest);
        var legs = new ShapedRecipe(new NamespacedKey(plugin, "craft_radiation_leggings"), suit.createPiece(Material.LEATHER_LEGGINGS, "штаны"));
        legs.shape("LYL", "L L", "L L"); legs.setIngredient('L', Material.LEATHER); legs.setIngredient('Y', Material.YELLOW_DYE); add("craft_radiation_leggings", legs);
        var boots = new ShapedRecipe(new NamespacedKey(plugin, "craft_radiation_boots"), suit.createPiece(Material.LEATHER_BOOTS, "ботинки"));
        boots.shape("   ", "L L", "L L"); boots.setIngredient('L', Material.LEATHER); add("craft_radiation_boots", boots);
    }

    private void add(String keyName, ShapedRecipe recipe) {
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        plugin.getServer().removeRecipe(key);
        if (!plugin.getServer().addRecipe(recipe)) {
            plugin.getLogger().warning("Could not register crafting recipe: " + key);
        }
    }
}
