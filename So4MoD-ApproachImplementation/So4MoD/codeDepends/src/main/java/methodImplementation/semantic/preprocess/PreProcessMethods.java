package com.nju.bysj.softwaremodularisation.semantic.preprocess;

import org.tartarus.snowball.ext.englishStemmer;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PreProcessMethods {

    public static List<String> tokenize(String context) {
        List<String> words = new ArrayList<String>();
        String code = context.replaceAll("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]", "");
        code = code.replaceAll("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*", "");

        code = code.replaceAll("import(\\sstatic)?\\s[\\S]+", "");

        code = code.replaceAll("package\\s[\\S]+", "");

        Pattern pattern = Pattern.compile("[A-Za-z_]+[A-Za-z0-9_]*");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            String token = matcher.group();
            words.add(token);
        }
        return words;
    }

    public static void tokenize(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> words = (ArrayList<String>) tokenize(doc.rawStr);
            doc.words = words;
        }
    }

    public static void removeStopWords(Corpus corpus, List<String> stopWordList) {
        for (Document doc : corpus.documents) {
            doc.words.removeAll(stopWordList);
        }
    }

    private static boolean isUnderlineCase(String token) {
        return token.contains("_");
    }

    private static boolean isCamelCase(String token) {
        if (token.toUpperCase().equals(token)) return false;

        boolean flag = false;
        for (int i = 1; i < token.length() - 1; i++) {
            char c = token.charAt(i);
            if ((c - 'A') >= 0 && (c - 'Z') <= 0) {
                flag = true;
            }
        }
        return flag;
    }

    public static List<String> splitIdentifier(String word) {
        if (isCamelCase(word)) {
            int firstDownCase = 1;
            int followingUpperCase;
            for (; firstDownCase < word.length(); firstDownCase++) {
                char c = word.charAt(firstDownCase);
                if ((c - 'a') >= 0 && (c - 'z') <= 0) {
                    break;
                }
            }
            for (followingUpperCase = firstDownCase; followingUpperCase < word.length(); followingUpperCase++) {
                char c = word.charAt(followingUpperCase);
                if ((c - 'A') >= 0 && (c - 'Z') <= 0) {
                    break;
                }
            }

            if (firstDownCase == 1) {
                List list1 = splitIdentifier(word.substring(0, followingUpperCase));
                List list2 = splitIdentifier(word.substring(followingUpperCase));
                list1.addAll(list2);
                return list1;
            } else {
                List list1 = splitIdentifier(word.substring(0, firstDownCase - 1));
                List list2 = splitIdentifier(word.substring(firstDownCase - 1));
                list1.addAll(list2);
                return list1;
            }

        } else if (isUnderlineCase(word)) {
            List list = new ArrayList();

            for (String token : word.split("_")) {
                list.addAll(splitIdentifier(token));
            }
            return list;
        } else {
            List list = new ArrayList<String>();
            list.add(word);
            return list;
        }

    }

    public static void splitIdentifier(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> ret = new ArrayList<>();
            for (String s : doc.words) {
                ret.addAll(splitIdentifier(s));
            }
            doc.words = ret;
        }
    }


    public static String stemming(String word) {
        englishStemmer stemmer = new englishStemmer();
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        }
        return word;
    }

    public static void stemming(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> ret = new ArrayList<>();
            for (String s : doc.words) {
                ret.add(stemming(s));
            }
            doc.words = ret.stream().filter(word -> !word.isEmpty()).collect(Collectors.toList());
        }
    }

    public static void toLowerCase(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> ret = new ArrayList<>();
            for (String s : doc.words) {
                ret.add(s.toLowerCase());
            }
            doc.words = ret;
        }
    }

    public static void filtering(Corpus corpus){
        Map<String,Integer> wordfrequency = new HashMap<>();
        for(Document document : corpus.documents){
            for(String w : document.words){
                if(!wordfrequency.containsKey(w)){
                    wordfrequency.put(w, 0);
                }
                wordfrequency.put(w, wordfrequency.get(w)+1);
            }
        }

        int frequencyThreshold = 5;
        for (Document document : corpus.documents) {
            List<String> tmp = new ArrayList<>();
            for (String word : document.words) {
                if (word.length() <= 1) continue;
                if (word.matches("-?[0-9]+.？[0-9]*")) continue;
                if (wordfrequency.get(word) <= frequencyThreshold) continue;
                tmp.add(word);
            }
            document.words = tmp;
        }
    }

    public static Map<String,Double> getDocCount(Corpus corpus){
        Map<String,Double> res = new HashMap<>();
        Set<String> wordSet = new HashSet<>();
        for(Document doc : corpus.documents){
            wordSet.addAll(doc.words);
        }
        for(String word : wordSet){
            for(Document document : corpus.documents){
                if(document.words.contains(word)){
                    if(!res.containsKey(word)){
                        res.put(word, 1.0);
                    } else {
                        res.put(word, res.get(word) + 1.0);
                    }
                }
            }
        }
        return res;
    }

    public static void tf_idf(Corpus corpus){
        Map<String, Double> docCount = getDocCount(corpus);
        for(Document document : corpus.documents){
            List<String> tmp = new ArrayList<>();
            Map<String, Double> docfrequency = new HashMap<>();
            if(document.words.size() == 0) continue;
            for (String word : document.words){
                if(!docfrequency.containsKey(word)) {
                    docfrequency.put(word, 0.0);
                }
                docfrequency.put(word, docfrequency.get(word) + 1.0);
            }
            Map<String, Double> doctfidf = new HashMap<>();
            for(Map.Entry<String, Double> entry : docfrequency.entrySet()){
                double tf = entry.getValue() / document.words.size();
                double idf = Math.log((double) corpus.documents.size() / (1 + docCount.get(entry.getKey())));
                double tfidf = tf*idf;
                doctfidf.put(entry.getKey(), tfidf);
            }
            List<Double> tivalue = new ArrayList<>();
            for(Map.Entry<String,Double> en : doctfidf.entrySet()){
                tivalue.add(en.getValue());
            }
            Collections.sort(tivalue);
            int threshIndex = (int)Math.floor(tivalue.size() * 0.2) - 1;
            if (threshIndex >= 0) {
                double thresh = tivalue.get(threshIndex);
                for(String word : document.words){
                    double dd = doctfidf.get(word);
                    if(new BigDecimal(dd).compareTo(new BigDecimal(thresh)) > 0){
                        tmp.add(word);
                    }
                }
                document.words = tmp;
            }
        }
    }


}
