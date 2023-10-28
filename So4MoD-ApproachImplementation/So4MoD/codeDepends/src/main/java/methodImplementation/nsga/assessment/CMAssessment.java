package com.nju.bysj.softwaremodularisation.nsga.assessment;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFileList;

public class CMAssessment {
    public double[][] classSimMatrix;
    public List<String> classFileList;
    public static List<HashMap<String, Integer>> allServiceFileWords;
    public double[][] classTFIDFList;

    CMAssessment() {
        readGlobalSrvFilesAndWords();
        loadData();
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

    public void readGlobalSrvFilesAndWords() {
        List<String> wordsList = new ArrayList<>();
        try {
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

    public void loadData() {
        try {
            classFileList = readFileList("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\files.flist");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public double caculateAllServiceCM(HashMap<Integer, List<Integer>> srvFilesMap) {
        double CM = 0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceEntrys.add(classFileList.get(fi)));
            CM += calculateSingleSrvCM(serviceEntrys);
        }
        double innerPart = CM / srvFilesMap.size();
        double outterPart = cacutaleAllOuter(srvFilesMap);
        return innerPart - outterPart;
    }

    private double calculateSingleSrvCM(List<String> serviceFiles) {
        List<Integer> fileIndexList = serviceFiles.stream()
                .map(f -> classFileList.indexOf(f))
                .collect(Collectors.toList());
        double edgeNums = 0;
        for (int i = 0; i < fileIndexList.size(); i++) {
            for (int j = i + 1; j < fileIndexList.size(); j++) {
                int src = fileIndexList.get(i), dest = fileIndexList.get(j);
                edgeNums += classSimMatrix[src][dest];
            }
        }
        return edgeNums / serviceFiles.size() / serviceFiles.size();
    }

    private double cacutaleAllOuter(HashMap<Integer, List<Integer>> srvFilesMap) {
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
                allOuter += calculatePairSrvSematic(files1, files2);
            }
        }
        return allOuter * 2 / (srvNum - 1) / srvNum;
    }

    private double calculatePairSrvSematic(List<Integer> files1, List<Integer> files2) {
        double sem = 0;
        for (Integer value : files1) {
            for (Integer integer : files2) {
                int src = value, dest = integer;
                sem += classSimMatrix[src][dest];
            }
        }
        return sem / files1.size() / files2.size() / 2;
    }

}
