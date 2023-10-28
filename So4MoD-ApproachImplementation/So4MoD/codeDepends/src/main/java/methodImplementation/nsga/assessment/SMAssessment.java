package com.nju.bysj.softwaremodularisation.nsga.assessment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFile;
import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFileList;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class SMAssessment {

    public static List<String> classFileList;
    public static int[][] classCallGraph;

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

    public static double caculateAllServiceSM(HashMap<Integer, List<Integer>> srvFilesMap) {
        double SM = 0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceEntrys.add(classFileList.get(fi)));
            SM += calculateSingleSrvSM(serviceEntrys);
        }
        double innerPart = SM / srvFilesMap.size();
        double outterPart = cacutaleAllOuter(srvFilesMap);
        return innerPart - outterPart;
    }

    private static double calculateSingleSrvSM(List<String> serviceFiles) {
        double relations = 0.0;
        List<Integer> fileIndexList = serviceFiles.stream()
                .map(f -> classFileList.indexOf(f))
                .collect(Collectors.toList());
        for (int i = 0; i < fileIndexList.size(); i++) {
            for (int j = 0; j < fileIndexList.size(); j++) {
                if (j != i) {
                    int src = fileIndexList.get(i), dest = fileIndexList.get(j);
                    relations += classCallGraph[src][dest];
                }
            }
        }
        return relations / serviceFiles.size() / serviceFiles.size();
    }

    private static double cacutaleAllOuter(HashMap<Integer, List<Integer>> srvFilesMap) {
        double allOuter = 0;
        List<Integer> srvList = new ArrayList<>(srvFilesMap.keySet());
        int srvNum = srvList.size();
        if (srvNum == 1) {
            return 0;
        }
        List<List<Integer>> srvFilesList = new ArrayList<>(srvFilesMap.values());
        for (int i = 0; i < srvFilesList.size(); i++) {
            List<Integer> files1 = srvFilesList.get(i);
            for (int j = i + 1; j < srvFilesList.size(); j++) {
                List<Integer> files2 = srvFilesList.get(j);
                allOuter += calculatePairSrvStructure(files1, files2);
            }
        }
        return allOuter * 2 / (srvNum - 1) / srvNum;
    }

    private static double calculatePairSrvStructure(List<Integer> files1, List<Integer> files2) {
        double relations = 0.0;
        for (Integer value : files1) {
            for (Integer integer : files2) {
                int src = value, dest = integer;
                relations += classCallGraph[src][dest];
                relations += classCallGraph[dest][src];
            }
        }
        return relations / files1.size() / files2.size() / 2;
    }
}
