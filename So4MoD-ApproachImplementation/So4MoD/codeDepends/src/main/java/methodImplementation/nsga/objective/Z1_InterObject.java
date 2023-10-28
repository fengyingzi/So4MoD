package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;

import java.util.*;
import java.util.stream.Collectors;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classSimMatrix;

public class Z1_InterObject extends AbstractObjectiveFunction {
    @Override
    public double getValue(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> srvFilesMap = Common.splitChromosomeToServiceFileIdMap(chromosome);
        return getAllServiceInterConnectivity(srvFilesMap);
    }

    private double getAllServiceInterConnectivity(HashMap<Integer, List<Integer>> srvFilesMap) {
        double totalInterConnectivity = 0;
        int serviceNum = srvFilesMap.size();
        List<Double> structures = new ArrayList<>();
        List<Double> semantics = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : srvFilesMap.entrySet()) {
            List<String> serviceFiles = new ArrayList<>();
            entry.getValue().forEach(fi -> serviceFiles.add(classFileList.get(fi)));
            double tmpStructure = getSingleServiceStructures(serviceFiles);
            structures.add(tmpStructure);
            double tmpSemantic = getSingleServiceSemantics(entry.getValue());
            semantics.add(tmpSemantic);
        }
        for (int i = 0; i < structures.size(); i++) {
            totalInterConnectivity += structures.get(i);
            totalInterConnectivity += semantics.get(i);
        }
        return totalInterConnectivity / serviceNum;
    }


    public double MaxMinNormalization(double x, double Maxx, double Minx) {
        if (Maxx == Minx) {
            return 0.0;
        }
        double molecule = x - Minx;
        double denominator = Maxx - Minx;
        return molecule / denominator;
    }

    private double getSingleServiceStructures(List<String> serviceFiles) {
        double relations = 0.0;
        List<Integer> fileIndexList = serviceFiles.stream()
                .map(f -> PreProcessLoadData.classFileList.indexOf(f))
                .collect(Collectors.toList());
        for (int i = 0; i < fileIndexList.size(); i++) {
            for (int j = 0; j < fileIndexList.size(); j++) {
                if (j != i) {
                    int src = fileIndexList.get(i), dest = fileIndexList.get(j);
                    relations += PreProcessLoadData.classCallGraph[src][dest];
                }
            }
        }
        double patterns = 0.0;
        for (int i = 0; i < fileIndexList.size(); i++) {
            for (int j = i + 1; j < fileIndexList.size(); j++) {
                int src1 = fileIndexList.get(i);
                int src2 = fileIndexList.get(j);
                patterns += PreProcessLoadData.classRelationGraph[src1][src2];
            }
        }
        return (relations + patterns) / 2 / serviceFiles.size() / serviceFiles.size();
    }

    private double getSingleServiceSemantics(List<Integer> serviceFiles) {
        int fileNum = serviceFiles.size();
        if (fileNum == 1) {
            return 0;
        }
        double totalCosineSim = 0;
        for (int i = 0; i < fileNum; i++) {
            for (int j = i + 1; j < fileNum; j++) {
                Integer f1 = serviceFiles.get(i);
                Integer f2 = serviceFiles.get(j);
                totalCosineSim += classSimMatrix[f1][f2];
            }
        }
        return totalCosineSim / fileNum / fileNum;
    }
}
