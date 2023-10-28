package com.nju.bysj.softwaremodularisation.nsga;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.FunctionalAtom;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.RelationFile;
import com.nju.bysj.softwaremodularisation.semantic.lda.Model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.nju.bysj.softwaremodularisation.common.FileDirManage.*;
import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFile;
import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFileList;
import static com.nju.bysj.softwaremodularisation.nsga.Common.readFileAsString;
import static com.nju.bysj.softwaremodularisation.nsga.PostProcessShowData.*;

public class PreProcessLoadData {

    public static Model model;
    public static Map<Integer, Double> concernTCMap;
    public static Set<Integer> concerns;
    public static List<String> allServiceFileList;
    public static List<HashMap<String, Integer>> allServiceFileWords;

    public static List<String> classFileList;
    public static int[][] classCallGraph;
    public static int[][] classRelationGraph;
    public static double[][] classTFIDFList;
    public static double[][] classSimMatrix;
    public static double[][] classEvolutionMatrix;
    public static int[] callInArray;
    public static int[][] classCallsMatrix;
    public static double[][] classCallSTRMatrix;
    public static int maxFileCallNums = 0;
    public static List<String> criticalClass = new ArrayList<>();
    public static List<String> finalClass = new ArrayList<>();
    public static List<String> fatherCriticalClass = new ArrayList<>();
    public static Map<String, List<String>> fatherCriticalClassExtend = new HashMap<>();
    public static Map<Integer, String> extendClassNumberMap = new HashMap<>();

    public static Map<List<Integer>, Integer> cacheOfInterface = new HashMap<>();

    public static int[][] relationGraph;

    public static List<FunctionalAtom> clusters;
    public static int faNums;
    public static HashMap<Chromosome, Boolean> historyRecord;

