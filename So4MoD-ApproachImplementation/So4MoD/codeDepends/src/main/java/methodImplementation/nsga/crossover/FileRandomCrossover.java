package com.nju.bysj.softwaremodularisation.nsga.crossover;

import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Population;

import java.util.ArrayList;
import java.util.List;

import static com.nju.bysj.softwaremodularisation.nsga.Common.randomGenerateFileChromosome;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.historyRecord;
import static com.nju.bysj.softwaremodularisation.nsga.objective.ConcernGranularityObjective.ifChromosomeOverload;

public class FileRandomCrossover extends AbstractCrossover {

    public FileRandomCrossover(CrossoverParticipantCreator crossoverParticipantCreator, float crossoverProbability) {
        super(crossoverParticipantCreator);
        this.crossoverProbability = crossoverProbability;
    }

    @Override
    public List<Chromosome> perform(Population population) {
        int parentSize = population.getPopulace().size();
        if (parentSize < 2) {
            return new ArrayList<>();
        }

        int crossNum = (int) (this.crossoverProbability * parentSize);
        crossNum = Math.max(crossNum, 1);

        List<Chromosome> children = new ArrayList<>();
        for (int i = 0; i < crossNum; i++) {
            Chromosome randomChromosome = randomGenerateFileChromosome();
            if (!historyRecord.containsKey(randomChromosome)) {
                historyRecord.put(randomChromosome, ifChromosomeOverload(randomChromosome));
                children.add(randomChromosome);
            }
        }

        return children;
    }


}
