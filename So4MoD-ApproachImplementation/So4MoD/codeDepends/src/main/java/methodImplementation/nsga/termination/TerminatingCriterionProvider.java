package com.nju.bysj.softwaremodularisation.nsga.termination;

import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;

public class TerminatingCriterionProvider {

	/*
	 * The most commonly used stopping criterion in Evolutionary Multi-objective Algorithms is an a priori fixed number of generations
	 * (or evaluations).
	 */
	public static TerminatingCriterion fixedTerminatingCriterion() {
		return (population, generationCount, maxGenerations) -> (generationCount <= maxGenerations);
	}

	public static TerminatingCriterion refactorTerminatingCriterion(int maxPopulationSize, int maxRecordSize) {
		return ((parentPopulation, generationCount, maxGenerations) -> {
			return generationCount <= maxGenerations && PreProcessLoadData.historyRecord.size() <= maxRecordSize;
//			long firstFrontSize = parentPopulation.getPopulace().stream().filter(p -> p.getRank() == 1).count();
//			System.out.println("parent size: " + parentPopulation.getPopulace().size() + " ; maxPopulation: " + maxPopulationSize);
//			System.out.println("first front: " + firstFrontSize + " ; percent: " + getFourBitsDoubleString(firstFrontSize*1.0 / maxPopulationSize));
//			return parentPopulation.getPopulace().size() != maxPopulationSize
//					|| !(firstFrontSize >= parentPopulation.getPopulace().size() * majority);
		});
	}
}
