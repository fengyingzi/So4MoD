package com.nju.bysj.softwaremodularisation.semantic.preprocess;

import java.util.List;

public class Document {
    public String rawStr;
    public List<String> words;

    @Override
    public String toString() {
        return "Document{" +
                "rawStr='" + rawStr + '\'' +
                ", words=" + words +
                '}';
    }
}
