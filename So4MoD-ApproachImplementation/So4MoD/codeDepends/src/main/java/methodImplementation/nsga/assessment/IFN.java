package com.nju.bysj.softwaremodularisation.nsga.assessment;

import com.alibaba.fastjson.JSONArray;

import java.io.IOException;
import java.util.*;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFile;
import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFileList;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class IFN {
    public static List<String> classFileList;
    public static int[][] classCallGraph;

    IFN(){
        getCallGraph();
    }

    public static void getCallGraph() {
        try {
            classFileList = readFileList("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\files.flist");
        } catch (IOException e) {
            e.printStackTrace();
        }
        int len = classFileList.size();
        classCallGraph = new int[len][len];
        JSONArray jsonArray = JSONArray.parseArray(readFile("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\classCallGraph.json"));
        for (int i = 0; i < jsonArray.size(); i++) {
            String objStr = jsonArray.get(i).toString();
            objStr = objStr.substring(1, objStr.length() - 1);
            int[] arr = Arrays.stream(objStr.split(",")).mapToInt(Integer::parseInt).toArray();
            classCallGraph[i] = arr;
        }
    }


    double caculateAllServiceIFN(HashMap<Integer, List<Integer>> srvFilesMap) {
        int srvSize = srvFilesMap.size();
        int IFNEntry = 0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            IFNEntry += calculateSingleSrvIFN(serviceInnerEntrys);
        }
        return (double) IFNEntry / srvSize;
    }

    private int calculateSingleSrvIFN(List<String> serviceInnerEntrys) {
        int n = classCallGraph.length;
        int interfaceNum = 0;
        Set<Integer> curSrv = new HashSet<>();
        for (String i : serviceInnerEntrys) {
            curSrv.add(classFileList.indexOf(i));
        }
        for (String i : serviceInnerEntrys) {
            int curClass = classFileList.indexOf(i);
            for (int j=0;j<n;j++){
                if((!curSrv.contains(j))&&(classCallGraph[j][curClass]!=0)){
                    interfaceNum++;
                    break;
                }
            }
        }
        return interfaceNum;
    }
}
