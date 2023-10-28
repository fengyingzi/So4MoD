package com.nju.bysj.softwaremodularisation.nsga.objective;

public class dataEntry {
    double score;
    int num;
    dataEntry(double s, int n){
        this.score = s;
        this.num = n;
    }
    public double getNumScore(){
        return score*num;
    }
}
