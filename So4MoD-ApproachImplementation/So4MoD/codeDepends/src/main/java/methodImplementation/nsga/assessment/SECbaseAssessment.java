package com.nju.bysj.softwaremodularisation.nsga.assessment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.*;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class SECbaseAssessment {
    public List<String> criticalClass = new ArrayList<>();
    public List<String> finalClass = new ArrayList<>();
    public List<String> fatherCriticalClass = new ArrayList<>();
    public Map<Integer, String> extendClassNumberMap = new HashMap<>();
    public Map<String, List<String>> fatherCriticalClassExtend = new HashMap<>();

    public SECbaseAssessment() {
        getClassNumberMap();
        getCriticalClass();
        getFinalCriticalClass();
        getFatherCriticalClass();
        getFatherCriticalClassExtend();
    }

    public double getAllServiceCCP(HashMap<Integer, List<Integer>> srvFilesMap) {
        double totalCCP = 0;
        List<Double> ccps = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            double ccp = calculateSingleSrvCCP(serviceInnerEntrys);
            ccps.add(ccp);
        }
        for (Double ccp : ccps) {
            totalCCP += ccp;
        }
        return totalCCP / srvFilesMap.size();
    }

    private double calculateSingleSrvCCP(List<String> serviceInnerEntrys) {
        int criticalNum = 0;
        for (String i : serviceInnerEntrys) {
            for (String j : this.criticalClass) {
                if (i.equals(j)) {
                    criticalNum++;
                }
            }
        }
        return 1 - (double) criticalNum / serviceInnerEntrys.size();
    }

    public double getAllServiceCCE(HashMap<Integer, List<Integer>> srvFilesMap) {
        double totalCCE = 0.0;
        List<Double> cces = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            double cce = calculateSingleSrvCCE(serviceInnerEntrys);
            cces.add(cce);
        }
        for (Double cce : cces) {
            totalCCE += cce;
        }
        return totalCCE / srvFilesMap.size();
    }

    private double calculateSingleSrvCCE(List<String> serviceInnerEntrys) {
        int finalNum = 0;
        int criticalClassNum = 0;
        for (String i : serviceInnerEntrys) {
            for (String j : this.finalClass) {
                if (i.equals(j)) {
                    finalNum++;
                }
            }
            for (String j : this.criticalClass) {
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

    public double getAllServiceCSP(HashMap<Integer, List<Integer>> srvFilesMap) {
        double totalCSP = 0.0;
        List<Double> csps = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            double csp = calculateSingleSrvCSP(serviceInnerEntrys);
            csps.add(csp);
        }
        for (Double csp : csps) {
            totalCSP += csp;
        }
        return totalCSP / srvFilesMap.size();
    }

    private double calculateSingleSrvCSP(List<String> serviceInnerEntrys) {
        int fatherCriticalNum = 0;
        int criticalClassNum = 0;
        for (String i : serviceInnerEntrys) {
            for (String j : this.fatherCriticalClass) {
                if (i.equals(j)) {
                    fatherCriticalNum++;
                }
            }
            for (String j : this.criticalClass) {
                if (i.equals(j)) {
                    criticalClassNum++;
                }
            }
        }
        if (criticalClassNum == 0) {
            return 1;
        }
        return 1 - (double) fatherCriticalNum / criticalClassNum;
    }

    public double getAllServiceCSI(HashMap<Integer, List<Integer>> srvFilesMap) {
        double totalCSI = 0.0;
        List<List<String>> services = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            services.add(serviceInnerEntrys);
        }
        List<Double> csis = calculateSrvsCSI(services);
        for (double csi : csis) {
            totalCSI += csi;
        }
        return totalCSI / srvFilesMap.size();
    }

    private List<Double> calculateSrvsCSI(List<List<String>> services) {
        List<Double> csis = new ArrayList<>();
        int allClassNum = 0;
        for (int i = 0; i < services.size(); i++) {
            allClassNum += services.get(i).size();
        }
        for (int i = 0; i < services.size(); i++) {
            List<String> curSrv = services.get(i);
            double curSrvCSI = 0.0;
            int criticalClassNum = 0;
            for (String className : curSrv) {
                if (this.criticalClass.contains(className)) {
                    criticalClassNum++;
                }
            }
            if (criticalClassNum == 0) {
                csis.add(1 - curSrvCSI);
                continue;
            }
            int curInnercsi = 0;
            for (String className : curSrv) {
                if (this.fatherCriticalClass.contains(className)) {
                    List<String> curClassChildren = this.fatherCriticalClassExtend.get(className);
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
                if (this.fatherCriticalClass.contains(className)) {
                    List<String> curClassChildren = this.fatherCriticalClassExtend.get(className);
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
            csis.add(1 - curSrvCSI);
        }
        return csis;
    }

    public double MaxMinNormalization(double x, double Maxx, double Minx) {
        if (Maxx == Minx) {
            return 0.0;
        }
        double molecule = x - Minx;
        double denominator = Maxx - Minx;
        return molecule / denominator;
    }

    public static String JsonToObjTest(String src) {
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

    public boolean judgeCriticalClass(String name) {
        for (String i : this.criticalClass) {
            if (i.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void getClassNumberMap() {
        String src = "C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\extend-data.json";
        String json = JsonToObjTest(src);
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray vertexArr = (JSONArray) jsonObject.get("vertex");
        for (Object i:vertexArr){
            JSONObject tmp = (JSONObject) i;
            String filenameRaw = (String)tmp.get("name");
            Integer fileNum = (Integer) tmp.get("index");
            extendClassNumberMap.put(fileNum, filenameRaw);
        }
    }

    public void getCriticalClass() {
        StringBuilder sb = new StringBuilder();
        String text = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\criticalClass.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ((text = bufferedReader.readLine()) != null) {
                sb.append(text);
            }
            String[] criticalString = sb.toString().split(",");
            Collections.addAll(this.criticalClass, criticalString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getFinalCriticalClass() {
        StringBuilder sb = new StringBuilder();
        String text = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\finalCriticalClass.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ((text = bufferedReader.readLine()) != null) {
                sb.append(text);
            }
            String[] finalString = sb.toString().split(",");
            Collections.addAll(this.finalClass, finalString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFatherCriticalClass() {
        StringBuilder sb = new StringBuilder();
        String text = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\fatherCriticalClass.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ((text = bufferedReader.readLine()) != null) {
                sb.append(text);
            }
            String[] fatherCriticalString = sb.toString().split(",");
            for (String fatherClassName : fatherCriticalString) {
                if (fatherClassName.length() == 0) {
                    continue;
                }
                if(criticalClass.contains(fatherClassName)){
                    fatherCriticalClass.add(fatherClassName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFatherCriticalClassExtend() {
        String src = "C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\extend-data.json";
        String json = JsonToObjTest(src);
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray vertexArr = (JSONArray) jsonObject.get("vertex");
        JSONArray edgeArr = (JSONArray) jsonObject.get("edge");
        System.out.println(vertexArr);
        System.out.println(edgeArr);
        for (Object i : vertexArr) {
            JSONObject tmp = (JSONObject) i;
            String filename = (String) tmp.get("name");
            if (judgeCriticalClass(filename)) {
                this.fatherCriticalClassExtend.put(filename, new ArrayList<>());
            }
        }
        for (Object i : edgeArr) {
            JSONObject tmp = (JSONObject) i;
            String fileSon = this.extendClassNumberMap.get(Integer.valueOf((String) tmp.get("src")));
            String fileFather = this.extendClassNumberMap.get(Integer.valueOf((String) tmp.get("dest")));
            if (judgeCriticalClass(fileFather)) {
                List<String> fatherExtendMap = this.fatherCriticalClassExtend.get(fileFather);
                fatherExtendMap.add(fileSon);
                this.fatherCriticalClassExtend.put(fileFather, fatherExtendMap);
            }
        }
        for (String i : this.fatherCriticalClassExtend.keySet()) {
            List<String> curSon = this.fatherCriticalClassExtend.get(i);
            Set<String> newCurSon = new HashSet<>(curSon);
            for (String j : curSon) {
                if (this.fatherCriticalClassExtend.containsKey(j)) {
                    newCurSon.addAll(this.fatherCriticalClassExtend.get(j));
                }
            }
            List<String> tmp = new ArrayList<>(newCurSon);
            this.fatherCriticalClassExtend.put(i, tmp);
        }
    }

}
