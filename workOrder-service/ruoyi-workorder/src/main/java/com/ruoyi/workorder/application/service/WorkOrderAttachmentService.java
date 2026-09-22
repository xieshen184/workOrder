package com.ruoyi.workorder.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.AttachmentDownload;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.domain.model.WorkOrderAttachment;
import com.ruoyi.workorder.domain.model.WorkOrderAttachmentStage;
import com.ruoyi.workorder.mapper.WorkOrderAttachmentMapper;
import com.ruoyi.workorder.storage.StoredObject;
import com.ruoyi.workorder.storage.WorkOrderAttachmentStorage;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工单附件应用服务。
 * 上传阶段附件保持未绑定状态；创建工单时再校验上传人并绑定，下载始终复用工单可见性。
 */
@Service
public class WorkOrderAttachmentService
{
    private static final long MAX_SIZE = 20L * 1024L * 1024L;
    private static final Set<String> ALLOWED = new HashSet<String>(Arrays.asList(
            "bmp", "gif", "jpg", "jpeg", "png", "webp", "pdf", "doc", "docx", "xls", "xlsx", "txt"));

    @Autowired private WorkOrderAttachmentMapper attachmentMapper;
    @Autowired private WorkOrderAttachmentStorage storage;
    @Autowired private WorkOrderQueryService queryService;

    public WorkOrderAttachment upload(String originalFilename, String contentType, long declaredSize,
            InputStream input, String bizStage, WorkOrderActor actor) throws IOException
    {
        WorkOrderAttachmentStage stage = WorkOrderAttachmentStage.fromUploadValue(bizStage);
        if (stage.getPermission() != null && !actor.hasPermission(stage.getPermission()))
            throw new ServiceException("无权上传该业务阶段附件", HttpStatus.FORBIDDEN);
        String safeName = FilenameUtils.getName(originalFilename == null ? "" : originalFilename);
        if (safeName.length() == 0 || safeName.length() > 255)
            throw new ServiceException("附件文件名不正确", HttpStatus.BAD_REQUEST);
        if (declaredSize <= 0 || declaredSize > MAX_SIZE)
            throw new ServiceException("附件大小必须大于0且不超过20MB", HttpStatus.BAD_REQUEST);
        String extension = FilenameUtils.getExtension(safeName).toLowerCase();
        if (!ALLOWED.contains(extension))
            throw new ServiceException("不支持该附件格式", HttpStatus.UNSUPPORTED_TYPE);

        // 同时校验客户端声明大小和实际写入大小，避免伪造 Content-Length 绕过限制。
        StoredObject stored = storage.store(input, extension);
        if (stored.getSize() <= 0 || stored.getSize() > MAX_SIZE)
        {
            storage.delete(stored.getObjectKey());
            throw new ServiceException("附件大小不符合要求", HttpStatus.BAD_REQUEST);
        }

        WorkOrderAttachment attachment = new WorkOrderAttachment();
        attachment.setBizStage(stage.name());
        attachment.setFileName(safeName);
        attachment.setObjectKey(stored.getObjectKey());
        attachment.setStorageProvider("LOCAL");
        attachment.setFileHash(stored.getSha256());
        attachment.setFileType(contentType);
        attachment.setFileSize(stored.getSize());
        attachment.setUploaderId(actor.getUserId());
        attachment.setUploaderName(actor.getDisplayName());
        attachment.setCreateTime(new Date());
        try
        {
            attachmentMapper.insert(attachment);
            return attachment;
        }
        catch (RuntimeException error)
        {
            // 文件系统不参与数据库事务，元数据写入失败时需要显式补偿删除。
            storage.delete(stored.getObjectKey());
            throw error;
        }
    }

    public AttachmentDownload open(Long id, WorkOrderActor actor)
    {
        WorkOrderAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) throw new ServiceException("附件不存在", HttpStatus.NOT_FOUND);
        // 未绑定附件仅上传人可读；绑定后完全服从所属工单的行级可见性。
        if (attachment.getOrderId() == null)
        {
            if (!actor.getUserId().equals(attachment.getUploaderId()))
                throw new ServiceException("无权访问该附件", HttpStatus.FORBIDDEN);
        }
        else
        {
            queryService.assertVisible(attachment.getOrderId(), actor);
        }
        java.nio.file.Path path = storage.resolve(attachment.getObjectKey());
        if (!Files.isRegularFile(path)) throw new ServiceException("附件文件不存在", HttpStatus.NOT_FOUND);
        return new AttachmentDownload(path, attachment.getFileName(), attachment.getFileType());
    }

    public void deleteUnbound(Long id, WorkOrderActor actor) throws IOException
    {
        WorkOrderAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) return;
        if (attachment.getOrderId() != null || !actor.getUserId().equals(attachment.getUploaderId()))
            throw new ServiceException("只能删除本人尚未绑定的附件", HttpStatus.FORBIDDEN);
        if (attachmentMapper.markDeletedIfUnbound(id, actor.getUserId()) == 1)
            storage.delete(attachment.getObjectKey());
    }
}
