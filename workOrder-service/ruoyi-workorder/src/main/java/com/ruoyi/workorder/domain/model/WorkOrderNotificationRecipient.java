package com.ruoyi.workorder.domain.model;

/** 服务端用户目录投影；手机号只用于将来的短信适配器，不写入日志或页面响应。 */
public class WorkOrderNotificationRecipient
{
    private Long userId;
    private String userName;
    private String nickName;
    private String phoneNumber;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String displayName() { return nickName == null || nickName.trim().isEmpty() ? userName : nickName; }
}
