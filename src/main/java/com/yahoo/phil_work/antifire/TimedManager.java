// implemented addRandom with range of times
// 25 Jul 2015 : PSW: Added isRainingAt() and rainOverridesTimed()
// 03 Aug 2015 : PSW: Made above world-based.

package com.yahoo.phil_work.antifire;

import java.util.HashMap;
import java.util.Map;
import java.lang.IllegalArgumentException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.ChunkSnapshot;

import com.yahoo.phil_work.antifire.TimedExtinguisher;
import com.yahoo.phil_work.antifire.IgniteCause;

class TimedManager implements Listener { 

	 private class TimedLength {
		boolean ifRandom;
		long min;
		long max;
		
		TimedLength (long fixed)
		{
			ifRandom = false;
			min = max = fixed;
		}
		
		TimedLength (long Min, long Max) {
			ifRandom = true;
			min = Min;
			max = Max;
		} 

		TimedLength (String Min, String Max) {
			if (plugin != null)
				plugin.getLogger().fine ("TimedLength parsed to '" + Min + "', '" + Max + "'");
//			else 
//				System.out.println  ("TimedLength parsed to '" + Min + "', '" + Max + "'");
			try {
				min = max = Long.parseLong (Min);
			} catch (NumberFormatException ex) {}
			ifRandom = false;
			
			if (Max.length() > 0) {
				try {
					max = Long.parseLong (Max);
				} catch (NumberFormatException ex) {}
				ifRandom = true;
			}
			else 
				ifRandom = false;
		}
		TimedLength (String[] init) {
			this (init[0], init.length > 1 ? init [1] : "");
			
			if (init[0].length() == 0) { // negatives get split at start 
				ifRandom = false;
				min = -max;
				max = min;
			}
		}		
		TimedLength (String init) {
			this (init.trim().split ("\\s*-\\s*", 2));
		}
		
		@Override public String toString() {
			if (ifRandom) 
				return Long.toString(min) + " - " + Long.toString(max);
			else
				return Long.toString(min);
		}
	}	
	private HashMap <String, TimedExtinguisher> WorldTimed = new HashMap<String, TimedExtinguisher> (8);
	private HashMap <IgniteCause, TimedLength> TimedCause = new HashMap<IgniteCause, TimedLength> ();
	private AntiFire plugin;

	TimedManager (AntiFire p) {
		this.plugin = p;

		for (World w : p.getServer().getWorlds()) {
			TimedExtinguisher te = new TimedExtinguisher ((Plugin)p, w);
			if (te != null)
				WorldTimed.put (w.getName(), te);
		}
	}
	TimedManager () {
		this.plugin = null;
	}

	public void initConfig() {
		ConfigurationSection conf = plugin.getConfig().getConfigurationSection ("nerf_fire.timedcauses");
		if (conf == null) {
			return;
		}	
		for (String k : conf.getKeys (false)) {
			TimedLength tl = new TimedLength (conf.getString (k)); // parses string either way

			TimedFireCauses tc = new TimedFireCauses (k, tl); 
			if (tc.Cause == null) {
				plugin.getLogger().warning ("Unrecognized ignite cause: '" + k + "'. Refer to http://bit.ly/1gsdblo");
				conf.set (k, null); // removes from memory
			}
			else {
				TimedCause.put (tc.Cause, tl);
			
				plugin.getLogger().config ("Added timed fire of delay " + tl.toString() + " ticks for " + tc.Cause);
				
				// normalize RAM config, for easy finding on modification by commands
				if ( !k.equals (tc.Cause.toString())) {
					conf.set (tc.Cause.toString(), tc.Time.toString()); // add canonical name
					conf.set (k, null);	// remove alt name
				}
			}
		}
	}		
	public boolean foreverBlocksToo () {
		return plugin.getConfig().getBoolean ("nerf_fire.timeNetherackToo");
	}
	public boolean foreverBlocksToo (World w) {
		if (w == null)
			return foreverBlocksToo();
		else
			return foreverBlocksToo (w.getName());
	}
	public boolean foreverBlocksToo (String worldName) {	
		String item = plugin.getConfig().getString ("nerf_fire.timeNetherackToo");
		
		if (item.equals ("true"))
			return true;
		else		
			return plugin.antiFire.ifConfigContains ("nerf_fire.timeNetherackToo", worldName);
	}
	
