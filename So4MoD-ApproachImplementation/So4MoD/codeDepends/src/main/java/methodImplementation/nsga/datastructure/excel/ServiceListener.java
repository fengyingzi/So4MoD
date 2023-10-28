package com.nju.bysj.softwaremodularisation.nsga.datastructure.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

public class ServiceListener extends AnalysisEventListener<ServiceData> {
    public List<ServiceData> serviceList = new ArrayList<>();

    @Override
    public void invoke(ServiceData serviceData, AnalysisContext analysisContext) {
        serviceList.add(serviceData);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        System.out.println("Excel get with" + serviceList.size() + "microservices");
    }
}
