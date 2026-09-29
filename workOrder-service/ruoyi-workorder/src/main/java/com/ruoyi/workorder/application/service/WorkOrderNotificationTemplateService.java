package com.ruoyi.workorder.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderNotificationTemplatePreview;
import com.ruoyi.workorder.application.model.WorkOrderNotificationTemplateQuery;
import com.ruoyi.workorder.application.model.WorkOrderNotificationTemplateSaveRequest;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTemplate;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.springframework.stereotype.Service;

/**
 * 消息模板管理深模块。
 *
 * <p>只允许修改数据库中已经登记的事件和渠道；占位符校验与预览共用同一套目录，避免出现
 * “预览成功、实际发送残留变量”的双重规则。</p>
 */
@Service
public class WorkOrderNotificationTemplateService
{
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9]*)}");
    private static final Map<String, WorkOrderNotificationTemplatePreview.Variable> VARIABLES = variables();
    private static final Map<String, String> EVENT_NAMES = eventNames();

    private final WorkOrderNotificationMapper mapper;

    public WorkOrderNotificationTemplateService(WorkOrderNotificationMapper mapper)
    {
        this.mapper = mapper;
    }

    public List<WorkOrderNotificationTemplate> list(WorkOrderNotificationTemplateQuery query)
    {
        validateQuery(query);
        List<WorkOrderNotificationTemplate> rows = mapper.selectTemplates(query == null
                ? new WorkOrderNotificationTemplateQuery() : query);
        if (rows != null) for (WorkOrderNotificationTemplate row : rows) decorate(row);
        return rows;
    }

    public WorkOrderNotificationTemplate get(Long id)
    {
        if (id == null) throw bad("缺少模板编号");
        WorkOrderNotificationTemplate template = mapper.selectTemplateById(id);
        if (template == null) throw new ServiceException("消息模板不存在", HttpStatus.NOT_FOUND);
        decorate(template);
        return template;
    }

    public WorkOrderNotificationTemplatePreview preview(WorkOrderNotificationTemplateSaveRequest request)
    {
        validate(request, true);
        WorkOrderNotificationTemplatePreview result = new WorkOrderNotificationTemplatePreview();
        result.setTitle(render(request.getTitleTemplate()));
        result.setContent(render(request.getContentTemplate()));
        result.setVariables(new ArrayList<WorkOrderNotificationTemplatePreview.Variable>(VARIABLES.values()));
        return result;
    }

    public WorkOrderNotificationTemplate update(Long id, WorkOrderNotificationTemplateSaveRequest request, String username)
    {
        validate(request, false);
        WorkOrderNotificationTemplate current = get(id);
        // 保存请求不接受事件和渠道字段；即使客户端附带，也必须与既有模板完全一致。
        if (!blank(request.getEventCode()) && !current.getEventCode().equals(request.getEventCode())) throw bad("模板事件不允许修改");
        if (!blank(request.getChannel()) && !current.getChannel().equals(request.getChannel())) throw bad("模板渠道不允许修改");
        current.setTemplateName(request.getTemplateName().trim());
        current.setTitleTemplate(request.getTitleTemplate().trim());
        current.setContentTemplate(request.getContentTemplate().trim());
        current.setEnabledFlag(request.getEnabledFlag());
        current.setMaxRetry(request.getMaxRetry());
        current.setRemark(trimToNull(request.getRemark()));
        current.setUpdateBy(username == null ? "" : username);
        current.setUpdateTime(new Date());
        if (mapper.updateTemplate(current) != 1) throw new ServiceException("消息模板已被删除，请刷新后重试", HttpStatus.CONFLICT);
        decorate(current);
        return current;
    }

    private void validateQuery(WorkOrderNotificationTemplateQuery query)
    {
        if (query == null) return;
        if (!blank(query.getEnabledFlag()) && !"0".equals(query.getEnabledFlag()) && !"1".equals(query.getEnabledFlag())) throw bad("启用状态不正确");
        if (!blank(query.getChannel()) && !"IN_APP".equals(query.getChannel()) && !"SMS".equals(query.getChannel())) throw bad("消息渠道不正确");
    }

    private void validate(WorkOrderNotificationTemplateSaveRequest request, boolean preview)
    {
        if (request == null) throw bad("模板内容不能为空");
        required(request.getTemplateName(), "模板名称"); required(request.getTitleTemplate(), "通知标题");
        required(request.getContentTemplate(), "通知内容");
        length(request.getTemplateName(), 64, "模板名称"); length(request.getTitleTemplate(), 200, "通知标题");
        length(request.getContentTemplate(), 2000, "通知内容"); length(request.getRemark(), 500, "备注");
        if (!"0".equals(request.getEnabledFlag()) && !"1".equals(request.getEnabledFlag())) throw bad("启用状态不正确");
        if (request.getMaxRetry() == null || request.getMaxRetry() < 0 || request.getMaxRetry() > 10) throw bad("最大重试次数应在0到10之间");
        if (preview)
        {
            if (blank(request.getEventCode()) || !EVENT_NAMES.containsKey(request.getEventCode())) throw bad("模板事件未登记");
            if (!"IN_APP".equals(request.getChannel()) && !"SMS".equals(request.getChannel())) throw bad("模板渠道不正确");
        }
        validateVariables(request.getTitleTemplate()); validateVariables(request.getContentTemplate());
    }

    private void validateVariables(String template)
    {
        Matcher matcher = VARIABLE.matcher(template == null ? "" : template);
        while (matcher.find()) if (!VARIABLES.containsKey(matcher.group(1))) throw bad("未知模板变量：${" + matcher.group(1) + "}");
        // 捕获缺失右括号、空变量等不能被正则识别的残缺占位符。
        String cleaned = matcher.replaceAll("");
        if (cleaned.contains("${")) throw bad("模板变量格式不正确");
    }

    private String render(String template)
    {
        String value = template == null ? "" : template;
        for (WorkOrderNotificationTemplatePreview.Variable item : VARIABLES.values())
            value = value.replace("${" + item.getName() + "}", item.getSample());
        return value;
    }

    private void decorate(WorkOrderNotificationTemplate template)
    {
        if (template != null) template.setEventName(EVENT_NAMES.containsKey(template.getEventCode())
                ? EVENT_NAMES.get(template.getEventCode()) : template.getEventCode());
    }

    private static Map<String, WorkOrderNotificationTemplatePreview.Variable> variables()
    {
        Map<String, WorkOrderNotificationTemplatePreview.Variable> values = new LinkedHashMap<String, WorkOrderNotificationTemplatePreview.Variable>();
        add(values, "orderNo", "工单编号", "WO202610080001"); add(values, "title", "工单标题", "门诊空调不制冷");
        add(values, "operatorName", "操作人", "调度员"); add(values, "reason", "原因", "现场仍需继续处理");
        add(values, "deadline", "截止时间", "2026-10-08 18:00");
        return values;
    }
    private static void add(Map<String, WorkOrderNotificationTemplatePreview.Variable> values, String name, String label, String sample)
    {
        values.put(name, new WorkOrderNotificationTemplatePreview.Variable(name, label, sample));
    }
    private static Map<String, String> eventNames()
    {
        Map<String, String> values = new LinkedHashMap<String, String>();
        List<String[]> rows = Arrays.asList(
                new String[]{"SUBMIT","工单提交"}, new String[]{"ASSIGN","工单派单"}, new String[]{"ACCEPT","维修接单"},
                new String[]{"ARRIVE","维修到场"}, new String[]{"PROGRESS","进度更新"}, new String[]{"REASSIGN_OLD","改派原处理人"},
                new String[]{"REASSIGN_NEW","改派新处理人"}, new String[]{"FINISH","维修完工"}, new String[]{"RETURN","退回返工"},
                new String[]{"CONFIRM","完工确认"}, new String[]{"EVALUATE","服务评价"}, new String[]{"CANCEL","工单取消"},
                new String[]{"DELAY_REQUEST","延期申请"}, new String[]{"DELAY_APPROVED","延期通过"}, new String[]{"DELAY_REJECTED","延期驳回"},
                new String[]{"SLA_WARNING","SLA预警"}, new String[]{"SLA_OVERDUE","SLA超时"}, new String[]{"AUTO_CLOSE","自动关闭"});
        for (String[] row : rows) values.put(row[0], row[1]);
        return values;
    }

    private void required(String value, String label) { if (blank(value)) throw bad(label + "不能为空"); }
    private void length(String value, int max, String label) { if (value != null && value.length() > max) throw bad(label + "不能超过" + max + "个字符"); }
    private String trimToNull(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private ServiceException bad(String message) { return new ServiceException(message, HttpStatus.BAD_REQUEST); }
}
