package com.nju.bysj.softwaremodularisation.nsga.assessment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static com.nju.bysj.softwaremodularisation.nsga.PreProcessLoadData.classFileList;

public class IFNAssessment {
    public static List<String> interfaces = new ArrayList<>();

    public static void getInterfaces() {
        StringBuilder sb = new StringBuilder();
        String text = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\interface.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ((text = bufferedReader.readLine()) != null) {
                sb.append(text);
            }
            String[] interfaceString = sb.toString().split(",");
            Collections.addAll(interfaces, interfaceString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double caculateAllServiceIFN(HashMap<Integer, List<Integer>> srvFilesMap) {
        int srvSize = srvFilesMap.size();
        int IFNEntry = 0;
        for (Map.Entry<Integer, List<Integer>> entrys : srvFilesMap.entrySet()) {
            List<String> serviceInnerEntrys = new ArrayList<>();
            entrys.getValue().forEach(fi -> serviceInnerEntrys.add(classFileList.get(fi)));
            IFNEntry += calculateSingleSrvIFN(serviceInnerEntrys);
        }
        return (double) IFNEntry / srvSize;
    }

    private static int calculateSingleSrvIFN(List<String> serviceInnerEntrys) {
        int interfaceNum = 0;
        for (String i : serviceInnerEntrys) {
            for (String j : interfaces) {
                if (i.equals(j)) {
                    interfaceNum++;
                }
            }
        }
        return interfaceNum;
    }
}
