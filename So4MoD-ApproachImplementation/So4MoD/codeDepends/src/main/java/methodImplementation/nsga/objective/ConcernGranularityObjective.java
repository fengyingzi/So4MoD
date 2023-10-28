package com.nju.bysj.softwaremodularisation.nsga.objective;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.ParameterConfig;
import com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.IntegerAllele;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFile;

public class ConcernGranularityObjective {

    public static boolean ifChromosomeOverload(Chromosome chromosome) {
        HashMap<Integer, List<Integer>> serviceFiles = Common.splitChromosomeToFAFileListMap(chromosome);
        HashMap<Integer, List<Integer>> serviceConcernMap = getServiceConcernMap(serviceFiles);
        int maxConcernNum = serviceConcernMap.values().stream().mapToInt(List::size).max().getAsInt();
        return maxConcernNum > ParameterConfig.overload_threshold;
    }

    private static class ServiceProb {
        int serviceId;
        double prob;

        public ServiceProb(int serviceId, double prob) {
            this.serviceId = serviceId;
            this.prob = prob;
        }
    }

    public static HashMap<Integer, List<Integer>> getServiceConcernMap(Map<Integer, List<Integer>> serviceFiles) {
        HashMap<Integer, List<ServiceProb>> concernServiceMap = new HashMap<>();
        for(int topic = 0; topic < PreProcessLoadData.model.K; topic++) {
            for (Map.Entry<Integer, List<Integer>> service : serviceFiles.entrySet()) {
                List<Integer> files = service.getValue();
                int serviceWordCount = 0;
                double serviceProbability = 0.0;

                for (int file : files) {
                    int fileWords = PreProcessLoadData.model.z[file].size();
                    serviceWordCount += fileWords;
                }

                for (int file : files) {
                    double fileProb = PreProcessLoadData.model.theta[file][topic];
                    int fileWords = PreProcessLoadData.model.z[file].size();
//                    System.out.println("  adding " + ((double) fileWords / serviceWordCount * fileProb) + " service: " + service.getKey() +
//                            " ; file: " + file + " - " + refactorServiceFileList.get(file).substring(59) +
//                            " ; serviceWordCount: " + serviceWordCount + " ; fileWords: " + fileWords + " ; fileProb: " + fileProb);
                    serviceProbability += (double) fileWords / serviceWordCount * fileProb;
                }

//                System.out.println(service.getKey() + " - topic" + topic + " prob: " + serviceProbability);
                if (serviceProbability >= ParameterConfig.service_threshold && PreProcessLoadData.concerns.contains(topic)) {
                    if (!concernServiceMap.containsKey(topic)) {
                        concernServiceMap.put(topic, new ArrayList<>());
                    }
                    concernServiceMap.get(topic).add(new ServiceProb(service.getKey(), serviceProbability));
//                    System.out.println(service.getKey() + " - topic" + topic + " prob: " + serviceProbability);
                }
            }
        }

        HashMap<Integer, List<Integer>> concernServiceListMap = new HashMap<>();
        List<Integer> allServiceFileIdList = PreProcessLoadData.allServiceFileList.stream()
                .map(f -> PreProcessLoadData.allServiceFileList.indexOf(f))
                .collect(Collectors.toList());

        for (Map.Entry<Integer, List<ServiceProb>> entry : concernServiceMap.entrySet()) {
            int concern = entry.getKey();
            Double originTC = PreProcessLoadData.concernTCMap.get(concern);
//            Double originTC = 0.0;
            List<ServiceProb> sortedDescServices = entry.getValue().stream()
                    .sorted((i1, i2) -> (Double.compare(i2.prob, i1.prob)))
                    .collect(Collectors.toList());
            double curTC = 1.0;
            List<Integer> selectedFile = new ArrayList<>();
            List<Integer> restFile = new ArrayList<>(allServiceFileIdList);

            List<Integer> selectedServices = new ArrayList<>();
            int index = 0;
            while (index < sortedDescServices.size() && curTC >= originTC) {
                int curService = sortedDescServices.get(index).serviceId;
                List<Integer> files = serviceFiles.get(curService);
                selectedServices.add(curService);

                for (int fileID : files) {
                    if (PreProcessLoadData.model.theta[fileID][concern] >= ParameterConfig.file_threshold) {
                        selectedFile.add(fileID);
                        restFile.remove((Integer) fileID);
                    }
                }

                curTC = calculateTC(selectedFile, restFile);
                index++;
            }
//            System.out.println("concern: " + concern + " ; before : " + sortedDescServices.size() + " ; after: " + selectedServices.size() + " ; index: " + index);
            concernServiceListMap.put(concern, selectedServices);
        }

        HashMap<Integer, List<Integer>> serviceConcernMap = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : concernServiceListMap.entrySet()) {
            Integer c = entry.getKey();
            for (int s : entry.getValue()) {
                if (!serviceConcernMap.containsKey(s)) {
                    serviceConcernMap.put(s, new ArrayList<>());
                }
                serviceConcernMap.get(s).add(c);
            }
        }

