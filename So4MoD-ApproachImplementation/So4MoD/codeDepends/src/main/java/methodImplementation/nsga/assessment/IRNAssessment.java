package com.nju.bysj.softwaremodularisation.nsga.assessment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.*;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class IRNAssessment {
    public Map<String, Map<String, Integer>> callMap;
    public Map<Integer, String> classNumberMap = new HashMap<>();
    public int allCalls;

    IRNAssessment() {
        allCalls = 0;
        getCallMap();
//        getClassNumberMap();
    }

    public String JsonToObjTest(String src) {
        String jsonStr = "";
        try {
            File jsonFile = new File(src);
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getCallMap() {
        String src = "C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\call-data.json";
        String json = JsonToObjTest(src);
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray vertexArr = (JSONArray) jsonObject.get("vertex");
        JSONArray edgeArr = (JSONArray) jsonObject.get("edge");
        callMap = new HashMap<>();
        for (Object i:vertexArr){
            JSONObject tmp = (JSONObject) i;
            String filenameRaw = (String)tmp.get("name");
            Integer fileNum = (Integer) tmp.get("index");
            classNumberMap.put(fileNum, filenameRaw);
        }
        for (Object i : edgeArr) {
            JSONObject tmp = (JSONObject) i;
            String sourceFile = (String) tmp.get("src");
            String targetFile = (String) tmp.get("dest");
            int callWeight = (int) Double.parseDouble((String) tmp.get("weight"));
            allCalls += callWeight;
            if (callMap.containsKey(sourceFile)) {
                Map<String, Integer> curCall = callMap.get(sourceFile);
                curCall.put(targetFile, callWeight);
                callMap.put(sourceFile, curCall);
            } else {
                Map<String, Integer> curCall = new HashMap<>();
                curCall.put(targetFile, callWeight);
                callMap.put(sourceFile, curCall);
            }
        }
    }

    public int caculateAllServiceIRN(HashMap<Integer, List<Integer>> srvFilesMap) {
        return calculateSrvPairIRN(srvFilesMap);
    }

    private int calculateSrvPairIRN(HashMap<Integer, List<Integer>> srvFilesMap) {
        int IRN = 0;
        List<Integer> srvList = new ArrayList<>(srvFilesMap.keySet());
        int srvNum = srvList.size();
        if (srvNum == 1) return 0;
        List<List<Integer>> srvFilesList = new ArrayList<>(srvFilesMap.values());
        for (int i = 0; i < srvFilesList.size(); i++) {
            List<Integer> files1 = srvFilesList.get(i);
            for (int j = 0; j < srvFilesList.size(); j++) {
                if (i != j) {
                    List<Integer> files2 = srvFilesList.get(j);
                    IRN += calculatePairSrvCall(files1, files2);
                }
            }
        }
        return IRN;
    }

    private int calculatePairSrvCall(List<Integer> files1, List<Integer> files2) {
        int res = 0;
        for (int i : files1) {
            String fileSrc = String.valueOf(i);
            if (callMap.containsKey(fileSrc)) {
                for (int j : files2) {
                    String fileTar = String.valueOf(j);
                    if (callMap.get(fileSrc).containsKey(fileTar)) {
                        res += callMap.get(fileSrc).get(fileTar);
                    }
                }
            }
        }
        return res;
    }
}
