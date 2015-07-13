package com.yahoo.phil_work.antifire;

import java.io.*;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;

import com.yahoo.phil_work.antifire.TimedManager;
import com.yahoo.phil_work.antifire.IgniteCause;

public class TimedManagerTest {
	TimedManager tm = new TimedManager();

	@Test
	public void parseTimedFireCauses () {
		TimedManager.TimedFireCauses tfc;
		for (IgniteCause ic : IgniteCause.values()) {
			tfc = tm.new TimedFireCauses (ic.toString(), 0);
			assertThat (tfc.Cause, is (equalTo (ic)));
			
			tfc = tm.new TimedFireCauses (ic, 0);
			assertThat (tfc.Cause, is (equalTo (ic)));
		}
		
		//String tests
		List<Long> testLongs = Arrays.asList((long)0, (long)10000, Long.MAX_VALUE, -1L);
		for (Long value : testLongs) {
			tfc = tm.new TimedFireCauses ("LAVA", value.toString());
			assertThat (tfc.isRandom(), is (false));
			assertThat (tfc.getTime(), is (equalTo (value)));
		}
						
		tfc = tm.new TimedFireCauses ("LAVA", "1-123456");
		assertThat (tfc.isRandom(), is (true));
		assertThat (tfc.getMin(), is (equalTo ((long)1)));
		assertThat (tfc.getMax(), is (equalTo ((long)123456)));

		tfc = tm.new TimedFireCauses ("LAVA", "1 - 123456");
		assertThat (tfc.isRandom(), is (true));
		assertThat (tfc.getMin(), is (equalTo ((long)1)));
		assertThat (tfc.getMax(), is (equalTo ((long)123456)));

		tfc = tm.new TimedFireCauses ("LAVA", " 1 - 123456 ");
		assertThat (tfc.isRandom(), is (true));
		assertThat (tfc.getMin(), is (equalTo ((long)1)));
		assertThat (tfc.getMax(), is (equalTo ((long)123456)));
	}
	
}