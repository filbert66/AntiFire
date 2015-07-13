package com.yahoo.phil_work.antifire;

import java.io.*;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;

import com.yahoo.phil_work.antifire.TimedExtinguisher;

public class TimedExtinguisherTest {

	@Test
	public void ticksConversion () {
		assertThat (1L, 
			is (equalTo (TimedExtinguisher.millisecsToTicks (TimedExtinguisher.ticksToMillisecs(1)))) );
		assertThat (200L, 
			is (equalTo (TimedExtinguisher.ticksToMillisecs (TimedExtinguisher.millisecsToTicks(200)))) );
	}
	
	// Per the nextRandom comment:
	//   min/max at 2*standard deviation. 70% should be within 1, and 27% within 2.
	@Test
	public void gaussianValidation () {
		double sum = 0;
		double oneDevCount = 0;
		double twoDevCount = 0;
	 	int CYCLES = 10000;
	 	int MIN = 0, MAX = 200, MEDIAN = 100;
	 	long stdDev = (MAX-MIN)/4;
		double Min = MAX, Max = MIN;
		
		for (int i=0; i<CYCLES; i++) {
			long sample = TimedExtinguisher.nextRandom (MIN, MAX);
			sum += sample;
			long delta = Math.abs (sample - MEDIAN);
			if (delta <= stdDev)
				oneDevCount++;
			else if (delta <= 2*stdDev)
				twoDevCount++;
			if (sample < Min) Min = sample;
			if (sample > Max) Max = sample;
		}
		System.out.println ("Median: " + sum/CYCLES + " expected " + MEDIAN);
		//rounding off the fractions, we should be close
		assertThat ((int)(sum / CYCLES), is(greaterThan (MEDIAN -3)));
		assertThat ((int)(sum / CYCLES), is(lessThan (MEDIAN+3)));

		//System.out.println ("within 1 * " + stdDev + ": " + oneDevCount);
		//System.out.println ("within 2 * " + stdDev + ": " + twoDevCount);
		System.out.println ("3sigma outliers: " + (CYCLES - oneDevCount - twoDevCount)/CYCLES + 
			" [" + Min + "," + Max + "]");
				
		assertThat (oneDevCount, is (closeTo (68 * CYCLES/100, 3* CYCLES/100)));
		assertThat (twoDevCount, is (closeTo (27 * CYCLES/100, 3* CYCLES/100)));
	} 
}