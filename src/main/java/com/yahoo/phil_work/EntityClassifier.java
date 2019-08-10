package com.yahoo.phil_work;

import java.util.List;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Animals;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.entity.WaterMob;

import java.util.logging.Logger;

/**
 * EntityClassifier
 *
 * Easy way to classify entities.
 *
 * @author Filbert66
 */
public class EntityClassifier {

		// Monsters: Blaze, CaveSpider, Creeper, Enderman, Giant, PigZombie, Silverfish, Skeleton, Spider, Witch, Wither, Zombie
		// Hostile not in that list: Slime/Magma, EnderDragon, Ghast
		// Animal class: Chicken, Cow, Horse, MushroomCow, Ocelot, Pig, Sheep, Wolf
		// Also peaceful: Bat, Snowman, Squid, Golem
		// Officially neutral: Wolf, Enderman, Spiders (both), ZombiePig

	private	static final List <EntityType> HostileNonMonster = Arrays.asList (EntityType.GHAST, EntityType.ENDER_DRAGON); 
	private	static final List <EntityType> PeacefulNonAnimal = Arrays.asList (EntityType.IRON_GOLEM, EntityType.SNOWMAN, EntityType.VILLAGER, EntityType.BAT);

	public static boolean canBeHostile (Entity e) {	
		return (e instanceof Monster || e instanceof Slime || HostileNonMonster.contains (e.getType()));
	}
	public static boolean isPeaceful (Entity e) {	
		return (e instanceof Animals || e instanceof WaterMob || PeacefulNonAnimal.contains (e.getType()));
	}
};