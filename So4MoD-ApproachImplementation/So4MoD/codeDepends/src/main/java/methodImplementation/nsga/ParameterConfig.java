package com.nju.bysj.softwaremodularisation.nsga;

public class ParameterConfig {

    public static double file_threshold = 0.012;

    public static double service_threshold = 0.048;

    public static double overload_threshold = 5;


    public static float overloadRemainThreshold = 0.2f;
    public static float crossoverProb = 0.8f;
    public static float mutationProb = 0.8f;
    public static float breakProb = 0.3f;

    public static int faMaxGeneration = 200;
    public static int faMaxPopulationSize = 100;
    public static int faMaxRecord = 00;
//    public static double faMaxFrontMajority = 0.8;

    public static int fileMaxGeneration = 1000;
    public static int fileMaxPopulationSize = 100;
    public static int fileMaxRecord = 25000000;
//    public static double fileMaxFrontMajority = 0.99;

    public static int experimentTimes = 1;


//    public static String originFilePrefix = "D:\\Development\\idea_projects\\mogu_blog_v2\\";
//    public static String curFilePrefix = "D:\\Development\\idea_projects\\mogu_blog_v2-merge2\\merge2\\";
//    public static String originFilePrefix = "D:\\Development\\idea_projects\\mogu_blog_v2\\";
//    public static String curFilePrefix = "D:\\Development\\idea_projects\\mogu_blog_v2-merge3\\merge3\\";
    public static String originFilePrefix = "D:\\Development\\idea_projects\\microservices-platform\\";
    public static String curFilePrefix = "D:\\Development\\idea_projects\\microservices-platform-merge2\\merge2\\";


}
