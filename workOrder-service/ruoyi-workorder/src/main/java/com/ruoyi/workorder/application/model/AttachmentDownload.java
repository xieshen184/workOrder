package com.ruoyi.workorder.application.model;

import java.nio.file.Path;

public class AttachmentDownload
{
    private final Path path;
    private final String fileName;
    private final String contentType;

    public AttachmentDownload(Path path, String fileName, String contentType)
    {
        this.path = path;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public Path getPath() { return path; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
}
