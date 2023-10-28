package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;

import java.util.HashMap;
import java.util.List;

public class Z6_GranularityObject extends AbstractObjectiveFunction {
    @Override
    public double getValue(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> srvFilesMap = Common.splitChromosomeToServiceFileIdMap(chromosome);
        return getAllServiceGra(srvFilesMap);
    }

    private double getAllServiceGra(HashMap<Integer, List<Integer>> srvFilesMap) {
        return 1 - (double) PreProcessLoadData.classFileList.size() / srvFilesMap.size();
    }
}
