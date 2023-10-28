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

package com.nju.bysj.softwaremodularisation.nsga.plugin;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.GroupItemAllele;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.IntegerAllele;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Population;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.*;

public class DefaultPluginProvider {

	public static PopulationProducer defaultPopulationProducer() {
		return (populationSize, chromosomeLength, geneticCodeProducer, fitnessCalculator) -> {
			List<Chromosome> populace = new ArrayList<>();
			for(int i = 0; i < populationSize; i++)
				populace.add(
					new Chromosome(
						geneticCodeProducer.produce(chromosomeLength)
					)
				);
			return new Population(populace);
		};
	}

	public static ChildPopulationProducer defaultChildPopulationProducer() {
		return (parentPopulation, crossover, mutation, populationSize) -> {
			List<Chromosome> populace = new ArrayList<>();
			while(populace.size() < populationSize)
				if((populationSize - populace.size()) == 1)
					populace.add(
						mutation.perform(
							Common.crowdedBinaryTournamentSelection(parentPopulation)
						)
					);
				else
					for(Chromosome chromosome : crossover.perform(parentPopulation))
						populace.add(mutation.perform(chromosome));

			return new Population(populace);
		};
	}

	public static PopulationProducer refactorInitPopulationProducer() {
		return (populationSize, chromosomeLength, geneticCodeProducer, fitnessCalculator) -> {
			List<Chromosome> populace = new ArrayList<>();
			GroupItemAllele initialGroup = new GroupItemAllele(new ArrayList<>());
			clusters.forEach(fa -> initialGroup.getGene().add(fa));
			Chromosome initialIndividual = new Chromosome(new ArrayList<GroupItemAllele>() {{ add(initialGroup); }});
			populace.add(initialIndividual);
			return new Population(populace);
		};
	}

	public static ChildPopulationProducer refactorGenerateChildrenProducer() {
		return (parentPopulation, crossover, mutation, populationSize) -> {
			return new Population(crossover.perform(parentPopulation));
		};
	}


	public static ChildPopulationProducer childrenProducer(float mutateProbability) {
		return (parentPopulation, crossover, mutation, populationSize) -> {
			List<Chromosome> populace;
			populace = crossover.perform(parentPopulation);

			int mutateNum = (int) (parentPopulation.getPopulace().size() * mutateProbability);
			mutateNum = Math.max(mutateNum, 1);
			HashSet<Integer> mutateParents = new HashSet<>();
			for (int i = 0; i < mutateNum; i++) {
				int id;
				do {
					id = ThreadLocalRandom.current().nextInt(0, parentPopulation.getPopulace().size());
				}  while (mutateParents.contains(id));
				mutateParents.add(id);
			}

			for (int id : mutateParents) {
				Chromosome child = mutation.perform(parentPopulation.get(id));
				if (!historyRecord.containsKey(child)) {
//					historyRecord.put(child, ifChromosomeOverload(child)); // modified by willch
					historyRecord.put(child, true);
					populace.add(child);
				}
			}

			return new Population(populace);
		};
	}

	public static ChildPopulationProducer randomChildrenProducer(float mutateProbability) {
		return (parentPopulation, crossover, mutation, populationSize) -> {
			List<Chromosome> populace;
			populace = crossover.perform(parentPopulation);

			int mutateNum = (int) (parentPopulation.getPopulace().size() * mutateProbability);
			mutateNum = Math.max(mutateNum, 1);
			Chromosome emptyChromosome = new Chromosome(parentPopulation.getPopulace().get(0));
			for (int i = 0; i < mutateNum; i++) {
				Chromosome child = mutation.perform(emptyChromosome);
				if (!historyRecord.containsKey(child)) {
					historyRecord.put(child, Boolean.FALSE);
					populace.add(child);
				}
			}

			return new Population(populace);
		};
	}

	public static PopulationProducer fileInitPopulationProducer() {
		return (populationSize, chromosomeLength, geneticCodeProducer, fitnessCalculator) -> {
			int overloadSrvFiles = classFileList.size();
			List<IntegerAllele> modularAlleles = new ArrayList<>(overloadSrvFiles);
			for (int i = 0; i < overloadSrvFiles; i++) {
				modularAlleles.add(new IntegerAllele(i));
			}
			Chromosome initIndividual = new Chromosome(new Chromosome(modularAlleles));
			historyRecord.put(initIndividual, true);
			List<Chromosome> populace = new ArrayList<Chromosome>() {{ add(initIndividual); }};
			return new Population(populace);
		};
	}
}
