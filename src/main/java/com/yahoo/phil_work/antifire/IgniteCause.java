package com.yahoo.phil_work.antifire;


import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.EntityType;


public enum IgniteCause {
	LAVA ("lava"),
	FLINT_AND_STEEL ("flint"),
	SPREAD ("spread"),
	LIGHTNING ("lightning"),
	FIRECHARGE ("charge"), // split to fire charge and fire ball
	FIREBALL ("ball"),
	// Now explosion causes
	ENDER_CRYSTAL ("crystal"),
	CREEPER ("creeper"),
	MINECART_TNT ("cart"),	// place before below for string matching
	PRIMED_TNT ("tnt"),
	WITHER ("wither"),
	WITHER_SKULL ("skull"),
	ENDER_DRAGON ("dragon"),
	
	UNKNOWN ("unknown"),
	;
	
	private final String configName;
	private final static Map<String, IgniteCause> BY_NAME = new HashMap();

	private IgniteCause (final String name) {
		configName = name;
	}
	public static IgniteCause getIgniteCause (final String name) {
		return BY_NAME.get (name);
	}
	public static IgniteCause matchIgniteCause (final String name) {
		if (name == null)
			return null;

		IgniteCause result = null;
		try {
			result = IgniteCause.valueOf (name); // direct match to enum name
		} catch (IllegalArgumentException e) {}
								
		if (result != null)
			return result;
		
		String adjusted = name.toLowerCase();
		result = getIgniteCause (adjusted); // by short name
		if (result != null)
			return result;
		
		for (IgniteCause ic : values()) {
			if (adjusted.indexOf (ic.configName) != -1)
				return ic;
		}
		return null;	
	}
	
	public static IgniteCause getIgniteCause (EntityType et) {
		IgniteCause result;
		switch (et) {
			case SMALL_FIREBALL: result = IgniteCause.FIRECHARGE; break;
			case FIREBALL: result = IgniteCause.FIREBALL; break;
			case WITHER_SKULL: result = IgniteCause.WITHER_SKULL; break;
			case PRIMED_TNT:   result = PRIMED_TNT; break;
			case MINECART_TNT: result = MINECART_TNT; break;
			case CREEPER:      result = CREEPER; break;
			case ENDER_DRAGON: result = ENDER_DRAGON; break;
			case WITHER:       result = WITHER; break; 
			case ENDER_CRYSTAL:result = ENDER_CRYSTAL; break; 
			case LIGHTNING:    result = LIGHTNING; break;
			default: result = UNKNOWN; break;
		}
		return result;
	}
	
	static {
		for (IgniteCause cause : values()) {
			BY_NAME.put (cause.configName, cause);
		}
	}
} 