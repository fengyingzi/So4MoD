package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.excel.ServiceData;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFile;
import static com.nju.bysj.softwaremodularisation.common.FileUtils.scan;
import static com.nju.bysj.softwaremodularisation.common.Utils.javaAndDirectoryFilter;
import static com.nju.bysj.softwaremodularisation.nsga.Common.splitChromosomeToServiceFileIdMap;
import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class MoJoFMFileObjective extends AbstractObjectiveFunction {

    public MoJoFMFileObjective() {
        this.objectiveFunctionTitle = "MoJoFM File Index";
    }

    @Override
    public double getValue(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> srvFilesMap = splitChromosomeToServiceFileIdMap(chromosome);
        int maxSrvFileSize = srvFilesMap.values().stream()
                .mapToInt(List::size).max().getAsInt();
        return maxSrvFileSize * 1.0 / classFileList.size();
    }

    public static void main(String[] args) throws Exception {
        String jsonString = readFile("D:\\Development\\idea_projects\\NSGA-II\\output\\fileRandomPrograms.json");
        JSONArray programs = JSONArray.parseArray(jsonString);

        String originSrvExcelPath = "D:\\School\\nju\\bysj\\common\\data\\mogu_blog_v2\\origin\\focus\\mogu_blog_v2-service.xlsx";
        List<ServiceData> serviceList = MoJoFMFAObjective.readOriginSrvToFileIdListMap(originSrvExcelPath);
        Map<String, List<String>> originSFMap = new HashMap<>();
        int allFileNum = 0;
        HashSet<Integer> differentFilesSet = new HashSet<>();
        for (ServiceData sd : serviceList) {
            List<String> files = scan(new File(sd.getPath()), javaAndDirectoryFilter);
            originSFMap.put(sd.getName(), files);
            differentFilesSet.add(files.size());
            allFileNum += files.size();
        }
        int maxMoJo = allFileNum - differentFilesSet.size();
        System.out.println("max mojo: " + maxMoJo);

        for (int i = 1; i < programs.size(); i++) {
            JSONObject program = programs.getJSONObject(i);
            System.out.println(program.getString("name") + " -------------------------------------------------------------");
            HashMap<Integer, List<String>> SFMap = MoJoFMFAObjective.getTargetSFMap(program);

            MoJoFMFAObjective.getMaximalDichotomyMatchingMoJoFM(SFMap, originSFMap, maxMoJo, true);
        }
    }
}
