package ru.artem.beforethefall.equipment;
import org.bukkit.Material;import org.bukkit.NamespacedKey;import org.bukkit.inventory.ItemStack;import org.bukkit.inventory.ShapedRecipe;import ru.artem.beforethefall.BeforeTheFallPlugin;
public final class SurvivalCraftingManager{
 private final BeforeTheFallPlugin plugin;public SurvivalCraftingManager(BeforeTheFallPlugin p){plugin=p;}
 public void register(){
  ShapedRecipe mask=new ShapedRecipe(new NamespacedKey(plugin,"craft_gas_mask"),plugin.getGasMaskManager().createMask());mask.shape("LGL","IRI"," L ");mask.setIngredient('L',Material.LEATHER);mask.setIngredient('G',Material.GLASS_PANE);mask.setIngredient('I',Material.IRON_INGOT);mask.setIngredient('R',Material.IRON_BARS);plugin.getServer().addRecipe(mask);
  ShapedRecipe filter=new ShapedRecipe(new NamespacedKey(plugin,"craft_filter"),plugin.getGasMaskManager().createFilter());filter.shape("PCP","IRI","PCP");filter.setIngredient('P',Material.PAPER);filter.setIngredient('C',Material.CHARCOAL);filter.setIngredient('I',Material.IRON_INGOT);filter.setIngredient('R',Material.REDSTONE);plugin.getServer().addRecipe(filter);
  ShapedRecipe core=new ShapedRecipe(new NamespacedKey(plugin,"craft_bunker_core"),new ItemStack(Material.LODESTONE));core.shape("OIO","IRI","OIO");core.setIngredient('O',Material.OBSIDIAN);core.setIngredient('I',Material.IRON_BLOCK);core.setIngredient('R',Material.REDSTONE_BLOCK);plugin.getServer().addRecipe(core);
 }
}