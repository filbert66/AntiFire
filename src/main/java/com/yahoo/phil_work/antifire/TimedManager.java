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

import com.yahoo.phil_work.antifire.TimedExtinguisher;
import com.yahoo.phil_work.antifire.IgniteCause;

class TimedManager implements Listener { 

	private HashMap <String, TimedExtinguisher> WorldTimed = new HashMap (8);
	private HashMap <IgniteCause, Long> TimedCause = new HashMap ();
	private Plugin plugin;

	TimedManager (Plugin p) {
		this.plugin = p;

		for (World w : p.getServer().getWorlds()) {
			TimedExtinguisher te = new TimedExtinguisher (p, w);
			if (te != null)
				WorldTimed.put (w.getName(), te);
		}
	}

	public void initConfig() {
		ConfigurationSection conf = plugin.getConfig().getConfigurationSection ("nerf_fire.timedcauses");
		if (conf == null) {
			return;
		}	
		for (String k : conf.getKeys (false)) {
			long del = conf.getLong (k);
			TimedFireCauses tc = new TimedFireCauses (k, del);
			if (tc.Cause == null) {
				plugin.getLogger().warning ("Unrecognized ignite cause: '" + k + "'. Refer to http://bit.ly/1gsdblo");
				conf.set (k, null); // removes from memory
			}
			else {
				TimedCause.put (tc.Cause, tc.BurnMillisecs);
			
				plugin.getLogger().config ("Added timed fire of delay " + del + " ticks for " + tc.Cause);
				
				// normalize RAM config, for easy finding on modification by commands
				if ( !k.equals (tc.Cause.toString())) {
					conf.set (tc.Cause.toString(), tc.BurnMillisecs); // add canonical name
					conf.set (k, null);	// remove alt name
				}
			}
		}
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
	public boolean setConfig (IgniteCause cause, long millisecs) {
		ConfigurationSection conf = plugin.getConfig().getConfigurationSection ("nerf_fire.timedcauses");

		if (cause == null)
			return false;
		else if (millisecs < 0) {
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
		else if (millisecs < 1000L/20) { //invalid delay
			return false;
		}
		TimedCause.put (cause, millisecs);
		
		// Now set config in RAM
		if (conf == null) {
			conf = plugin.getConfig().createSection ("nerf_fire.timedcauses");
			plugin.getLogger().config ("Creating first timedcauses config item for " + cause);
		}	
		conf.set (cause.toString(), millisecs); 
		return true;
	}
	public boolean clearConfig (IgniteCause cause) {
		return setConfig (cause, -1);
	}
	public void clearConfig () {
		for (IgniteCause c : IgniteCause.values())
			clearConfig (c);
	}
	public long getConfig (IgniteCause cause) {
		if (! ifTimedDelayFor (cause))
			return -1;
		
		return TimedCause.get (cause);
	}
	public long getConfig (String cause) {
		return getConfig (IgniteCause.matchIgniteCause (cause));
	}
	
	// Class for parsing configuration strings
	class TimedFireCauses {	
		TimedFireCauses (IgniteCause c, long d) {
			Cause = c;
			BurnMillisecs = d;
		}
		TimedFireCauses (String c, long d) {
			this (IgniteCause.matchIgniteCause(c), d);
		}
		TimedFireCauses (String cuz, String del) {
			this (cuz, 0);
			try {
				BurnMillisecs = Long.parseLong (del);
			} catch (NumberFormatException ex) {}
		}
		TimedFireCauses (String[] init) {
			this (init [0], init [1]);
		}		
		TimedFireCauses (String init) {
			this (init.split (" |,", 2));
		}
		
		IgniteCause Cause;
		long BurnMillisecs;
	}
	
	public boolean ifTimedDelayFor (IgniteCause cause) {
		return (TimedCause.containsKey (cause));
	}
	
	public void setTimedDelay (IgniteCause cause, BlockState state) {
		if (ifTimedDelayFor (cause)) {
			long fireDelay = TimedCause.get (cause); // in millisecs
			fireDelay = fireDelay * 20L / 1000L;
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
				event.setCancelled (true); // stop going out
				plugin.getLogger().fine ("Stopped block fading in " + w.getName());
			}
			else {
				plugin.getLogger().finer ("Unwatched block fade at " + loc);
			}
		}
		else
			plugin.getLogger().finer ("Ignoring block fade from " + event.getBlock().getType() + " to " + newState.getType());
	}
}
