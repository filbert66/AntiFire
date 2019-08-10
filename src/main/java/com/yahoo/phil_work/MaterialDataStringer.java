/* MaterialDataStringer.java
 * History
 *  05 Dec 2013 : Corrected dark oak to "DARK_OAK"
 *  14 Jan 2013 : Work around bug in Tree.setSpecies() that doesn't set type & data properly for new types.
 *  24 Mar 2016 : Added 1.9 new blocks: Banner, 
 *  13 Dec 2017 : Remove use of magic IDs
 *  	too many new block IDs! Don't want to have to maintain this any more; 
 *        keep old data strings and wait for what's new in 1.13.
 ************
 *  3 Aug 2019 : Replace with new Spigit 1.13 API/names
 *                   MaterialData-> BlockData or Material
 * 					
 * 
 */
package com.yahoo.phil_work;

import java.util.HashMap;
import java.lang.reflect.Constructor;
import java.lang.IllegalArgumentException;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.material.*;
import org.bukkit.entity.EntityType;

// @SuppressWarnings("deprecation") // until new block data interface is present
public class MaterialDataStringer  {

	// Future: Read in from separate config file.
    private static final HashMap<String, Material> altNames = new HashMap<String, Material>();
	// private static boolean newTreeTypes = true;
    static
    {		
    	try {
        altNames.put("GUNPOWDER", Material.GUNPOWDER);
        altNames.put("SULPHUR", Material.GUNPOWDER);
        
        altNames.put("WOOD_SHOVEL", Material.WOODEN_SHOVEL);
        altNames.put("GOLD_SHOVEL", Material.GOLDEN_SHOVEL);

        altNames.put("FIRECHARGE", Material.FIRE_CHARGE);
        altNames.put("PLANKS", Material.OAK_PLANKS);
        altNames.put("RED_FLOWER", Material.RED_DYE);
        altNames.put("LIT_PUMPKIN", Material.JACK_O_LANTERN);
                
		altNames.put("WITHER_SKULL", Material.WITHER_SKELETON_SKULL);
        
        altNames.put("GENERIC_LOG", Material.OAK_LOG);
        altNames.put("REDWOOD_LOG", Material.SPRUCE_LOG);
		altNames.put("DARKOAK_LOG", Material.DARK_OAK_LOG);
		
        altNames.put("GENERIC_WOOD", Material.OAK_WOOD);
        altNames.put("REDWOOD_WOOD", Material.SPRUCE_WOOD);
		altNames.put("DARKOAK_WOOD", Material.DARK_OAK_WOOD);

        altNames.put("GENERIC_LEAVES", Material.OAK_LEAVES);
        altNames.put("REDWOOD_LEAVES", Material.SPRUCE_LEAVES);
        altNames.put("DARKOAK_LEAVES", Material.DARK_OAK_LEAVES);

        altNames.put("GENERIC_SAPLING", Material.OAK_SAPLING);
        altNames.put("REDWOOD_SAPLING", Material.SPRUCE_SAPLING);
		altNames.put("DARKOAK_SAPLING", Material.DARK_OAK_SAPLING);

        altNames.put("GLYPHED_SANDSTONE", Material.CHISELED_SANDSTONE);
        altNames.put("GLYPHED_RED_SANDSTONE", Material.CHISELED_RED_SANDSTONE);
       // altNames.put("CRACKED_SANDSTONE", new Sandstone (SandstoneType.CRACKED));

		altNames.put("DANDELION_YELLOW", Material.YELLOW_DYE);
		altNames.put("GREY_DYE", Material.GRAY_DYE);
		altNames.put("LIGHT_GREY_DYE", Material.LIGHT_GRAY_DYE);
		altNames.put("SILVER_DYE", Material.LIGHT_GRAY_DYE);
		altNames.put("CACTUS_GREEN", Material.GREEN_DYE);
		altNames.put("ROSE_RED", Material.RED_DYE);
		altNames.put("INK_SAC", Material.BLACK_DYE);
		altNames.put("INK_SACK", Material.BLACK_DYE);
 
		altNames.put("GREY_WOOL", Material.GRAY_WOOL);
		altNames.put("LIGHT_GREY_WOOL", Material.LIGHT_GRAY_WOOL);
		altNames.put("SILVER_WOOL", Material.LIGHT_GRAY_WOOL);

		altNames.put("TALL_FERN", Material.LARGE_FERN);
		altNames.put("DEAD_GRASS", Material.DEAD_BUSH);
		
		altNames.put("BLAZE_EGG", Material.BLAZE_SPAWN_EGG);
		altNames.put("CREEPER_EGG", Material.CREEPER_SPAWN_EGG);
		altNames.put("SLIME_EGG", Material.SLIME_SPAWN_EGG);
		altNames.put("ZOMBIE_EGG", Material.ZOMBIE_SPAWN_EGG);
		altNames.put("SPIDER_EGG", Material.SPIDER_SPAWN_EGG);
		altNames.put("CAVE_SPIDER_EGG", Material.CAVE_SPIDER_SPAWN_EGG);
		altNames.put("ENDERMAN_EGG", Material.ENDERMAN_SPAWN_EGG);
		altNames.put("GHAST_EGG", Material.GHAST_SPAWN_EGG);
		altNames.put("PIG_ZOMBIE_EGG", Material.ZOMBIE_PIGMAN_SPAWN_EGG);
		altNames.put("PIGMAN_EGG",Material.ZOMBIE_PIGMAN_SPAWN_EGG);
		altNames.put("SILVERFISH_EGG", Material.SILVERFISH_SPAWN_EGG);
		altNames.put("MAGMA_CUBE_EGG", Material.MAGMA_CUBE_SPAWN_EGG);
		altNames.put("LAVA_SLIME_EGG", Material.MAGMA_CUBE_SPAWN_EGG);
		altNames.put("WITHER_EGG", Material.WITHER_SKELETON_SPAWN_EGG);
		altNames.put("BAT_EGG", Material.BAT_SPAWN_EGG);
		altNames.put("WITCH_EGG", Material.WITCH_SPAWN_EGG);
		altNames.put("PIG_EGG", Material.PIG_SPAWN_EGG);
		altNames.put("SHEEP_EGG", Material.SHEEP_SPAWN_EGG);
		altNames.put("COW_EGG", Material.COW_SPAWN_EGG);
		altNames.put("CHICKEN_EGG", Material.CHICKEN_SPAWN_EGG);
		altNames.put("SQUID_EGG", Material.SQUID_SPAWN_EGG);
		altNames.put("WOLF_EGG", Material.WOLF_SPAWN_EGG);
		altNames.put("MUSHROOM_COW_EGG", Material.MOOSHROOM_SPAWN_EGG);
		altNames.put("MOOSHROOM_EGG", Material.MOOSHROOM_SPAWN_EGG);
		altNames.put("OCELOT_EGG", Material.OCELOT_SPAWN_EGG);
		altNames.put("HORSE_EGG", Material.HORSE_SPAWN_EGG);
		altNames.put("VILLAGER_EGG", Material.VILLAGER_SPAWN_EGG);
		
		altNames.put("GREY_BANNER", Material.GRAY_BANNER);
		altNames.put("LIGHT_GREY_BANNER", Material.LIGHT_GRAY_BANNER);
		altNames.put("SILVER_BANNER", Material.LIGHT_GRAY_BANNER);
	
		} catch (Throwable t) {
			System.out.println ("MaterialDataStringer: FAILURE during static initialization: " + t);
		}
    }
	 
	static public Material matchMaterial (String name) {
		Material mat = Material.matchMaterial (name);
		if (mat == null) {
			name = name.toUpperCase();
			
			mat = altNames.get (name);
			if (mat != null) {
				// Bukkit.getLogger().config ("Matched alternative name '" + name + "' for " + mat);
			}
		}
		return mat;
	}
	
	//DEBUG USE ONLY
	static public Set<String> getAltNames () {
		return altNames.keySet();
	}

    // Caution: If name not found, will throw null pointer exception
    public MaterialDataStringer (String name) {
    	this.matchMaterial (name);   
     	// throw new IllegalArgumentException (name + " not found");
    }
}