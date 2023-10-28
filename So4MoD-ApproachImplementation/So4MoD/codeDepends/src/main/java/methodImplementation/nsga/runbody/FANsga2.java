/*
 * MIT License
 *
 * Copyright (c) 2019 Debabrata Acharya
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nju.bysj.softwaremodularisation.nsga.runbody;

import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.IntegerAllele;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.nju.bysj.softwaremodularisation.nsga.Common.*;
import static com.nju.bysj.softwaremodularisation.nsga.PostProcessShowData.*;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.clusters;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.historyRecord;

public class FANsga2 {

	public static final int DOMINANT = 1;
	public static final int INFERIOR = 2;
	public static final int NON_DOMINATED = 3;

	private final Configuration configuration;

	public FANsga2(Configuration configuration) {
		this.configuration = configuration;
	}

	public Population run(int iteration, String programPath, String objectivePath, String frontPath, String costPath, boolean ifCutOverload) {
		if(!this.configuration.isSetup())
			throw new UnsupportedOperationException(Configuration.CONFIGURATION_NOT_SETUP + "\n" + this.configuration);

		Population parent = this.preparePopulation(
			this.configuration.getPopulationProducer().produce(
				this.configuration.getPopulationSize(),
				this.configuration.getChromosomeLength(),
				this.configuration.getGeneticCodeProducer(),
				null
			)
		);

		Population child = this.preparePopulation(
			this.configuration.getChildPopulationProducer().produce(
				parent,
				this.configuration.getCrossover(),
				this.configuration.getMutation(),
				this.configuration.getPopulationSize()
			)
		);

		int generation = 0;
		while(configuration.getTerminatingCriterion().shouldRun(parent, ++generation, this.configuration.getGenerations())) {
			parent = this.getChildFromCombinedPopulation(
				this.preparePopulation(
					Common.combinePopulation(parent, child, this.configuration.getPopulationSize(), ifCutOverload)
				)
			);

			child = this.preparePopulation(
				this.configuration.getChildPopulationProducer().produce(
					parent,
					this.configuration.getCrossover(),
					this.configuration.getMutation(),
					this.configuration.getPopulationSize()
				)
			);
			if (generation % 10 == 0) {
				System.out.println("  generation: " + generation);
			}
		}

		parent = this.getChildFromCombinedPopulation(
				this.preparePopulation(Common.combinePopulation(parent, child, this.configuration.getPopulationSize(), ifCutOverload)));
		List<Chromosome> firstFront = Common.splitPopulationByNonDominatedFront(parent).get(0);
		System.out.println("last population：" + parent.getPopulace().size());
		System.out.println("front：" + firstFront.size());
		firstFront = firstFront.stream().filter(ch -> !historyRecord.get(ch)).collect(Collectors.toList());
		System.out.println("filtering：" + firstFront.size());
		System.out.println("iterations：" + (generation - 1));
		int sc = 1;
		for (Chromosome chromosome : firstFront) {
			List<IntegerAllele> geneticCode = new ArrayList<>();
			chromosome.getGeneticCode().forEach(gc -> geneticCode.add((IntegerAllele) gc));
			outputFrontData.add(geneticCode);
			System.out.println("geneticCode: " + geneticCode +
					" ; fitness: " + chromosome.getObjectiveValues() + " ; record: " + historyRecord.get(chromosome));

			outputObjectiveData.add(chromosome.getObjectiveValues());
			Program curProgram = new Program("reconstruction plan" + sc);
			curProgram.children = new ArrayList<>();
			List<List<Integer>> groupSet = chromosome.getSortedGroupMapValueSet();
			for (int i = 0; i < groupSet.size(); i++) {
				SubService subService = new SubService("service" + (i+1), new ArrayList<>());
				for (int j = 0; j < groupSet.get(i).size(); j++) {
					int faId = groupSet.get(i).get(j);
					subService.children.add(new FAFile("FA - " + faId, clusters.get(faId).fileList));
				}
				curProgram.children.add(subService);
			}
			outputPrograms.add(curProgram);
			sc ++;
		}

		long count = historyRecord.values().stream().filter(f -> !f).count();
		try {
			writeStringToFile(JSONObject.toJSONString(outputPrograms), programPath + iteration + ".json");
			writeObjectivesToFile(objectivePath + iteration + ".txt");
			writeFrontToFile(frontPath + iteration + ".txt");
			appendStringToFile("i:" + (generation - 1) +
					"\nnotOverload:" + count +
					"\noverload:" + (historyRecord.size() - count),
					costPath + iteration + ".txt", true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("no-filtering：" + count + " ; filtering：" + (historyRecord.size() - count));
		return child;
	}

	/**
	 * This method takes a `Population` object and basically performs all the operations needed to be performed on the parent
	 * population in each generation. It executes the following operations on the population instance in order.
	 *
	 * - It calculates the objective values of all the chromosomes in the population based on the objective functions set
	 *	in the `Configuration` instance.
	 * - It then runs fast non-dominated sort on the population as defined in `NSGA-II paper [DOI: 10.1109/4235.996017] Section III Part A.`
	 * - It then assigns crowding distance to each chromosome.
	 * - Finally, it sorts the chromosomes in the population based on its assigned rank.
	 *
	 * @param	population	the population instance to undergo the above steps
	 * @return				the same population instance that was passed as an argument
	 */
	public Population preparePopulation(Population population) {
		if (population.size() == 0) {
			return population;
		}

		Common.calculateObjectiveValues(population, this.configuration.objectives);
		this.fastNonDominatedSort(population);
		this.crowdingDistanceAssignment(population);
		population.getPopulace().sort(Comparator.comparingInt(Chromosome::getRank));
		return population;
	}

	/**
	 * This method takes a `Population` of size `2N` (_a combination of parent and child, both of size `N`,
	 * according to the originally proposed algorithm_) and returns a new `Population` instance of size `N` by
	 * selecting the first `N` chromosomes from the combined population, based on their rank. If it has to choose `M` chromosomes
	 * of rank `N` such that `M &gt; N`, it then sorts the `M` chromosomes based on their crowding distance.
	 *
	 * @param	combinedPopulation	the combined population of parent and child of size 2N
	 * @return						the new population of size N chosen from the combined population passed as parameter
	 */
	public Population getChildFromCombinedPopulation(Population combinedPopulation) {
		List<List<Chromosome>> frontLists = Common.splitPopulationByNonDominatedFront(combinedPopulation);
		List<Chromosome> offSpring = new ArrayList<>();
		int index = 0;
		for (; index < frontLists.size(); index++) {
			if (offSpring.size() + frontLists.get(index).size() <= this.configuration.getPopulationSize()) {
				offSpring.addAll(frontLists.get(index));
			} else {
				break;
			}
		}

		if (offSpring.size() < this.configuration.getPopulationSize() && index < frontLists.size()) {
			int restNum = this.configuration.getPopulationSize() - offSpring.size();
			List<Chromosome> chromosomes = frontLists.get(index);
			chromosomes.sort(Comparator.comparingDouble(Chromosome::getCrowdingDistance).reversed());
			offSpring.addAll(chromosomes.subList(0, restNum));
		}

		return new Population(offSpring);
	}

	/**
	 * This is an implementation of the fast non-dominated sorting algorithm as defined in the
	 * NSGA-II paper [DOI: 10.1109/4235.996017] Section III Part A.
	 *
	 * @param   population  the population object that needs to undergo fast non-dominated sorting algorithm
	 */
	public void fastNonDominatedSort(Population population) {
		if (population.getPopulace().size() == 0) {
			return;
		}

		List<Chromosome> populace = population.getPopulace();
		for(Chromosome chromosome : populace) {
			chromosome.reset();
		}

		for(int i = 0; i < populace.size() - 1; i++) {
			for (int j = i + 1; j < populace.size(); j++)
				switch (this.dominates(populace.get(i), populace.get(j))) {
					case FANsga2.DOMINANT:
						populace.get(i).addDominatedChromosome(populace.get(j));
						populace.get(j).incrementDominatedCount(1);
						break;

					case FANsga2.INFERIOR:
						populace.get(i).incrementDominatedCount(1);
						populace.get(j).addDominatedChromosome(populace.get(i));
						break;

					case FANsga2.NON_DOMINATED:
						break;
				}
			if(populace.get(i).getDominatedCount() == 0)
				populace.get(i).setRank(1);
		}

		if(population.getLast().getDominatedCount() == 0)
			population.getLast().setRank(1);

		while(Common.populaceHasUnsetRank(populace)) {
			populace.forEach(chromosome -> {
				if(chromosome.getRank() != -1) {
					chromosome.getDominatedChromosomes().forEach(dominatedChromosome -> {
						if(dominatedChromosome.getDominatedCount() > 0) {
							dominatedChromosome.incrementDominatedCount(-1);
							if(dominatedChromosome.getDominatedCount() == 0)
								dominatedChromosome.setRank(chromosome.getRank() + 1);
						}
					});
				}
			});
		}
	}

	/**
	 * This is the implementation of the crowding distance assignment algorithm as defined in the
	 * NSGA-II paper [DOI: 10.1109/4235.996017] Section III Part B.
	 * this ensures diversity preservation.
	 *
	 * @param   population   the population whose crowding distances are to be calculated.
	 */
	public void crowdingDistanceAssignment(Population population) {
		int size = population.size();

		for(int i = 0; i < this.configuration.objectives.size(); i++) {
			int iFinal = i;
			population.getPopulace().sort(Collections.reverseOrder(Comparator.comparingDouble(c -> c.getObjectiveValue(iFinal))));
			Common.normalizeSortedObjectiveValues(population, i);
			population.get(0).setCrowdingDistance(Double.MAX_VALUE);
			population.getLast().setCrowdingDistance(Double.MAX_VALUE);

			double maxNormalizedObjectiveValue = population.selectMaximumNormalizedObjectiveValue(i);
			double minNormalizedObjectiveValue = population.selectMinimumNormalizedObjectiveValue(i);

			for(int j = 1; j < size; j++)
				if(population.get(j).getCrowdingDistance() < Double.MAX_VALUE) {
					double previousChromosomeObjectiveValue = population.get(j - 1).getNormalizedObjectiveValues().get(i);
					double nextChromosomeObjectiveValue = population.get(j + 1).getNormalizedObjectiveValues().get(i);
					double objectiveDifference = nextChromosomeObjectiveValue - previousChromosomeObjectiveValue;
					double minMaxDifference = maxNormalizedObjectiveValue - minNormalizedObjectiveValue;

					population.get(j).setCrowdingDistance(
						Common.roundOff(
							population.get(j).getCrowdingDistance() +
							(objectiveDifference / minMaxDifference),
							10
						)
					);
				}
		}
	}

	/**
	 * This method checks whether one chromosome dominates the other chromosome or not. While the actual domination
	 * logic has been described in the `isDominant(Chromosome, Chromosome)` method, the `dominates(Chromosome, Chromosome)
	 * method returns one among the three values based on whether chromosome1 is dominant over chromosome2,
	 * or is inferior to chromosome2 or whether both of them are non-dominating, by returning
	 * `com.debacharya.nsgaii.NSGA2.DOMINANT`, `com.debacharya.nsgaii.NSGA2.INFERIOR` or
	 * `com.debacharya.nsgaii.NSGA2.NON_DOMINATED` respectively.
	 *
	 * @param	chromosome1	the chromosome to check whether it is dominating, inferior or non-dominated
	 * @param	chromosome2	the chromosome against which chromosome1 is checked
	 * @return				either NSGA2.DOMINANT, NSGA2.INFERIOR or NSGA2.NON_DOMINATED
	 */
	public int dominates(Chromosome chromosome1, Chromosome chromosome2) {
		if(this.isDominant(chromosome1, chromosome2)) return FANsga2.DOMINANT;
		else if(this.isDominant(chromosome2, chromosome1)) return FANsga2.INFERIOR;
		else return FANsga2.NON_DOMINATED;
	}

	/**
	 * This method checks whether chromosome1 dominates chromosome2.
	 * Requires that none of the values of the objective function values of chromosome1 is smaller
	 * than the values of the objective function values of chromosome2 and
	 * at least one of the values of the objective function of chromosome1 is greater than
	 * the corresponding value of the objective function of chromosome2.
	 *
	 * @param   chromosome1     the chromosome that may dominate
	 * @param   chromosome2     the chromosome that may be dominated
	 * @return                  boolean logic whether chromosome1 dominates chromosome2.
	 */
	public boolean isDominant(Chromosome chromosome1, Chromosome chromosome2) {

		boolean atLeastOneIsBetter = false;

		for(int i = 0; i < this.configuration.objectives.size(); i++)
			if(chromosome1.getObjectiveValues().get(i) < chromosome2.getObjectiveValues().get(i))
				return false;
			else if(chromosome1.getObjectiveValues().get(i) > chromosome2.getObjectiveValues().get(i))
				atLeastOneIsBetter = true;

		return atLeastOneIsBetter;
	}
}