    static {
        try {
            readGlobalSrvFilesAndWords();
            readOverloadSrvFilesAndCallMatrixAndTFIDFMatrix();
            loadRelationGraph();
            getClassNumberMap();
            getCriticalClass();
            getFinalCriticalClass();
            getFatherCriticalClass();
            getFatherCriticalClassExtend();
            getCallArrays();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, Double> readConcernTCMap() throws IOException {
        Map<Integer, Double> concernTCMap = new HashMap<>();

        FileInputStream is = new FileInputStream(concernDir);
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            String[] datas = line.split(" ");
            concernTCMap.put(Integer.parseInt(datas[0]), Double.parseDouble(datas[1]));
        }
        is.close();
        isr.close();
        br.close();
        System.out.println("concern - tc success, the focus：" + concernTCMap.size());
        return concernTCMap;
    }

    public static void readGlobalSrvFilesAndWords() throws IOException {
        allServiceFileList = readFileList(filenameDir);
        System.out.println("refactoring all service file list success" + allServiceFileList.size());
        List<String> wordsList = readFileList(wordsDir);
        System.out.println("wordList size:" + wordsList.size());
        int allFileIndex = 0;
        allServiceFileWords = new ArrayList<>();
        for (int i = 1; i < wordsList.size(); i += 2) {
            String[] words = wordsList.get(allFileIndex++).split(" ");
            HashMap<String, Integer> wordsFrequency = new HashMap<>();
            for (String w : words) {
                wordsFrequency.put(w, wordsFrequency.getOrDefault(w, 0) + 1);
            }
            allServiceFileWords.add(wordsFrequency);
        }
        System.out.println("refactoring all service words success" + allServiceFileWords.size());
    }

    private static void writeFileList(List<String> fileList, String outputPath) throws IOException {
        FileWriter writer = new FileWriter(outputPath, false);
        StringBuilder sb = new StringBuilder();
        fileList.forEach(f -> sb.append(f).append("\n"));
        writer.write(sb.toString());
        writer.close();
    }

    public static void readOverloadSrvFilesAndCallMatrixAndTFIDFMatrix() throws IOException {
        classFileList = readFileList(fileNameDir);
        int len = classFileList.size();
        classCallGraph = new int[len][len];
        JSONArray jsonArray = JSONArray.parseArray(readFile(classCallMatrixDir));
        System.out.println(jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            String objStr = jsonArray.get(i).toString();
            objStr = objStr.substring(1, objStr.length() - 1);
            int[] arr = Arrays.stream(objStr.split(",")).mapToInt(Integer::parseInt).toArray();
            classCallGraph[i] = arr;
            maxFileCallNums = Math.max(maxFileCallNums, Arrays.stream(arr).max().getAsInt());
        }
        File file = new File("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\c.txt");
        FileWriter out = new FileWriter(file);
        BufferedWriter bw = new BufferedWriter(out);
        for (int i = 0; i < classCallGraph.length; i++) {
            StringBuilder tmpstr = new StringBuilder();
            for (int j = 0; j < classCallGraph[i].length; j++) {
                if (j == classCallGraph[i].length - 1) {
                    tmpstr.append(classCallGraph[i][j]);
                } else {
                    tmpstr.append(classCallGraph[i][j]).append(",");
                }
            }
            bw.write(tmpstr.toString());
            bw.newLine();
        }
        bw.close();
        System.out.println("max-call:" + maxFileCallNums);
        HashSet<String> wordSet = new HashSet<>();
        for (String s : classFileList) {
            int allFileId = allServiceFileList.indexOf(s);
            HashMap<String, Integer> wordFrequency = allServiceFileWords.get(allFileId);
            wordSet.addAll(wordFrequency.keySet());
        }
        List<String> wordSetList = new ArrayList<>(wordSet);
        int wordSetLen = wordSetList.size();
        HashMap<String, Integer> wordOccurrenceMap = new HashMap<>();
        for (String word : wordSetList) {
            wordOccurrenceMap.put(word, getCurWordOccurrences(word));
        }

        classTFIDFList = new double[len][wordSetLen];
        for (int i = 0; i < len; i++) {
            int allFileId = allServiceFileList.indexOf(classFileList.get(i));
            HashMap<String, Integer> wordFrequency = allServiceFileWords.get(allFileId);
            int curDocWordsNum = wordFrequency.values().stream().reduce(Integer::sum).get();
            for (int j = 0; j < wordSetLen; j++) {
                String curWord = wordSetList.get(j);
                if (wordFrequency.containsKey(curWord)) {
                    classTFIDFList[i][j] = wordFrequency.get(curWord) * 1.0 / curDocWordsNum;
                    classTFIDFList[i][j] *= (len * 1.0 / (wordOccurrenceMap.get(curWord) + 1));
                }
            }
        }

        classSimMatrix = new double[len][len];
        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j < len; j++) {
                double sim = calculateCosineSim(classTFIDFList[i], classTFIDFList[j]);
                classSimMatrix[i][j] = sim;
                classSimMatrix[j][i] = sim;
            }
        }
        file = new File("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\s.txt");
        out = new FileWriter(file);
        bw = new BufferedWriter(out);
        for (int i = 0; i < classSimMatrix.length; i++) {
            StringBuilder tmpstr = new StringBuilder();
            for (int j = 0; j < classSimMatrix[i].length; j++) {
                if (j == classSimMatrix[i].length - 1) {
                    tmpstr.append(classSimMatrix[i][j]);
                } else {
                    tmpstr.append(classSimMatrix[i][j]).append(",");
                }
            }
            bw.write(tmpstr.toString());
            bw.newLine();
        }
        bw.close();
    }

    public static int getCurWordOccurrences(String curWord) {
        int count = 0;
        for (int i = 0; i < classFileList.size(); i++) {
            int allFileId = allServiceFileList.indexOf(classFileList.get(i));
            HashMap<String, Integer> wordFrequency = allServiceFileWords.get(allFileId);
            if (wordFrequency.containsKey(curWord)) {
                count++;
            }
        }
        return count;
    }

    public static double calculateCosineSim(double[] tfIdfVec1, double[] tfIdfVec2) {
        double sum = 0;
        for (int i = 0; i < tfIdfVec1.length; i++) {
            sum += tfIdfVec1[i] * tfIdfVec2[i];
        }
        return sum / calculateVecSize(tfIdfVec1) / calculateVecSize(tfIdfVec2);
    }


    public static double calculateVecSize(double[] vec) {
        double sum = 0;
        for (double v : vec) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }


    public static void readCluster() {
        String clusterStr = readFile(clusterDir);
        JSONObject jsonObject = JSONObject.parseObject(clusterStr);
        JSONArray fileArray = jsonObject.getJSONArray("clusters");
        clusters = new ArrayList<>();
        for (int i = 0; i < fileArray.size(); i++) {
            JSONArray cluster = fileArray.getJSONArray(i);
            List<String> clusterFileList = cluster.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            clusters.add(new FunctionalAtom(clusterFileList));
        }
        faNums = clusters.size();
    }


    public static void readRelationMatrix() {
        RelationFile relationFile = JSONObject.parseObject(readFileAsString(relationDir), RelationFile.class);
        relationGraph = relationFile.dependGraph;
    }


    public static void experimentInitDataStructure(String granularity) {
        historyRecord = new HashMap<>();
        outputPrograms = new ArrayList<>();
        if (granularity.equals("fa")) {
            Program origin = generateFAOriginProgram();
            outputPrograms.add(origin);
        } else {
            Program origin = generateFileOriginProgram();
            outputPrograms.add(origin);
        }
        outputObjectiveData = new ArrayList<>();
        List<Double> originObjectives = new ArrayList<Double>() {{
            add(0.0);
            add(0.0);
            add(0.0);
        }};
        outputObjectiveData.add(originObjectives);
        outputFrontData = new ArrayList<>();
    }

    public static Program generateFAOriginProgram() {
        Program origin = new Program("originProgram");
        SubService originService = new SubService("originService", new ArrayList<>());
        for (int j = 0; j < faNums; j++) {
            originService.children.add(new FAFile("FA - " + j, clusters.get(j).fileList));
        }
        origin.children = new ArrayList<>();
        origin.children.add(originService);
        return origin;
    }

    public static Program generateFileOriginProgram() {
        Program origin = new Program("originProgram");
        SubService originService = new SubService("originService", new ArrayList<>());
        for (int j = 0; j < classFileList.size(); j++) {
            int fileId = j;
            ArrayList<String> children = new ArrayList<String>() {{
                add(classFileList.get(fileId));
            }};
            originService.children.add(new FAFile("File - " + fileId, children));
        }
        origin.children = new ArrayList<>();
        origin.children.add(originService);
        return origin;
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

    public static boolean judgeCriticalClass(String name) {
        for (String i : criticalClass) {
            if (i.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static void getClassNumberMap() {

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

    public static void getCriticalClass() {
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
            Collections.addAll(criticalClass, criticalString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getFinalCriticalClass() {
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
            Collections.addAll(finalClass, finalString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getFatherCriticalClass() {
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
//                int fatherClassNumber = Integer.parseInt(i);
//                String fatherClassName = extendClassNumberMap.get(fatherClassNumber);
                if(criticalClass.contains(fatherClassName)){
                    fatherCriticalClass.add(fatherClassName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getFatherCriticalClassExtend() {
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
                fatherCriticalClassExtend.put(filename, new ArrayList<>());
            }
        }
        for (Object i : edgeArr) {
            JSONObject tmp = (JSONObject) i;
            String fileSon = extendClassNumberMap.get(Integer.valueOf((String) tmp.get("src")));
            String fileFather = extendClassNumberMap.get(Integer.valueOf((String) tmp.get("dest")));
            if (judgeCriticalClass(fileFather)) {
                List<String> fatherExtendMap = fatherCriticalClassExtend.get(fileFather);
                fatherExtendMap.add(fileSon);
                fatherCriticalClassExtend.put(fileFather, fatherExtendMap);
            }
        }
        for (String i : fatherCriticalClassExtend.keySet()) {
            List<String> curSon = fatherCriticalClassExtend.get(i);
            Set<String> newCurSon = new HashSet<>(curSon);
            for (String j : curSon) {
                if (fatherCriticalClassExtend.containsKey(j)) {
                    newCurSon.addAll(fatherCriticalClassExtend.get(j));
                }
            }
            List<String> tmp = new ArrayList<>(newCurSon);
            fatherCriticalClassExtend.put(i, tmp);
        }
    }

    private static void getCallArrays() {
        int classNum = classFileList.size();
        callInArray = new int[classNum];
        classCallsMatrix = new int[classNum][classNum];
        String src = "C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\call-data.json";
        String json = JsonToObjTest(src);
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONArray edgeArr = (JSONArray) jsonObject.get("edge");
        for (Object i : edgeArr) {
            JSONObject tmp = (JSONObject) i;
            String sourceFile = (String) tmp.get("src");
            String targetFile = (String) tmp.get("dest");
            int callWeight = (int) Double.parseDouble((String) tmp.get("weight"));
            callInArray[Integer.parseInt(targetFile)] += callWeight;
            classCallsMatrix[Integer.parseInt(sourceFile)][Integer.parseInt(targetFile)] += callWeight;
        }
        classCallSTRMatrix = new double[classNum][classNum];
        for (int i = 0; i < classNum; i++) {
            for (int j = i + 1; j < classNum; j++) {
                if (callInArray[i] > 0 && callInArray[j] > 0) {
                    double cj = (double) classCallsMatrix[i][j] / callInArray[j];
                    double ci = (double) classCallsMatrix[j][i] / callInArray[i];
                    classCallSTRMatrix[i][j] = (cj + ci) / 2;
                    classCallSTRMatrix[j][i] = (cj + ci) / 2;
                }
                else if(callInArray[i] == 0 && callInArray[j] > 0){
                    double cj = (double) classCallsMatrix[i][j] / callInArray[j];
                    classCallSTRMatrix[i][j] = cj;
                    classCallSTRMatrix[j][i] = cj;
                }
                else if(callInArray[i] > 0 && callInArray[j] == 0){
                    double ci = (double) classCallsMatrix[j][i] / callInArray[i];
                    classCallSTRMatrix[i][j] = ci;
                    classCallSTRMatrix[j][i] = ci;
                }
            }
        }
    }

    public static void loadRelationGraph() {
        int len = classFileList.size();
        classRelationGraph = new int[len][len];
        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j < len; j++) {
                for (int k = 0; k < len; k++) {
                    if (i != k && j != k) {
                        if ((classCallGraph[i][k] > 0) && (classCallGraph[j][k] > 0)) {
                            classRelationGraph[i][j] = 1;
                            classRelationGraph[j][i] = 1;
                            break;
                        }
                    }
                }
            }
        }
    }
}
