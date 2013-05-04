/* 
 * Fireman.java
 * 
 * History: 
 * 27 Mar 2012 : PSW: Created from scratch
 * 12 Apr 2012 : PSW: Ported to new plugin project AntiFire
 * 17 Apr 2012 : PSW : Added EntityCombustByEntityEvent for lightning fire ignition
 *  8 May 2012 : PSW : Use new pluginName and colors param.
 * 14 Aug 2012 : PSW : Added configurable firestart logging;
 *                   : Added nerf_fire.nostartby.explosion
 * 21 Aug 2012 : PSW : Added param to printConfig()
 *                   : Added use of nerf_fire.logflushsecs and auto-flush feature
 * 27 Nov 2012 : PSW : Added nostartby.fireball, logstart.fireball
 * 30 Dec 2012 : PSW : Check FireResistantList in BlockBurnEvent; use getTargetBlock()
 * 20 Mar 2013 : PSW : Changed logging level on noLogging;
 *                     onFireDestroyBlock: added check to prevent burnt block if new location is fire resistant. 
 *             : PSW : Handle new BlockIgniteEvent.getIgnitingBlock() and event.getIgnitingEntity()
 * 5  Apr 2013 : PSW : Add new nerf_fire.wooddropscharcoal; 
 *					   Handle new ENDER_CRYSTAL and EXPLOSION causes.
 *                     nodamageto.player.fromfire now works when entities light players on fire
 *                     Add nerf_fire.noburnentityby.player, permission antifire.burnentity.
 */

 package com.yahoo.phil_work.antifire;

 
 import java.util.List;
 import java.util.Random;
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
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.EventPriority;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import	org.bukkit.block.Block;
import	org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.CoalType;
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

import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;


import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

import com.yahoo.phil_work.BlockId;
import com.yahoo.phil_work.BlockIdList;
import com.yahoo.phil_work.antifire.FireLogEntry;
import com.yahoo.phil_work.antifire.AntifireLog;

public class AntiFireman implements Listener
{
	private final AntiFire plugin;
	public	BlockIdList FireResistantList;
	private static final Random rng = new Random();
	
	// Metrics
	public int logEntries = 0, fireProofed = 0, nerfedStart=0;
	
	
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
		plugin.getConfig().getStringList ("nerf_fire.nostartby.explosion"); 
		plugin.getConfig().getStringList ("nerf_fire.nostartby.fireball");  
		plugin.getConfig().getStringList ("nerf_fire.nostartby.crystal");

		if (plugin.getConfig().isString ("nerf_fire.logstart") || plugin.getConfig().isList ("nerf_fire.logstart")) // old style 
		{
			List <String> logstart = plugin.getConfig().getStringList ("nerf_fire.logstart"); // logs who and were in console
			plugin.log.config ("old style logstart; setting .lava, .player, .lightning to the same");
			
			// Set values that are now checked by new code
			plugin.getConfig().set ("nerf_fire.logstart.lava", logstart);
			plugin.getConfig().set ("nerf_fire.logstart.player", logstart);
			plugin.getConfig().set ("nerf_fire.logstart.lightning", logstart);					
			plugin.getConfig().set ("nerf_fire.logstart.explosion", logstart);
			plugin.getConfig().set ("nerf_fire.logstart.crystal", logstart);
		} else { // new style
			plugin.log.fine ("new style logstart found");

			plugin.getConfig().getStringList ("nerf_fire.logstart.lava");
			plugin.getConfig().getStringList ("nerf_fire.logstart.player");
			plugin.getConfig().getStringList ("nerf_fire.logstart.lightning");
			plugin.getConfig().getStringList ("nerf_fire.logstart.explosion");
			plugin.getConfig().getStringList ("nerf_fire.logstart.crystal");
		}

