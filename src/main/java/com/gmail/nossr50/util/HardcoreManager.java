package com.gmail.nossr50.util;

import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.datatypes.interactions.NotificationType;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.NotificationManager;
import com.gmail.nossr50.worldguard.WorldGuardManager;
import com.gmail.nossr50.worldguard.WorldGuardUtils;
import org.bukkit.entity.Player;

import java.util.HashMap;

public final class HardcoreManager {
    private HardcoreManager() {}

    public static void invokeStatPenalty(Player player) {

        if(WorldGuardUtils.isWorldGuardLoaded()) {
            if(!WorldGuardManager.getInstance().hasHardcoreFlag(player))
                return;
        }

        double statLossPercentage = Config.getInstance().getHardcoreDeathStatPenaltyPercentage();
        int levelThreshold = Config.getInstance().getHardcoreDeathStatPenaltyLevelThreshold();

        if(mcMMO.getUserManager().queryPlayer(player) == null)
            return;

        PlayerProfile playerProfile = mcMMO.getUserManager().queryPlayer(player);
        int totalLevelsLost = 0;

        HashMap<String, Integer> levelChanged = new HashMap<>();
        HashMap<String, Float> experienceChanged = new HashMap<>();

        for (PrimarySkillType primarySkillType : PrimarySkillType.NON_CHILD_SKILLS) {
            if (!primarySkillType.getHardcoreStatLossEnabled()) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0F);
                continue;
            }

            int playerSkillLevel = playerProfile.getExperienceManager().getSkillLevel(primarySkillType);
            int playerSkillXpLevel = playerProfile.getExperienceManager().getSkillXpValue(primarySkillType);

            if (playerSkillLevel <= 0 || playerSkillLevel <= levelThreshold) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0F);
                continue;
            }

            double statsLost = Math.max(0, (playerSkillLevel - levelThreshold)) * (statLossPercentage * 0.01D);
            int levelsLost = (int) statsLost;
            int xpLost = (int) Math.floor(playerSkillXpLevel * (statsLost - levelsLost));
            levelChanged.put(primarySkillType.toString(), levelsLost);
            experienceChanged.put(primarySkillType.toString(), (float) xpLost);

            totalLevelsLost += levelsLost;
        }

        if (!EventUtils.handleStatsLossEvent(player, levelChanged, experienceChanged)) {
            return;
        }

        NotificationManager.sendPlayerInformation(player, NotificationType.HARDCORE_MODE, "Hardcore.DeathStatLoss.PlayerDeath", String.valueOf(totalLevelsLost));
    }

    public static void invokeVampirism(Player killer, Player victim) {

        if(WorldGuardUtils.isWorldGuardLoaded()) {
            if(!WorldGuardManager.getInstance().hasHardcoreFlag(killer) || !WorldGuardManager.getInstance().hasHardcoreFlag(victim))
                return;
        }

        double vampirismStatLeechPercentage = Config.getInstance().getHardcoreVampirismStatLeechPercentage();
        int levelThreshold = Config.getInstance().getHardcoreVampirismLevelThreshold();

        if(mcMMO.getUserManager().queryPlayer(killer) == null || mcMMO.getUserManager().queryPlayer(victim) == null)
            return;

        PlayerProfile killerProfile = mcMMO.getUserManager().queryPlayer(killer);
        PlayerProfile victimProfile = mcMMO.getUserManager().queryPlayer(victim);
        int totalLevelsStolen = 0;

        HashMap<String, Integer> levelChanged = new HashMap<>();
        HashMap<String, Float> experienceChanged = new HashMap<>();

        for (PrimarySkillType primarySkillType : PrimarySkillType.NON_CHILD_SKILLS) {
            if (!primarySkillType.getHardcoreVampirismEnabled()) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0F);
                continue;
            }

            int killerSkillLevel = killerProfile.getExperienceManager().getSkillLevel(primarySkillType);
            int victimSkillLevel = victimProfile.getExperienceManager().getSkillLevel(primarySkillType);

            if (victimSkillLevel <= 0 || victimSkillLevel < killerSkillLevel / 2 || victimSkillLevel <= levelThreshold) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0F);
                continue;
            }

            int victimSkillXpLevel = victimProfile.getExperienceManager().getSkillXpValue(primarySkillType);

            double statsStolen = victimSkillLevel * (vampirismStatLeechPercentage * 0.01D);
            int levelsStolen = (int) statsStolen;
            int xpStolen = (int) Math.floor(victimSkillXpLevel * (statsStolen - levelsStolen));
            levelChanged.put(primarySkillType.toString(), levelsStolen);
            experienceChanged.put(primarySkillType.toString(), (float) xpStolen);

            totalLevelsStolen += levelsStolen;
        }

        if (!EventUtils.handleVampirismEvent(killer, victim, levelChanged, experienceChanged)) {
            return;
        }

        if (totalLevelsStolen > 0) {
            NotificationManager.sendPlayerInformation(killer, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Killer.Success", String.valueOf(totalLevelsStolen), victim.getName());
            NotificationManager.sendPlayerInformation(victim, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Victim.Success", killer.getName(), String.valueOf(totalLevelsStolen));
        }
        else {
            NotificationManager.sendPlayerInformation(killer, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Killer.Failure", victim.getName());
            NotificationManager.sendPlayerInformation(victim, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Victim.Failure", killer.getName());
        }
    }

    /**
     * Check if Hardcore Stat Loss is enabled for one or more skill types
     *
     * @return true if Stat Loss is enabled for one or more skill types
     */
    public static boolean isStatLossEnabled() {
        boolean enabled = false;

        for (PrimarySkillType primarySkillType : PrimarySkillType.NON_CHILD_SKILLS) {
            if (primarySkillType.getHardcoreStatLossEnabled()) {
                enabled = true;
                break;
            }
        }

        return enabled;
    }

    /**
     * Check if Hardcore Vampirism is enabled for one or more skill types
     *
     * @return true if Vampirism is enabled for one or more skill types
     */
    public static boolean isVampirismEnabled() {
        boolean enabled = false;

        for (PrimarySkillType primarySkillType : PrimarySkillType.NON_CHILD_SKILLS) {
            if (primarySkillType.getHardcoreVampirismEnabled()) {
                enabled = true;
                break;
            }
        }

        return enabled;
    }
}
