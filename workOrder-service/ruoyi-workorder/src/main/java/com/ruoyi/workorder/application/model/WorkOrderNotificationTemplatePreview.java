package com.ruoyi.workorder.application.model;

import java.util.ArrayList;
import java.util.List;

/** 保存前预览结果，同时返回客户端可展示的变量目录。 */
public class WorkOrderNotificationTemplatePreview
{
    private String title;
    private String content;
    private List<Variable> variables = new ArrayList<Variable>();

    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public List<Variable> getVariables() { return variables; }
    public void setVariables(List<Variable> value) { variables = value; }

    public static class Variable
    {
        private String name;
        private String label;
        private String sample;
        public Variable() { }
        public Variable(String name, String label, String sample)
        {
            this.name = name; this.label = label; this.sample = sample;
        }
        public String getName() { return name; }
        public void setName(String value) { name = value; }
        public String getLabel() { return label; }
        public void setLabel(String value) { label = value; }
        public String getSample() { return sample; }
        public void setSample(String value) { sample = value; }
    }
}
