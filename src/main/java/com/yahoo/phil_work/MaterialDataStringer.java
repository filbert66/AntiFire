package com.yahoo.phil_work;

import java.util.HashMap;
import java.lang.reflect.Constructor;
import java.lang.IllegalArgumentException;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.CoalType;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.material.*;
import org.bukkit.entity.EntityType;
import org.bukkit.GrassSpecies;
import org.bukkit.SandstoneType;
import org.bukkit.SkullType;
import org.bukkit.TreeSpecies;

public class MaterialDataStringer extends MaterialData {

	// Future: Read in from separate config file.
    private static final HashMap<String, MaterialData> altNames = new HashMap<String, MaterialData>();
	private static boolean newTreeTypes = false;
    static
    {
    	try {
			newTreeTypes = Bukkit.getBukkitVersion().startsWith("1.7");    
		} catch (NullPointerException e) {
			newTreeTypes = false;
		}
		
    	try {
        altNames.put("GUNPOWDER", new MaterialData (Material.SULPHUR));
        altNames.put("WOOD_SHOVEL", new MaterialData (Material.WOOD_SPADE));
        altNames.put("STONE_SHOVEL", new MaterialData (Material.STONE_SPADE));
        altNames.put("IRON_SHOVEL", new MaterialData (Material.IRON_SPADE));
        altNames.put("GOLD_SHOVEL", new MaterialData (Material.GOLD_SPADE));
        altNames.put("DIAMOND_SHOVEL", new MaterialData (Material.DIAMOND_SPADE)); 
        altNames.put("FIRECHARGE", new MaterialData (Material.FIREBALL));
        altNames.put("ENDER_EYE", new MaterialData (Material.EYE_OF_ENDER));  
        altNames.put("PLANKS", new MaterialData (Material.WOOD));
        altNames.put("MYCELIUM", new MaterialData (Material.MYCEL));
        altNames.put("RED_FLOWER", new MaterialData (Material.RED_ROSE));
        altNames.put("POPPY", new MaterialData (Material.RED_ROSE));
        altNames.put("DANDELION", new MaterialData (Material.YELLOW_FLOWER));
        altNames.put("LILY_PAD", new MaterialData (Material.WATER_LILY));
        altNames.put("NETHER_PORTAL", new MaterialData (Material.PORTAL));
        altNames.put("LIT_PUMPKIN", new MaterialData (Material.JACK_O_LANTERN));
                
        altNames.put("CHARCOAL", new Coal (CoalType.CHARCOAL));

        altNames.put("WITHER_SKULL", new Skull (Material.SKULL_ITEM, (byte)SkullType.WITHER.ordinal()));
        altNames.put("CREEPER_HEAD",  new Skull (Material.SKULL_ITEM, (byte)SkullType.CREEPER.ordinal()));
        altNames.put("SKELETON_SKULL",  new Skull (Material.SKULL_ITEM, (byte)SkullType.SKELETON.ordinal()));
        altNames.put("PLAYER_HEAD",  new Skull (Material.SKULL_ITEM, (byte)SkullType.PLAYER.ordinal()));
        altNames.put("ZOMBIE_HEAD",  new Skull (Material.SKULL_ITEM, (byte)SkullType.ZOMBIE.ordinal()));
        
        altNames.put("GENERIC_LOG", new Tree (TreeSpecies.GENERIC));
        altNames.put("OAK_LOG", new Tree (TreeSpecies.GENERIC));
        altNames.put("BIRCH_LOG", new Tree (TreeSpecies.BIRCH));
        altNames.put("JUNGLE_LOG", new Tree (TreeSpecies.JUNGLE));
        altNames.put("REDWOOD_LOG", new Tree (TreeSpecies.REDWOOD));
        altNames.put("SPRUCE_LOG", new Tree (TreeSpecies.REDWOOD));
        if (newTreeTypes) {
			altNames.put("ACACIA_LOG", new Tree (TreeSpecies.valueOf ("ACACIA")));
			altNames.put("DARKOAK_LOG", new Tree (TreeSpecies.valueOf ("DARKOAK")));
		}        
		Tree tree = new Tree (Material.WOOD); tree.setSpecies (TreeSpecies.GENERIC);
        altNames.put("GENERIC_WOOD", tree);  altNames.put("GENERIC_PLANKS", tree); 
        altNames.put("OAK_WOOD", tree);      altNames.put("OAK_PLANKS", tree);
        tree = tree.clone(); tree.setSpecies (TreeSpecies.BIRCH);
        altNames.put("BIRCH_WOOD", tree);        altNames.put("BIRCH_PLANKS", tree);
        tree = tree.clone(); tree.setSpecies (TreeSpecies.JUNGLE);
        altNames.put("JUNGLE_WOOD", tree);       altNames.put("JUNGLE_PLANKS", tree);
        tree = tree.clone(); tree.setSpecies (TreeSpecies.REDWOOD);
        altNames.put("REDWOOD_WOOD", tree);        altNames.put("REDWOOD_PLANKS", tree);
        altNames.put("SPRUCE_WOOD", tree);        altNames.put("SPRUCE_PLANKS", tree);
        if (newTreeTypes) {
			tree = tree.clone(); tree.setSpecies (TreeSpecies.valueOf ("ACACIA"));
			altNames.put("ACACIA_WOOD", tree); 			altNames.put("ACACIA_PLANKS", tree);
			tree = tree.clone(); tree.setSpecies (TreeSpecies.valueOf ("DARKOAK"));
			altNames.put("DARKOAK_WOOD", tree);			altNames.put("DARKOAK_PLANKS", tree);
		} 
        altNames.put("GENERIC_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.GENERIC.ordinal()));
        altNames.put("OAK_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.GENERIC.ordinal()));
        altNames.put("BIRCH_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.BIRCH.ordinal()));
        altNames.put("JUNGLE_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.JUNGLE.ordinal()));
        altNames.put("REDWOOD_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.REDWOOD.ordinal()));
        altNames.put("SPRUCE_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.REDWOOD.ordinal()));
        if (newTreeTypes) {
			altNames.put("ACACIA_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.valueOf ("ACACIA").ordinal()));
			altNames.put("DARKOAK_LEAVES", new Tree (Material.LEAVES, (byte)TreeSpecies.valueOf ("DARKOAK").ordinal()));
		} 
        altNames.put("GENERIC_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.GENERIC.ordinal()));
        altNames.put("OAK_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.GENERIC.ordinal()));
        altNames.put("BIRCH_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.BIRCH.ordinal()));
        altNames.put("JUNGLE_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.JUNGLE.ordinal()));
        altNames.put("REDWOOD_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.REDWOOD.ordinal()));
        altNames.put("SPRUCE_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.REDWOOD.ordinal()));
        if (newTreeTypes) {
			altNames.put("ACACIA_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.valueOf ("ACACIA").ordinal()));
			altNames.put("DARKOAK_SAPLING", new Tree (Material.SAPLING, (byte)TreeSpecies.valueOf ("DARKOAK").ordinal()));
		}
        altNames.put("SMOOTH_SANDSTONE", new Sandstone (SandstoneType.SMOOTH));
        altNames.put("GLYPHED_SANDSTONE", new Sandstone (SandstoneType.GLYPHED));
        altNames.put("CHISELED_SANDSTONE", new Sandstone (SandstoneType.GLYPHED));
        altNames.put("CRACKED_SANDSTONE", new Sandstone (SandstoneType.CRACKED));

		altNames.put("ORANGE_DYE", new Dye(Material.INK_SACK, DyeColor.ORANGE.getDyeData()));
		altNames.put("MAGENTA_DYE", new Dye(Material.INK_SACK, DyeColor.MAGENTA.getDyeData()));
		altNames.put("LIGHT_BLUE_DYE", new Dye(Material.INK_SACK, DyeColor.LIGHT_BLUE.getDyeData()));
		altNames.put("YELLOW_DYE", new Dye(Material.INK_SACK, DyeColor.YELLOW.getDyeData()));
		altNames.put("DANDELION_YELLOW", new Dye(Material.INK_SACK, DyeColor.YELLOW.getDyeData()));
		altNames.put("LIME_DYE", new Dye(Material.INK_SACK, DyeColor.LIME.getDyeData()));
		altNames.put("PINK_DYE", new Dye(Material.INK_SACK, DyeColor.PINK.getDyeData()));
		altNames.put("GRAY_DYE", new Dye(Material.INK_SACK, DyeColor.GRAY.getDyeData()));
		altNames.put("GREY_DYE", new Dye(Material.INK_SACK, DyeColor.GRAY.getDyeData()));
		altNames.put("LIGHT_GRAY_DYE", new Dye(Material.INK_SACK, DyeColor.SILVER.getDyeData()));
		altNames.put("LIGHT_GREY_DYE", new Dye(Material.INK_SACK, DyeColor.SILVER.getDyeData()));
		altNames.put("SILVER_DYE", new Dye(Material.INK_SACK, DyeColor.SILVER.getDyeData()));
		altNames.put("CYAN_DYE", new Dye(Material.INK_SACK, DyeColor.CYAN.getDyeData()));
		altNames.put("PURPLE_DYE", new Dye(Material.INK_SACK, DyeColor.PURPLE.getDyeData()));
		altNames.put("BLUE_DYE", new Dye(Material.INK_SACK, DyeColor.BLUE.getDyeData()));
		altNames.put("LAPIS_LAZULI", new Dye(Material.INK_SACK, DyeColor.BLUE.getDyeData()));
		altNames.put("COCOA_BEANS", new Dye(Material.INK_SACK, DyeColor.BROWN.getDyeData()));
		altNames.put("BROWN_DYE", new Dye(Material.INK_SACK, DyeColor.BROWN.getDyeData()));
		altNames.put("GREEN_DYE", new Dye(Material.INK_SACK, DyeColor.GREEN.getDyeData()));
		altNames.put("CACTUS_GREEN", new Dye(Material.INK_SACK, DyeColor.GREEN.getDyeData()));
		altNames.put("ROSE_RED", new Dye(Material.INK_SACK, DyeColor.RED.getDyeData()));
		altNames.put("RED_DYE", new Dye(Material.INK_SACK, DyeColor.RED.getDyeData()));
		altNames.put("BLACK_DYE", new Dye(Material.INK_SACK, DyeColor.BLACK.getDyeData()));
		altNames.put("INK_SAC", new Dye(Material.INK_SACK, DyeColor.BLACK.getDyeData()));
		altNames.put("INK_SACK", new Dye(Material.INK_SACK, DyeColor.BLACK.getDyeData()));
 
 		altNames.put("ORANGE_WOOL", new Wool(Material.WOOL, DyeColor.ORANGE.getWoolData()));
		altNames.put("MAGENTA_WOOL", new Wool (Material.WOOL, DyeColor.MAGENTA.getWoolData()));
		altNames.put("LIGHT_BLUE_WOOL", new Wool (Material.WOOL, DyeColor.LIGHT_BLUE.getWoolData()));
		altNames.put("YELLOW_WOOL", new Wool (Material.WOOL, DyeColor.YELLOW.getWoolData()));
		altNames.put("LIME_WOOL", new Wool (Material.WOOL, DyeColor.LIME.getWoolData()));
		altNames.put("PINK_WOOL", new Wool (Material.WOOL, DyeColor.PINK.getWoolData()));
		altNames.put("GRAY_WOOL", new Wool (Material.WOOL, DyeColor.GRAY.getWoolData()));
		altNames.put("GREY_WOOL", new Wool (Material.WOOL, DyeColor.GRAY.getWoolData()));
		altNames.put("LIGHT_GRAY_WOOL", new Wool (Material.WOOL, DyeColor.SILVER.getWoolData()));
		altNames.put("LIGHT_GREY_WOOL", new Wool (Material.WOOL, DyeColor.SILVER.getWoolData()));
		altNames.put("SILVER_WOOL", new Wool (Material.WOOL, DyeColor.SILVER.getWoolData()));
		altNames.put("CYAN_WOOL", new Wool (Material.WOOL, DyeColor.CYAN.getWoolData()));
		altNames.put("PURPLE_WOOL", new Wool (Material.WOOL, DyeColor.PURPLE.getWoolData()));
		altNames.put("BLUE_WOOL", new Wool (Material.WOOL, DyeColor.BLUE.getWoolData()));
		altNames.put("BROWN_WOOL", new Wool (Material.WOOL, DyeColor.BROWN.getWoolData()));
		altNames.put("GREEN_WOOL", new Wool (Material.WOOL, DyeColor.GREEN.getWoolData()));
		altNames.put("RED_WOOL", new Wool (Material.WOOL, DyeColor.RED.getWoolData()));
		altNames.put("BLACK_WOOL", new Wool (Material.WOOL, DyeColor.BLACK.getWoolData()));

		altNames.put("TALL_GRASS", new LongGrass(GrassSpecies.NORMAL));
		altNames.put("TALL_FERN", new LongGrass(GrassSpecies.FERN_LIKE));
		altNames.put("DEAD_GRASS", new LongGrass(GrassSpecies.DEAD));
		
		altNames.put("BLAZE_EGG", new SpawnEgg (EntityType.BLAZE));
		altNames.put("CREEPER_EGG", new SpawnEgg (EntityType.CREEPER));
		altNames.put("SLIME_EGG", new SpawnEgg (EntityType.SLIME));
		altNames.put("ZOMBIE_EGG", new SpawnEgg (EntityType.ZOMBIE));
		altNames.put("SPIDER_EGG", new SpawnEgg (EntityType.SPIDER));
		altNames.put("CAVE_SPIDER_EGG", new SpawnEgg (EntityType.CAVE_SPIDER));
		altNames.put("ENDERMAN_EGG", new SpawnEgg (EntityType.ENDERMAN));
		altNames.put("GHAST_EGG", new SpawnEgg (EntityType.GHAST));
		altNames.put("PIG_ZOMBIE_EGG", new SpawnEgg (EntityType.PIG_ZOMBIE));
		altNames.put("PIGMAN_EGG", new SpawnEgg (EntityType.PIG_ZOMBIE));
		altNames.put("SILVERFISH_EGG", new SpawnEgg (EntityType.SILVERFISH));
		altNames.put("MAGMA_CUBE_EGG", new SpawnEgg (EntityType.MAGMA_CUBE));
		altNames.put("LAVA_SLIME_EGG", new SpawnEgg (EntityType.MAGMA_CUBE));
		altNames.put("WITHER_EGG", new SpawnEgg (EntityType.WITHER));
		altNames.put("BAT_EGG", new SpawnEgg (EntityType.BAT));
		altNames.put("WITCH_EGG", new SpawnEgg (EntityType.WITCH));
		altNames.put("PIG_EGG", new SpawnEgg (EntityType.PIG));
		altNames.put("SHEEP_EGG", new SpawnEgg (EntityType.SHEEP));
		altNames.put("COW_EGG", new SpawnEgg (EntityType.COW));
		altNames.put("CHICKEN_EGG", new SpawnEgg (EntityType.CHICKEN));
		altNames.put("SQUID_EGG", new SpawnEgg (EntityType.SQUID));
		altNames.put("WOLF_EGG", new SpawnEgg (EntityType.WOLF));
		altNames.put("MUSHROOM_COW_EGG", new SpawnEgg (EntityType.MUSHROOM_COW));
		altNames.put("MOOSHROOM_EGG", new SpawnEgg (EntityType.MUSHROOM_COW));
		altNames.put("OCELOT_EGG", new SpawnEgg (EntityType.OCELOT));
		altNames.put("HORSE_EGG", new SpawnEgg (EntityType.HORSE));
		altNames.put("VILLAGER_EGG", new SpawnEgg (EntityType.VILLAGER));
		altNames.put("HORSE_EGG", new SpawnEgg (EntityType.HORSE));
		
		} catch (Throwable t) {
			System.out.println ("MaterialDataStringer: FAILURE during static initialization: " + t);
		}
    }
	 
	static public Material matchMaterial (String name) {
		Material mat = Material.matchMaterial (name);
		if (mat == null) {
			name = name.toUpperCase();
			
			MaterialData md = altNames.get (name);
			if (md != null) {
				mat = md.getItemType();
				// Bukkit.getLogger().config ("Matched alternative name '" + name + "' for " + mat);
			}
		}
		return mat;
	}
	static public MaterialData matchMaterialData (String name) {
		MaterialData md = altNames.get (name.toUpperCase());
		if (md != null) {
			// Bukkit.getLogger().config ("Matched alternative name '" + name + "' for " + md);
		} else {
			Material mat = Material.getMaterial (name.toUpperCase().trim());
			if (mat != null) {
				// make MaterialData with no data
				md = new MaterialData (mat);
			}						
		}				
		return md;
	}
	
	//DEBUG USE ONLY
	static public Set<String> getAltNames () {
		return altNames.keySet();
	}

	public MaterialDataStringer (int type, final byte data) {
        super(type, data);
    }
	public MaterialDataStringer (final Material type, final byte data) {
        super(type, data);
    }
    public MaterialDataStringer (MaterialData md) {
    	this (md.getItemType(), md.getData());
    }
    // Caution: If name not found, will throw null pointer exception
    public MaterialDataStringer (String name) {
    	this (matchMaterialData (name));   
     	// throw new IllegalArgumentException (name + " not found");
    }

    @Override
    public String toString() {

	    Class<? extends MaterialData> MDC = getItemType().getData();
	    Constructor<? extends MaterialData> ctor;
	    if (MDC == null) {
	    	// no data; use Material toString() to avoid "(0)" appendage
	    	return getItemType().toString();
	    } 
	    // else all this just to get a nice string??
		try {
			ctor = MDC.getConstructor (Material.class, byte.class);
		} catch (NoSuchMethodException ex) {
			try {
				ctor = MDC.getConstructor (int.class, byte.class);
			} catch (NoSuchMethodException nex) {
				throw new AssertionError("no (Material/int,byte) constructor for " + MDC);
			}

			try {
				return ctor.newInstance (getItemTypeId(), getData()).toString();
			} catch (InstantiationException nex) {
				final Throwable t = nex.getCause();
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				}
				if (t instanceof Error) {
					throw (Error) t;
				}
				throw new AssertionError(t);
			} catch (Throwable t) {
				throw new AssertionError(t);
			}		
	
		} catch (SecurityException ex) {
			throw new AssertionError(ex);
		}
		try {
			return ctor.newInstance (getItemType(), getData()).toString();
		} catch (InstantiationException ex) {
            final Throwable t = ex.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new AssertionError(t);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }		
    }
}