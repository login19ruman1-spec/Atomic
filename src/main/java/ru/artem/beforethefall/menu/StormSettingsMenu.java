package ru.artem.beforethefall.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import java.util.Arrays;

public final class StormSettingsMenu implements Listener {
    private final BeforeTheFallPlugin plugin;
    public StormSettingsMenu(BeforeTheFallPlugin plugin) { this.plugin=plugin; }
    public void open(Player player) {
        MenuHolder holder=new MenuHolder();
        Inventory inv=Bukkit.createInventory(holder,27,ChatColor.DARK_GRAY+"Настройки пепельной бури");
        holder.inventory=inv;
        inv.setItem(10,item(Material.FIREWORK_STAR,"&6Плотность радиоактивного пепла","&7Частиц за цикл: &f"+plugin.getConfig().getInt("ash-storm.particle-count",220),"&eНажми: изменить"));
        inv.setItem(13,item(Material.IRON_SWORD,"&cУрон от бури","&7Урон: &f"+plugin.getConfig().getDouble("ash-storm.damage",0.0),"&eНажми: изменить"));
        inv.setItem(16,item(Material.ENDER_EYE,"&5Затемнение","&7Включено: &f"+plugin.getConfig().getBoolean("ash-storm.blindness",true),"&eНажми: переключить"));
        inv.setItem(22,item(Material.BARRIER,"&cЗакрыть"));
        player.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof MenuHolder))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||e.getClickedInventory()!=e.getView().getTopInventory())return;
        switch(e.getSlot()){
            case 10 -> cycleInt("ash-storm.particle-count",new int[]{100,220,350,500},220);
            case 13 -> cycleDouble("ash-storm.damage",new double[]{0,1,2,4},0);
            case 16 -> plugin.getConfig().set("ash-storm.blindness",!plugin.getConfig().getBoolean("ash-storm.blindness",true));
            case 22 -> {p.closeInventory();return;}
            default -> {return;}
        }
        plugin.saveConfig();open(p);
    }
    private void cycleInt(String path,int[] values,int fallback){int c=plugin.getConfig().getInt(path,fallback),n=values[0];for(int i=0;i<values.length;i++)if(values[i]==c){n=values[(i+1)%values.length];break;}plugin.getConfig().set(path,n);}
    private void cycleDouble(String path,double[] values,double fallback){double c=plugin.getConfig().getDouble(path,fallback),n=values[0];for(int i=0;i<values.length;i++)if(Double.compare(values[i],c)==0){n=values[(i+1)%values.length];break;}plugin.getConfig().set(path,n);}
    private ItemStack item(Material type,String name,String... lore){ItemStack s=new ItemStack(type);ItemMeta m=s.getItemMeta();m.setDisplayName(color(name));m.setLore(Arrays.stream(lore).map(this::color).toList());s.setItemMeta(m);return s;}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
    private static final class MenuHolder implements InventoryHolder{private Inventory inventory;public Inventory getInventory(){return inventory;}}
}
