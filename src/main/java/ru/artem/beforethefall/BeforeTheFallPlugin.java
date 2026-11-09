package ru.artem.beforethefall;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.artem.beforethefall.command.BeforeTheFallCommand;
import ru.artem.beforethefall.disaster.NuclearBlastManager;
import ru.artem.beforethefall.equipment.GasMaskManager;
import ru.artem.beforethefall.radiation.RadiationManager;
import ru.artem.beforethefall.weather.AshStormManager;

public final class BeforeTheFallPlugin extends JavaPlugin {
    private RadiationManager radiationManager;
    private AshStormManager ashStormManager;
    private NuclearBlastManager nuclearBlastManager;
    private GasMaskManager gasMaskManager;
    @Override public void onEnable() {
        saveDefaultConfig();
        radiationManager = new RadiationManager(this);
        ashStormManager = new AshStormManager(this);
        gasMaskManager = new GasMaskManager(this);
        nuclearBlastManager = new NuclearBlastManager(this, radiationManager);
        radiationManager.start(); ashStormManager.start(); gasMaskManager.start();
        BeforeTheFallCommand executor = new BeforeTheFallCommand(this, radiationManager, ashStormManager, nuclearBlastManager);
        PluginCommand command = getCommand("beforethefall");
        if (command != null) { command.setExecutor(executor); command.setTabCompleter(executor); }
        getLogger().info("BeforeTheFall Paper port enabled. Minecraft 1.21.4 / Java 21.");
    }
    @Override public void onDisable() {
        if (radiationManager != null) radiationManager.save();
        if (ashStormManager != null) ashStormManager.stop();
        if (gasMaskManager != null) gasMaskManager.stop();
    }
    public GasMaskManager getGasMaskManager() { return gasMaskManager; }
    public NuclearBlastManager getNuclearBlastManager() { return nuclearBlastManager; }
    public void reloadPluginConfig() {
        reloadConfig();
        if (radiationManager != null) radiationManager.reload();
        if (ashStormManager != null) ashStormManager.reload();
    }
}
