package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;

import java.io.*;
import java.util.*;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.*;

public class Z3_SecurityObjective extends AbstractObjectiveFunction {

    public Z3_SecurityObjective() {
        this.objectiveFunctionTitle = "Security Index";
    }


    @Override
    public double getValue(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> srvFilesMap = Common.splitChromosomeToServiceFileIdMap(chromosome);
        double CCP = getAllServiceCCP(srvFilesMap);
        double CCE = getAllServiceCCE(srvFilesMap);
        double CSP = getAllServiceCSP(srvFilesMap);
        double CSI = getAllServiceCSI(srvFilesMap);
        return CCP + CCE + CSP + CSI;
    }

    private double getAllServiceCCP(HashMap<Integer, List<Integer>> srvFilesMap) {
        double CCP = 0.0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            double ccp = calculateSingleSrvCCP(serviceInnerEntrys);
            CCP += ccp;
        }
        return CCP / srvFilesMap.size();
    }

    private double calculateSingleSrvCCP(List<String> serviceInnerEntrys) {
        int criticalNum = 0;
        for (String i : serviceInnerEntrys) {
            if (criticalClass.contains(i)) {
                criticalNum++;
            }
        }
        return 1 - (double) criticalNum / serviceInnerEntrys.size();
    }

    private double getAllServiceCCE(HashMap<Integer, List<Integer>> srvFilesMap) {
        double cces = 0.0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            double cce = calculateSingleSrvCCE(serviceInnerEntrys);
            cces += cce;
        }
        return cces / srvFilesMap.size();
    }

    private double calculateSingleSrvCCE(List<String> serviceInnerEntrys) {
        int finalNum = 0;
        int criticalClassNum = 0;
        for (String i : serviceInnerEntrys) {
            for (String j : finalClass) {
                if (i.equals(j)) {
                    finalNum++;
                }
            }
            for (String j : criticalClass) {
                if (i.equals(j)) {
                    criticalClassNum++;
                }
            }
        }
        if (criticalClassNum == 0) {
            return 0;
        }
        return (double) finalNum / criticalClassNum;
    }

    private double getAllServiceCSP(HashMap<Integer, List<Integer>> srvFilesMap) {
        double csps = 0.0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            double csp = calculateSingleSrvCSP(serviceInnerEntrys);
            csps += csp;
        }
        return csps / srvFilesMap.size();
    }

    private double calculateSingleSrvCSP(List<String> serviceInnerEntrys) {
        int fatherCriticalNum = 0;
        int criticalClassNum = 0;
        for (String i : serviceInnerEntrys) {
            if (criticalClass.contains(i)) {
                criticalClassNum++;
                if (fatherCriticalClass.contains(i)) {
                    fatherCriticalNum++;
                }
            }
        }
        if (criticalClassNum == 0) {
            return 1;
        }
        return 1 - (double) fatherCriticalNum / criticalClassNum;
    }

    private double getAllServiceCSI(HashMap<Integer, List<Integer>> srvFilesMap) {
        List<List<String>> services = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            services.add(serviceInnerEntrys);
        }
        double csis = calculateSrvsCSI(services);
        return csis / srvFilesMap.size();
    }

    private double calculateSrvsCSI(List<List<String>> services) {
        double csis = 0.0;
        int allClassNum = 0;
        for (List<String> service : services) {
            allClassNum += service.size();
        }
        for (int i = 0; i < services.size(); i++) {
            List<String> curSrv = services.get(i);
            double curSrvCSI = 0.0;
            //内部计算
            int criticalClassNum = 0;
            for (String className : curSrv) {
                if (criticalClass.contains(className)) {
                    criticalClassNum++;
                }
            }
            if (criticalClassNum == 0) {
                csis += 1 - curSrvCSI;
                continue;
            }
            int curInnercsi = 0;
            for (String className : curSrv) {
                if (fatherCriticalClass.contains(className)) {
                    List<String> curClassChildren = fatherCriticalClassExtend.get(className);
                    for (String candidate : curSrv) {
                        if ((curClassChildren != null) && (!candidate.equals(className)) && (curClassChildren.contains(candidate))) {
                            curInnercsi++;
                        }
                    }
                }
            }
            if (curSrv.size() > 1) {
                curSrvCSI += (double) curInnercsi / (curSrv.size() - 1) / criticalClassNum;
            }
            for (String className : curSrv) {
                if (fatherCriticalClass.contains(className)) {
                    List<String> curClassChildren = fatherCriticalClassExtend.get(className);
                    for (int k = 0; k < services.size(); k++) {
                        if (k != i) {
                            List<String> candidateSrv = services.get(k);
                            int curOuttercsi = 0;
                            for (String candidate : candidateSrv) {
                                if ((curClassChildren != null) && (curClassChildren.contains(candidate))) {
                                    curOuttercsi++;
                                }
                            }
                            curSrvCSI += (double) curOuttercsi / (allClassNum - curSrv.size()) / criticalClassNum;
                        }
                    }
                }
            }
            csis += (1 - curSrvCSI);
        }
        return csis;
    }
}
