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
        plugin.getServer().addRecipe(mask);

        // Correct orientation: redstone on the sides, iron in the centre.
        var filter = new ShapedRecipe(new NamespacedKey(plugin, "craft_filter"), plugin.getGasMaskManager().createFilter());
        filter.shape("PCP", "RIR", "PCP");
        filter.setIngredient('P', Material.PAPER);
        filter.setIngredient('C', Material.CHARCOAL);
        filter.setIngredient('I', Material.IRON_INGOT);
        filter.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(filter);

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
        plugin.getServer().addRecipe(nuke);

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
        plugin.getServer().addRecipe(bunker);
    }
}
