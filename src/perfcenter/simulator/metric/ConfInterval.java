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
package perfcenter.simulator.metric;

import java.util.ArrayList;

import perfcenter.baseclass.ModelParameters;
import perfcenter.simulator.SimulationParameters;
import umontreal.iro.lecuyer.probdist.StudentDist;
import cern.colt.list.DoubleArrayList;
import static cern.jet.stat.Descriptive.sampleVariance;
import static cern.jet.stat.Descriptive.mean;

/**
 * Given a set of data points, this class helps to calculate confidence intervals for that set.
 */
public class ConfInterval { //ARCHITECTURE: merge this class with MetricSim
	public double totalDurationPrev = 0;
	public double totalValuePrev = 0;
	public double totalSamplesPrev = 0; // FIXME: this should be weeded out eventually, just keep time and val

	private double data[];
	private ArrayList<Double> ci;
	public double prob;
	
	public ConfInterval(double respProb) {
		prob = respProb;
		data = new double[(int) ModelParameters.getNumberOfReplications()];
		ci = new ArrayList<Double>();
	}

	/**
	 * here we save the performance measures in corresponding matrix cells (based on replicationNumber & sampleNumber)
	 * 
	 * These metrices are later used to calculate the confidence intervals
	 */

	public void recordCISample(double val) {
		assert val >= 0 : val;
		data[SimulationParameters.replicationNo] = val;
	}

	/**
	 * calls functions to calculate confidence intervals for different performance measures
	 * 
	 */
	public void calculateConfidenceIntervals() {
//		for(int i=0;i<data.length;i++) {
//			for(int j=0;j<data[i].length;j++) {
//				String value = Double.toString(data[i][j]);
//				int dot = value.indexOf('.');
//				value = value.substring(0, dot+2);
//				System.out.print(value+ ",");
//			}
//			System.out.println("\n");
//		}
		DoubleArrayList dataMeanVector = new DoubleArrayList();

		// use welches procedure to decide the data vector over which confidence
		// interval to be calculated

		dataMeanVector = welchsProcedure(data);

		ci = confIntervals(dataMeanVector, prob);
	}

	/**
	 * here we 1. smooth the high frequency occilations in data 2. decide the cutoff points from the resulting vector
	 * 
	 * 
	 * @param data
	 *            = matrix[replication number][sample number] containing performance measure values
	 * @return
	 */

//	public int decideCutOff(double data[][]) {
//		int index = 0;
//		double tolerance = 0.05;
//
//		data[0][0] = 0;
//
//		double temp[] = new double[(int) ModelParameters.getTotalNumberOfSamples()];
//		for (int i = 0; i < ModelParameters.getTotalNumberOfSamples(); i++) {
//			double sum = 0;
//			for (int j = 0; j < ModelParameters.getNumberOfReplications(); j++) {
//				sum = sum + data[j][i];
//			}
//			temp[i] = sum / ModelParameters.getNumberOfReplications();
//		}
//
//		int window = 10;
//		for (int i = 0; i < ModelParameters.getTotalNumberOfSamples(); i++) {
//			if (i < window) {
//				double sum = 0;
//				for (int j = 0; j <= 2 * i; j++) {
//					sum = sum + temp[j];
//				}
//				temp[i] = sum / (2 * i + 1);
//			} else {
//				double sum = 0;
//				for (int j = i - window; j <= i + window && j < ModelParameters.getTotalNumberOfSamples(); j++) {
//					sum = sum + temp[j];
//				}
//				temp[i] = sum / (2 * window + 1);
//			}
//		}
//
//		boolean indexFound = false;
//
//		for (int i = window + 1; i < ModelParameters.getTotalNumberOfSamples() && !indexFound; i++) {
//			boolean flag = true;
//
//			for (int j = i - window; j < i; j++) {
//				double tempVal = (temp[i] - temp[j]) / temp[i];
//				if (tempVal > temp[i] * tolerance)
//					flag = false;
//			}
//			if (flag) {
//				indexFound = true;
//				index = i;
//			}
//		}
//		return (index);
//	}

	public DoubleArrayList welchsProcedure(double data[]) {
		// call decideCutOff() to decide the suitable portion of data after
		// ignoring the parts affected by warmup or cool off effect

//		int begin = (int) ModelParameters.getStartUpSampleNumber();// decideCutOff(data);
//		int end = (int) ModelParameters.getCoolDownSampleNumber();// PerfSim.numOfSampleIntervals; //XXX

		// this vector contains the means of replications of the portion of
		// matrix selected above
		DoubleArrayList returnVal = new DoubleArrayList();

		for (int i = 0; i < ModelParameters.getNumberOfReplications(); i++) {
			returnVal.add(data[i]);
		}

		return (returnVal);
	}

	// calculate confidence interval here
	public ArrayList<Double> confIntervals(DoubleArrayList dataMeanVector, double probability) {

		ArrayList<Double> temp = new ArrayList<Double>();

		double mean = mean(dataMeanVector);

		double a = ((1 - probability) / 2) + probability;

		double confidence = 
				StudentDist.inverseF(dataMeanVector.size(), a) * Math.sqrt(sampleVariance(dataMeanVector, mean)) / Math.sqrt(dataMeanVector.size());

		temp.add(new Double(mean));
		temp.add(new Double(confidence));
		return (temp);
	}

	public double getCI() {
		return ((Double) ci.get(1)).doubleValue();
	}

	public double getMean() {
		return ((Double) ci.get(0)).doubleValue();
	}

	public void clearValuesButKeepConfInts() {
		totalDurationPrev = 0;
		totalSamplesPrev = 0;
		totalValuePrev = 0;
	}


	public double getDataPoint(int replicationNumber) {
		return data[replicationNumber];
	}
}
