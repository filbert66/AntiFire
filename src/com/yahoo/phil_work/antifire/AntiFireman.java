/* 
 * Fireman.java
 * 
 * History: 
 * 27 Mar 2012 : PSW: Created from scratch
 * 12 Apr 2012 : PSW: Ported to new plugin project AntiFire
 * 17 Apr 2012 : PSW : Added EntityCombustByEntityEvent for lightning fire ignition
 */

 package com.yahoo.phil_work.antifire;

 
 import java.util.List;
// import java.util.Random;
 import java.util.logging.Logger;
import java.util.Collection;
import java.util.LinkedList;
//import java.util.regex.Pattern;
import java.util.logging.Level;
import java.lang.Character;
import java.util.NoSuchElementException;
 
// import net.minecraft.server.WorldServer;
// import net.minecraft.server.WorldManager;
//import net.minecraft.server.MinecraftServer;

import org.bukkit.event.entity.EntityCombustEvent; // when a player/entity is burnt by fire
import org.bukkit.event.entity.EntityCombustByBlockEvent; // entity burnt by lava or burning block
import org.bukkit.event.entity.EntityCombustByEntityEvent;// entity burnt by lightning, blaze, or FIRE_ASPECT weapon
import org.bukkit.event.block.BlockIgniteEvent; // when a block is lit
import org.bukkit.event.block.BlockBurnEvent; // when a block is destroyed
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import	org.bukkit.block.Block;
import	org.bukkit.block.BlockState;
//import org.bukkit.block.ContainerBlock;
import org.bukkit.material.MaterialData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
// import org.bukkit.entity.Entity;
// import org.bukkit.entity.Fireball;
// import org.bukkit.entity.TNTPrimed;
//import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;

//import org.bukkit.inventory.ItemStack;
//import org.bukkit.craftbukkit.CraftServer;
//import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.Bukkit;

import com.yahoo.phil_work.BlockId;
import com.yahoo.phil_work.BlockIdList;
import com.yahoo.phil_work.antifire.FireLogEntry;
import com.yahoo.phil_work.antifire.AntifireLog;

public class AntiFireman implements Listener
{
	private final AntiFire plugin;
	private	BlockIdList FireResistantList;

	public AntiFireman (AntiFire instance)
	{
		this.plugin = instance;
	}

	public void initConfig () 
	{
		boolean writeDefault = false;
		
		// customConfigurationFile = new File(getDataFolder(), "users.yml");

		if ( !plugin.getDataFolder().exists()) {
			writeDefault = true;
			plugin.getConfig().options().copyDefaults(true);
			plugin.log.info ("No config found in " + plugin.pdfFile.getName() + "/; writing defaults");
		}
		
		// Worlds must be listed below the following entries
		plugin.getConfig().getStringList ("nerf_fire.nostartby.lightning");  
		plugin.getConfig().getStringList ("nerf_fire.nostartby.lava"); 
		plugin.getConfig().getStringList ("nerf_fire.nostartby.player");  // should be user-specific (Permissions!), or could be region-based
		plugin.getConfig().getStringList ("nerf_fire.logstart"); // logs who and were in console

		plugin.getConfig().getBoolean ("nerf_fire.nostartby.op", false);  // OPs can do by default
		
		plugin.getConfig().getStringList ("nerf_fire.no	to.block"); 
		
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.player.fromlava");		
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.player.fromfire");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.nonplayer.fromlava");		// Break out to mob, item, painting, player, drops
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.nonplayer.fromfire");

		plugin.getConfig().getStringList ("nerf_fire.nospread"); // by adjacent fire
		
		plugin.getConfig().getBoolean ("nerf_fire.whitelist", false);  // blacklist is default

		FireResistantList = new BlockIdList (this.plugin);
		FireResistantList.loadBlockList ("nerf_fire.blocklist");
		
