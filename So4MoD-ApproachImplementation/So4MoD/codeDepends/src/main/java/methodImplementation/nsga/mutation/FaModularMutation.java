package com.nju.bysj.softwaremodularisation.nsga.mutation;

import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.IntegerAllele;

import java.util.concurrent.ThreadLocalRandom;

public class FaModularMutation extends AbstractMutation {

    private final float breakProbability;

    public FaModularMutation(float mutationProbability, float breakProbability) {
        super(mutationProbability);
        this.breakProbability = breakProbability;
    }

    @Override
    public Chromosome perform(Chromosome chromosome) {
        Chromosome child = new Chromosome(chromosome);

        int geneIndex = ThreadLocalRandom.current().nextInt(0, PreProcessLoadData.faNums);

        int maxModuleId = chromosome.getGeneticCode().stream()
                .mapToInt(gc -> ((IntegerAllele) (gc)).getGene())
                .max().getAsInt();
        if (ThreadLocalRandom.current().nextDouble(0, 1) <= breakProbability) {
            maxModuleId = Math.min(maxModuleId + 1, PreProcessLoadData.faNums - 1);
            child.getGeneticCode().set(geneIndex, new IntegerAllele(maxModuleId));
        } else {
            int nextIndex;
            do {
                nextIndex = ThreadLocalRandom.current().nextInt(0, PreProcessLoadData.faNums);
            } while (nextIndex == geneIndex);
            child.getGeneticCode().set(geneIndex,
                    child.getGeneticCode().get(nextIndex));
        }

        return child;
    }
}
