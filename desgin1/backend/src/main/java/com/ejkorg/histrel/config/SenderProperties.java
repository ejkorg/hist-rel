package com.ejkorg.histrel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sender")
public class SenderProperties {
    private int senderId;
    private int numberOfDataToSend;
    private int countLimitTrigger;
    private String listFile;
    private String startDate;
    private String endDate;
    private String testerType;
    private String dataType;
    // getters + setters
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public int getNumberOfDataToSend() { return numberOfDataToSend; }
    public void setNumberOfDataToSend(int numberOfDataToSend) { this.numberOfDataToSend = numberOfDataToSend; }
    public int getCountLimitTrigger() { return countLimitTrigger; }
    public void setCountLimitTrigger(int countLimitTrigger) { this.countLimitTrigger = countLimitTrigger; }
    public String getListFile() { return listFile; }
    public void setListFile(String listFile) { this.listFile = listFile; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getTesterType() { return testerType; }
    public void setTesterType(String testerType) { this.testerType = testerType; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
}