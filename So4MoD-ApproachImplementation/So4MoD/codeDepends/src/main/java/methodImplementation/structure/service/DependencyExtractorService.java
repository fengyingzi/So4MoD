package com.nju.bysj.softwaremodularisation.structure.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nju.bysj.softwaremodularisation.structure.DependsCommand;
import com.nju.bysj.softwaremodularisation.common.FileUtils;
import com.nju.bysj.softwaremodularisation.structure.LangRegister;
import com.nju.bysj.softwaremodularisation.structure.ParameterException;
import com.nju.bysj.softwaremodularisation.structure.addons.DV8MappingFileBuilder;
import com.nju.bysj.softwaremodularisation.structure.extractor.AbstractLangProcessor;
import com.nju.bysj.softwaremodularisation.structure.extractor.LangProcessorRegistration;
import com.nju.bysj.softwaremodularisation.structure.extractor.UnsolvedBindings;
import com.nju.bysj.softwaremodularisation.structure.format.DependencyDumper;
import com.nju.bysj.softwaremodularisation.structure.format.detail.UnsolvedSymbolDumper;
import com.nju.bysj.softwaremodularisation.structure.format.path.*;
import com.nju.bysj.softwaremodularisation.structure.generator.DependencyGenerator;
import com.nju.bysj.softwaremodularisation.structure.generator.FileDependencyGenerator;
import com.nju.bysj.softwaremodularisation.structure.generator.FunctionDependencyGenerator;
import com.nju.bysj.softwaremodularisation.structure.matrix.core.DependencyMatrix;
import com.nju.bysj.softwaremodularisation.structure.matrix.transform.MatrixLevelReducer;
import com.nju.bysj.softwaremodularisation.structure.matrix.transform.strip.LeadingNameStripper;
import com.nju.bysj.softwaremodularisation.structure.util.FileUtil;
import com.nju.bysj.softwaremodularisation.structure.util.FolderCollector;
import com.nju.bysj.softwaremodularisation.structure.util.TemporaryFile;
import edu.emory.mathcs.backport.java.util.Arrays;
import net.sf.ehcache.CacheManager;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.*;
import static com.nju.bysj.softwaremodularisation.common.Utils.javaAndDirectoryFilter;

@Service
public class DependencyExtractorService {

	public static void main(String[] args) throws IOException {
        String source = "C:\\Users\\blank\\Desktop\\sample.daytrader7-master\\sample.daytrader7-master";
		String outputDir = "C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\codeDepends\\src\\main\\java\\com\\nju\\bysj\\softwaremodularisation\\data\\output";
        getStructureData(source, outputDir, "Extend");
    }

    public static void getStructureData(String source, String outputDir, String dependencyType) throws IOException {
        String lang = "java";
        String outputName = "dependency";
        mineParser(lang, source, outputName, outputDir);

        String jsonFilePath = outputDir + '/' + outputName + ".json";
        String jsonString = readFile(jsonFilePath);
        JSONObject jsonObject = JSONObject.parseObject(jsonString);

        HashMap<String, Object> structureData = new HashMap<>();
        structureData.put("vertex",new ArrayList<>());
        structureData.put("edge", new ArrayList<>());
        structureData.put("module", new ArrayList<>());

        ArrayList<Object> vertexMap = (ArrayList<Object>) structureData.get("vertex");
        ArrayList<Object> edgeMap = (ArrayList<Object>) structureData.get("edge");
        JSONArray fileArray = jsonObject.getJSONArray("variables");
        int index = 0;
        for (Object aFileArray : fileArray) {
            String filePath = aFileArray.toString();
            HashMap<String, Object> entityInfoMap = new HashMap<>();
            entityInfoMap.put("index", index);
            entityInfoMap.put("name", filePath);
            vertexMap.add(entityInfoMap);
            index ++;
        }

        JSONArray depArray = jsonObject.getJSONArray("cells");
        for(Object depInfo: depArray) {
            HashMap<String, Object> edgeInfo = new HashMap<>();
            JSONObject depInfoObject = JSONObject.parseObject(depInfo.toString());
            HashMap<String, Object> depInfoMap = (HashMap<String, Object>) depInfoObject.getInnerMap();
            HashMap<String, Object> depTypeMap = (HashMap<String, Object>) JSONObject.parseObject(depInfoMap.get("values").toString()).getInnerMap();
            String src = depInfoMap.get("src").toString();
            String dest = depInfoMap.get("dest").toString();
            if(!depTypeMap.containsKey(dependencyType)) {
                continue;
            }
            String weight = depTypeMap.get(dependencyType).toString();
            edgeInfo.put("src", src);
            edgeInfo.put("dest", dest);
            edgeInfo.put("weight", weight);
            edgeInfo.put("dependencyType", dependencyType);
            edgeMap.add(edgeInfo);

        }

        JSONObject structureJson = new JSONObject(structureData);
        String callJsonStr = JSONObject.toJSONString(structureJson);
        System.out.println(jsonFilePath);
        writeObjString(callJsonStr, outputDir + "/data.json");
        getRelationJson(jsonFilePath, outputDir + "/relation.json", outputDir + "/relationWeight.json", "Call");

    }

