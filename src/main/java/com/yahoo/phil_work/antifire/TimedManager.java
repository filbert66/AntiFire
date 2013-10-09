package com.yahoo.phil_work.antifire;

import java.util.HashMap;
import java.util.Map;
import java.lang.IllegalArgumentException;

import org.bukkit.plugin.Plugin;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;

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
		// TODO actually read configuration data.
		TimedCause.put (IgniteCause.FIRECHARGE, 10000L); // hard-code 10 second delay
		// TimedCause.put (IgniteCause.FLINT_AND_STEEL, 30000L); // to test fading
	}		

	// Class for parsing configuration strings
	class TimedFireCauses {	
		TimedFireCauses (String cuz, String del) {
			Cause = IgniteCause.matchIgniteCause(cuz);
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
		long fireDelay = TimedCause.get (cause); // in millisecs
		if (ifTimedDelayFor (cause)) {
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
