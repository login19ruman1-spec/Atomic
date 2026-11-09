package ru.artem.beforethefall.equipment;
import org.bukkit.ChatColor;import org.bukkit.Material;import org.bukkit.NamespacedKey;import org.bukkit.inventory.ItemStack;import org.bukkit.inventory.ShapedRecipe;import org.bukkit.inventory.meta.ItemMeta;import ru.artem.beforethefall.BeforeTheFallPlugin;
public final class SurvivalCraftingManager{
 private final BeforeTheFallPlugin plugin;public SurvivalCraftingManager(BeforeTheFallPlugin p){plugin=p;}
 public void register(){
  ShapedRecipe mask=new ShapedRecipe(new NamespacedKey(plugin,"craft_gas_mask"),plugin.getGasMaskManager().createMask());mask.shape("LGL","IRI"," L ");mask.setIngredient('L',Material.LEATHER);mask.setIngredient('G',Material.GLASS_PANE);mask.setIngredient('I',Material.IRON_INGOT);mask.setIngredient('R',Material.IRON_BARS);plugin.getServer().addRecipe(mask);
  ShapedRecipe filter=new ShapedRecipe(new NamespacedKey(plugin,"craft_filter"),plugin.getGasMaskManager().createFilter());filter.shape("PCP","IRI","PCP");filter.setIngredient('P',Material.PAPER);filter.setIngredient('C',Material.CHARCOAL);filter.setIngredient('I',Material.IRON_INGOT);filter.setIngredient('R',Material.REDSTONE);plugin.getServer().addRecipe(filter);
  ItemStack charge=new ItemStack(Material.TNT);ItemMeta meta=charge.getItemMeta();meta.setDisplayName(ChatColor.DARK_RED+"Ядерный заряд");meta.setCustomModelData(3);meta.setLore(java.util.List.of(ChatColor.GRAY+"Размести и активируй ПКМ"));charge.setItemMeta(meta);
  ShapedRecipe nuke=new ShapedRecipe(new NamespacedKey(plugin,"craft_nuclear_charge"),charge);nuke.shape("URU","TCT","URU");nuke.setIngredient('U',Material.URANIUM_INGOT);nuke.setIngredient('R',Material.REDSTONE_BLOCK);nuke.setIngredient('T',Material.TNT);nuke.setIngredient('C',Material.CLOCK);plugin.getServer().addRecipe(nuke);
  ShapedRecipe core=new ShapedRecipe(new NamespacedKey(plugin,"craft_bunker_core"),new ItemStack(Material.LODESTONE));core.shape("OIO","IRI","OIO");core.setIngredient('O',Material.OBSIDIAN);core.setIngredient('I',Material.IRON_BLOCK);core.setIngredient('R',Material.REDSTONE_BLOCK);plugin.getServer().addRecipe(core);
 }
}
