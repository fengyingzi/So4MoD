package com.nju.bysj.softwaremodularisation.nsga.assessment;

import java.io.*;
import java.util.*;

public class utils {
    public static Map<Integer, String> classNumberMap = new HashMap<>();
    public static Map<Integer, String> shortClassNumberMap = new HashMap<>();
    public static Map<String, String> arrange = new HashMap<>();

    public static void getClassNumberMap() {
        int start = 0;
        try {
            Scanner scanner = new Scanner(new File("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\files.flist"));
            while (scanner.hasNextLine()) {
                String classname = scanner.nextLine();
                classNumberMap.put(start, classname);
                String[] classnamelist = classname.split("\\\\");
                String shortname = classnamelist[classnamelist.length-1].split("\\.")[0];
                shortClassNumberMap.put(start, shortname);
                start++;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void readcsv(){
        String File = "C:\\Users\\blank\\Desktop\\Mono2Micro-FSE-2021-master\\datasets_runtime\\daytrader\\fosci_output\\daytrader_n_candidate_35_repeat_4.csv";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        String line = "";
        String[] Line;
        try (BufferedReader br = new BufferedReader(new FileReader(File))) {
            while ((line = br.readLine()) != null) {
                String[] linedata = line.split(",");
                arrange.put(linedata[0], linedata[1]);
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i=0;i<shortClassNumberMap.size();i++){
            sb.append(arrange.getOrDefault(shortClassNumberMap.get(i), "-1"));
            sb.append(", ");
        }
        sb.append("]");
        String res = sb.toString();
        System.out.println(res);
    }

    public static void main(String[] args){
        getClassNumberMap();
        readcsv();
    }
}
