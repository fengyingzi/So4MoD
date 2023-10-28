package com.nju.bysj.softwaremodularisation.nsga.termination;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Population;

public class NonDominatedMajority implements TerminatingCriterion {
	private final double majority;

	public NonDominatedMajority() {
		this(80.0d);
	}

	public NonDominatedMajority(double majority) {
		this.majority = majority;
	}

	@Override
	public boolean shouldRun(Population population, int generationCount, int maxGenerations) {
		if((maxGenerations > 0) && (maxGenerations < generationCount))
			return false;
		return Common.percent(
			this.majority,
			population.size()
		) > population.getPopulace().stream().filter(e -> e.getRank() == 1).count();
	}
}
