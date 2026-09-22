package com.ruoyi.web.controller.workorder;

import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import org.springframework.stereotype.Component;

@Component
public class WorkOrderActorFactory
{
    public WorkOrderActor current()
    {
        LoginUser login = SecurityUtils.getLoginUser();
        SysUser user = login.getUser();
        SysDept dept = user.getDept();
        String displayName = StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
        return new WorkOrderActor(login.getUserId(), user.getUserName(), displayName, user.getPhonenumber(),
                login.getDeptId(), dept == null ? null : dept.getDeptName(), login.getPermissions());
    }
}
