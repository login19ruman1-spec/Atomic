package ru.artem.beforethefall.equipment;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import java.util.List;
public final class ProtectiveSuitManager {
 private final NamespacedKey marker; private final BeforeTheFallPlugin plugin;
 public ProtectiveSuitManager(BeforeTheFallPlugin plugin){this.plugin=plugin;marker=new NamespacedKey(plugin,"radiation_suit_piece");}
 public ItemStack createPiece(Material type,String part){
  ItemStack item=new ItemStack(type); LeatherArmorMeta meta=(LeatherArmorMeta)item.getItemMeta();
  meta.setColor(Color.fromRGB(232,190,34)); meta.setDisplayName(ChatColor.GOLD+"Защитный костюм — "+part);
  meta.setLore(List.of(ChatColor.GRAY+"Жёлтая защитная экипировка",ChatColor.GREEN+"Часть радиационного комплекта"));
  meta.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1); item.setItemMeta(meta); return item;
 }
 private boolean isPiece(ItemStack item,Material expected){
  if(item==null||item.getType()!=expected||!item.hasItemMeta())return false;
  Byte value=item.getItemMeta().getPersistentDataContainer().get(marker,PersistentDataType.BYTE);
  return value!=null&&value==(byte)1;
 }
 public boolean hasFullSuit(Player p){
  var inv=p.getInventory();
  boolean head = isPiece(inv.getHelmet(),Material.LEATHER_HELMET)
   || (plugin.getGasMaskManager()!=null && plugin.getGasMaskManager().isProtected(p));
  return head&&isPiece(inv.getChestplate(),Material.LEATHER_CHESTPLATE)
   &&isPiece(inv.getLeggings(),Material.LEATHER_LEGGINGS)&&isPiece(inv.getBoots(),Material.LEATHER_BOOTS);
 }
}
