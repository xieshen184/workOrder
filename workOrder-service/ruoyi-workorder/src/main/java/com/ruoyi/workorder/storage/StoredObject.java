package com.ruoyi.workorder.storage;

public class StoredObject
{
    private final String objectKey;
    private final String sha256;
    private final long size;

    public StoredObject(String objectKey, String sha256, long size)
    {
        this.objectKey = objectKey;
        this.sha256 = sha256;
        this.size = size;
    }

    public String getObjectKey() { return objectKey; }
    public String getSha256() { return sha256; }
    public long getSize() { return size; }
}
