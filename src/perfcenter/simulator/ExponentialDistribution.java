/*
 * Copyright (C) 2011-12  by Varsha Apte - <varsha@cse.iitb.ac.in>, et al.
 * This file is distributed as part of PerfCenter
 *
 *  This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package perfcenter.simulator;

import java.util.Random;

/** Exponential distribution which will give the random numbers from distribution exp(l) given the mean l. */
public class ExponentialDistribution {
	private static Random r;// changed to static by niranjan

	/**
	 * Create a new random number generator based on the current time.
	 */
	public ExponentialDistribution() { // initializes r based on the current time
		r = new Random();
	}

	/**
	 * Create a new random number generator using <TT>seed</TT> as a seed. A seed is a number used to generate a certain pattern of random numbers. It
	 * is <B>NOT</B> the upper or lower bound, nor the expected value.
	 * 
	 * @param seed
	 *            the seed for the random number generator
	 */
	public ExponentialDistribution(long seed) {
		r = new java.util.Random(seed);
	}

	/**
	 * Select a number from an exponential distribution. returns random number selected from exponential distribution with expected value alpha.
	 * 
	 * @return a random real number
	 */
	public double nextExp(double alpha) {
		double x;
		while ((x = r.nextDouble()) == 0)
			; // loop until x!=0
		return (-alpha * Math.log(x));
	}

	/**
	 * Select a number from a uniform distribution.
	 * 
	 * @post returns a random int uniformly distributed between low and high inclusive.
	 * @param low
	 *            the minimum random integer to return
	 * @param high
	 *            the maximum random integer to return
	 * @return a random integer between low and high
	 */
	public int nextUniformInt(int low, int high) {
		double x;
		while ((x = r.nextDouble()) == 1)
			; // loop until x!=1;
		return (int) Math.floor(x * (high - low + 1) + low);
	}
}
