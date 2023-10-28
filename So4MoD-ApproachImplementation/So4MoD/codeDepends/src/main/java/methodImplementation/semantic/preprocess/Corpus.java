package com.nju.bysj.softwaremodularisation.semantic.preprocess;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import static com.nju.bysj.softwaremodularisation.common.FileUtils.readFile;


public class Corpus {
    public ArrayList<String> fileNames;

    public ArrayList<Document> documents;

    public void init(ArrayList<String> fileNames, ArrayList<Document> documents) {
        this.fileNames = fileNames;
        this.documents = documents;
    }

    public void init(String path, FileFilter filter) {
        fileNames = new ArrayList<>();
        documents = new ArrayList<>();

        scan(new File(path), filter);
        for (int i = 0; i < fileNames.size(); i++) {
            String filename = fileNames.get(i);
            Document document = new Document();
            document.rawStr = readFile(filename);
            document.words = PreProcessMethods.tokenize(document.rawStr);
            documents.add(document);
        }
    }

    public void init(List<String>paths, FileFilter filter) {
        fileNames = new ArrayList<String>();
        documents = new ArrayList<Document>();

        for(String path:paths){
            scan(new File(path), filter);
            for (int i = 0; i < fileNames.size(); i++) {
                String filename = fileNames.get(i);
                Document document = new Document();
                document.words = PreProcessMethods.tokenize(readFile(filename));
                documents.add(document);
            }
        }
    }

    private void scan(File file, FileFilter filter) {
        File[] files = file.listFiles(filter);

        for (File f : files) {
            if (f.isFile()) {
                fileNames.add(f.getAbsolutePath());
            } else {
                scan(f, filter);
            }
        }
    }

    public int getFileIDByName(String filename) {
        return fileNames.indexOf(filename);
    }

    public String getFileNameByID(int id) {
        return fileNames.get(id);
    }

    public int wordFrequency(String word) {
        int frequency = 0;
        for (Document doc : this.documents) {
            for (String s : doc.words) {
                if (word.equals(s)) {
                    frequency++;
                }
            }
        }
        return frequency;
    }

    public Corpus combine(ArrayList<Corpus> corpuses){
        return null;
    }

}




