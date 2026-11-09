package ru.artem.beforethefall.bunker;
import org.bukkit.ChatColor;import org.bukkit.Location;import org.bukkit.Material;import org.bukkit.block.Block;import org.bukkit.event.EventHandler;import org.bukkit.event.Listener;import org.bukkit.event.block.BlockBreakEvent;import org.bukkit.event.block.BlockPlaceEvent;import ru.artem.beforethefall.BeforeTheFallPlugin;import java.util.ArrayList;import java.util.List;import java.util.UUID;
public final class BunkerManager implements Listener{
 private final BeforeTheFallPlugin plugin;private final List<Core> cores=new ArrayList<>();
 public BunkerManager(BeforeTheFallPlugin p){plugin=p;for(String raw:p.getConfig().getStringList("bunkers.cores")){String[] a=raw.split(",");if(a.length!=4)continue;try{cores.add(new Core(UUID.fromString(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3])));}catch(IllegalArgumentException ignored){}}}
 @EventHandler public void onPlace(BlockPlaceEvent e){Block b=e.getBlockPlaced();if(b.getType()!=Material.LODESTONE)return;cores.add(new Core(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ()));save();e.getPlayer().sendMessage(ChatColor.GREEN+"Ядро бункера активировано. Радиус защиты: "+radius()+" блоков.");}
 @EventHandler public void onBreak(BlockBreakEvent e){Block b=e.getBlock();if(cores.removeIf(c->c.world.equals(b.getWorld().getUID())&&c.x==b.getX()&&c.y==b.getY()&&c.z==b.getZ())){save();e.getPlayer().sendMessage(ChatColor.RED+"Ядро бункера разрушено. Защита отключена.");}}
 public boolean isProtected(Location l){if(l.getWorld()==null)return false;double r=radius();for(Core c:cores){if(!c.world.equals(l.getWorld().getUID()))continue;double dx=l.getX()-(c.x+.5),dz=l.getZ()-(c.z+.5),dy=l.getY()-(c.y+.5);if(dx*dx+dz*dz<=r*r&&dy>=-plugin.getConfig().getInt("bunkers.depth",12)&&dy<=plugin.getConfig().getInt("bunkers.height",8))return true;}return false;}
 private int radius(){return Math.max(4,plugin.getConfig().getInt("bunkers.radius",12));}
 private void save(){List<String> v=new ArrayList<>();for(Core c:cores)v.add(c.world+","+c.x+","+c.y+","+c.z);plugin.getConfig().set("bunkers.cores",v);plugin.saveConfig();}
 private record Core(UUID world,int x,int y,int z){}
}