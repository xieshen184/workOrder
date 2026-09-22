package com.ruoyi.workorder.domain.model;

import java.util.Date;

public class WorkOrderAttachment
{
    private Long id;
    private Long orderId;
    private String bizStage;
    private Long bizRefId;
    private String fileName;
    private String objectKey;
    private String storageProvider;
    private String fileHash;
    private String fileType;
    private Long fileSize;
    private Long uploaderId;
    private String uploaderName;
    private Date createTime;
    private String downloadUrl;

    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long v) { orderId=v; }
    public String getBizStage() { return bizStage; } public void setBizStage(String v) { bizStage=v; }
    public Long getBizRefId() { return bizRefId; } public void setBizRefId(Long v) { bizRefId=v; }
    public String getFileName() { return fileName; } public void setFileName(String v) { fileName=v; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false)
    public String getObjectKey() { return objectKey; } public void setObjectKey(String v) { objectKey=v; }
    public String getStorageProvider() { return storageProvider; } public void setStorageProvider(String v) { storageProvider=v; }
    public String getFileHash() { return fileHash; } public void setFileHash(String v) { fileHash=v; }
    public String getFileType() { return fileType; } public void setFileType(String v) { fileType=v; }
    public Long getFileSize() { return fileSize; } public void setFileSize(Long v) { fileSize=v; }
    public Long getUploaderId() { return uploaderId; } public void setUploaderId(Long v) { uploaderId=v; }
    public String getUploaderName() { return uploaderName; } public void setUploaderName(String v) { uploaderName=v; }
    public Date getCreateTime() { return createTime; } public void setCreateTime(Date v) { createTime=v; }
    public String getDownloadUrl() { return downloadUrl; } public void setDownloadUrl(String v) { downloadUrl=v; }
}
