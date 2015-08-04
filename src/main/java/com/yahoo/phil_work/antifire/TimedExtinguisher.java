// See here: http://wiki.bukkit.org/Scheduler_Programming
// Removed doDaylightCycle dependency
// implemented addRandom with gaussian distribution
// Added remove() to catch same block being rescheduled

 package com.yahoo.phil_work.antifire;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.ArrayList;
import java.lang.NullPointerException;
import java.util.Random;

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
	private static final Random rng = new Random();

	BukkitTask myTask;
	
	TimedExtinguisher (Plugin p, World w) 
	{ 
		if (p == null || w == null)
			throw new NullPointerException("in TimedExtinguisher constructor");
		world = w;	
		plugin = p; 
		FixedLengthBlocks = new TreeSet<TimedBlock> (new TimedBlock.TimedBlockComparator());
		
		String worldName = w.getName();
	}
	static long ticksToMillisecs (final long ticks) {
		return ticks * 1000 /20L; 
	}
	static long millisecsToTicks (final long ms) {
		return ms * 20L /1000L; 
	}
	// adds block to extinguish with standard bell curve between min/max, such that min/max are at 
	//   2*standard deviation. 70% should be within 1, and 27% within 2.
	static long nextRandom (long min, long max) {
		long width = max - min;
		long mean = (max + min) /2;
		
		return (long)(rng.nextGaussian() * width/4 + mean);
	}
	// adds block to extinguish with standard bell curve between min/max
	public void addRandom (BlockState state, long minTicks, long maxTicks) {
		long delay = nextRandom (minTicks, maxTicks);
		plugin.getLogger().fine ("Random " + delay + " from (" +minTicks+ ", " +maxTicks + ")");
		this.add (state, delay);
	}
	// Managing list of blocks to put out
	// delay MUST be in TICKS
	public void add (BlockState state, long delay) {
		// expiry in TICKs
		if (delay < 0)
			delay = 0;
		long expiry = ticksToMillisecs (delay) + System.currentTimeMillis();
		
		if (state == null) return;
		
		if (state.getWorld().getUID() == this.world.getUID()) {
			TimedBlock tb = new TimedBlock (state, expiry);
			
			// if (contains (state.getLocation())) // don't search twice
				remove (state.getLocation()); // in case already set with different timer
			FixedLengthBlocks.add (tb);
	
	  	    if (expiry == FixedLengthBlocks.first().getExpiry()) 
			{ // either was empty or this one is earliest and sorted to top.
				try {
					super.cancel();	// don't erase the list!
				} catch (IllegalStateException e) {
					plugin.getLogger().fine ("First run; wasn't scheduled");
				}
				try {
					// FUTURE in 1.8: Will I be able to call this any more? May have to use this.runTaskLater()...
					//   but 1.7 had bug there that wouldn't allow rescheduling a task.
					myTask = plugin.getServer().getScheduler().runTaskLater (plugin, this, delay); // reschedule
				} catch (IllegalArgumentException e) {
					// plugin was null
				} catch (IllegalStateException e) {
					plugin.getLogger().warning ("Error; was already scheduled" + e);
				}
			}
			else {
				long future = FixedLengthBlocks.first().getExpiry() - System.currentTimeMillis();
				plugin.getLogger().fine ("Already scheduled for '" + world.getName() + "' in " + future + " ms");
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
	public void remove (Location loc) {
		World w = loc.getWorld();
		if (w.getUID() == this.world.getUID()) {
		  // unfortunately have to do a linear search...
		  int i = 0;
		  TimedBlock found = null;
		  
		  for (TimedBlock b : FixedLengthBlocks) {
		  	// plugin.getLogger().finer (w.getName() + " block " + i++ + " expires " + b.getExpiry());
		  	
		  	int x = loc.getBlockX(), y = loc.getBlockY(), z= loc.getBlockZ();
		  	Location bl = b.getLocation();
		  	int bx = bl.getBlockX(), by = bl.getBlockY(), bz = bl.getBlockZ();
			if (x==bx && y==by && z==bz) {
				found = b;
				break;
			}
		  }
		  if (found != null)
		  	FixedLengthBlocks.remove (found);
	  }
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
		final long now = System.currentTimeMillis();
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
						plugin.getLogger().finer ("block update to put out fire at " + b.getExpiry() + " worked");
				}
				else // Shouldn't get this error unless user or fade put the fire out, or block burnt
					plugin.getLogger().info ("Fire block changed before timeout to " + b.getBlock().getType());
				
				deleteList.add (b);
				//				FixedLengthBlocks.remove (b);  // Causes Concurrent Access exception. 
			}
			else {
				nextOut = b.getExpiry();
				plugin.getLogger().fine ("Got beyond now (" + now + ") to next extinguish scheduled at " + nextOut);
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
			long delay = millisecsToTicks (nextOut - now);
			if (delay == 0) delay++;	// ensure longer wait
			
			myTask = plugin.getServer().getScheduler().runTaskLater (this.plugin, this, delay);  
			plugin.getLogger().fine ("Rescheduled extinguish in " + world.getName() + " in " + delay + " ticks");
		} catch (IllegalArgumentException e) {
			// plugin was null
		} catch (IllegalStateException e) {
			plugin.getLogger().warning ("How could task (" + this.getTaskId() + ") already be scheduled? " + e);
		}
	}
}

