package com.nju.bysj.softwaremodularisation.nsga.assessment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFile;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.JsonToObjTest;

public class CouplingAssessment {
    public double[][] classSimMatrix;
    public static List<String> classFileList;
    public static List<HashMap<String, Integer>> allServiceFileWords;
    public double[][] classTFIDFList;
    public static int[] callInArray;
    public static int[][] classCallsMatrix;
    public static double[][] classCallSTRMatrix;

    CouplingAssessment() {
        getCallGraph();
        readGlobalSrvFilesAndWords();
        loadData();
        getCallArrays();
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
                } else if (callInArray[i] == 0 && callInArray[j] > 0) {
                    double cj = (double) classCallsMatrix[i][j] / callInArray[j];
                    classCallSTRMatrix[i][j] = cj;
                    classCallSTRMatrix[j][i] = cj;
                } else if (callInArray[i] > 0 && callInArray[j] == 0) {
                    double ci = (double) classCallsMatrix[j][i] / callInArray[i];
                    classCallSTRMatrix[i][j] = ci;
                    classCallSTRMatrix[j][i] = ci;
                }
            }
        }
    }

    public void getCallGraph() {
        try {
            classFileList = readFileList("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\files.flist");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> readFileList(String inputPath) throws IOException {
        List<String> dataList = new ArrayList<>();
        InputStreamReader isr = new InputStreamReader(new FileInputStream(inputPath), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            dataList.add(line);
        }
        isr.close();
        br.close();
        return dataList;
    }

    public void readGlobalSrvFilesAndWords() {
        List<String> wordsList = new ArrayList<>();
        try {
            classFileList = readFileList("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\files.flist");
            wordsList = readFileList("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\words.dat");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    }

    public int getCurWordOccurrences(String curWord) {
        int count = 0;
        for (int i = 0; i < classFileList.size(); i++) {
            HashMap<String, Integer> wordFrequency = allServiceFileWords.get(i);
            if (wordFrequency.containsKey(curWord)) {
                count++;
            }
        }
        return count;
    }

    public static double calculateVecSize(double[] vec) {
        double sum = 0;
        for (double v : vec) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    public static double calculateCosineSim(double[] tfIdfVec1, double[] tfIdfVec2) {
        double sum = 0;
        for (int i = 0; i < tfIdfVec1.length; i++) {
            sum += tfIdfVec1[i] * tfIdfVec2[i];
        }
        return sum / calculateVecSize(tfIdfVec1) / calculateVecSize(tfIdfVec2);
    }

    public void loadData() {
        int len = classFileList.size();
        classSimMatrix = new double[len][len];
        HashSet<String> wordSet = new HashSet<>();
        for (int i = 0; i < classFileList.size(); i++) {
            HashMap<String, Integer> wordFrequency = allServiceFileWords.get(i);
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
            int allFileId = classFileList.indexOf(classFileList.get(i));
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
    }

    public double caculateAllServiceCou(HashMap<Integer, List<Integer>> srvFilesMap) {
        double Cou = 0;
        if (srvFilesMap.size() < 2) {
            return Cou;
        }
        List<List<Integer>> srvFilesList = new ArrayList<>(srvFilesMap.values());
        for (int i = 0; i < srvFilesList.size(); i++) {
            List<Integer> files1 = srvFilesList.get(i);
            for (int j = i + 1; j < srvFilesList.size(); j++) {
                List<Integer> files2 = srvFilesList.get(j);
                Cou += calculatePairSrvCou(files1, files2);
            }
        }
        return 2 * Cou / srvFilesMap.size() / (srvFilesMap.size() - 1);
    }

    private double calculatePairSrvCou(List<Integer> files1, List<Integer> files2) {
        double res = 0;
        for (int i : files1) {
            for (int j : files2) {
                res += classCallSTRMatrix[i][j] / 2;
                res += classSimMatrix[i][j] / 2;
            }
        }
        return res / files1.size() / files2.size();
    }

}