	public boolean rainOverridesTimed () {
		return plugin.getConfig().getBoolean ("nerf_fire.rainOverridesTimed", false);
	}
	public boolean rainOverridesTimed (World w) {
		if (w == null)
			return rainOverridesTimed();
			
		String worldName = w.getName();
		String item = plugin.getConfig().getString ("nerf_fire.rainOverridesTimed");
		
		if (item.equals ("true"))
			return true;
		else		
			return plugin.antiFire.ifConfigContains ("nerf_fire.rainOverridesTimed", worldName);
	}

	/*
	 * These config functions all deal in user-consummable millisecond delays
	 */
	// setConfig will NOT affect exsting timed blocks. 
	//   if provided delay is <0, clears any configuration for that Cause
	//   if provided delay is < 1 tick, fails.
	
	// TODO: Function to clear any timed blocks
	public boolean setConfig (String cause, long millisecs) {
		return setConfig (IgniteCause.matchIgniteCause (cause), millisecs);
	}
	public boolean setConfig (IgniteCause cause, String delaySpec) {
		TimedLength tl = new TimedLength (delaySpec);
		
		return setConfig (cause, tl);
	}
	public boolean setConfig (IgniteCause cause, long millisecs) {
		TimedLength tl = new TimedLength (millisecs);
		return setConfig (cause, tl);
	}
	public boolean setConfig (IgniteCause cause, TimedLength tl) {
		ConfigurationSection conf = plugin.getConfig().getConfigurationSection ("nerf_fire.timedcauses");

		if (cause == null)
			return false;
		else if (tl.min < 0) {
			if (ifTimedDelayFor (cause)) {
				plugin.getLogger().config ("Removing any timed fire for cause " + cause);
				TimedCause.remove (cause);
			}				
			if (conf != null) {
				conf.set (cause.toString(), null); // removes mapping from RAM config
				if (conf.getKeys(false).isEmpty())
					conf.getParent().set ("timedcauses", null); // remove section
			}
			return true;
		}
		else if (tl.min < 1000L/20 || tl.max < 1000L/20) { //invalid delay
			return false;
		}
		TimedCause.put (cause, tl);
		
		// Now set config in RAM
		if (conf == null) {
			conf = plugin.getConfig().createSection ("nerf_fire.timedcauses");
			plugin.getLogger().config ("Creating first timedcauses config item for " + cause);
		}	
		conf.set (cause.toString(), tl.toString()); 
		return true;
	}
	public boolean clearConfig (IgniteCause cause) {
		return setConfig (cause, -1);
	}
	public void clearConfig () {
		for (IgniteCause c : IgniteCause.values())
			clearConfig (c);
	}
/**	public long getConfig (IgniteCause cause) {
		if (! ifTimedDelayFor (cause))
			return -1;
		
		return TimedCause.get (cause);
	}
	**/
	/**
	public long getConfig (String cause) {
		return getConfig (IgniteCause.matchIgniteCause (cause));
	}
	**/
	
