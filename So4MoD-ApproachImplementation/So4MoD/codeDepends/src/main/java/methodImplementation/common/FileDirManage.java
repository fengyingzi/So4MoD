package com.nju.bysj.softwaremodularisation.common;

import java.io.File;


public class FileDirManage {

    /**
     * RefactorBackend/src/main/dataFiles
     */
    public static String rootPath = System.getProperty("user.dir") + "/better-architecture-personal/src/main/dataFiles";

    public static String wordsDir = rootPath + "/words.dat";
    public static String filenameDir = rootPath + "/files.flist";

    public static String perplexityDir = rootPath + "/perplexity.txt";

    public static String concernDir = rootPath + "/concern.txt";

    public static String dependencyDir = rootPath + "/dependency.json";
    public static String relationDir = rootPath + "/relation.json";
    public static String fileNameDir = rootPath + "/files.flist";
    public static String classCallMatrixDir = rootPath + "/classCallGraph.json";

    public static String clusterDir = rootPath + "/cluster.json";


    public static boolean checkNLPProcess() {
        File wordFile = new File(wordsDir);
        File filenameFile = new File(filenameDir);
        return wordFile.exists() && filenameFile.exists();
    }

    public static boolean checkModelPerplexity() {
        File perplexityFile = new File(perplexityDir);
        return perplexityFile.exists();
    }

    public static boolean checkRelationDependency() {
        File relationFile = new File(relationDir);
        return relationFile.exists();
    }

    public static boolean checkCallMatrix() {
        File overloadServiceFilenameFile = new File(filenameDir);
        File overloadServiceCallMatrixFile = new File(classCallMatrixDir);
        return overloadServiceFilenameFile.exists() && overloadServiceCallMatrixFile.exists();
    }

    public static boolean checkDependencyJSON() {
        File dependencyFile = new File(dependencyDir);
        return dependencyFile.exists();
    }

    public static boolean checkConcernFile() {
        File concernFile = new File(concernDir);
        return concernFile.exists();
    }

    public static boolean checkClusterFile() {
        File clusterFile = new File(clusterDir);
        return clusterFile.exists();
    }

}