	public static void mineParser(String lang, String source, String outputName, String outputDir) {
        try {
            LangRegister langRegister = new LangRegister();
            langRegister.register();
            DependsCommand app = new DependsCommand();
            app.setLang(lang);
            app.setSrc(source);
            app.setOutput(outputName);
            mineExecutor(app, outputDir);
        } catch (Exception e){
            if (e instanceof CommandLine.PicocliException) {
                CommandLine.usage(new DependsCommand(), System.out);
            } else if (e instanceof ParameterException){
                System.err.println(e.getMessage());
            } else {
                System.err.println("Exception encountered. If it is a design error, please report issue to us." );
                e.printStackTrace();
            }
        }
    }

    private static void mineExecutor(DependsCommand app, String outputDir) throws ParameterException {
        String lang = app.getLang();
        String inputDir = app.getSrc();
        String[] includeDir = app.getIncludes();
        String outputName = app.getOutputName();

        outputDir = (outputDir == null) ? "D:\\" : outputDir;
        String[] outputFormat = app.getFormat();

        inputDir = FileUtil.uniqFilePath(inputDir);
        boolean supportImplLink = false;
        if (app.getLang().equals("cpp") || app.getLang().equals("python")) supportImplLink = true;

        if (app.isAutoInclude()) {
            FolderCollector includePathCollector = new FolderCollector();
            List<String> additionalIncludePaths = includePathCollector.getFolders(inputDir);
            additionalIncludePaths.addAll(Arrays.asList(includeDir));
            includeDir = additionalIncludePaths.toArray(new String[] {});
        }

        AbstractLangProcessor langProcessor = LangProcessorRegistration.getRegistry().getProcessorOf(lang);
        if (langProcessor == null) {
            System.err.println("Not support this language: " + lang);
            return;
        }

        if (app.isDv8map()) {
            DV8MappingFileBuilder dv8MapfileBuilder = new DV8MappingFileBuilder(langProcessor.supportedRelations());
            dv8MapfileBuilder.create(outputDir+File.separator+"depends-dv8map.mapping");
        }

        long startTime = System.currentTimeMillis();

        FilenameWritter filenameWritter = new EmptyFilenameWritter();
        if (!StringUtils.isEmpty(app.getNamePathPattern())) {
            if (app.getNamePathPattern().equals("dot")||
                    app.getNamePathPattern().equals(".")) {
                filenameWritter = new DotPathFilenameWritter();
            } else if (app.getNamePathPattern().equals("unix")||
                    app.getNamePathPattern().equals("/")) {
                filenameWritter = new UnixPathFilenameWritter();
            } else if (app.getNamePathPattern().equals("windows")||
                    app.getNamePathPattern().equals("\\")) {
                filenameWritter = new WindowsPathFilenameWritter();
            } else{
                throw new ParameterException("Unknown name pattern paremater:" + app.getNamePathPattern());
            }
        }

        /* by default use file dependency generator */
        DependencyGenerator dependencyGenerator = new FileDependencyGenerator();
        if (!StringUtils.isEmpty(app.getGranularity())) {
            /* method parameter means use method generator */
            if (app.getGranularity().equals("method"))
                dependencyGenerator = new FunctionDependencyGenerator();
            else if (app.getGranularity().equals("file"))
                /*no action*/;
            else if (app.getGranularity().startsWith("L"))
                /*no action*/;
            else
                throw new ParameterException("Unknown granularity parameter:" + app.getGranularity());
        }

        if (app.isStripLeadingPath() || app.getStrippedPaths().length > 0) {
            dependencyGenerator.setLeadingStripper(new LeadingNameStripper(app.isStripLeadingPath(),inputDir,app.getStrippedPaths()));
        }

        if (app.isDetail()) {
            dependencyGenerator.setGenerateDetail(true);
        }

        dependencyGenerator.setFilenameRewritter(filenameWritter);
        langProcessor.setDependencyGenerator(dependencyGenerator);

        langProcessor.buildDependencies(inputDir, includeDir, app.getTypeFilter(), supportImplLink, app.isOutputExternalDependencies());
        DependencyMatrix matrix = langProcessor.getDependencies();

        if (app.getGranularity().startsWith("L")) {
            matrix = new MatrixLevelReducer(matrix,app.getGranularity().substring(1)).shrinkToLevel();
        }
        DependencyDumper output = new DependencyDumper(matrix);
        output.outputResult(outputName,outputDir,outputFormat);
        if (app.isOutputExternalDependencies()) {
            Set<UnsolvedBindings> unsolved = langProcessor.getExternalDependencies();
            UnsolvedSymbolDumper unsolvedSymbolDumper = new UnsolvedSymbolDumper(unsolved,app.getOutputName(),app.getOutputDir(),
                    new LeadingNameStripper(app.isStripLeadingPath(),inputDir,app.getStrippedPaths()));
            unsolvedSymbolDumper.output();
        }
        long endTime = System.currentTimeMillis();
        TemporaryFile.getInstance().delete();
        CacheManager.create().shutdown();
        System.out.println("Consumed time: " + (float) ((endTime - startTime) / 1000.00) + " s,  or "
                + (float) ((endTime - startTime) / 60000.00) + " min.");
    }

