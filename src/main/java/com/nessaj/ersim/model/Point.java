package com.nessaj.ersim.model;

import javax.persistence.*;

@Entity
@Table(name = "points")
public class Point {
    @Id
    private String id;
    private String ptId;
    private String ptName;
    private String deviceLocalNum;
    private String signalType;
    private Integer modelType;
    private String linkedProp;

    public Point() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPtId() {
        return ptId;
    }

    public void setPtId(String ptId) {
        this.ptId = ptId;
    }

    public String getPtName() { return ptName; }
    public void setPtName(String ptName) { this.ptName = ptName; }
    public String getDeviceLocalNum() { return deviceLocalNum; }
    public void setDeviceLocalNum(String deviceLocalNum) { this.deviceLocalNum = deviceLocalNum; }
    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }
    public Integer getModelType() { return modelType; }
    public void setModelType(Integer modelType) { this.modelType = modelType; }
    public String getLinkedProp() { return linkedProp; }
    public void setLinkedProp(String linkedProp) { this.linkedProp = linkedProp; }
}