package com.nju.bysj.softwaremodularisation.semantic.service;

import com.nju.bysj.softwaremodularisation.semantic.lda.Estimator;
import com.nju.bysj.softwaremodularisation.semantic.lda.LDAOption;
import com.nju.bysj.softwaremodularisation.semantic.lda.Model;
import com.nju.bysj.softwaremodularisation.semantic.preprocess.Corpus;
import com.nju.bysj.softwaremodularisation.semantic.preprocess.Document;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.nju.bysj.softwaremodularisation.common.FileDirManage.*;
import static com.nju.bysj.softwaremodularisation.common.Utils.javaAndDirectoryFilter;
import static com.nju.bysj.softwaremodularisation.semantic.preprocess.CommonStopWordList.myStopWords;
import static com.nju.bysj.softwaremodularisation.semantic.preprocess.PreProcessMethods.*;

@Service
public class LDAService {

    public void nlpPreprocess(String[] srvPaths) {
        Corpus corpus = preByService(srvPaths);
        StringBuilder content1 = new StringBuilder();
        content1.append(corpus.documents.size()).append("\n");
        for (Document doc : corpus.documents) {
            String line = String.join(" ", doc.words);
            line += "\n\n";
            content1.append(line);
        }

        StringBuilder content2 = new StringBuilder();
        for (String filename : corpus.fileNames) {
            content2.append(filename).append("\n");
        }

        try {
            File file = new File(wordsDir);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
                if (!file.exists()) {
                    file.createNewFile();
                }
            }

            FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false);
            fileWriter.write(content1.toString());
            fileWriter.close();

            File file2 = new File(filenameDir);
            if (!file2.getParentFile().exists()) {
                file2.getParentFile().mkdirs();
                if (!file2.exists()) {
                    file2.createNewFile();
                }
            }

            FileWriter fileWriter2 = new FileWriter(file2.getAbsolutePath(), false);
            fileWriter2.write(content2.toString());
            fileWriter2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        LDAService ldaService = new LDAService();
        ldaService.nlpPreprocess(new String[]{"C:\\Users\\blank\\Desktop\\spring-petclinic-main\\src\\main\\java"});
    }

    private Corpus preByService(String[] srvPaths) {
        Corpus allCorpus = new Corpus();
        allCorpus.init(new ArrayList<>(), new ArrayList<>());
        for(String mpath : srvPaths){
            Corpus corpus = new Corpus();
            corpus.init(mpath, javaAndDirectoryFilter);

            splitIdentifier(corpus);
            toLowerCase(corpus);
            removeStopWords(corpus, myStopWords);
            filtering(corpus);
            tf_idf(corpus);
            stemming(corpus);

            allCorpus.fileNames.addAll(corpus.fileNames);
            allCorpus.documents.addAll(corpus.documents);
        }
        Map<String, Integer> wordfrequency = new HashMap<>();
        for (Document document : allCorpus.documents) {
            for (String w : document.words) {
                if (!wordfrequency.containsKey(w)) {
                    wordfrequency.put(w, 0);
                }
                wordfrequency.put(w, wordfrequency.get(w) + 1);
            }
        }
        return allCorpus;
    }

    public void ldaProcess(int k, double alpha, double beta, int iter) throws IOException {
        LDAOption option = new LDAOption();

        option.est = true;
        option.inf = false;

        option.alpha = 1.0 / k;
        option.beta = beta;
        option.K = k;
        option.niters = iter;

        option.dir = rootPath;
        option.dfile = "words.dat";
        option.savestep = 100;
        option.twords = 50;

        Estimator estimator = new Estimator();
        estimator.init(option);
        estimator.estimate();

        File perplexityFile = new File(perplexityDir);
        if (!perplexityFile.exists()) {
            perplexityFile.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(perplexityFile.getAbsolutePath(), false);
        Model model = estimator.trnModel;
        fileWriter.write(model.perplexity() + "");
        fileWriter.close();
    }
}
