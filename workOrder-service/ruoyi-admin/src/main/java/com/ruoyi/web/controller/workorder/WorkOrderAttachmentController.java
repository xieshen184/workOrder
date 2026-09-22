package com.ruoyi.web.controller.workorder;

import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.workorder.application.model.AttachmentDownload;
import com.ruoyi.workorder.application.service.WorkOrderAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/workorder/attachments")
public class WorkOrderAttachmentController extends BaseController
{
    @Autowired private WorkOrderAttachmentService attachmentService;
    @Autowired private WorkOrderActorFactory actorFactory;

    @PreAuthorize("@ss.hasPermi('workorder:attachment:upload')")
    @Log(title = "工单附件", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult upload(MultipartFile file,
            @RequestParam(value = "bizStage", required = false, defaultValue = "SUBMIT") String bizStage) throws Exception
    {
        if (file == null || file.isEmpty()) return error("请选择需要上传的附件");
        try (InputStream input = file.getInputStream())
        {
            return success(attachmentService.upload(file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), input, bizStage, actorFactory.current()));
        }
    }

    @PreAuthorize("@ss.hasPermi('workorder:attachment:download')")
    @GetMapping("/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws Exception
    {
        AttachmentDownload download = attachmentService.open(id, actorFactory.current());
        response.setContentType(download.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : download.getContentType());
        FileUtils.setAttachmentResponseHeader(response, download.getFileName());
        FileUtils.writeBytes(download.getPath().toString(), response.getOutputStream());
    }

    @PreAuthorize("@ss.hasPermi('workorder:attachment:upload')")
    @Log(title = "工单附件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) throws Exception
    {
        attachmentService.deleteUnbound(id, actorFactory.current());
        return success();
    }
}
