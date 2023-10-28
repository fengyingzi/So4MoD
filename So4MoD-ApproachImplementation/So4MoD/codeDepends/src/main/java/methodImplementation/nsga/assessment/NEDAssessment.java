package com.nju.bysj.softwaremodularisation.nsga.assessment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class NEDAssessment {
    public static double caculateAllServiceNED(HashMap<Integer, List<Integer>> srvFilesMap) {
        int allEntry = 0;
        int NEDEntry = 0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            if (serviceInnerEntrys.size() >= 5 && serviceInnerEntrys.size() <= 20) {
                NEDEntry += serviceInnerEntrys.size();
            }
            allEntry += serviceInnerEntrys.size();
        }
        return 1 - (double) NEDEntry / allEntry;
    }
}
