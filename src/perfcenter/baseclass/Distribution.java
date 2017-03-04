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
/*
 * Defines distribution
 * exp, poi and nor can be represented using this class
 */
package perfcenter.baseclass;

import perfcenter.simulator.ExponentialDistribution;
import cern.jet.random.Beta;
import cern.jet.random.Binomial;
import cern.jet.random.ChiSquare;
import cern.jet.random.Gamma;
import cern.jet.random.Normal;
import cern.jet.random.Poisson;
import cern.jet.random.Uniform;

/**
 * This represents a distribution.
 * 
 * It can represent distributions like poisson, exponential, normal, binomial, uniform, beta,
 * normal, chi square and gamma.
 * 
 * @author akhila
 */
public class Distribution {

	String name_;
	Variable value1_;
	Variable value2_;
	double distributionMean = 0.0;

	public Distribution() {
		name_ = "exp";
		value1_ = new Variable("local", 0);
		value2_ = new Variable("local", 0);
		this.calculateMean();
	}

	public Distribution(String name) {
		name_ = name;
		value1_ = new Variable("local", 0);
		value2_ = new Variable("local", 0);
	}

	public Distribution(String name, double val) {
		name_ = name;
		value1_ = new Variable("local", val);
		value2_ = new Variable("local", 0);
	}

	public Distribution(String name, double val1, double val2) {
		name_ = name;
		value1_ = new Variable("local", val1);
		value2_ = new Variable("local", val2);
	}

	Distribution getCopy() {
		Distribution dcpy = new Distribution(name_);
		if (value1_.getName().compareToIgnoreCase("local") != 0) {
			dcpy.value1_ = value1_;
		} else {
			dcpy.value1_.value = value1_.value;
		}

		if (value2_.getName().compareToIgnoreCase("local") != 0) {
			dcpy.value2_ = value2_;
		} else {
			dcpy.value2_.value = value2_.value;
		}
		return dcpy;
	}

	public void addName(String name) {
		name_ = name;
	}

	public void addParam1(Variable val) {
		value1_ = val;
	}

	public void addParam2(Variable var) {
		value2_ = var;
	}

	public double getServiceTime() {
		return value1_.getValue();
	}

	public void setServiceTime(double servtime) {
		value1_.value = servtime;
	}

	public String toString() {
		return new StringBuilder(name_).append(value1_.getValue()).toString();
	}

	public void calculateMean() {
		if (name_.compareToIgnoreCase("exp") == 0) {
			// parameter list for normal distribution is (mean)
			distributionMean = value1_.getValue();
		}
		if (name_.compareToIgnoreCase("poi") == 0) {
			// parameter list for normal distribution is (mean)
			distributionMean = value1_.getValue();
		}
		if (name_.compareToIgnoreCase("nor") == 0) {
			// parameter list for normal distribution is (mean,variance)
			// mean is (mean)
			distributionMean = value1_.getValue();
		}
		if (name_.compareToIgnoreCase("binom") == 0) {
			// parameter list for Binomial distribution is (n,probability)
			// mean is n*p
			distributionMean = value1_.getValue() * value2_.getValue();
		}
		if (name_.compareToIgnoreCase("uni") == 0) {
			// parameter list for Uniform distribution is (a,b)
			// mean is (a+b)/2
			distributionMean = (value1_.getValue() + value2_.getValue()) / 2;
		}
		if (name_.compareToIgnoreCase("beta") == 0) {
			// parameter list for beta distribution is (alpha,beta) both are non-negative shape parameters
			// mean is alpha/(alpha+beta)
			distributionMean = (value1_.getValue()) / (value1_.getValue() + value2_.getValue());
		}
		if (name_.compareToIgnoreCase("chisqr") == 0) {
			// parameter list for ChiSquare distribution is (k) which is non-negative, degree of freedom
			// mean is k
			distributionMean = value1_.getValue();
		}

		if (name_.compareToIgnoreCase("gamma") == 0) {
			// parameter list for Gamma distribution is (k>0,theta>0) k is (shape,real) theta is(scale,real)
			// mean is = k*theta
			distributionMean = value1_.getValue() * value2_.getValue();
		}
		if (name_.compareToIgnoreCase("const") == 0) {
			distributionMean = value1_.getValue();
		}
	}

	public double nextRandomVal(double procspeed) {

		if (name_.compareToIgnoreCase("exp") == 0) {
			// parameter list for distribution is (mean)
			ExponentialDistribution exp = new ExponentialDistribution();
			return exp.nextExp(value1_.getValue() / procspeed);

		}
		if (name_.compareToIgnoreCase("poi") == 0) {
			// parameter list for normal distribution is (mean)
			return Poisson.staticNextInt(value1_.getValue());
		}
		if (name_.compareToIgnoreCase("nor") == 0) {
			// parameter list for normal distribution is (mean,variance)
			return Normal.staticNextDouble(value1_.getValue(), value2_.getValue());
		}
		if (name_.compareToIgnoreCase("binom") == 0) {
			// parameter list for normal distribution is (n,p)
			return Binomial.staticNextInt((int) value1_.getValue(), value2_.getValue());
		}

		if (name_.compareToIgnoreCase("uni") == 0) {
			// parameter list for normal distribution is (a,b)
			return Uniform.staticNextDoubleFromTo(value1_.getValue(), value2_.getValue());
		}

		if (name_.compareToIgnoreCase("beta") == 0) {
			// parameter list for normal distribution is (alpha,beta)
			return Beta.staticNextDouble(value1_.getValue(), value2_.getValue());
		}

		if (name_.compareToIgnoreCase("chisqr") == 0) {
			// parameter list for normal distribution is (k)
			return ChiSquare.staticNextDouble(value1_.getValue());
		}

		if (name_.compareToIgnoreCase("gamma") == 0) {
			// parameter list for normal distribution is (k,theta)
			return Gamma.staticNextDouble(value1_.getValue(), value2_.getValue());
		}
		if (name_.compareToIgnoreCase("const") == 0) {

			return value1_.getValue();
		}
		return (0);
	}
}
