package ru.artem.beforethefall.equipment;
import org.bukkit.Bukkit;import org.bukkit.ChatColor;import org.bukkit.Location;import org.bukkit.Material;import org.bukkit.NamespacedKey;import org.bukkit.Particle;import org.bukkit.Sound;import org.bukkit.SoundCategory;import org.bukkit.entity.Player;import org.bukkit.event.EventHandler;import org.bukkit.event.Listener;import org.bukkit.event.block.Action;import org.bukkit.event.player.PlayerInteractEvent;import org.bukkit.inventory.EquipmentSlot;import org.bukkit.inventory.ItemStack;import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;import org.bukkit.persistence.PersistentDataContainer;import org.bukkit.persistence.PersistentDataType;import org.bukkit.scheduler.BukkitTask;import ru.artem.beforethefall.BeforeTheFallPlugin;import java.util.List;import java.util.HashSet;import java.util.Set;
public final class GasMaskManager implements Listener {
 private static final int MAX_DURABILITY=100; private final BeforeTheFallPlugin plugin; private final NamespacedKey marker,filter,filterExpiry,filterItem,maskDurability; private BukkitTask task; private int ticks; private final Set<java.util.UUID> wearing=new HashSet<>();
 public GasMaskManager(BeforeTheFallPlugin p){plugin=p;marker=new NamespacedKey(p,"gas_mask");filter=new NamespacedKey(p,"filter_ticks");filterExpiry=new NamespacedKey(p,"filter_expiry");filterItem=new NamespacedKey(p,"replacement_filter");maskDurability=new NamespacedKey(p,"mask_durability");}
 public ItemStack createMask(){ItemStack item=new ItemStack(Material.LEATHER_HELMET);ItemMeta meta=item.getItemMeta();meta.setDisplayName(ChatColor.DARK_GREEN+"Противогаз");meta.setItemModel(new NamespacedKey(plugin, "gas_mask"));
  EquippableComponent equip = meta.getEquippable();
  equip.setSlot(EquipmentSlot.HEAD);
  equip.setDamageOnHurt(false);
  equip.setDispensable(false);
  equip.setSwappable(false);
  equip.setCameraOverlay(new NamespacedKey(plugin, "gui/gasmask_overlay"));
  meta.setEquippable(equip);meta.setLore(List.of(ChatColor.GRAY+"Защита от радиоактивной пыли и пепла",ChatColor.GRAY+"Прочность: 100%",ChatColor.GRAY+"Сменный фильтр: "+plugin.getConfig().getInt("gas-mask.filter-duration-seconds",900)+" секунд",ChatColor.YELLOW+"Надень в слот шлема"));meta.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);meta.getPersistentDataContainer().set(filterExpiry,PersistentDataType.LONG,0L);meta.getPersistentDataContainer().set(maskDurability,PersistentDataType.INTEGER,MAX_DURABILITY);item.setItemMeta(meta);return item;}
 public ItemStack createFilter(){ItemStack item=new ItemStack(Material.PAPER);ItemMeta meta=item.getItemMeta();meta.setDisplayName(ChatColor.GREEN+"Сменный фильтр");meta.setItemModel(new NamespacedKey(plugin, "gas_filter"));meta.setLore(List.of(ChatColor.GRAY+"Картридж для противогаза",ChatColor.GRAY+"Ресурс: "+plugin.getConfig().getInt("gas-mask.filter-duration-seconds",900)+" секунд"));meta.getPersistentDataContainer().set(filterItem,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;}
 public boolean isFilter(ItemStack i){if(i==null||!i.hasItemMeta())return false;Byte v=i.getItemMeta().getPersistentDataContainer().get(filterItem,PersistentDataType.BYTE);return v!=null&&v==(byte)1;}
 public boolean isMask(ItemStack i){if(i==null||!i.hasItemMeta())return false;Byte v=i.getItemMeta().getPersistentDataContainer().get(marker,PersistentDataType.BYTE);return v!=null&&v==(byte)1;}
 private int filterTicks(ItemStack i){
  if(!isMask(i))return 0;
  PersistentDataContainer pdc=i.getItemMeta().getPersistentDataContainer();
  Long expiry=pdc.get(filterExpiry,PersistentDataType.LONG);
  if(expiry!=null)return (int)Math.max(0L,(expiry-System.currentTimeMillis())/500L);
  // One-time migration from the old tick-count representation.
  Integer legacy=pdc.get(filter,PersistentDataType.INTEGER);
  if(legacy==null||legacy<=0)return 0;
  long migratedExpiry=System.currentTimeMillis()+legacy*500L;
  pdc.set(filterExpiry,PersistentDataType.LONG,migratedExpiry);
  pdc.remove(filter);
  ItemMeta migrated=i.getItemMeta();
  migrated.getPersistentDataContainer().set(filterExpiry,PersistentDataType.LONG,migratedExpiry);
  migrated.getPersistentDataContainer().remove(filter);
  i.setItemMeta(migrated);
  return legacy;
 }
 private int durability(ItemStack i){if(!isMask(i))return 0;Integer v=i.getItemMeta().getPersistentDataContainer().get(maskDurability,PersistentDataType.INTEGER);return v==null?MAX_DURABILITY:v;}
 public int remainingSeconds(ItemStack i){return Math.max(0,filterTicks(i)/2);} public int remainingDurability(ItemStack i){return durability(i);}
 private void setFilterTicks(ItemStack i,int value){
  ItemMeta m=i.getItemMeta();
  int safe=Math.max(0,value);
  long expiry=safe==0?0L:System.currentTimeMillis()+safe*500L;
  m.getPersistentDataContainer().set(filterExpiry,PersistentDataType.LONG,expiry);
  m.getPersistentDataContainer().remove(filter);
  updateLore(m,safe,durability(i));
  i.setItemMeta(m);
 }
 private void updateLore(ItemMeta m,int f,int d){m.setLore(List.of(ChatColor.GRAY+"Защита от радиоактивной пыли и пепла",ChatColor.GRAY+"Прочность маски: "+d+"%",ChatColor.GRAY+"Фильтр: "+f/2+" сек.",d<=50?ChatColor.RED+"Стекло треснуло!":ChatColor.GREEN+"Визор цел"));}
 private void setMaskCondition(Player p,ItemStack mask,int value,boolean fogged){int d=Math.max(0,Math.min(MAX_DURABILITY,value));if(d==0){p.getInventory().setHelmet(null);p.playSound(p.getLocation(),Sound.ENTITY_ITEM_BREAK,SoundCategory.PLAYERS,1f,.75f);p.sendMessage(ChatColor.RED+"Противогаз разрушен! Его нужно изготовить заново.");return;}ItemMeta m=mask.getItemMeta();int old=durability(mask);m.getPersistentDataContainer().set(maskDurability,PersistentDataType.INTEGER,d);String model = d <= 50
                ? (fogged ? "gas_mask_cracked_fogged" : "gas_mask_cracked")
                : (fogged ? "gas_mask_fogged" : "gas_mask");
        NamespacedKey modelKey = new NamespacedKey(plugin, model);
        if (!modelKey.equals(m.getItemModel())) m.setItemModel(modelKey);
        String overlay = d <= 50
                ? "gui/gasmask_overlay_cracked"
                : (fogged ? "gui/gasmask_overlay_ash" : "gui/gasmask_overlay");
        EquippableComponent equip = m.getEquippable();
        equip.setSlot(EquipmentSlot.HEAD);
        equip.setCameraOverlay(new NamespacedKey(plugin, overlay));
        m.setEquippable(equip);if(old!=d)updateLore(m,filterTicks(mask),d);mask.setItemMeta(m);}
 private ItemStack migrateLegacyMask(ItemStack old){
  if(old==null||old.getType()==Material.LEATHER_HELMET||!isMask(old))return old;
  ItemStack updated=new ItemStack(Material.LEATHER_HELMET);
  ItemMeta source=old.getItemMeta(),target=updated.getItemMeta();
  if(source.hasDisplayName())target.setDisplayName(source.getDisplayName());
  if(source.hasLore())target.setLore(source.getLore());
  if(source.hasItemModel())target.setItemModel(source.getItemModel());
  source.getPersistentDataContainer().copyTo(target.getPersistentDataContainer(),true);
  updated.setItemMeta(target);
  return updated;
 }
 public boolean isProtected(Player p){ItemStack h=p.getInventory().getHelmet();return isMask(h)&&filterTicks(h)>0&&durability(h)>0;}
 @EventHandler public void onFilterUse(PlayerInteractEvent e){if(e.getHand()!=EquipmentSlot.HAND||(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK))return;ItemStack held=e.getItem();if(!isFilter(held))return;ItemStack helmet=e.getPlayer().getInventory().getHelmet();if(!isMask(helmet)){e.getPlayer().sendMessage(ChatColor.RED+"Сначала надень противогаз в слот шлема.");e.setCancelled(true);return;}int duration=Math.max(1,plugin.getConfig().getInt("gas-mask.filter-duration-seconds",900));setFilterTicks(helmet,duration*2);if(held.getAmount()>1)held.setAmount(held.getAmount()-1);else e.getPlayer().getInventory().setItemInMainHand(null);e.getPlayer().sendMessage(ChatColor.GREEN+"Фильтр установлен: "+duration/60+" минут защиты.");e.getPlayer().playSound(e.getPlayer().getLocation(),Sound.BLOCK_ANVIL_USE,SoundCategory.PLAYERS,0.65f,1.25f);e.setCancelled(true);}
 public void start(){task=Bukkit.getScheduler().runTaskTimer(plugin,()->{ticks++;for(Player p:Bukkit.getOnlinePlayers()){ItemStack h=p.getInventory().getHelmet();if(!isMask(h)){wearing.remove(p.getUniqueId());continue;}if(h.getType()!=Material.LEATHER_HELMET){h=migrateLegacyMask(h);p.getInventory().setHelmet(h);}if(wearing.add(p.getUniqueId()))p.playSound(p.getLocation(),Sound.ITEM_ARMOR_EQUIP_LEATHER,SoundCategory.PLAYERS,0.8f,1.0f);if(ticks%100==0)p.playSound(p.getLocation(),Sound.BLOCK_FIRE_AMBIENT,SoundCategory.PLAYERS,0.12f,0.65f);int left=filterTicks(h),d=durability(h);boolean fogged=ticks%8>=4;if(fogged&&ticks%8==4){Location breath=p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(.28));p.getWorld().spawnParticle(Particle.CLOUD,breath,3,.035,.035,.035,.002);}if(left>0){if(ticks%80==0){d--;setMaskCondition(p,h,d,fogged);if(d<=0)continue;}else setMaskCondition(p,h,d,fogged);if(left==1){p.sendMessage(ChatColor.RED+"Фильтр противогаза исчерпан!");p.playSound(p.getLocation(),Sound.BLOCK_BEACON_DEACTIVATE,SoundCategory.PLAYERS,0.75f,0.8f);}}else setMaskCondition(p,h,d,fogged);}},10L,10L);}
 public void stop(){if(task!=null)task.cancel();}
}