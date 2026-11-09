package ru.artem.beforethefall.disaster;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import ru.artem.beforethefall.radiation.RadiationManager;

import java.util.ArrayDeque;
import java.util.Queue;

/** Applies gradual post-blast damage to currently loaded terrain only; never force-loads chunks. */
public final class AftermathManager {
    private final BeforeTheFallPlugin plugin;
    private final RadiationManager radiation;
    public AftermathManager(BeforeTheFallPlugin plugin, RadiationManager radiation) {
        this.plugin = plugin; this.radiation = radiation;
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

        final double radius = plugin.getConfig().getDouble("aftermath.terrain-radius", 900.0);
        final double radiusSquared = radius * radius;
        Queue<Column> columns = new ArrayDeque<>();
        for (Chunk chunk : world.getLoadedChunks()) {
            double cx = chunk.getX() * 16.0 + 8.0 - center.getX();
            double cz = chunk.getZ() * 16.0 + 8.0 - center.getZ();
            if (cx * cx + cz * cz > radiusSquared) continue;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int bx = chunk.getX() * 16 + x, bz = chunk.getZ() * 16 + z;
                double dx = bx + .5 - center.getX(), dz = bz + .5 - center.getZ();
                if (dx * dx + dz * dz <= radiusSquared) columns.add(new Column(bx, bz));
            }
        }
        new BukkitRunnable() {
            @Override public void run() {
                int done = 0;
                while (!columns.isEmpty() && done < 64) {
                    Column column = columns.poll();
                    if (column == null) break;
                    if (!world.isChunkLoaded(column.x >> 4, column.z >> 4)) continue;
                    transformColumn(world, column.x, column.z);
                    done++;
                }
                if (columns.isEmpty()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void transformColumn(World world, int x, int z) {
        Block top = world.getHighestBlockAt(x, z);
        int topY = top.getY();
        for (int y = Math.max(world.getMinHeight(), topY - 18); y <= Math.min(world.getMaxHeight() - 1, topY + 24); y++) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            String name = type.name();
            if (name.contains("LEAVES") || name.contains("SAPLING") || name.contains("FLOWER")
                    || name.contains("TULIP") || name.contains("GRASS") || name.contains("FERN")
                    || name.contains("VINE") || name.contains("CROP") || name.contains("BUSH")
                    || name.contains("MOSS") || name.contains("ROOTS") || name.contains("BAMBOO")
                    || name.contains("SUGAR_CANE") || name.contains("CACTUS")) {
                block.setType(Material.AIR, false);
            }
        }
        Block surface = world.getHighestBlockAt(x, z);
        Material type = surface.getType();
        if (type == Material.GRASS_BLOCK || type == Material.DIRT || type == Material.PODZOL || type == Material.MYCELIUM) {
            surface.setType(((x * 31 + z * 17) & 3) == 0 ? Material.BASALT : Material.COARSE_DIRT, false);
        } else if (type == Material.SAND || type == Material.RED_SAND || type == Material.SANDSTONE) {
            surface.setType(Material.GRAVEL, false);
        }
    }
    private record Column(int x, int z) { }
}
