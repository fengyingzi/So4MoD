package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;

import java.util.*;
import java.util.stream.Collectors;

public class Z4_cohObject extends AbstractObjectiveFunction {

    @Override
    public double getValue(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> srvFilesMap = Common.splitChromosomeToServiceFileIdMap(chromosome);
        return getAllServiceCoh(srvFilesMap);
    }

    public double getAllServiceCoh(HashMap<Integer, List<Integer>> srvFilesMap) {
        double Coh = 0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceEntrys.add(PreProcessLoadData.classFileList.get(fi)));
            Coh += calculateSingleSrvCoh(serviceEntrys);
        }
        return 1 - (Coh / srvFilesMap.size());
    }

    private double calculateSingleSrvCoh(List<String> serviceFiles) {
        double relations = 0.0;
        double structureWeight = 0.5;
        double semanticWeight = 0.5;
        List<Integer> fileIndexList = serviceFiles.stream()
                .map(f -> PreProcessLoadData.classFileList.indexOf(f))
                .collect(Collectors.toList());
        if (fileIndexList.size() < 2) {
            return 1.0;
        }
        for (int i = 0; i < fileIndexList.size(); i++) {
            for (int j = i+1; j < fileIndexList.size(); j++) {
                int src = fileIndexList.get(i), dest = fileIndexList.get(j);
                relations += PreProcessLoadData.classCallSTRMatrix[src][dest] * structureWeight;
                relations += PreProcessLoadData.classSimMatrix[src][dest] * semanticWeight;
            }
        }
        return 1 - (2 * relations / serviceFiles.size() / (serviceFiles.size() - 1));
    }
}