    public static void getRelationJson(String dependsPath, String outputPath1, String outputPath2, String dependencyType) throws IOException {
        String jsonString = readFile(dependsPath);
        System.out.println("read: dependency success");
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        JSONArray fileArray = jsonObject.getJSONArray("variables");
        List<String> fullFiles = new ArrayList<>();
        for (Object aFileArray : fileArray) {
            fullFiles.add(aFileArray.toString());
        }


        int len = fullFiles.size();
        int[][] dependGraph = new int[len][len];
        int[][] dependWeightGraph = new int[len][len];

        JSONArray cellArray = jsonObject.getJSONArray("cells");
        for (int i = 0; i < cellArray.size(); i++){
            JSONObject subObj = cellArray.getJSONObject(i);
            int srcId = subObj.getIntValue("src");
            int destId = subObj.getIntValue("dest");
            JSONObject deps = subObj.getJSONObject("values");
            if(deps.containsKey(dependencyType)){
                dependGraph[srcId][destId] = 1;
                dependWeightGraph[srcId][destId] = deps.getIntValue(dependencyType);
            }
        }

        FileUtils.FileDependence object1 = new FileUtils.FileDependence(fullFiles, dependGraph);
        FileUtils.FileDependence object2 = new FileUtils.FileDependence(fullFiles, dependWeightGraph);
        String relationString1 = JSONObject.toJSONString(object1, true);
        String relationString2 = JSONObject.toJSONString(object2, true);
        writeObjString(relationString1, outputPath1);
        writeObjString(relationString2, outputPath2);
    }


    public void getServiceCallGraph(String servicePath, String dependsPath, String outputFilesPath,  String outputGraphPath) throws IOException {
        List<String> srvFileList = scan(new File(servicePath), javaAndDirectoryFilter);
        int len = srvFileList.size();

        String jsonString = readFile(dependsPath);
        System.out.println("read: " + dependsPath + " success");
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        JSONArray fileArray = jsonObject.getJSONArray("variables");
        List<String> fileSequence = new ArrayList<>();
        for (Object aFileArray : fileArray) {
            fileSequence.add(aFileArray.toString());
        }

        int[][] callGraph = new int[len][len];
        JSONArray cellArray = jsonObject.getJSONArray("cells");
        for (int i = 0; i < cellArray.size(); i++){
            JSONObject subObj = cellArray.getJSONObject(i);
            int srcId = subObj.getIntValue("src");
            int destId = subObj.getIntValue("dest");
            String srcFile = fileSequence.get(srcId), destFile = fileSequence.get(destId);
            if (srvFileList.contains(srcFile) && srvFileList.contains(destFile)) {
                int srcIndex = srvFileList.indexOf(srcFile), destIndex = srvFileList.indexOf(destFile);
                JSONObject dependencyArr = subObj.getJSONObject("values");
                double callDouble = dependencyArr.getDoubleValue("Call");
                int weight = (int) callDouble;
                if (weight > 0) {
                    callGraph[srcIndex][destIndex] = weight;
                }
            }
        }

        writeByLine(srvFileList, outputFilesPath);
        String callJsonStr = JSONObject.toJSONString(callGraph);
        writeObjString(callJsonStr, outputGraphPath);
    }

}
