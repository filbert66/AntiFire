// See here: http://wiki.bukkit.org/Scheduler_Programming
 package com.yahoo.phil_work.antifire;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.ArrayList;
import java.lang.NullPointerException;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.World;
import org.bukkit.Location;

import com.yahoo.phil_work.antifire.TimedBlock;

// Good only per World. 
// For multiple worlds use  more than one instance of this.
//
class TimedExtinguisher extends	BukkitRunnable {
	private Plugin plugin;
	private TreeSet <TimedBlock> FixedLengthBlocks = null;
	private World world;
	BukkitTask myTask;
	
	TimedExtinguisher (Plugin p, World w) 
	{ 
		if (p == null || w == null)
			throw new NullPointerException("in Scheduled constructor");
		world = w;	
		plugin = p; 
		FixedLengthBlocks = new TreeSet<TimedBlock> (new TimedBlock.TimedBlockComparator());
		
		String worldName = w.getName();
		if (w.getGameRuleValue ("doDaylightCycle").equals (String.valueOf (false))) {
			plugin.getLogger().warning ("scheduled fire outage won't work without doDaylightCycle=true in " + worldName);
			throw new IllegalStateException ("need daylightCycle in " + worldName);
		}
	}

	// Managing list of blocks to put out
	// delay MUST be in ticks
	public void add (BlockState state, long delay) {
		long expiry = delay + this.world.getFullTime();
		
		if (state == null) return;
		
		if (state.getWorld().getUID() == this.world.getUID()) {
			TimedBlock tb = new TimedBlock (state, expiry);
			FixedLengthBlocks.add (tb);
	
	  	    if (expiry == FixedLengthBlocks.first().getExpiry()) 
			{ // either was empty or this one is earliest and sorted to top.
				try {
					super.cancel();	// don't erase the list!
				} catch (IllegalStateException e) {
					plugin.getLogger().fine ("First run; wasn't scheduled");
				}
				try {
					myTask = plugin.getServer().getScheduler().runTaskLater (plugin, this, delay); // reschedule
				} catch (IllegalArgumentException e) {
					// plugin was null
				} catch (IllegalStateException e) {
					plugin.getLogger().warning ("Error; was already scheduled" + e);
				}
			}
			else {
				long future = FixedLengthBlocks.first().getExpiry() - world.getFullTime();
				plugin.getLogger().fine ("Already scheduled for '" + world.getName() + "' in " + future + " ticks");
			}
		} else
			plugin.getLogger().warning ("attempted to log block to wrong TimedExtinguisher");
	}
	
	public boolean contains (Location loc) {
		World w = loc.getWorld();
		if (w.getUID() == this.world.getUID()) {
		  // unfortunately have to do a linear search...
		  int i = 0;
		  for (TimedBlock b : FixedLengthBlocks) {
		  	// plugin.getLogger().finer (w.getName() + " block " + i++ + " expires " + b.getExpiry());
		  	
		  	int x = loc.getBlockX(), y = loc.getBlockY(), z= loc.getBlockZ();
		  	Location bl = b.getLocation();
		  	int bx = bl.getBlockX(), by = bl.getBlockY(), bz = bl.getBlockZ();
			if (x==bx && y==by && z==bz) {
				return true;
			}
		  }
	  }
	  plugin.getLogger().finer ("Can't find location " + loc + " in timedExtingisher for " + world.getName()); 
	  return false;
	}	
	public boolean isEmpty() {
		return FixedLengthBlocks == null || FixedLengthBlocks.isEmpty();
	}
	
	// Returns true if put out fire
	private boolean putOutFire (BlockState b) {
		if (b.getBlock().getType() == Material.FIRE) {
			b.setType (Material.AIR);
			plugin.getLogger().finer ("Put out fire at " + b.getLocation());
			return true;
		}
		else return false;
	}

	@Override
	public void cancel () {
		plugin.getLogger().info ("Canceling extinguisher task " + myTask.getTaskId());
		FixedLengthBlocks.clear();
		try {
			super.cancel();
		} catch (IllegalStateException e) {
			// not scheduled
		}
	}

	@Override
	public void run () {
		final long now = this.world.getFullTime();
		long nextOut = now + 20 * 60 * 60 * 4; // not used, but to avoid compiler warning
		
		plugin.getLogger().finer ("periodic extinguisher task running for " + FixedLengthBlocks.size() + " blocks");
		
	 	ArrayList<TimedBlock> deleteList = new ArrayList();
		
		for (TimedBlock b : FixedLengthBlocks) {
			if (b.getExpiry() <= now + 1) { //always getting called with -1 before now

				if (putOutFire (b.getState())) { // only works if was still fire
					if ( !b.update(true))  // state was captured before fire, so force update
						plugin.getLogger().warning ("Unable to put out block at " + b.getLocation() + 
						"; changed to " + b.getBlock().getType());
					else
						plugin.getLogger().finer ("block update to put out fire worked");
				}
				else // Shouldn't get this error unless user or fade put the fire out, or block burnt
					plugin.getLogger().info ("Fire block changed before timeout to " + b.getBlock().getType());
				
				deleteList.add (b);
				//				FixedLengthBlocks.remove (b);  // Causes Concurrent Access exception. 
			}
			else {
				nextOut = b.getExpiry();
				// plugin.getLogger().fine ("Got beyond now (" + now + ") to next extinguish scheduled at " + nextOut);
				break; // found the next timeout in sorted list; can stop
			}
		} 
		// Outside of iterator loop, can not modify the TreeSet
		for (TimedBlock b: deleteList) {
			FixedLengthBlocks.remove (b);
		}
		deleteList.clear();
  
		if (FixedLengthBlocks.isEmpty() || plugin.getServer().getScheduler().isQueued(myTask.getTaskId())) {
			try {
				// stop next schedule
				if (FixedLengthBlocks.isEmpty())
					plugin.getLogger().info ("Processed all blocks; canceling task " + this.getTaskId());
				else 
					plugin.getLogger().fine ("Rescheduling task " + this.getTaskId());

				super.cancel();  // no more to put out
			} catch (IllegalStateException e) {
				// not scheduled should be normal condition
			}
		}
	    if ( !FixedLengthBlocks.isEmpty()) try {
			long delay = nextOut - now;
			myTask = plugin.getServer().getScheduler().runTaskLater (this.plugin, this, delay);  
			plugin.getLogger().fine ("Rescheduled extinguish in " + world.getName() + " in " + delay + " ticks");
		} catch (IllegalArgumentException e) {
			// plugin was null
		} catch (IllegalStateException e) {
			plugin.getLogger().warning ("How could task (" + this.getTaskId() + ") already be scheduled? " + e);
		}
	}
}

