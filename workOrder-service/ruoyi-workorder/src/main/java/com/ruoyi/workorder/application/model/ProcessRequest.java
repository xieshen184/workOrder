package com.ruoyi.workorder.application.model;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ProcessRequest
{
    @Size(max = 2000, message = "处理内容不能超过2000个字符")
    private String content;
    @Size(max = 10, message = "单次最多绑定10个附件")
    private List<Long> attachmentIds = new ArrayList<Long>();
    @NotNull(message = "缺少工单版本")
    @Min(value = 0, message = "工单版本不正确")
    private Integer version;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<Long> getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(List<Long> attachmentIds) { this.attachmentIds = attachmentIds; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
