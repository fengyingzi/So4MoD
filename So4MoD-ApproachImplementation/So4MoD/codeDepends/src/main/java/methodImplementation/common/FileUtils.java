package com.nju.bysj.softwaremodularisation.common;

import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.structure.service.Scanner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Scanner
public class FileUtils {

    public static void writeByLine(List<String> dataList, String outputPath) throws IOException {
        File file = new File(outputPath);
        if (file.exists()) {
            file.delete();
        }

        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(osw);

        for (String data : dataList) {
            bw.write(data);
            bw.newLine();
            bw.flush();
        }

        osw.close();
        bw.close();
    }

    public static class FileDependence {
        public List<String> fileSequence;
        public int[][] dependGraph;

        public FileDependence(List<String> fileSequence, int[][] dependGraph) {
            this.fileSequence = fileSequence;
            this.dependGraph = dependGraph;
        }
    }

    public static String readFile(String path) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            FileInputStream is = new FileInputStream(path);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            is.close();
            isr.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static List<String> readFileList(String inputPath) throws IOException {
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

    public static void writeObjString(String jsonStr, String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
        Files.createFile(path);
        System.out.println(outputPath + " create success");
        try {
            Files.write(path, jsonStr.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(outputPath + " write success");
    }

    public static List<String> scan(File file, FileFilter filter) {
        File[] files = file.listFiles(filter);
        List<String> fileList = new ArrayList<>();
        for (File f : files) {
            if (f.isFile()) {
                fileList.add(f.getAbsolutePath());
            } else {
                fileList.addAll(scan(f, filter));
            }
        }
        return fileList;
    }

    static class FACluster {
        public List<List<String>> clusters;
    }

    public static void writeClusterToJson(String outputPath, List<List<Integer>> clusters, List<String> fileSequence) throws IOException {
        FACluster object = new FACluster();
        List<List<String>> fileClusters = new ArrayList<>();
        for (List<Integer> cluster : clusters) {
            List<String> fileCluster = new ArrayList<>();
            for (int n : cluster) {
                fileCluster.add(fileSequence.get(n));
            }
            fileClusters.add(fileCluster);
        }
        object.clusters = fileClusters;

        String objString = JSONObject.toJSONString(object, true);
        Path path = Paths.get(outputPath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
        Files.createFile(path);
        System.out.println(outputPath + " create success");
        try {
            Files.write(path, objString.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(outputPath + " write success");
    }

    public static List<List<Integer>> newCluster(int k, List<Integer> medoides) {
        List<List<Integer>> clusters = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            List<Integer> item = new ArrayList<>();
            item.add(medoides.get(i));
            clusters.add(item);
        }
        return clusters;
    }

    public static void printClusters(int k, List<Integer> medoides, List<List<Integer>> clusters, List<String> fileSequence) {
        for (int i = 0; i < k; i++) {
            List<Integer> cluster = clusters.get(i);
            System.out.println("cluster" + (i+1) + " center: " + medoides.get(i) + " fileNum: " + cluster.size() + " =======================================================");
//            for (Integer n : cluster) {
//                    System.out.print(n + " ");
//                System.out.println(fileSequence.get(n).substring(29));
//            }
//                System.out.println();
        }
    }


    public static void main(String[] args) {
        System.out.println("D:\\Development\\idea_projects\\microservices-platform-master\\".length());
    }
}
