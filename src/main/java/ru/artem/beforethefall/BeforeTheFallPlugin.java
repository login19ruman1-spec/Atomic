package ru.artem.beforethefall;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.artem.beforethefall.command.BeforeTheFallCommand;
import ru.artem.beforethefall.disaster.NuclearBlastManager;
import ru.artem.beforethefall.equipment.GasMaskManager;
import ru.artem.beforethefall.equipment.SurvivalCraftingManager;
import ru.artem.beforethefall.radiation.RadiationManager;
import ru.artem.beforethefall.weather.AshStormManager;
import ru.artem.beforethefall.menu.StormSettingsMenu;
import ru.artem.beforethefall.bunker.BunkerManager;
import ru.artem.beforethefall.resourcepack.ResourcePackManager;
public final class BeforeTheFallPlugin extends JavaPlugin {
 private RadiationManager radiationManager; private AshStormManager ashStormManager; private NuclearBlastManager nuclearBlastManager; private GasMaskManager gasMaskManager; private ru.artem.beforethefall.equipment.ProtectiveSuitManager protectiveSuitManager; private StormSettingsMenu stormSettingsMenu; private BunkerManager bunkerManager;
 @Override public void onEnable() {
  saveDefaultConfig(); protectiveSuitManager=new ru.artem.beforethefall.equipment.ProtectiveSuitManager(this); stormSettingsMenu=new StormSettingsMenu(this); getServer().getPluginManager().registerEvents(stormSettingsMenu,this);
  radiationManager=new RadiationManager(this); ashStormManager=new AshStormManager(this); gasMaskManager=new GasMaskManager(this); nuclearBlastManager=new NuclearBlastManager(this,radiationManager); bunkerManager=new BunkerManager(this);
  getServer().getPluginManager().registerEvents(gasMaskManager,this); getServer().getPluginManager().registerEvents(new ru.artem.beforethefall.equipment.NuclearChargeListener(this),this); getServer().getPluginManager().registerEvents(bunkerManager,this); getServer().getPluginManager().registerEvents(new ResourcePackManager(this),this);
  new SurvivalCraftingManager(this).register(); radiationManager.start(); ashStormManager.start(); gasMaskManager.start();
  BeforeTheFallCommand executor=new BeforeTheFallCommand(this,radiationManager,ashStormManager,nuclearBlastManager); PluginCommand command=getCommand("beforethefall");
  if(command!=null){command.setExecutor(executor);command.setTabCompleter(executor);} getLogger().info("BeforeTheFall Atomic Survival enabled. Paper 1.21.4 / Java 21.");
 }
 @Override public void onDisable(){if(radiationManager!=null)radiationManager.save();if(ashStormManager!=null)ashStormManager.stop();if(gasMaskManager!=null)gasMaskManager.stop();}
 public GasMaskManager getGasMaskManager(){return gasMaskManager;} public ru.artem.beforethefall.equipment.ProtectiveSuitManager getProtectiveSuitManager(){return protectiveSuitManager;} public RadiationManager getRadiationManager(){return radiationManager;} public BunkerManager getBunkerManager(){return bunkerManager;} public AshStormManager getAshStormManager(){return ashStormManager;}
 public void openStormSettings(org.bukkit.entity.Player player){stormSettingsMenu.open(player);} public NuclearBlastManager getNuclearBlastManager(){return nuclearBlastManager;}
 public void reloadPluginConfig(){reloadConfig();if(radiationManager!=null)radiationManager.reload();if(ashStormManager!=null)ashStormManager.reload();}
}