		if (writeDefault)
			plugin.saveDefaultConfig();
	}
	
	public void printConfig() 
	{
		for (String key : plugin.getConfig().getConfigurationSection ("nerf_fire").getKeys(true)) {
			key = "nerf_fire." + key;
			
			if	(plugin.getConfig().isConfigurationSection (key))
				continue;
			
			else if (plugin.getConfig().isInt(key))
				plugin.log.config (key + ": " + plugin.getConfig().getInt(key));
			else if (plugin.getConfig().isBoolean(key)) 
				plugin.log.config (key + ": " + plugin.getConfig().getBoolean(key));
			else if (key.indexOf ("blocklist") > 0) 
				FireResistantList.printList ();
			else if (plugin.getConfig().isString(key))
			{
				plugin.log.config (key + ": " + plugin.getConfig().getString(key));
			}
			else if (plugin.getConfig().isList (key)) {
				plugin.log.config (key + ": " + plugin.getConfig().getList(key)); 
			} else 
				plugin.log.config ("Fireman: Unrecognized config key: " + key);
		}
	}

	private boolean ifStringContains (String s, String pattern)
	{
		int found = s.indexOf (pattern);
		boolean b = false;
		
		//plugin.log.finest ("ifStringContains (" + s + "," + pattern + "). Initial found: " + found);
		
		if (found != -1) {	
			if (found + pattern.length() == s.length()) {  // pattern at end
				// Was it at start? 
				if (found == 0)
					b = true; // at start
				else { // was previous char word boundary?
					Character c = new Character (s.charAt (found - 1));
					if ( !(c.isLetterOrDigit(c) || c.equals('_')))  // word border
						b= true;
				}
			} else {
				Character c = new Character (s.charAt(found + pattern.length()));
				if (c.isLetterOrDigit(c) || c.equals('_')) { // word continues!
					b= false;
					//plugin.log.finer ("'" + pattern + "' is only a partial word within '" + s + "', found char " + c);
					
					return ifStringContains (s.substring (found + pattern.length()), pattern); // keep looking
				}
				else {
					b= true;
					//plugin.log.finer ("found '" + pattern + "' within '" + s + "'");
				}
			}
		}
		// else plugin.log.finer ("'" + pattern + "' not in '" + s + "'");
		
		return b;
	}
	
	/* 
	 * Created to allow admins to define configs asList or asString, and search either way.
	 */
	private boolean ifConfigContains (String configkey, String pattern) 
	{		
		if (plugin.getConfig().isString(configkey)) {
			String configVal = plugin.getConfig().getString(configkey);
			// Can't just call .contains() since world_the_end contains 'world'
			//   So match the key with the desired pattern plus a non-word(-alphanum) boundary either side
			// Couldn't get darn regex in Java to work, though worked in Perl. ArgH!
//			String regex = "(\\W|\\A)" + pattern + "(\\W|\\z)";
//			boolean b = Pattern.matches (regex, configVal);
//			plugin.log.finer ("regex '" + regex + "' search within '" + configVal + "' = " + b);

			return ifStringContains (configVal, pattern);

		} else if (plugin.getConfig().isList (configkey)) {
			// plugin.log.finer ("Testing '" + configkey + "' as a list for '" + pattern + "'");
			return plugin.getConfig().getList(configkey).contains (pattern);
		} else {
			plugin.log.fine ("ifConfigContains: unexpected configkey '" + configkey + "'");
			return false;
		}
	}

	/*
	 * Begin Event Handlers
	 */
		
	@EventHandler (ignoreCancelled = true)
	public void onFireStart (BlockIgniteEvent event)
	{	
		Player p = event.getPlayer();
		String worldName = event.getBlock().getWorld().getName();

		boolean disallow = false;
		Level loglevel = Level.FINE;
		
		switch (event.getCause()) {
			case LAVA:
				disallow = ifConfigContains ("nerf_fire.nostartby.lava", worldName);
				break;
			case FLINT_AND_STEEL:
				disallow = ifConfigContains ("nerf_fire.nostartby.player", worldName);
				if (disallow) {
					if (p.isOp() && !plugin.getConfig().getBoolean ("nerf_fire.nostartby.op", false)) {
						disallow = false;
					} else if (p.isPermissionSet ("antifire.nerf_fire.startfire") && 
							   p.hasPermission ("antifire.nerf_fire.startfire") ) {
						disallow = false;
						plugin.log.fine (p.getDisplayName() + " has permission .startfire. Overriding nostartby.player");
					}
				}
				if (disallow) 			
					p.sendMessage (plugin.pdfFile.getName() + " says you don't have fire start permissions");
				loglevel = Level.INFO;
				break;
			case LIGHTNING:// need to test
				plugin.log.fine ("Lightning strike starting fire at " + event.getBlock().getLocation());
				disallow = ifConfigContains ("nerf_fire.nostartby.lightning", worldName);
				break;
			case SPREAD:
				disallow = ifConfigContains ("nerf_fire.nospread", worldName);
				loglevel = Level.FINER;
				break;

			default:
				plugin.log.warning ("Unknown fire ignition cause " + event.getCause());
				// what about fire aspect weapons, and fireball explosions?
				return;
		}

		if (disallow) {
			plugin.log.log (loglevel, "blocked fire start by " + (p != null ? p.getDisplayName():event.getCause()) + " in " + worldName);
			event.setCancelled (true);
		}
		else {  // check FireresistantList first
			boolean whitelist = plugin.getConfig().getBoolean ("nerf_fire.whitelist");
			Location loc = event.getBlock().getLocation();
			Block block = getBurningBlockFrom(loc);
			if (block == null)
				block = event.getBlock();

			BlockId b = new BlockId (block.getTypeId(),block.getData());
				
			if ( !FireResistantList.isEmpty())
				// Checking for whitelist (list of IDs to burn) or blacklist (IDs to not burn)
				if ((whitelist && !FireResistantList.contains (b)) || 
					( !whitelist && FireResistantList.contains (b)) )
				{
					plugin.log.fine ("blocked fire start on resistant block type " + block.getType() + " in " + worldName);
					if (p != null)
						p.sendMessage (plugin.pdfFile.getName() + " says block " + 
									   block.getType() + (b.hasData() ? ":"+ block.getData():"") + " is fire resistant");
					event.setCancelled (true);
					return;
				}	
				else {
					plugin.log.fine (block.getType() + (whitelist ? "": " not") + " in nerf_fire.blocklist");
				}

			
			if (event.getCause() != BlockIgniteEvent.IgniteCause.SPREAD)
			{
				String starter = (p != null ? p.getDisplayName() : event.getCause().toString());
				
				plugin.fireLog.add (starter, loc);	// maybe should log in all cases; yes, but not to logger
				if (ifConfigContains ("nerf_fire.logstart", worldName)) {
					plugin.log.info (plugin.fireLog.list.getLast().toStringNoDate());  // logger already includes date
					plugin.log.fine ("Found " + worldName + " in nerf_fire.logstart");
				}
			}
			else
				plugin.log.log (loglevel, "Allowing fire start by " + (p != null ? p.getDisplayName():event.getCause()) + " in " + worldName);
		}
	}		
	// Fire burns above, or if something is there, along the side of a block
	// Can return an "incorrect" block if more than one block is adjacent to this airspace. 
	private Block getBurningBlockFrom (final Location loc) {
		double x = loc.getX(), y = loc.getY(), z= loc.getZ();
		
		Location test = new Location (loc.getWorld(), x, y, z);
		
		Block b = test.getBlock ();
		plugin.log.finest ("Checking burner of type " + b.getType() + " at " + test);
		if (b.getType() !=  Material.AIR)
			return b;
		
		y = loc.getY() - 1d;  // underneath
		test = new Location (loc.getWorld(), x, y, z);
		b = test.getBlock ();
		plugin.log.finest ("Checking burner of type " + b.getType() + " under at " + test);
		if (b.getType() !=  Material.AIR)
			return b;
		y += 1;
		
		// Look around. Not sure which might have started it, but look on X first
		for (x = loc.getX() - 1d; x <= (loc.getX() + 1d); x+=2d) {
			test = new Location (loc.getWorld(), x, y, z);
			b = test.getBlock ();
			plugin.log.finest ("Checking burner of type " + b.getType() + " at X " + test);
			if (b.getType() !=  Material.AIR)
				return b;
		}
		x = loc.getX();

		for (z = loc.getZ() - 1d; z <= (loc.getZ() + 1d); z+=2d) {
			test = new Location (loc.getWorld(), x, y, z);
			b = test.getBlock ();
			plugin.log.finest ("Checking burner of type " + b.getType() + " at Z " + test);
			if (b.getType() !=  Material.AIR)
				return b;
		}
		z = loc.getZ();
		
		// Check above?
		y = loc.getY() + 1d;  // underneath
		test = new Location (loc.getWorld(), x, y, z);
		b = test.getBlock ();
		plugin.log.finest ("Checking burner of type " + b.getType() + " above " + test);
		if (b.getType() !=  Material.AIR)
			return b;
		y -= 1;
		
		plugin.log.fine ("Unable to find ignition source for " + loc);
		return null;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onFireDestroyBlock (BlockBurnEvent event)
	{
		String worldName = event.getBlock().getWorld().getName();
				
		if (ifConfigContains ("nerf_fire.nodamageto.block", worldName)) {
			plugin.log.finer ("stopped block (" + event.getBlock().getType() + ") destruction by fire in " + worldName);
			event.setCancelled (true);
			// will this put the fire out, or will it burn indefinitely like netherack?
			// Burns indefinitely.
		}
	}	
	
	@EventHandler (ignoreCancelled = true)
	public void onEntityCombust (EntityCombustEvent event) {
		Entity e = event.getEntity();
		String worldName = e.getLocation().getWorld().getName();

		if (e instanceof Player) {
			Player p = (Player)e;
			// Presume it's a burning block; don't know how to check if a block is on fire.
			if (ifConfigContains ("nerf_fire.nodamageto.player.fromfire", worldName)) {
				plugin.log.fine ("stopped combustion to player " + p.getDisplayName());
				event.setDuration (0);  // on fire for zero
				event.setCancelled (true);
			}
		} else { // Catches mobs from catching fire from sunlight!!
			// Presume it's a burning block; don't know how to check if a block is on fire.
			if (ifConfigContains ("nerf_fire.nodamageto.nonplayer.fromfire", worldName)) {
				plugin.log.fine ("stopped combustion to " + e.getType() );
				event.setDuration (0);
				event.setCancelled (true);
			}
		}
	}
	/*
	 *  WHat other event should I be handling? EntityCombustByEntityEvent for lightning, which can catch entities on fire
	 */
	@EventHandler (ignoreCancelled = true)
	public void onFireBurnEntity (EntityCombustByBlockEvent event)
	{
		Entity e = event.getEntity();
		String worldName = e.getLocation().getWorld().getName();
		Block burner = event.getCombuster();
		
		if (burner == null) {
			onEntityCombust ((EntityCombustEvent) event);
			return;
		}
		
		plugin.log.fine ("Entity " + e.getType() + " combust by " + burner.getType());
		
		if (e instanceof Player) {
			Player p = (Player)e;
			// Checked NMS movement code. Lava burning sets source to null!
			if (burner.getType() == Material.LAVA &&
				ifConfigContains ("nerf_fire.nodamageto.player.fromlava", worldName) ) {
				plugin.log.fine ("stopped combust to player " + p.getDisplayName() + " from lava in " + worldName);
				event.setDuration (0);
				event.setCancelled (true);
				return;
			}
			
		} else {
			if (burner.getType() == Material.LAVA &&
				ifConfigContains ("nerf_fire.nodamageto.nonplayer.fromlava", worldName) ) {
				plugin.log.fine ("stopped combust to " + e.getType() + " from lava in " + worldName);
				event.setDuration (0);
				event.setCancelled (true);
				return;
			}
		}		
	}	
	
	// EntityblockEvent: Handles Lighting ignition source
	@EventHandler (ignoreCancelled = true)
	public void onEntityBurnEntity (EntityCombustByEntityEvent event)
	{
		Entity e = event.getEntity();
		String worldName = e.getLocation().getWorld().getName();
		Entity burner = event.getCombuster();
		// The combuster can be a WeatherStorm a Blaze, or an Entity holding a FIRE_ASPECT enchanted item.
				
		plugin.log.fine ("Entity " + e.getType() + " combust by " + burner.getType()); // move after check below
		if (burner.getType() != EntityType.LIGHTNING) {
			plugin.log.finer ("Burn entity by non-lighting: " + burner.getType());
			return; 			// Only looking for lightning
		}
		
		if (e instanceof Player) {
			Player p = (Player)e;
			if (ifConfigContains ("nerf_fire.nodamageto.player.fromlightning", worldName) ) {
				plugin.log.fine ("stopped combust to player " + p.getDisplayName() + " from lightning in " + worldName);
				event.setDuration (0);
				event.setCancelled (true);
				return;
			}
			
		} else {
			if (ifConfigContains ("nerf_fire.nodamageto.nonplayer.fromlightning", worldName) ) {
				plugin.log.fine ("stopped combust to " + e.getType() + " from lightning in " + worldName);
				event.setDuration (0);
				event.setCancelled (true);
				return;
			}
		}		
	}	
	
	/* 
	 * Apparent sequence of events: 
	 *    EntityDamageEvent (not yet on fire)
	 *    EntityCombustEvent ()
	 *    EntityDamageByBlockEvent (to Creeper by LAVA)- cancelled
	 *    EntityCombustByBlockEvent (to Creeper by null) -- still called!
	 *    cancel both events, and still get damage
	 */
	@EventHandler (ignoreCancelled = true)
	public void  onFireDamageEntity (EntityDamageEvent event) {
		Entity damagee = event.getEntity();
		String worldName = damagee.getLocation().getWorld().getName();
		boolean makeLog = true;
		boolean nerfdamage = false;
		
	    switch (event.getCause()) {
		    case LAVA:
				nerfdamage = (damagee instanceof Player) ? 
				    ifConfigContains ("nerf_fire.nodamageto.player.fromlava", worldName) :
					ifConfigContains ("nerf_fire.nodamageto.nonplayer.fromlava", worldName) ;
				break;

			case FIRE_TICK: // this gets called from EntityDamageEvent (not ByBlock)
				plugin.log.finest (damagee.getType() + " damaged by fire tick");
				makeLog = false;
			case FIRE:
				nerfdamage =  (damagee instanceof Player) ? 
					ifConfigContains ("nerf_fire.nodamageto.player.fromfire", worldName) :
					ifConfigContains ("nerf_fire.nodamageto.nonplayer.fromfire", worldName) ;
				break;
				
			default:
				makeLog = false;
				break;
		} 
		if (nerfdamage) {
			if (damagee.getFireTicks() > 0) {
				plugin.log.fine (damagee.getType() + " was on fire; nerfed");
				damagee.setFireTicks(0); 
			}
			else plugin.log.finer (event.getDamage() + " damage to " + damagee.getType() + " but was NOT on fire");

			event.setDamage (0); // just to make sure
			event.setCancelled (true);
		}
		
		if (makeLog) plugin.log.finer ("nerf_fire: caught " + event.getCause() + " damaging " + damagee.getType());
	}
	@EventHandler (ignoreCancelled = true)
	public void  onFireblockDamageEntity (EntityDamageByBlockEvent event) {
		onFireDamageEntity ((EntityDamageEvent)event);
	}

	
}

