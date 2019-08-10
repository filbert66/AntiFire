/***
 * 30 Jul 2015 : PSW : Added isPlaced() to be compatible with MC 1.8
 */

package com.yahoo.phil_work.antifire;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockState;
import java.util.Comparator;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.material.MaterialData; // have to include until the remove the Deprecated functions that I have to override.
import org.bukkit.World;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

class TimedBlock implements BlockState
{
	private BlockState state;
    private long expires;
 
 	public TimedBlock (BlockState b, long e) {
 		state = b;
 		this.expires = e;
	}   
 	public TimedBlock (Block b, long e) { 
 		this (b.getState(), e);
	}
	    
    public void setExpiry (long e) { expires = e; }
    public long getExpiry () { return expires; }
 		
	static public class TimedBlockComparator implements Comparator<TimedBlock> {
		// Returns 0 if two disparate blocks expire at the same time
		// so can return compare (a,b) == 0 even though a.equals (b) != true
		/*  If needed, could add 
		 *   If expires equal, calculate vectors and take diff of vectors. If magnitude of result is zero, they are the point
		 *   Or maybe Location has a comparison function that ignores yaw, roll.
		 */
		@Override
		public int compare (TimedBlock s1,TimedBlock s2)
		{
			return (int)(s1.expires - s2.expires); 
		}
	} 
    
    public BlockState getState() { return state; }
    
    // Begin BlockState routines
    public Block getBlock() { return state.getBlock(); }

    @Deprecated public MaterialData getData() { return state.getData(); }
	public Material getType() { return state.getType(); } 

	public byte getLightLevel() { return state.getLightLevel(); }

	public World getWorld() { return state.getWorld(); }

	public int getX() { return state.getX(); }
	public int getY() { return state.getY(); }
	public int getZ() { return state.getZ(); }

	public Location getLocation() { return state.getLocation(); }

	public Location getLocation(Location loc) {
		if (loc == null)
			return null;
		else { 
			loc = state.getLocation();
			return loc;
		}
	}
  	public Chunk getChunk() { return state.getChunk(); }

	@Deprecated public void setData(MaterialData data) { state.setData(data); }
	public void setBlockData(BlockData data) { state.setBlockData (data); }
	public BlockData getBlockData​() { return state.getBlockData (); }
	public void setType(Material type) { state.setType (type); }
	

	public boolean update() { return state.update(); }

	public boolean update(boolean force) { return state.update (force); }

	public boolean update(boolean force, boolean applyPhysics) {
		return state.update (force, applyPhysics); 
	}
	public boolean isPlaced () { return false; } // never used

	@Deprecated
	public void setRawData(byte data) { state.setRawData(data); } 
//	@Deprecated
//	public boolean setTypeId(int type) { return state.setTypeId(type); }  
	@Deprecated
	public byte getRawData() { return state.getRawData(); } 
//	@Deprecated
//	public int getTypeId() { return state.getTypeId(); } 
	
	// MEtaData routines
	public boolean hasMetadata (String s) { return state.hasMetadata (s); }
	public void removeMetadata (String s, Plugin p) { state.removeMetadata (s,p); }
	public void setMetadata (String s, MetadataValue v) { state.setMetadata (s,v); }
	public List <MetadataValue> getMetadata (String s) { return state.getMetadata (s); } 
}

