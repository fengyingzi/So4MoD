package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;

import java.util.*;
import java.util.stream.Collectors;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classSimMatrix;

public class Z2_ExterObject extends AbstractObjectiveFunction {
    @Override
    public double getValue(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> srvFilesMap = Common.splitChromosomeToServiceFileIdMap(chromosome);
        return 1 + getAllServiceExterConnectivity(srvFilesMap);
    }

    private double getAllServiceExterConnectivity(HashMap<Integer, List<Integer>> srvFilesMap) {
        double totalExterConnectivity = 0;
        List<Double> structures = new ArrayList<>();
        List<Double> semantics = new ArrayList<>();
        List<Integer> srvList = new ArrayList<>(srvFilesMap.keySet());
        int srvNum = srvList.size();
        if (srvNum == 1) {
            return 0;
        }
        List<List<Integer>> srvFilesList = new ArrayList<>(srvFilesMap.values());
        for (int i = 0; i < srvFilesList.size(); i++) {
            List<Integer> files1 = srvFilesList.get(i);
            for (int j = 0; j < srvFilesList.size(); j++) {
                if(i!=j){
                    List<Integer> files2 = srvFilesList.get(j);
                    double tmpStructure = calculatePairSrvStructure(files1, files2);
                    structures.add(tmpStructure);
                    double tmpSemantic = calculatePairSrvSemantics(files1, files2);
                    semantics.add(tmpSemantic);
                }
            }
        }
        for (int i = 0; i < structures.size(); i++) {
            totalExterConnectivity += structures.get(i);
            totalExterConnectivity += semantics.get(i);
        }
        return -totalExterConnectivity * 2 / (srvNum - 1) / srvNum;
    }


    private double calculatePairSrvStructure(List<Integer> files1, List<Integer> files2) {
        double relations = 0.0;
        for (Integer value : files1) {
            for (Integer integer : files2) {
                int src = value, dest = integer;
                relations += PreProcessLoadData.classCallGraph[src][dest];
                relations += PreProcessLoadData.classCallGraph[dest][src];
            }
        }
        double patterns = 0.0;
        for (int i = 0; i < files1.size(); i++) {
            for (int j = 0; j < files2.size(); j++) {
                int src1 = files1.get(i);
                int src2 = files2.get(j);
                patterns += PreProcessLoadData.classRelationGraph[src1][src2];
            }
        }
        return (relations + patterns) / 2 / files1.size() / files2.size() / 2;
    }

    private double calculatePairSrvSemantics(List<Integer> files1, List<Integer> files2) {
        if (files1.size() <= 1 && files2.size() <= 1) {
            return 0;
        }
        double totalSim = 0;
        for (int i : files1) {
            for (int j : files2) {
                totalSim += classSimMatrix[i][j];
            }
        }
        return totalSim / files1.size() / files2.size() / 2;
    }
}
