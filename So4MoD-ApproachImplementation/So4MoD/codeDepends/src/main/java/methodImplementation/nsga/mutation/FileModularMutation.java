package com.nju.bysj.softwaremodularisation.nsga.mutation;

import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.IntegerAllele;

import java.util.concurrent.ThreadLocalRandom;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class FileModularMutation extends AbstractMutation {

    private final float breakProbability;

    public FileModularMutation(float mutationProbability, float breakProbability) {
        super(mutationProbability);
        this.breakProbability = breakProbability;
    }

    @Override
    public Chromosome perform(Chromosome chromosome) {
        Chromosome child = new Chromosome(chromosome);

        int geneIndex = ThreadLocalRandom.current().nextInt(0, classFileList.size());

        int maxModuleId = chromosome.getGeneticCode().stream()
                .mapToInt(gc -> ((IntegerAllele) (gc)).getGene())
                .max().getAsInt();
        if (ThreadLocalRandom.current().nextDouble(0, 1) <= breakProbability) {
            maxModuleId = Math.min(maxModuleId + 1, classFileList.size() - 1);
            child.getGeneticCode().set(geneIndex, new IntegerAllele(maxModuleId));
        } else {
            int nextIndex;
            do {
                nextIndex = ThreadLocalRandom.current().nextInt(0, classFileList.size());
            } while (nextIndex == geneIndex);
            child.getGeneticCode().set(geneIndex,
                    child.getGeneticCode().get(nextIndex));
        }

        return child;
    }
}
