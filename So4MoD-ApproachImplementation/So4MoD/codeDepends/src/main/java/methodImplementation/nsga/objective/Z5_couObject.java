package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Z5_couObject extends AbstractObjectiveFunction {
    @Override
    public double getValue(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> srvFilesMap = Common.splitChromosomeToServiceFileIdMap(chromosome);
        return 1 - getAllServiceCou(srvFilesMap);
    }

    public double getAllServiceCou(HashMap<Integer, List<Integer>> srvFilesMap) {
        double Cou = 0;
        if (srvFilesMap.size() < 2) {
            return Cou;
        }
        List<List<Integer>> srvFilesList = new ArrayList<>(srvFilesMap.values());
        int sumClass = 0;
        for(int i=0;i<srvFilesList.size();i++){
            sumClass += srvFilesList.get(i).size();
            Collections.sort(srvFilesList.get(i));
        }
        for (int i = 0; i < srvFilesList.size(); i++) {
            List<Integer> files1 = srvFilesList.get(i);
            for (int j = i + 1; j < srvFilesList.size(); j++) {
                List<Integer> files2 = srvFilesList.get(j);
                Cou += calculatePairSrvCou(files1, files2, sumClass);
            }
        }
        return 2 * Cou / srvFilesMap.size() / (srvFilesMap.size() - 1);
    }

    private double calculatePairSrvCou(List<Integer> files1, List<Integer> files2, int sumClass) {
        double res = 0;
        double structureWeight = 0.5;
        double semanticWeight = 0.5;
        int interfaceClassNum1 = 0;
        int interfaceClassNum2 = 0;
        for (int i : files1) {
            for (int j : files2) {
                res += PreProcessLoadData.classCallSTRMatrix[i][j] * structureWeight;
                res += PreProcessLoadData.classSimMatrix[i][j] * semanticWeight;
            }
        }
        if(PreProcessLoadData.cacheOfInterface.containsKey(files1)){
            interfaceClassNum1 = PreProcessLoadData.cacheOfInterface.get(files1);
        }
        else{
            for (int i : files1) {
                for (int j=0;j<sumClass;j++){
                    if((!files1.contains(j))&&(PreProcessLoadData.classCallGraph[j][i]>0)){
                        interfaceClassNum1++;
                        break;
                    }
                }
            }
            PreProcessLoadData.cacheOfInterface.put(files1,interfaceClassNum1);
        }
        if(PreProcessLoadData.cacheOfInterface.containsKey(files2)){
            interfaceClassNum2 = PreProcessLoadData.cacheOfInterface.get(files2);
        }
        else{
            for(int i:files2){
                for(int j=0;j<sumClass;j++){
                    if((!files2.contains(j))&&(PreProcessLoadData.classCallGraph[j][i]>0)){
                        interfaceClassNum2++;
                        break;
                    }
                }
            }
            PreProcessLoadData.cacheOfInterface.put(files2,interfaceClassNum2);
        }
        return res / interfaceClassNum1 / interfaceClassNum2;
    }

}
