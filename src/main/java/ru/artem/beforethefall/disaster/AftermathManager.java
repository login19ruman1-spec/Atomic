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

/** Gradual, chunk-safe post-blast terrain conversion. Newly explored chunks are processed too. */
public final class AftermathManager implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final RadiationManager radiation;
    private final List<BlastSite> sites = new ArrayList<>();
    private final Set<String> scheduledChunks = ConcurrentHashMap.newKeySet();

    public AftermathManager(BeforeTheFallPlugin plugin, RadiationManager radiation) {
        this.plugin = plugin;
        this.radiation = radiation;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void apply(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        long days = Math.max(1L, plugin.getConfig().getLong("aftermath.dark-sky-days", 3L));
        if (plugin.getConfig().getBoolean("aftermath.dark-sky", true)) {
            world.setStorm(true);
            world.setThundering(true);
            world.setWeatherDuration((int)Math.min(Integer.MAX_VALUE, days * 24000L));
            world.setThunderDuration((int)Math.min(Integer.MAX_VALUE, days * 24000L));
        }
        if (plugin.getAshStormManager() != null) plugin.getAshStormManager().setActive(true);
        radiation.contaminateWater(center, plugin.getConfig().getDouble("aftermath.water-radius", 1650.0),
                20L * 60L * 60L * plugin.getConfig().getLong("aftermath.contamination-hours", 72L));
        BlastSite site = new BlastSite(world.getUID(), center.getX(), center.getZ(),
                Math.max(1.0, plugin.getConfig().getDouble("aftermath.terrain-radius", 900.0)));
        sites.add(site);
        for (Chunk chunk : world.getLoadedChunks()) scheduleChunk(site, world, chunk.getX(), chunk.getZ());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (BlastSite site : List.copyOf(sites)) {
            if (site.world.equals(event.getWorld().getUID()))
                scheduleChunk(site, event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
        }
    }

    private void scheduleChunk(BlastSite site, World world, int chunkX, int chunkZ) {
        String id = site.id + ":" + chunkX + ":" + chunkZ;
        if (!scheduledChunks.add(id)) return;
        Queue<Column> columns = new ArrayDeque<>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int bx = chunkX * 16 + x, bz = chunkZ * 16 + z;
            double dx = bx + .5 - site.x, dz = bz + .5 - site.z;
            if (dx * dx + dz * dz <= site.radius * site.radius) columns.add(new Column(bx, bz));
        }
        if (columns.isEmpty()) { scheduledChunks.remove(id); return; }
        new BukkitRunnable() {
            @Override public void run() {
                int done = 0;
                while (!columns.isEmpty() && done < 48) {
                    Column column = columns.poll();
                    if (column != null && world.isChunkLoaded(column.x >> 4, column.z >> 4)) {
                        transformColumn(world, column.x, column.z);
                        done++;
                    }
                }
                if (columns.isEmpty()) {
                    scheduledChunks.remove(id);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void transformColumn(World world, int x, int z) {
        Block top = world.getHighestBlockAt(x, z);
        int topY = top.getY();
        for (int y = Math.max(world.getMinHeight(), topY - 18); y <= Math.min(world.getMaxHeight() - 1, topY + 28); y++) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            String name = type.name();
            if (name.contains("LEAVES") || name.contains("SAPLING") || name.contains("FLOWER")
                    || name.contains("TULIP") || name.contains("GRASS") || name.contains("FERN")
                    || name.contains("VINE") || name.contains("CROP") || name.contains("BUSH")
                    || name.contains("MOSS") || name.contains("ROOTS") || name.contains("BAMBOO")
                    || name.contains("SUGAR_CANE") || name.contains("CACTUS") || name.contains("AZALEA")) {
                block.setType(Material.AIR, false);
            }
        }

        Block surface = world.getHighestBlockAt(x, z);
        Material surfaceType = surface.getType();
        if (surfaceType.isAir() || surface.isLiquid() || surfaceType == Material.BEDROCK) return;

        // Stable per-column randomization prevents large square/stripe-like patches.
        long seed = (((long)x) * 341873128712L) ^ (((long)z) * 132897987541L) ^ world.getUID().getLeastSignificantBits();
        Random random = new Random(seed);
        Material crust = chooseCrust(random, surfaceType);
        int crustDepth = 1 + random.nextInt(3);
        for (int layer = 0; layer < crustDepth; layer++) {
            Block target = world.getBlockAt(x, surface.getY() - layer, z);
            if (target.getType().isAir() || target.isLiquid() || target.getType() == Material.BEDROCK) break;
            target.setType(layer == 0 ? crust : chooseSubsoil(random), false);
        }
        // Broken, rocky ground below the visible crust.
        int depth = 2 + random.nextInt(4);
        for (int layer = crustDepth; layer < crustDepth + depth; layer++) {
            Block target = world.getBlockAt(x, surface.getY() - layer, z);
            if (target.getType().isAir() || target.isLiquid() || target.getType() == Material.BEDROCK) break;
            if (random.nextDouble() < .72) target.setType(chooseSubsoil(random), false);
        }
    }

    private Material chooseCrust(Random random, Material original) {
        double roll = random.nextDouble();
        if (original == Material.SAND || original == Material.RED_SAND || original == Material.SANDSTONE) {
            if (roll < .46) return Material.SAND;
            if (roll < .76) return Material.GRAVEL;
            return Material.BASALT;
        }
        if (roll < .30) return Material.BASALT;
        if (roll < .58) return Material.GRAVEL;
        if (roll < .76) return Material.COARSE_DIRT;
        if (roll < .88) return Material.SAND;
        return Material.STONE;
    }

    private Material chooseSubsoil(Random random) {
        double roll = random.nextDouble();
        if (roll < .34) return Material.BASALT;
        if (roll < .58) return Material.STONE;
        if (roll < .78) return Material.GRAVEL;
        if (roll < .90) return Material.TUFF;
        return Material.COARSE_DIRT;
    }

    private record Column(int x, int z) { }
    private record BlastSite(UUID id, double x, double z, double radius) {
        private BlastSite(UUID world, double x, double z, double radius) { this(world, x, z, radius); }
    }
}
