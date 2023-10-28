package com.nju.bysj.softwaremodularisation.nsga.crossover;

import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.FunctionalAtom;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.GroupItemAllele;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Population;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SingleCrossover extends AbstractCrossover {

    private double moveCrossover = 0.6;

    private final HashSet<Chromosome> historyRecord;

    public SingleCrossover(CrossoverParticipantCreator crossoverParticipantCreator, float crossoverProbability) {
        super(crossoverParticipantCreator);
        this.crossoverProbability = crossoverProbability;
        historyRecord = new HashSet<>();
    }

    public SingleCrossover(CrossoverParticipantCreator crossoverParticipantCreator, float crossoverProbability,
                           double moveCrossover) {
        super(crossoverParticipantCreator);
        this.crossoverProbability = crossoverProbability;
        this.moveCrossover = moveCrossover;
        historyRecord = new HashSet<>();
    }

    public HashSet<Chromosome> getHistoryRecord() {
        return historyRecord;
    }

    private List<Integer> selectCrossoverIndividuals(int num, Population population) {
        Set<Integer> selected = new HashSet<>(num);
        int sc = 0;
        while (sc < num) {
            int id = ThreadLocalRandom.current().nextInt(0, population.getPopulace().size());
            if (!selected.contains(id)) {
                selected.add(id);
                sc ++;
            }
        }
        return new ArrayList<>(selected);
    }

    private Chromosome singleParentCrossover(Chromosome parent) {
        if (parent.getGeneticCode().size() == 1) {
            return pullUpCrossover(parent);
        }
        return ThreadLocalRandom.current().nextDouble(0, 1.0) <= moveCrossover ?
                moveCrossover(parent) : pullUpCrossover(parent);
    }

    private Chromosome pullUpCrossover(Chromosome parent) {
        int selectGroupItemId = ThreadLocalRandom.current().nextInt(0, parent.getGeneticCode().size());
        GroupItemAllele selectGroupItem = (GroupItemAllele) parent.getGeneticCode().get(selectGroupItemId);
        int selectFAId = ThreadLocalRandom.current().nextInt(0, selectGroupItem.getGene().size());

        Chromosome child = new Chromosome(parent);
        GroupItemAllele targetGi = (GroupItemAllele) (child.getGeneticCode().get(selectGroupItemId));
        FunctionalAtom childFA = targetGi.getGene().get(selectFAId);
        targetGi.getGene().remove(childFA);
        if (targetGi.getGene().size() == 0) {
            child.getGeneticCode().remove(selectGroupItemId);
        }
        GroupItemAllele newGroupItem = new GroupItemAllele(new ArrayList<FunctionalAtom>() {{ add(childFA); }});
        child.getGeneticCode().add(newGroupItem);
        return child;
    }

    private Chromosome moveCrossover(Chromosome parent) {
        int srcGroupItemId = ThreadLocalRandom.current().nextInt(0, parent.getGeneticCode().size());
        GroupItemAllele srcGroupItem = (GroupItemAllele) parent.getGeneticCode().get(srcGroupItemId);
        int selectFAId = ThreadLocalRandom.current().nextInt(0, srcGroupItem.getGene().size());

        int destGroupItemId;
        do {
            destGroupItemId = ThreadLocalRandom.current().nextInt(0, parent.getGeneticCode().size());
        } while (destGroupItemId == srcGroupItemId);

        Chromosome child = new Chromosome(parent);
        GroupItemAllele sourceGi = (GroupItemAllele) (child.getGeneticCode().get(srcGroupItemId));
        GroupItemAllele destGi = (GroupItemAllele) (child.getGeneticCode().get(destGroupItemId));
        FunctionalAtom childFA = sourceGi.getGene().get(selectFAId);
        sourceGi.getGene().remove(selectFAId);
        destGi.getGene().add(childFA);
        if (sourceGi.getGene().size() == 0) {
            child.getGeneticCode().remove(srcGroupItemId);
        }
        return child;
    }

    @Override
    public List<Chromosome> perform(Population population) {
        int crossoverNum = population.getPopulace().size() * crossoverProbability > 1 ?
                (int)(population.getPopulace().size() * crossoverProbability) : 1;
        List<Integer> crossoverIndividuals = selectCrossoverIndividuals(crossoverNum, population);

        List<Chromosome> children = new ArrayList<>(crossoverNum);
        for (int id : crossoverIndividuals) {
            Chromosome curIndividual = population.getPopulace().get(id);
            Chromosome child = singleParentCrossover(curIndividual);
            if (!historyRecord.contains(child)) {
                historyRecord.add(child);
//                System.out.println("add child " + historyRecord.size());
                children.add(child);
            }
        }
        return children;
    }
}
