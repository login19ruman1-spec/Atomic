package ru.artem.beforethefall.disaster;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import ru.artem.beforethefall.radiation.RadiationManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Chunk-safe aftermath conversion. Water and ice are never painted as radioactive soil. */
public final class AftermathManager implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final RadiationManager radiation;
    private final List<BlastSite> sites = new ArrayList<>();
    private final Set<String> scheduledChunks = ConcurrentHashMap.newKeySet();

    public AftermathManager(BeforeTheFallPlugin plugin, RadiationManager radiation) {
        this.plugin = plugin; this.radiation = radiation;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void apply(Location center) {
        World world = center.getWorld(); if (world == null) return;
        long days = Math.max(1L, plugin.getConfig().getLong("aftermath.dark-sky-days", 3L));
        if (plugin.getConfig().getBoolean("aftermath.dark-sky", true)) {
            world.setStorm(true); world.setThundering(true);
            world.setWeatherDuration((int)Math.min(Integer.MAX_VALUE, days * 24000L));
            world.setThunderDuration((int)Math.min(Integer.MAX_VALUE, days * 24000L));
        }
        if (plugin.getAshStormManager() != null) plugin.getAshStormManager().setActive(true);
        radiation.contaminateWater(center, plugin.getConfig().getDouble("aftermath.water-contamination-radius", 3000.0),
            20L * 60L * 60L * plugin.getConfig().getLong("aftermath.contamination-hours", 72L));
        BlastSite site = new BlastSite(world.getUID(), center.getX(), center.getZ(),
            Math.max(1.0, plugin.getConfig().getDouble("aftermath.terrain-radius", 900.0)));
        sites.add(site);
        for (Chunk chunk : world.getLoadedChunks()) scheduleChunk(site, world, chunk.getX(), chunk.getZ());
    }

    @EventHandler public void onChunkLoad(ChunkLoadEvent event) {
        for (BlastSite site : List.copyOf(sites))
            if (site.world.equals(event.getWorld().getUID()))
                scheduleChunk(site, event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    private void scheduleChunk(BlastSite site, World world, int cx, int cz) {
        String id = site.world + ":" + Double.doubleToLongBits(site.x) + ":" + Double.doubleToLongBits(site.z) + ":" + cx + ":" + cz;
        if (!scheduledChunks.add(id)) return;
        Queue<Column> columns = new ArrayDeque<>();
        for (int x=0;x<16;x++) for(int z=0;z<16;z++) {
            int bx=cx*16+x,bz=cz*16+z; double dx=bx+.5-site.x,dz=bz+.5-site.z;
            if(dx*dx+dz*dz<=site.radius*site.radius) columns.add(new Column(bx,bz));
        }
        if(columns.isEmpty()){scheduledChunks.remove(id);return;}
        new BukkitRunnable(){ public void run(){
            int done=0;
            while(!columns.isEmpty()&&done<32){
                Column c=columns.poll();
                if(c!=null&&world.isChunkLoaded(c.x>>4,c.z>>4)){transformColumn(world,c.x,c.z);done++;}
            }
            if(columns.isEmpty()){scheduledChunks.remove(id);cancel();}
        }}.runTaskTimer(plugin,1L,1L);
    }

    private void transformColumn(World world,int x,int z) {
        Block surface=world.getHighestBlockAt(x,z);
        Material top=surface.getType();

        // Ice must crack/melt, not become dirt or stone. Leave liquid water untouched.
        if(top==Material.ICE||top==Material.FROSTED_ICE||top==Material.PACKED_ICE||top==Material.BLUE_ICE){
            surface.setType(top==Material.ICE?Material.WATER:Material.AIR,false);
            return;
        }
        if(surface.isLiquid()||top==Material.WATER||top==Material.BEDROCK||top.isAir()) return;

        int topY=surface.getY();
        for(int y=Math.max(world.getMinHeight(),topY-18);y<=Math.min(world.getMaxHeight()-1,topY+24);y++){
            Block b=world.getBlockAt(x,y,z); String n=b.getType().name();
            if(n.contains("LEAVES")||n.contains("SAPLING")||n.contains("FLOWER")||n.contains("TULIP")
                ||n.contains("GRASS")||n.contains("FERN")||n.contains("VINE")||n.contains("CROP")
                ||n.contains("BUSH")||n.contains("MOSS")||n.contains("ROOTS")||n.contains("BAMBOO")
                ||n.contains("SUGAR_CANE")||n.contains("CACTUS")||n.contains("AZALEA"))
                b.setType(Material.AIR,false);
        }

        surface=world.getHighestBlockAt(x,z); top=surface.getType();
        if(top.isAir()||surface.isLiquid()||top==Material.BEDROCK||top==Material.WATER) return;
        // A natural, broken-looking surface: no generic STONE top layer.
        long seed=(((long)x)*341873128712L)^(((long)z)*132897987541L)^world.getUID().getLeastSignificantBits();
        Random random=new Random(seed);
        Material crust=chooseSurface(random,top);
        int crustDepth=1+random.nextInt(2);
        for(int layer=0;layer<crustDepth;layer++){
            Block b=world.getBlockAt(x,surface.getY()-layer,z);
            if(b.getType().isAir()||b.isLiquid()||b.getType()==Material.BEDROCK)break;
            b.setType(layer==0?crust:chooseSubsoil(random),false);
        }
        int depth=2+random.nextInt(3);
        for(int layer=crustDepth;layer<crustDepth+depth;layer++){
            Block b=world.getBlockAt(x,surface.getY()-layer,z);
            if(b.getType().isAir()||b.isLiquid()||b.getType()==Material.BEDROCK)break;
            if(random.nextDouble()<.65)b.setType(chooseSubsoil(random),false);
        }
    }

    private Material chooseSurface(Random r,Material original){
        double v=r.nextDouble();
        if(original==Material.SAND||original==Material.RED_SAND||original==Material.SANDSTONE)
            return v<.48?Material.SAND:v<.82?Material.GRAVEL:Material.BASALT;
        return v<.38?Material.BASALT:v<.72?Material.GRAVEL:v<.88?Material.COARSE_DIRT:Material.SAND;
    }
    private Material chooseSubsoil(Random r){
        double v=r.nextDouble();
        return v<.38?Material.BASALT:v<.66?Material.GRAVEL:v<.84?Material.TUFF:Material.COARSE_DIRT;
    }
    private record Column(int x,int z){}
    private record BlastSite(UUID world,double x,double z,double radius){}
}
