package com.ruoyi.workorder.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface WorkOrderAttachmentStorage
{
    StoredObject store(InputStream input, String extension) throws IOException;
    Path resolve(String objectKey);
    void delete(String objectKey) throws IOException;
}
