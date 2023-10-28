package com.nju.bysj.softwaremodularisation.nsga.assessment;

import com.nju.bysj.softwaremodularisation.nsga.Common;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.Chromosome;
import com.nju.bysj.softwaremodularisation.nsga.datastructure.IntegerAllele;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.nju.bysj.softwaremodularisation.nsga.assessment.IFNAssessment.caculateAllServiceIFN;
import static com.nju.bysj.softwaremodularisation.nsga.assessment.NEDAssessment.caculateAllServiceNED;
import static com.nju.bysj.softwaremodularisation.nsga.assessment.SMAssessment.caculateAllServiceSM;

public class Caculate {
    public static List<Chromosome> chromosomeRes = new ArrayList<>();

    public static void readChromosome(String filePath) {
        try {
            List<String> allLines = Files.readAllLines(Paths.get(filePath));
            for (String line : allLines) {
                String[] tmpGeneticCodeString = line.substring(1, line.length() - 1).split(", ");
                List<IntegerAllele> tmpGeneticCode = new ArrayList<>();
                for (String i : tmpGeneticCodeString) {
                    tmpGeneticCode.add(new IntegerAllele(Integer.parseInt(i)));
                }
                Chromosome tmp = new Chromosome(tmpGeneticCode);
                chromosomeRes.add(tmp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        readChromosome("C:\\Users\\blank\\Desktop\\smd_test\\better-architecture-personal\\src\\main\\dataFiles\\fileFront-1.txt");
//        List<Double> NEDRes = new ArrayList<>();
//        List<Double> IFNRes = new ArrayList<>();
        List<Double> SMRes = new ArrayList<>();
        List<Double> SEC_ccp = new ArrayList<>();
        List<Double> SEC_cce = new ArrayList<>();
        List<Double> SEC_csp = new ArrayList<>();
        List<Double> SEC_csi = new ArrayList<>();
        List<Integer> IRNRes = new ArrayList<>();
//        List<Double> COHRes = new ArrayList<>();
//        List<Double> COURes = new ArrayList<>();
        List<Double> CMRes = new ArrayList<>();
        List<Double> IFN = new ArrayList<>();
//        IFNAssessment.getInterfaces();
        SMAssessment.getCallGraph();
        SECbaseAssessment secobj = new SECbaseAssessment();
        IRNAssessment irnObj = new IRNAssessment();
//        CohesionAssessment cohOjb = new CohesionAssessment();
//        CouplingAssessment couObj = new CouplingAssessment();
        CMAssessment cmObj = new CMAssessment();
        IFN ifn = new IFN();
        int c = 0;
        int n = 0;
        for (Chromosome curDevide : chromosomeRes) {
            HashMap<Integer, List<Integer>> srvFilesMap = Common.splitChromosomeToServiceFileIdMap(curDevide);
            c += srvFilesMap.size();
            n++;
//            NEDRes.add(caculateAllServiceNED(srvFilesMap));
//            IFNRes.add(caculateAllServiceIFN(srvFilesMap));
            SMRes.add(caculateAllServiceSM(srvFilesMap));
            SEC_ccp.add(secobj.getAllServiceCCP(srvFilesMap));
            SEC_cce.add(secobj.getAllServiceCCE(srvFilesMap));
            SEC_csp.add(secobj.getAllServiceCSP(srvFilesMap));
            SEC_csi.add(secobj.getAllServiceCSI(srvFilesMap));
            IRNRes.add(irnObj.caculateAllServiceIRN(srvFilesMap));
//            COHRes.add(cohOjb.caculateAllServiceCoh(srvFilesMap));
//            COURes.add(couObj.caculateAllServiceCou(srvFilesMap));
            CMRes.add(cmObj.caculateAllServiceCM(srvFilesMap));
            IFN.add(ifn.caculateAllServiceIFN(srvFilesMap));
        }
//        double aveNED = 0;
//        double aveIFN = 0;
        double aveSM = 0;
        double aveSEC_ccp = 0;
        double aveSEC_cce = 0;
        double aveSEC_csp = 0;
        double aveSEC_csi = 0;
        double aveIRN = 0;
//        double aveCOH = 0;
//        double aveCOU = 0;
        double aveCM = 0;
        double aveIFN_rel = 0;
        for (int i = 0; i < SMRes.size(); i++) {
//            aveNED += NEDRes.get(i);
//            aveIFN += IFNRes.get(i);
            aveSM += SMRes.get(i);
            aveSEC_ccp += SEC_ccp.get(i);
            aveSEC_cce += SEC_cce.get(i);
            aveSEC_csp += SEC_csp.get(i);
            aveSEC_csi += SEC_csi.get(i);
            aveIRN += IRNRes.get(i);
//            aveCOH += COHRes.get(i);
//            aveCOU += COURes.get(i);
            aveCM += CMRes.get(i);
            aveIFN_rel += IFN.get(i);
        }
        System.out.println("split：" + (double) c / n + "sc");
        System.out.println("SEC-ccp" + aveSEC_ccp / SEC_ccp.size());
        System.out.println("SEC-cce：" + aveSEC_cce / SEC_cce.size());
        System.out.println("SEC-csp：" + aveSEC_csp / SEC_csp.size());
        System.out.println("SEC-csi：" + aveSEC_csi / SEC_csi.size());
        System.out.println("IFN：" + aveIFN_rel / IFN.size());
        System.out.println("SMQ：" + aveSM / SMRes.size());
        System.out.println("CMQ：" + aveCM / CMRes.size());
        System.out.println("IRN：" + aveIRN / IRNRes.size() + ", total：" + irnObj.allCalls);

    }
}