        return serviceConcernMap;
    }


    private static double calculateTC(List<Integer> selected, List<Integer> rest) {
        int r_int = 0;
        int r_ext = 0;
        for (int i : selected) {
            for (int j : selected) {
                if (i != j && PreProcessLoadData.relationGraph[i][j] == 1) {
                    r_int += 1;
                }
            }
        }
        for (int i : selected) {
            for (int j : rest) {
                if (PreProcessLoadData.relationGraph[i][j] == 1) {
                    r_ext += 1;
                }
            }
        }
        return r_int / (r_int + r_ext + 0.000000000000001);
    }

    public static void main(String[] args) throws IOException {
        String jsonString = readFile("D:\\Development\\idea_projects\\NSGA-II\\output\\faPrograms.json");
        JSONArray programs = JSONArray.parseArray(jsonString);

        for (int i = 1; i < programs.size(); i++) {
            JSONObject program = programs.getJSONObject(i);
            System.out.println(program.getString("name") + " -------------------------------------------------------------");
            HashMap<Integer, List<Integer>> moduleFAMap = new HashMap<>();
            JSONArray services = program.getJSONArray("children");
            List<IntegerAllele> faIndexList = new ArrayList<>(PreProcessLoadData.faNums);
            for (int j = 0; j < PreProcessLoadData.faNums; j++) {
                faIndexList.add(new IntegerAllele(-1));
            }

            for (int j = 0; j < services.size(); j++) {
                JSONObject service = services.getJSONObject(j);
                String serviceName = service.getString("name");
                int serviceId = Integer.parseInt(serviceName.substring("service".length()));
                JSONArray fas = service.getJSONArray("children");
                moduleFAMap.put(j + 1, new ArrayList<>());
                for (int k = 0; k < fas.size(); k++) {
                    String faName = fas.getJSONObject(k).getString("name");
                    int faId = Integer.parseInt(faName.substring("FA - ".length()));
                    moduleFAMap.get(j + 1).add(faId);
                    faIndexList.set(faId, new IntegerAllele(serviceId));
                }
            }

            Chromosome chromosome = new Chromosome(faIndexList);
            HashMap<Integer, List<Integer>> serviceFiles = Common.splitChromosomeToFAFileListMap(chromosome);
            System.out.println(ifChromosomeOverload(chromosome));

            HashMap<Integer, List<Integer>> serviceConcernMap = getServiceConcernMap(serviceFiles);
            for (Map.Entry<Integer, List<Integer>> entry : moduleFAMap.entrySet()) {
                int sid = entry.getKey();
                System.out.println("service: " + sid);
                System.out.println("  fa list: " + moduleFAMap.get(sid) + " ; files: " + serviceFiles.get(sid).size());
                System.out.println("  concerns: " + serviceConcernMap.getOrDefault(sid, new ArrayList<>()) +
                        "  sum: " + serviceConcernMap.getOrDefault(sid, new ArrayList<>()).size());
            }
        }
    }
}
