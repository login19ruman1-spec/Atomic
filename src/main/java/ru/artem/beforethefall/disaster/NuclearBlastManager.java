package ru.artem.beforethefall.disaster;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import ru.artem.beforethefall.radiation.RadiationManager;
import java.util.concurrent.ThreadLocalRandom;

public final class NuclearBlastManager {
    private final BeforeTheFallPlugin plugin;
    private final RadiationManager radiation;
    public NuclearBlastManager(BeforeTheFallPlugin plugin, RadiationManager radiation) { this.plugin=plugin;this.radiation=radiation; }
    public void detonate(Location origin,float power) {
        World world=origin.getWorld();if(world==null)return;
        float p=Math.max(4.0f,Math.min(40.0f,power));double radius=p*9.0;
        world.playSound(origin,Sound.ENTITY_GENERIC_EXPLODE,8.0f,.45f);
        world.spawnParticle(Particle.FLASH,origin,1);world.spawnParticle(Particle.EXPLOSION_EMITTER,origin,1);
        radiation.addFalloutZone(origin,Math.min(18.0,p*.75),radius*1.8,20L*60L*60L);
        for(Player player:world.getPlayers()){
            double d=player.getLocation().distance(origin);
            if(plugin.getBunkerManager()!=null&&plugin.getBunkerManager().isProtected(player.getLocation()))continue;
            if(d>radius*1.35)continue;
            double exposure=Math.max(0.0,1.0-d/(radius*1.35));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,(int)(80+exposure*180),0,true,false,true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,(int)(240+exposure*600),0,true,false,true));
            player.damage(Math.max(2.0,26.0*exposure));
            // Acute neutron/gamma exposure is not stopped by a particle filter.
            radiation.add(player,35.0*exposure);
        }
        new BukkitRunnable(){int phase=0;public void run(){
            if(phase>=12){cancel();return;}
            double h=phase*4.0,r=2.0+phase*2.3;int n=100+phase*35;
            for(int i=0;i<n;i++){double a=ThreadLocalRandom.current().nextDouble(Math.PI*2),rr=ThreadLocalRandom.current().nextDouble(r*.35,r);
                Location at=origin.clone().add(Math.cos(a)*rr,h+ThreadLocalRandom.current().nextDouble(-1,2),Math.sin(a)*rr);
                world.spawnParticle(Particle.LARGE_SMOKE,at,1,.5,1.2,.5,.015);
                if(i%4==0)world.spawnParticle(Particle.CLOUD,at,1,.8,1.5,.8,.01);
                if(i%9==0)world.spawnParticle(Particle.ASH,at,1,1,1.8,1,.02);
            }
            if(phase==1||phase==4)world.playSound(origin,Sound.ENTITY_GENERIC_EXPLODE,5,.65f);phase++;
        }}.runTaskTimer(plugin,2L,5L);
    }
}