		plugin.getConfig().getBoolean ("nerf_fire.nostartby.op", false);  // OPs can do by default
		
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.block"); 
		
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.player.fromlava");		
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.player.fromfire");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.nonplayer.fromlava");		// Break out to mob, item, painting, player, drops
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.nonplayer.fromfire");

		plugin.getConfig().getStringList ("nerf_fire.nospread"); // by adjacent fire
		plugin.getConfig().getStringList ("nerf_fire.wooddropscharcoal"); 
		
		plugin.getConfig().getBoolean ("nerf_fire.whitelist", false);  // blacklist is default
		
		plugin.getConfig().getInt ("nerf_fire.logflushsecs"); // default in config.yml

		FireResistantList = new BlockIdList (this.plugin);
		FireResistantList.loadBlockList ("nerf_fire.blocklist");
		
		if (writeDefault)
			plugin.saveDefaultConfig();
	}
	
	private void printMsg (CommandSender requestor, String msg)
	{
		if (requestor == null)
			plugin.log.config (msg);
		else if (requestor instanceof Player) {
			int colon = msg.indexOf(':');
			
			requestor.sendMessage (ChatColor.BLUE + msg.substring(0,colon+1) + 
								   ChatColor.GRAY + msg.substring (colon + 1, msg.length()) );
		}
		else { // server console, and doesn't look good in server.log
			requestor.sendMessage (msg);
		}
	}
	public boolean printConfig() 
	{
		return printConfig (null);
	}
	
	public boolean printConfig(CommandSender requestor) 
	{
		for (String key : plugin.getConfig().getConfigurationSection ("nerf_fire").getKeys(true)) {
			key = "nerf_fire." + key;
			
			if	(plugin.getConfig().isConfigurationSection (key))
				continue;
			
			else if (plugin.getConfig().isInt(key))
				printMsg (requestor, key + ": " + plugin.getConfig().getInt(key));
			else if (plugin.getConfig().isBoolean(key)) 
				printMsg (requestor, key + ": " + plugin.getConfig().getBoolean(key));
			else if (key.indexOf ("blocklist") > 0) 
				FireResistantList.printList (requestor);
			else if (plugin.getConfig().isString(key))
			{
				printMsg (requestor, key + ": " + plugin.getConfig().getString(key));
			}
			else if (plugin.getConfig().isList (key)) {
				printMsg (requestor, key + ": " + plugin.getConfig().getList(key)); 
			} else 
				plugin.log.warning ("Fireman: Unrecognized config key: " + key);
		}
		return true;
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
	public boolean ifConfigContains (String configkey, String pattern) 
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
			//plugin.log.finer ("Testing '" + configkey + "' as a list for '" + pattern + "'");
			return plugin.getConfig().getList(configkey).contains (pattern);
		} else {
			
			// plugin.log.finer ("ifConfigContains: configkey '" + configkey + "' not in config.yml");
			return false;
		}
	}
	
	/*
	 * Begin Event Handlers
	 */
		
	// Handle new (1.5.1) event.getIgnitingBlock() and event.getIgnitingEntity
	// 1.5.1 also calls now for lightning, fireballs 
	@EventHandler (ignoreCancelled = true)
	public void onFireStart (BlockIgniteEvent event)
	{	
		Player p = event.getPlayer();
		String worldName = event.getBlock().getWorld().getName();
		Block target = null;
		
		boolean disallow = false;
		boolean shouldLog = false;
		Level loglevel = Level.FINE;
		
		switch (event.getCause()) {
			case LAVA:
				disallow = ifConfigContains ("nerf_fire.nostartby.lava", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.lava", worldName);
				break;
				
			case FIREBALL: 
				if (p == null) {  // docs say only by player, but can happen with plugins starting them
					disallow = ifConfigContains ("nerf_fire.nostartby.fireball", worldName);
					shouldLog = ifConfigContains ("nerf_fire.logstart.fireball", worldName);
					break;
				} // else it were a player and fall through
			case FLINT_AND_STEEL:
				disallow = ifConfigContains ("nerf_fire.nostartby.player", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.player", worldName);

				if (disallow) {
					if (p.isOp() && !plugin.getConfig().getBoolean ("nerf_fire.nostartby.op", false)) {
						disallow = false;
					} else if (p.isPermissionSet ("antifire.startfire") && p.hasPermission ("antifire.startfire")) {
						disallow = false;
						plugin.log.fine (p.getDisplayName() + " has permission .startfire. Overriding nostartby.player");
					}
				}
				if (disallow)  {			
					p.sendMessage (plugin.pluginName + " says you don't have fire start permissions");
					loglevel = Level.INFO;
				}
				else if (event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) // if fireball, he might have looked away
					target = p.getTargetBlock(null, 5); // what is he trying to ignite w/in clickable distance?
				break;
				
			case LIGHTNING:// need to test
				plugin.log.fine ("Lightning strike starting fire at " + event.getBlock().getLocation());
				disallow = ifConfigContains ("nerf_fire.nostartby.lightning", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.lightning", worldName);
				break;
				
			case SPREAD:
				disallow = ifConfigContains ("nerf_fire.nospread", worldName);
				// shouldLog = ifConfigContains ("nerf_fire.logstart.spread", worldName);
				loglevel = Level.FINER;
				break;

			case EXPLOSION:
				plugin.log.finer ("Explosion starting fire at " + event.getBlock().getLocation());
				disallow = ifConfigContains ("nerf_fire.nostartby.explosion", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.explosion", worldName);
				break;
			
			case ENDER_CRYSTAL:
				plugin.log.fine ("Ender Crystal starting fire at " + event.getBlock().getLocation());
				disallow = ifConfigContains ("nerf_fire.nostartby.crystal", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.crystal", worldName);
				break;			
			
			default:
				plugin.log.warning ("Unknown fire ignition cause: " + event.getCause() + ". Time for a plugin update?");
				// what about fire aspect weapons, and fireball explosions?
				return;
		}

		if (disallow) {
			plugin.log.log (loglevel, "blocked fire start by " + (p != null ? p.getDisplayName():event.getCause()) + " in " + worldName);
			event.setCancelled (true);
			this.nerfedStart++;
		}
		else {  // check FireresistantList first
			boolean whitelist = plugin.getConfig().getBoolean ("nerf_fire.whitelist");
			Location loc = event.getBlock().getLocation();
			Block block = (target != null ? target : getBurningBlockFrom (loc));
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
						p.sendMessage (plugin.pluginName + " says block " + 
									   block.getType() + (b.hasData() ? ":"+ block.getData():"") + " is fire resistant");
					event.setCancelled (true);
					this.fireProofed++;
					return;
				}	
				else {
					plugin.log.finer (block.getType() + (whitelist ? "": " not") + " in nerf_fire.blocklist");
				}

			
			if (shouldLog)
			{
				String starter = (p != null ? p.getDisplayName() : event.getCause().toString());
				
				if (plugin.fireLog.add (starter, loc) > 10) // don't flush to disk until we get 10 events
				{
					plugin.flushLog (null, plugin.getConfig().getInt ("nerf_fire.logflushsecs"));
				}
				plugin.log.info (plugin.fireLog.list.getLast().toStringNoDate(false));  // logger already includes date
				// plugin.log.fine ("Found " + worldName + " in nerf_fire.logstart");
				
				logEntries++;
			}
			else
				plugin.log.log (loglevel, "Allowing fire start by " + (p != null ? p.getDisplayName():event.getCause()) + " in " + worldName);
		}
	}		
		
	/****
	 * getBurningBlockFrom(loc) returns, for a given AIR block about to be set afire, the block that is "burning"
	 *   NOT the same as the block that lit this burning block, since fire can jump 3 blocks!
	 * Fire burns above, or if something is there, along the side of a block
	 * Can return an "incorrect" block if more than one block is adjacent to this airspace. 
	 */
	private Block getBurningBlockFrom (final Location loc) {
		double x = loc.getX(), y = loc.getY(), z= loc.getZ();
		
		Location test = new Location (loc.getWorld(), x, y, z);
		
		Block b = test.getBlock ();
		Material m = b.getType();
		plugin.log.finest ("Checking burner of type " + m + " at " + test);
		if (m !=  Material.AIR && m != Material.FIRE)
			return b;
		
		y = loc.getY() - 1d;  // underneath
		test = new Location (loc.getWorld(), x, y, z);
		b = test.getBlock ();
		 m = b.getType();
		plugin.log.finest ("Checking burner of type " + m + " under at " + test);
		if (m !=  Material.AIR && m != Material.FIRE)
			return b;
		y += 1;
		
		// Look around. Not sure which might have started it, but look on X first
		for (x = loc.getX() - 1d; x <= (loc.getX() + 1d); x+=2d) {
			test = new Location (loc.getWorld(), x, y, z);
			b = test.getBlock ();
			 m = b.getType();
			plugin.log.finest ("Checking burner of type " + m + " at X " + test);
			if (m !=  Material.AIR && m != Material.FIRE)
				return b;
		}
		x = loc.getX();

		for (z = loc.getZ() - 1d; z <= (loc.getZ() + 1d); z+=2d) {
			test = new Location (loc.getWorld(), x, y, z);
			b = test.getBlock ();
			 m = b.getType();
			plugin.log.finest ("Checking burner of type " + m + " at Z " + test);
			if (m !=  Material.AIR && m != Material.FIRE)
				return b;
		}
		z = loc.getZ();
		
		// Check above?
		y = loc.getY() + 1d;  // underneath
		test = new Location (loc.getWorld(), x, y, z);
		b = test.getBlock ();
		 m = b.getType();
		plugin.log.finest ("Checking burner of type " +m + " above " + test);
		if (m !=  Material.AIR && m != Material.FIRE)
			return b;
		y -= 1;
		
		plugin.log.fine ("Unable to find ignition source for " + loc);
		return null;
	}
	
	// Always deletes supplied block. 
	// If config is set, replaces LOGs with 0-3 charcoal.
	// Chances of any drop: 50%. Chances of 1-3: even.
	private void burnUpBlock (Block b) {
		Material m = b.getType();
		World w = b.getWorld();
		String worldName = w.getName();

		plugin.log.finer ("AF: burning up block of type " + m);

		if (m == Material.LOG && ifConfigContains ("nerf_fire.wooddropscharcoal", worldName)) {
			int amount = rng.nextInt(2) * (1+ rng.nextInt (3));
			
			if (amount > 0) {
				MaterialData mat = new MaterialData (Material.COAL, CoalType.CHARCOAL.getData());
				w.dropItem (b.getLocation(), mat.toItemStack (amount));
				plugin.log.fine ("AF: dropping " + amount + " charcoal at " + b.getLocation());
			}
		}
				
		b.setType (Material.AIR);
	}

	
	@EventHandler (ignoreCancelled = true)
	public void onFireDestroyBlock (BlockBurnEvent event)
	{
		String worldName = event.getBlock().getWorld().getName();
		boolean stopEvent = false;
				
		if (ifConfigContains ("nerf_fire.nodamageto.block", worldName)) {
			plugin.log.finer ("stopped block (" + event.getBlock().getType() + ") destruction by fire in " + worldName);
			event.setCancelled (true);
			// will this put the fire out, or will it burn indefinitely like netherack?
			// Burns indefinitely.
		}
		else if ( !FireResistantList.isEmpty()) {
			boolean whitelist = plugin.getConfig().getBoolean ("nerf_fire.whitelist");
			Block block = event.getBlock();
			BlockId b = new BlockId (block.getTypeId(), block.getData());

			// Checking for whitelist (list of IDs to burn) or blacklist (IDs to not burn)
			if ((whitelist && !FireResistantList.contains (b)) || 
				( !whitelist && FireResistantList.contains (b)) )
			{
				plugin.log.fine ("blocked fire destroying a resistant block type " + block.getType() + " in " + worldName);
				event.setCancelled (true); // stops breaking, but not fire, on resistant block
				
				Block fireLoc = getFireBlockFrom (block.getLocation());
				if (fireLoc != null) {
					// found an adjacent burning location
					plugin.log.finer ("stopped fire on resistant block type at " + fireLoc.getLocation());
					fireLoc.setType (Material.AIR);
				}	
			}	
			else  // have fire resistance, burning block is not resistant, but block underneath might be
			{
				// remove non-resistant block so following call doesn't return this one
				burnUpBlock (block);
				
				// Check to see if we should allow spread of the displaced block
				if ( ifConfigContains ("nerf_fire.nospread", worldName)) {
					event.setCancelled (true); // don't allow burnt block to become FIRE
					return;
				}

				Block newBurner = getBurningBlockFrom (block.getLocation());
				if (newBurner != null) {
					// this block would be burning by NMS default handling
					b = new BlockId (newBurner.getTypeId(), newBurner.getData());
					
					if ((whitelist && !FireResistantList.contains (b)) || 
						( !whitelist && FireResistantList.contains (b)) )
					{
						event.setCancelled (true); // stop processing and setting this on fire
						plugin.log.fine ("blocked fire restarting on a resistant block type " + newBurner.getType() + " in " + worldName);
					}
				}	
			}
		// else damage blocks, no fire resistance, but check for nospread
		} else if ( ifConfigContains ("nerf_fire.nospread", worldName)) {
			event.setCancelled (true); // prevent any spread by default NMS code
			burnUpBlock (event.getBlock());
			return;
		}	
		
		// Keep processing event, but check for drops
		burnUpBlock (event.getBlock());
	}	
	
	// Fire burns above, or if something is there, along the side of a block
	// May return a fire block that is also adjacent to a burnable block, but regardless it is adjacent to supplied block. 
	private Block getFireBlockFrom (final Location loc) {
		double x = loc.getX(), y = loc.getY(), z= loc.getZ();
		
		Location test = new Location (loc.getWorld(), x, y, z);
		
		Block b = test.getBlock ();
		plugin.log.finest ("Checking for fire at type " + b.getType() + " at " + test);
		if (b.getType() ==  Material.FIRE)
			return b;
		
		y = loc.getY() - 1d;  // underneath
		test = new Location (loc.getWorld(), x, y, z);
		b = test.getBlock ();
		plugin.log.finest ("Checking for fire at type " + b.getType() + " under at " + test);
		if (b.getType() ==  Material.FIRE)
			return b;
		y += 1;
		
		// Look around. Not sure which might have started it, but look on X first
		for (x = loc.getX() - 1d; x <= (loc.getX() + 1d); x+=2d) {
			test = new Location (loc.getWorld(), x, y, z);
			b = test.getBlock ();
			plugin.log.finest ("Checking for fire at type " + b.getType() + " at X " + test);
			if (b.getType() ==  Material.FIRE)
				return b;
		}
		x = loc.getX();

		for (z = loc.getZ() - 1d; z <= (loc.getZ() + 1d); z+=2d) {
			test = new Location (loc.getWorld(), x, y, z);
			b = test.getBlock ();
			plugin.log.finest ("Checking for fire at type " + b.getType() + " at Z " + test);
			if (b.getType() ==  Material.FIRE)
				return b;
		}
		z = loc.getZ();
		
		// Check above?
		y = loc.getY() + 1d;  // underneath
		test = new Location (loc.getWorld(), x, y, z);
		b = test.getBlock ();
		plugin.log.finest ("Checking for fire at type " + b.getType() + " above " + test);
		if (b.getType() ==  Material.FIRE)
			return b;
		y -= 1;
		
		plugin.log.fine ("Unable to find burning source for " + loc);
		return null;
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
	// 1.5.1: Now Zombies can call this when lighting players on fire. Handle it??
	@EventHandler (ignoreCancelled = true)
	public void onEntityBurnEntity (EntityCombustByEntityEvent event)
	{
		Entity burned = event.getEntity();
		String burnedName = (burned instanceof Player) ? ((Player)burned).getDisplayName() : burned.getType().toString();
		String worldName = burned.getLocation().getWorld().getName();
		Entity burner = event.getCombuster();
		String burnerName = (burner instanceof Player) ? ((Player)burner).getDisplayName() : burner.getType().toString();
		
		boolean stopEvent = false;
		
		// The combuster can be a WeatherStorm a Blaze, or an Entity holding a FIRE_ASPECT enchanted item.
		// Called for fire-aspect by players/zoms on cows or mobs.
		//  May need new Permission to allow Players to set Entities on fire
				
		plugin.log.fine ("Entity " + burned.getType() + " combust by " + burner.getType()); // move after check below
		if (burner.getType() == EntityType.LIGHTNING) {			
			if (burned instanceof Player) {
				if (ifConfigContains ("nerf_fire.nodamageto.player.fromlightning", worldName) ) {
					stopEvent = true;
				}
			
			} else {
				if (ifConfigContains ("nerf_fire.nodamageto.nonplayer.fromlightning", worldName) ) {
					stopEvent = true;
				}
			}	
		}		
		else 
		{
			if (burned.getType() == EntityType.PLAYER) 
			{
			//  Need to handle by checking nodamageto.nonplayer.fromfire and player.fromfire if entity is player
			//  May need new Permission to allow Players to set Entities on fire
				if (ifConfigContains ("nerf_fire.nodamageto.player.fromfire", worldName) ) {
					stopEvent = true;
				}
			} 
			
			if (burner.getType() == EntityType.PLAYER) 
			{
				Player p = (Player)burner;
				boolean disallow = false;
				
				// Add check if player can set fire to entities (mobs) by permission
				disallow = ifConfigContains ("nerf_fire.noburnentityby.player", worldName);
				// shouldLog = ifConfigContains ("nerf_fire.loglightentityby.player", worldName);
 
				if (disallow) {
					if (p.isOp() && !plugin.getConfig().getBoolean ("nerf_fire.noburnentityby.op", false)) {
						disallow = false;
					} else if (p.isPermissionSet ("antifire.burnentity") && p.hasPermission ("antifire.burnentity")) {
						disallow = false;
						plugin.log.fine (p.getDisplayName() + " has permission .burnentity. Overriding noburnentityby.player");
					}
				}
				if (disallow)  {			
					p.sendMessage (plugin.pluginName + " says you don't have permissions to burn entities");
					stopEvent = disallow;
				}
				else if (ifConfigContains ("nerf_fire.nodamageto.nonplayer.fromfire", worldName) ) 
					stopEvent = true;
			}
		}  			

		if (stopEvent) {
			event.setDuration (0);
			event.setCancelled (true);
			plugin.log.fine ("stopped combust to " + burnedName + " by " + burnerName);
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
	public void onFireDamageEntity (EntityDamageEvent event) {
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
		
		// if (makeLog) plugin.log.finer ("nerf_fire: caught " + event.getCause() + " damaging " + damagee.getType());
	}
	@EventHandler (ignoreCancelled = true)
	public void  onFireblockDamageEntity (EntityDamageByBlockEvent event) {
		onFireDamageEntity ((EntityDamageEvent)event);
	}

	// New handler of nerf_fire.nostartby.explosion
	@EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onExplosionPrime (ExplosionPrimeEvent event) 
	{		
		String worldName = event.getEntity().getLocation().getWorld().getName();

		if (ifConfigContains ("nerf_fire.nostartby.explosion", worldName) && event.getFire()) {
			event.setFire (false);
			plugin.log.fine ("set " + event.getEntityType() + " explosion to NOT cause fire");
		}
		//continue processing
	}
	
}
