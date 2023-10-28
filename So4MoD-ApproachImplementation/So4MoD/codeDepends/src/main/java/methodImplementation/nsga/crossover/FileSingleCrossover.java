package com.nju.bysj.softwaremodularisation.nsga.crossover;

import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Population;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.historyRecord;

public class FileSingleCrossover extends AbstractCrossover {

    public FileSingleCrossover(CrossoverParticipantCreator crossoverParticipantCreator, float crossoverProbability) {
        super(crossoverParticipantCreator);
        this.crossoverProbability = crossoverProbability;
    }

    private void listCopy(Chromosome src, Chromosome dest, int ss, int ds, int len) {
        for (int i = 0; i < len; i++) {
            dest.getGeneticCode().set(ds + i,
                    src.getGeneticCode().get(ss + i));
        }
    }

    private void singlePointCrossover(List<Chromosome> children, Chromosome p1, Chromosome p2, int pointIndex) {
        int maxLen = p1.getLength();
        Chromosome c1 = new Chromosome(p2);
        Chromosome c2 = new Chromosome(p1);

        listCopy(p2, c1, maxLen - 1 - pointIndex, 0, pointIndex + 1);
        listCopy(p1, c2, 0, maxLen - 1 - pointIndex, pointIndex + 1);

        if (!historyRecord.containsKey(c1)) {
            historyRecord.put(c1, true);
//                System.out.println("add child " + historyRecord.size());
            children.add(c1);
        }
        if (!historyRecord.containsKey(c2)) {
            historyRecord.put(c2, true);
//                System.out.println("add child " + historyRecord.size());
            children.add(c2);
        }
    }


    @Override
    public List<Chromosome> perform(Population population) {
        int parentSize = population.getPopulace().size();
        if (parentSize < 2) {
            return new ArrayList<>();
        }

        int crossNum = (int) (this.crossoverProbability * parentSize);
        crossNum = Math.max(crossNum, 1);

        int[] parentArr1 = new int[crossNum];
        int[] parentArr2 = new int[crossNum];
        int[] crossPointArr = new int[crossNum];

        for (int i = 0; i < crossNum; i++) {
            parentArr1[i] = ThreadLocalRandom.current().nextInt(0, parentSize);
            do {
                parentArr2[i] = ThreadLocalRandom.current().nextInt(0, parentSize);
            } while (parentArr1[i] == parentArr2[i]);
            crossPointArr[i] = ThreadLocalRandom.current().nextInt(0, classFileList.size() - 1);
        }

        List<Chromosome> children = new ArrayList<>();
        for (int i = 0; i < crossNum; i++) {
            singlePointCrossover(children,
                    population.getPopulace().get(parentArr1[i]),
                    population.getPopulace().get(parentArr2[i]),
                    crossPointArr[i]);
        }

        return children;
    }


}
