/* 
 * AntiFireman.java
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
 * 16 May 2013 : PSW : Split noburnentityby.player to noburnmobby.[player|mob], noburnplayerby.[player|mob]
 *                     Fixed that mobs were alight for a tic even when noburnmobby.player
 * 13 Jun 2013 : PSW : Added charcoaldrop config node check to burnUpBlock();
 *                     Fixed bug that was burning block (or making charcoal) even when "nerf_fire.nodamageto.block" set.
 * 22 Sep 2013 : PSW : Added lava place functionality in BucketEmpty. 
 * 25 Sep 2013 : PSW : Adding Timed-based functions and config.
 * 10 Oct 2013 : PSW : Support command to modify TimedMgr configuration.
 * 22 Nov 2013 : PSW : Use new MaterialDataStringer.
 * 04 Dec 2013 : PSW : Used MaterialDataStringer in fireproof player messages.
 * 16 Jan 2013 : PSW : Recognize LOG_2 Material type for new logs; added anydropchance for all charcoal
 * 09 Mar 2014 : PSW : Fixed TNT not triggering when "burnt up";
 *                     New in 1.7.2-R0.3 ProjectileSource
 *    May 2014 : PSW : Use new UUID firelog calls.
 * 18 Dec 2014 : PSW : Expand .nonplayer to hostilemob, peacefulmob, mob, drops, painting, item
 * 21 Jan 2015 : PSW : avoid NPE on restart with timedMgr.
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
import java.lang.Class;
import java.util.NoSuchElementException;
import java.util.UUID;
 
import org.bukkit.event.entity.EntityCombustEvent; // when a player/entity is burnt by fire
import org.bukkit.event.entity.EntityCombustByBlockEvent; // entity burnt by lava or burning block
import org.bukkit.event.entity.EntityCombustByEntityEvent;// entity burnt by lightning, blaze, or FIRE_ASPECT weapon
import org.bukkit.event.block.BlockIgniteEvent; // when a block is lit
import org.bukkit.event.block.BlockBurnEvent; // when a block is destroyed
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import org.bukkit.Material;
import org.bukkit.material.Coal;
import org.bukkit.TreeSpecies;
import org.bukkit.material.Tree;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.CoalType;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Player;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.enchantments.Enchantment;

import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.permissions.PermissionAttachmentInfo;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

import com.yahoo.phil_work.BlockId;
import com.yahoo.phil_work.BlockIdList;
import com.yahoo.phil_work.MaterialDataStringer;
import com.yahoo.phil_work.EntityClassifier;
import com.yahoo.phil_work.antifire.FireLogEntry;
import com.yahoo.phil_work.antifire.AntifireLog;
import com.yahoo.phil_work.antifire.IgniteCause;
import com.yahoo.phil_work.antifire.TimedManager;

public class AntiFireman implements Listener
{
	private final AntiFire plugin;
	public	BlockIdList FireResistantList;
	private static final Random rng = new Random();
	private TimedManager timedMgr = null;
	
	// Metrics
	public int logEntries = 0, fireProofed = 0, nerfedStart=0, nerfedLava = 0;
	
	
	public AntiFireman (AntiFire instance)
	{
		this.plugin = instance;
	}

	public void initConfig () 
	{
		boolean writeDefault = false;
		
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
	
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.hostilemob.fromfire");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.peacefulmob.fromfire");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.mob.fromfire"); // prob not used, but in case
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.drops.fromfire");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.painting.fromfire");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.item.fromfire");

		plugin.getConfig().getStringList ("nerf_fire.nodamageto.hostilemob.fromlava");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.peacefulmob.fromlava");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.mob.fromlava");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.drops.fromlava");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.painting.fromlava");
		plugin.getConfig().getStringList ("nerf_fire.nodamageto.item.fromlava");

		plugin.getConfig().getStringList ("nerf_fire.nospread"); // by adjacent fire
		plugin.getConfig().getStringList ("nerf_fire.wooddropscharcoal"); 
		
		plugin.getConfig().getBoolean ("nerf_fire.whitelist", false);  // blacklist is default
		
		plugin.getConfig().getInt ("nerf_fire.logflushsecs"); // default in config.yml

		FireResistantList = new BlockIdList (this.plugin);
		FireResistantList.loadBlockList ("nerf_fire.blocklist");
		
		// if config has timed set 
		if (plugin.getConfig().isConfigurationSection ("nerf_fire.timedcauses")) { // only incur overhead if this config turns it on
			if (timedMgr == null) {
				createTimedMgr();
			}
			timedMgr.initConfig();
		} else
			plugin.log.config ("no timed causes configured for fire length; disabling timer task");
		
		if (writeDefault)
			plugin.saveDefaultConfig();
	}
	private void createTimedMgr () {
		plugin.log.info ("Starting timed fire manager");
		timedMgr = new TimedManager (this.plugin);
	
		plugin.getServer().getPluginManager().registerEvents (timedMgr, plugin);
	}
	public boolean clearTimedConfig() {
		if (timedMgr != null) {
			timedMgr.clearConfig ();
			return true;
		}
		return false;
	}
	public boolean clearTimedConfig (IgniteCause cause) {
		if (timedMgr != null && timedMgr.ifTimedDelayFor (cause)) {
			return timedMgr.clearConfig (cause);
		}
		return false;
	}
	public boolean setTimedConfig (IgniteCause cause, String delay) {
		if (cause == null)
			return false;
			
		if (timedMgr == null) {
			createTimedMgr();
			plugin.log.info ("Late-starting timed fire manager");
			timedMgr.initConfig();
		}
		return timedMgr.setConfig (cause, delay);
	}
	public boolean setTimedConfig (IgniteCause cause, long delay) 
	{
		if (cause == null)
			return false;
			
		if (timedMgr == null) {
			createTimedMgr();
			plugin.log.info ("Late-starting timed fire manager");
			timedMgr.initConfig();
		}
		if (delay > 0)
			return timedMgr.setConfig (cause, delay);
		else 
			return false;
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
	public boolean printConfigKey (CommandSender requestor, String key) {
		if (plugin.getConfig().isInt(key))
			printMsg (requestor, key + ": " + plugin.getConfig().getInt(key));
		else if (plugin.getConfig().isLong(key))
			printMsg (requestor, key + ": " + plugin.getConfig().getLong(key));
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
		} else {
			plugin.log.warning ("Fireman: Unrecognized config key: " + key);
			return false;
		}
		return true;
	}
	
	// Prints to requestor the config.yml from the supplied node			
	public boolean printConfig(CommandSender requestor, String node) {
      if ( !plugin.getConfig().isConfigurationSection (node)) {
        plugin.log.warning ("Not a config section: '" + node + "'");
        return false;
      }

      for (String key : plugin.getConfig().getConfigurationSection(node).getKeys(true)) {
            String FQK = node + "." + key;
            
			if	(plugin.getConfig().isConfigurationSection (FQK))
				continue;
			printConfigKey (requestor, FQK);	
      }
      return true;
	}

	public boolean printConfig(CommandSender requestor) 
	{
		for (String key : plugin.getConfig().getConfigurationSection ("nerf_fire").getKeys(true)) {
			key = "nerf_fire." + key;
			
			if	(plugin.getConfig().isConfigurationSection (key))
				continue;
			printConfigKey (requestor, key);	
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
		if (!plugin.getConfig().isSet (configkey) && configkey.contains ("nodamageto") && !configkey.contains (".player")) {
			plugin.log.fine (configkey + " not set; changing to nonplayer");
			int l= configkey.length();
			configkey = "nerf_fire.nodamageto.nonplayer.from" + configkey.substring (l-4, l); //"lava" or "fire"
		}
			
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
		IgniteCause detailedCause = IgniteCause.UNKNOWN;
		
		boolean disallow = false;
		boolean shouldLog = false;
		Level loglevel = Level.FINE;
		
		switch (event.getCause()) {
			case LAVA:
				disallow = ifConfigContains ("nerf_fire.nostartby.lava", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.lava", worldName);
				detailedCause = IgniteCause.LAVA;
				break;
				
			case FIREBALL: 
				detailedCause = IgniteCause.getIgniteCause (event.getIgnitingEntity().getType());
				
				if (p == null) {  // docs say only by player, but can happen with plugins starting them
					disallow = ifConfigContains ("nerf_fire.nostartby.fireball", worldName);
					shouldLog = ifConfigContains ("nerf_fire.logstart.fireball", worldName);
					break;
				} // else it were a player and fall through
			case FLINT_AND_STEEL:
				disallow = ifConfigContains ("nerf_fire.nostartby.player", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.player", worldName);
				detailedCause = IgniteCause.FLINT_AND_STEEL;

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
				detailedCause = IgniteCause.LIGHTNING;
				break;
				
			case SPREAD:
				disallow = ifConfigContains ("nerf_fire.nospread", worldName);
				// shouldLog = ifConfigContains ("nerf_fire.logstart.spread", worldName);
				loglevel = Level.FINER;
				detailedCause = IgniteCause.SPREAD;
				break;

			case EXPLOSION:
				plugin.log.finer ("Explosion starting fire at " + event.getBlock().getLocation());
				disallow = ifConfigContains ("nerf_fire.nostartby.explosion", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.explosion", worldName);
				detailedCause = IgniteCause.getIgniteCause (event.getIgnitingEntity().getType());
				break;
			
			case ENDER_CRYSTAL:
				plugin.log.fine ("Ender Crystal starting fire at " + event.getBlock().getLocation());
				disallow = ifConfigContains ("nerf_fire.nostartby.crystal", worldName);
				shouldLog = ifConfigContains ("nerf_fire.logstart.crystal", worldName);
				detailedCause = IgniteCause.ENDER_CRYSTAL;
				break;			
			
			default:
				plugin.log.warning ("Unknown fire ignition cause: " + event.getCause() + ". Time for a plugin update?");
				// what about fire aspect weapons, and fireball explosions?
				detailedCause = IgniteCause.UNKNOWN;
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
									   new MaterialDataStringer (block.getType(),block.getData()) + " is fire resistant");
					event.setCancelled (true);
					this.fireProofed++;
					return;
				}	
				else {
					plugin.log.finer (block.getType() + (whitelist ? "": " not") + " in nerf_fire.blocklist");
				}

			// Call TimedBlock here.
			if (timedMgr != null && 
				timedMgr.ifTimedDelayFor (detailedCause) && (!burnsForever (block) || timedMgr.foreverBlocksToo()) ) 
				timedMgr.setTimedDelay (detailedCause, event.getBlock().getState());
			
			if (shouldLog)
			{
				String starter = event.getCause().toString();
				UUID starterId = null;
				if (p != null) {
					starterId = p.getUniqueId();
					starter = p.getDisplayName();
				}
				
				if (plugin.fireLog.add (starter, starterId, loc) > 10) // don't flush to disk until we get 10 events
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

	boolean burnsForever (final Block b) {
		Material m = b.getType();
		return (m == Material.NETHERRACK) || 
				(m == Material.BEDROCK && b.getWorld().getEnvironment() == World.Environment.THE_END);
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
		
		// TODO: REPLACE CHECK WITH m.isFlammable()
		// No, because players can purposefully light fire to inFlammable objects. 
		// isFlammable is only used during fire spread.
		
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
	// Now maximum is driven by config, and if not configured to be random, drops the max. 
	private void burnUpBlock (Block b) {
		Material m = b.getType();
		World w = b.getWorld();
		String worldName = w.getName();

		// Should move config validation to startup, not runtime/eachtime code

		if ((m == Material.LOG || m == Material.LOG_2) && ifConfigContains ("nerf_fire.wooddropscharcoal", worldName)) {
			int amount = 0;
			int max = plugin.getConfig().getInt ("nerf_fire.charcoaldrop.max");
			Tree t = new Tree (m,b.getData());
	
			// Have to use getSpecies() to strip out log orientation bits in the data byte
			// can't use getSpecies.toString() since that will incorrectly show GENERIC for ACACIA
			plugin.log.fine ("AF: burning up log of type " + m + ":" + t.getSpecies().getData()); 

			int anyDropChance = plugin.getConfig().getInt ("nerf_fire.charcoaldrop.anydropchance");
			if (rng.nextInt (100) > anyDropChance) {
				plugin.log.finer ("No drop due to anydropchance of " +anyDropChance + "%");
				max = 0;
			}		
		    // Allow max configurable by wood type
			else if (plugin.getConfig().isSet ("nerf_fire.charcoaldrop.treetypemax")) {
				// plugin.log.finer ("Found new treetypemax");
				
				TreeSpecies treeType = t.getSpecies();
				// plugin.log.finer ("detected LOG of type " + treeType + " data: " + treeType.getData());
				List <Short> treeTypeMax = plugin.getConfig().getShortList ("nerf_fire.charcoaldrop.treetypemax");

				// Could call treeType.ordinal() to get int value
				if (treeTypeMax.size() > treeType.getData()) {
					max = treeTypeMax.get(treeType.getData());
					// plugin.log.finer ("Reset max charcoal for type " + treeType + " to " + max);
				}
				else
					plugin.log.warning ("charcoaldrop.treetypemax missing value for " + TreeSpecies.getByData(b.getData()) + "(" + b.getData() + "); using charcoaldrop.max");
			} else if (plugin.getConfig().isSet ("nerf_fire.charcoaldrop.speciesmax")) {
				boolean found = false;
				for (String matString : plugin.getConfig().getConfigurationSection ("nerf_fire.charcoaldrop.speciesmax").getKeys(/*deep=*/false)) {
					MaterialData md = MaterialDataStringer.matchMaterialData (matString);
					if (md == null) {
						plugin.log.warning ("speciesmax: '" + matString + "' unrecognized Material. Refer to http://bit.ly/19sfyhe");
					} 
					else if (md.getItemType() == m) 
					{  // Need to be careful not to compare orientation bits in data
						Tree testTree = new Tree (m, md.getData()); // gives orientation bits, but getSpecies() removes them
						if (testTree.getSpecies() == t.getSpecies()) {
							max = plugin.getConfig().getInt ("nerf_fire.charcoaldrop.speciesmax." + matString, max);
							found = true;
							break;
						}
					} // else plugin.log.info ("Not matching config ." + matString + "= " + md);
				}
				if (!found)
					plugin.log.warning ("speciesmax doesn't contain " + new MaterialData (m,b.getData()) + "; using .max");
			}

			if (max > 0 && plugin.getConfig().getBoolean ("nerf_fire.charcoaldrop.random"))
			{
				amount = 1 + rng.nextInt (max);
				// Changed to 75% chance, and make that chance also configurable as 0-100 int
/**
Chc	Max	average
75%	1	0.75
75%	2	1.125
75%	3	1.5
75%	4	1.875
75%	5	2.25
75%	6	2.625
***/
			} else 
				amount = max;
			
			if (amount > 0) {
				MaterialData mat = new Coal (CoalType.CHARCOAL);
				w.dropItem (b.getLocation(), mat.toItemStack (amount));
				plugin.log.fine ("AF: dropping " + amount + " (max:" + max + ") charcoal at " + b.getLocation());
			}
		}
		
		b.setType (Material.AIR);
		
		if (m == Material.TNT) {  // just removed TNT; replace w primed
			Location loc = b.getLocation();
			try {
				TNTPrimed tnt = w.spawn (loc, TNTPrimed.class);
				if (tnt == null) 
					plugin.log.warning ("Unable to spawn TNT at " + loc);
				/** else 
					tnt.setFuseTicks (tntTicks); **/
			} catch (Throwable t) {
				plugin.log.warning (t + ": Unable to spawn and trigger TNT at " + loc);
			}						
		}
	}

	
	@EventHandler (ignoreCancelled = true)
	public void onFireDestroyBlock (BlockBurnEvent event)
	{
		String worldName = event.getBlock().getWorld().getName();
		boolean stopEvent = false;
				
		if (ifConfigContains ("nerf_fire.nodamageto.block", worldName)) {
			plugin.log.finer ("stopped block (" + event.getBlock().getType() + ") destruction by fire in " + worldName);
			event.setCancelled (true);
			return; // don't check for spread
			// will this put the fire out, or will it burn indefinitely like netherack?
			// Burns indefinitely.
		}
		else if (timedMgr != null && timedMgr.isBeingTimed (getFireBlockFrom (event.getBlock().getLocation()))) {
			plugin.log.finer ("stopped timed fire block destruction at (" + event.getBlock().getLocation());
			event.setCancelled (true);
			return; // don't check for spread
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
				return; // don't allow spread on fire resistance
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
			//plugin.log.info ("burnt up block w/o spread " + event.getBlock().getType());
			burnUpBlock (event.getBlock());
			return;
		}	
		
		// Keep processing event, which allows for spread, but check for drops
		burnUpBlock (event.getBlock());
		//plugin.log.info ("burnt up block " + event.getBlock().getType());
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
	
	private String getConfigSubString (Entity e) {
		EntityType et = e.getType();
		String configItem;

		if (e instanceof Player) {
			configItem = "player";
		} else if (e instanceof LivingEntity) {
			if (EntityClassifier.canBeHostile (e))
				configItem = "hostilemob";
			else if (EntityClassifier.isPeaceful (e))
				configItem = "peacefulmob";
			else {
				configItem = "mob";
				plugin.log.finer (et + " neither peaceful nor hostile");
			}
		} else if (et == EntityType.DROPPED_ITEM)
			configItem = "drops";
		else if (e instanceof Hanging)
			configItem = "painting";
		else 
			configItem = "item";
			
		return configItem;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onEntityCombust (EntityCombustEvent event) {
		Entity e = event.getEntity();
		String worldName = e.getLocation().getWorld().getName();
		String configItem = "nerf_fire.nodamageto." + getConfigSubString (e) + ".fromfire";
		
		if (ifConfigContains (configItem, worldName)) {
			plugin.log.fine ("stopped combustion to " + (e instanceof Player ? ((Player)e).getDisplayName() : e.getType()));
			event.setDuration (0);
			event.setCancelled (true);
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
		else if (burner.getType() == Material.LAVA) {
			String configItem = "nerf_fire.nodamageto." + getConfigSubString (e) + ".fromlava";
			String name = (e instanceof Player ? ((Player)e).getDisplayName() : e.getType().toString());
			
			if (ifConfigContains (configItem, worldName)) {
				plugin.log.fine ("stopped combust to " + name + " from lava in " + worldName);
				event.setDuration (0);
				event.setCancelled (true);
			}
		} else
			plugin.log.fine ("Entity " + e.getType() + " combust by " + burner.getType());
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
			String configItem = "nerf_fire.nodamageto." + getConfigSubString (burned) + ".fromlightning";
		
			if (ifConfigContains (configItem, worldName))
				stopEvent = true;
		}		
		else 
		{
			stopEvent = ifNerfCombustion (burned, burner);
		}  			

		if (stopEvent) {
			if (burned.getFireTicks() > 0) {
				// There is a non-conditional call "setonfire(1)" in NMS.EntityHuman with a fire aspect weapon before this event
				plugin.log.fine (burned.getType() + " was on fire; nerfed");
				burned.setFireTicks(0); 
			}

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
	public void onFireDamageEntity (EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();
		boolean makeLog = true;
		boolean nerfdamage = false;
			
		switch (event.getCause()) {
			case FIRE:
			case FIRE_TICK:
				nerfdamage = ifNerfCombustion (damagee, damager);
				break;

			// might be player attacking with fire aspect weapon, which already set entity on fire
			//  in which case we need to turn off fire but not the attack damage
			// This avoids mobs dropping "cooked" items on death.
			case ENTITY_ATTACK: 
				if (damager instanceof HumanEntity && ((HumanEntity)damager).getItemInHand().containsEnchantment(Enchantment.FIRE_ASPECT)) {
					if (ifNerfCombustion (damagee, damager) && damagee.getFireTicks() > 0) {
						plugin.log.fine ("ENTITY_ATTACK: " + damagee.getType() + " was on fire; nerfed");
						damagee.setFireTicks(0); 
					}
				}
				break;			
				
			default:
				nerfdamage = false;
				break;
		}
		if (nerfdamage) {
			if (damagee.getFireTicks() > 0) {
				plugin.log.fine ("EntityDamageByEntityEvent: " + damagee.getType() + " was on fire; nerfed");
				damagee.setFireTicks(0); 
			}

			event.setDamage (0); // just to make sure
			event.setCancelled (true);
		}
		else 
			plugin.log.finer ("Allowing " + damagee.getType() + " to be damaged by " + damager + ", cause "+ event.getCause());
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
		String configItem = "nerf_fire.nodamageto." + getConfigSubString (damagee);
		
	    switch (event.getCause()) {
		    case LAVA:
				nerfdamage = ifConfigContains (configItem + ".fromlava", worldName);
				break;

			case FIRE_TICK: // this gets called from EntityDamageEvent (not ByBlock)
				plugin.log.finest (damagee.getType() + " damaged by fire tick");
				makeLog = false;
			case FIRE:
				nerfdamage = ifConfigContains (configItem + ".fromfire", worldName);
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
		} else 
			plugin.log.finer ("Allowing " + damagee.getType() + " to be damaged by cause "+ event.getCause());
		
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
	
	@EventHandler (ignoreCancelled = true)
	public void onBucketEmpty (PlayerBucketEmptyEvent event)
	{
		Player p = event.getPlayer();
		Material contents = event.getBucket();
		boolean disallow = false;
		boolean shouldLog = false;
		Location loc = p.getLocation();
		String worldName = loc.getWorld().getName();
		
		if (contents == Material.LAVA_BUCKET) {
			disallow = ifConfigContains ("nerf_fire.noplacelavaby.player", worldName);		
			shouldLog = ifConfigContains ("nerf_fire.logplace.lava", worldName);
			
			if (disallow) {
				if (p.isOp() && !plugin.getConfig().getBoolean ("nerf_fire.noplacelavaby.op", false)) {
					disallow = false;
				} else if (p.isPermissionSet ("antifire.placelava") && p.hasPermission ("antifire.placelava")) {
					disallow = false;
					plugin.log.fine (p.getDisplayName() + " has permission .placelava. Overriding config noplacelavaby.player");
					
					/***
					for (PermissionAttachmentInfo i : p.getEffectivePermissions())
						if (i.getPermission().indexOf ("antifire") != -1)
							plugin.log.finer (p.getDisplayName() + " has permission " + i.getPermission() + " = " + i.getValue());
					***/
				}
			}
			if (disallow) {
				p.sendMessage (plugin.pluginName + " says you don't have lava place permissions");
				plugin.log.info ("Blocked lava place by " + p.getDisplayName());
				event.setCancelled (true);
				this.nerfedLava++;
			}
			else if (shouldLog) {
				String logEvent = p.getDisplayName() + "_placed_lava";
				if (plugin.fireLog.add (logEvent, p.getUniqueId(), loc) > 10) // don't flush to disk until we get 10 events
				{
					plugin.flushLog (null, plugin.getConfig().getInt ("nerf_fire.logflushsecs"));
				}
				plugin.log.info (plugin.fireLog.list.getLast().toStringNoDate(false));  // logger already includes date
				logEntries++;
			}
			else
				 plugin.log.info ("Allowing lava place by " + p.getDisplayName() + " in " + worldName);
		}
		else
			plugin.log.fine ("Placing non-lava bucket of " + contents);
		
	}
	
	private boolean ifNerfCombustion (Entity burned, Entity burner) {
		String worldName = burned.getLocation().getWorld().getName();

		// New ProjectileSource makes this 1.7.2-R0.3 dependent
		if (burner instanceof Projectile) {
			ProjectileSource shooter = ((Projectile)burner).getShooter();
			if (shooter != null) { 
				if (shooter instanceof LivingEntity) {
					plugin.log.fine ("Determined " + ((LivingEntity)shooter).getType() + " to have shot the " + burner.getType());
					burner = (Entity)shooter;
				}
				else
					plugin.log.fine ("Determined " + shooter + " to have shot the " + burner.getType());
	
			}
		}
			
		if (burned.getType() == EntityType.PLAYER) 
		{
			boolean disallow = false;
			
			if (burner.getType() == EntityType.PLAYER)
				disallow = ifConfigContains ("nerf_fire.noburnplayerby.player", worldName);
			else 
				disallow = ifConfigContains ("nerf_fire.noburnplayerby.mob", worldName);

			if (disallow && burner.getType() == EntityType.PLAYER) {
				Player p = (Player)burner;
				if (p.isOp() && !plugin.getConfig().getBoolean ("nerf_fire.noburnplayerby.op", false)) {
					disallow = false;
				} else if (p.isPermissionSet ("antifire.burnplayer") && p.hasPermission ("antifire.burnplayer")) {
					disallow = false;
					plugin.log.fine (p.getDisplayName() + " has permission .burnplayer. Overriding noburnplayerby.player");
				}
				if (disallow) 
					p.sendMessage (plugin.pluginName + " says you don't have permissions to burn players");
			}
			if (disallow)  {			
				return true;
			}
			else if (ifConfigContains ("nerf_fire.nodamageto.player.fromfire", worldName) ) {
				return true;
			}
		} 
		
		if (burner.getType() == EntityType.PLAYER) 
		{
			Player p = (Player)burner;
			boolean disallow = false;
			
			// Because of earlier conditional, we know burned is NOT a player
			disallow = ifConfigContains ("nerf_fire.noburnmobby.player", worldName);

			if (disallow) {
				if (p.isOp() && !plugin.getConfig().getBoolean ("nerf_fire.noburnmobby.op", false)) {
					disallow = false;
				} else if (p.isPermissionSet ("antifire.burnmob") && p.hasPermission ("antifire.burnmob")) {
					disallow = false;
					plugin.log.fine (p.getDisplayName() + " has permission .burnmob. Overriding noburnmobby.player");
				}
			}
			if (disallow)  {			
				p.sendMessage (plugin.pluginName + " says you don't have permissions to burn mobs");
				return true;
			}
			else if (ifConfigContains ("nerf_fire.nodamageto." + getConfigSubString (burned) + ".fromfire", worldName) ) 
				return true;
		}
		else { // Burning mob by mob
			if (ifConfigContains ("nerf_file.noburnmobby.mob", worldName))
				return true;
		}	
		return false;	
	}
}