	// Class for parsing configuration strings
	class TimedFireCauses {	
		TimedFireCauses (IgniteCause c, long d) {
			Cause = c;
			Time = new TimedLength (d);
		}
		TimedFireCauses (IgniteCause c, TimedLength tl) {
			Cause = c;
			Time = tl;
		}
		TimedFireCauses (String cuz, TimedLength tl) {
			this (cuz, 0);
			Time = tl;
		}
		TimedFireCauses (String c, long d) {
			this (IgniteCause.matchIgniteCause(c), d);
		}
		TimedFireCauses (String cuz, String delay) {
			this (cuz, 0);
			Time = new TimedLength (delay);
		}		
/**		TimedFireCauses (String[] init) {
			this (init [0], init [1]);
		}		
		TimedFireCauses (String init) {
			this (init.split (" |,", 2));
		}
**/
		boolean isRandom() {
			return Time.ifRandom;
		}
		long getTime() {
			return Time.min;
		}
		long getMin() {
			return getTime();
		}
		long getMax() {
			return Time.max;
		}
		IgniteCause Cause;
		TimedLength Time;
	}
	
	public boolean ifTimedDelayFor (IgniteCause cause) {
		return (TimedCause.containsKey (cause));
	}
	
	public void setTimedDelay (IgniteCause cause, BlockState state) {
		if (ifTimedDelayFor (cause)) {
			TimedLength fireDelay = TimedCause.get (cause); // in millisecs
			this.add (state, fireDelay); // 20 ticks per 1000ms
		}
	}	

	public boolean isBeingTimed (Location loc) {
		if (loc == null)
			return false;
			
		World w = loc.getWorld();
		TimedExtinguisher te = WorldTimed.get (w.getName());
	
		return (te != null && te.contains (loc));
	}
	public boolean isBeingTimed (Block b) {
		return isBeingTimed (b.getLocation());
	}
	
	// Add this block to the list of blocks to be extinguished
	// Delay in TICKS
  	public void add (BlockState state, long delay) {
		World w = state.getLocation().getWorld();
		TimedExtinguisher te = WorldTimed.get (w.getName());
		if (te != null) {
			te.add (state, delay);
			plugin.getLogger().finer ("Added " + delay + " ticks delayed extinguish at " + state.getLocation());
		}
	}
	public void add (BlockState s, long min, long max) {
		addRandom (s, min, max);
	}
	public void addRandom (BlockState state, long min, long max) {
		World w = state.getLocation().getWorld();
		TimedExtinguisher te = WorldTimed.get (w.getName());
		if (te != null) {
			te.addRandom (state, min, max);
			plugin.getLogger().finer ("Added random [" + min + ", " + max + "] ticks delayed extinguish at " + state.getLocation());
		}
	}
	// Does conversion to ticks before adding
	public void add (BlockState state, TimedLength tl) {
		long Min = TimedExtinguisher.millisecsToTicks (tl.min);

		if (tl.ifRandom) 
			addRandom (state, Min, TimedExtinguisher.millisecsToTicks (tl.max));
		else
			add (state, Min);
	}
	
	private boolean isRainingAt (Location loc) {
		return (loc.getWorld().hasStorm());
	}
	
	// Stop timed fire blocks from fading 
	@EventHandler (ignoreCancelled = true)
	public void onFireOut (BlockFadeEvent  event)
	{	
		BlockState newState = event.getNewState();
		if (event.getBlock().getType() == Material.FIRE &&
			newState.getType() == Material.AIR) 
		{
			Location loc = newState.getLocation();
			// it's a block that's going out. Check if it is being timed.
			World w = loc.getWorld();
			TimedExtinguisher te = WorldTimed.get (w.getName());
		
			if (te != null && te.contains (loc)) {
				if (rainOverridesTimed(w) && isRainingAt (loc)) { 
					plugin.getLogger().fine ("Allowed rain to put out timed block at " + loc);
					te.remove (loc);
				} else {
					event.setCancelled (true); // stop going out
					plugin.getLogger().fine ("Stopped block fading in " + w.getName());
				}
			}
			else {
				plugin.getLogger().finer ("Unwatched block fade at " + loc);
			}
		}
		else
			plugin.getLogger().finer ("Ignoring block fade from " + event.getBlock().getType() + " to " + newState.getType());
	}
}
