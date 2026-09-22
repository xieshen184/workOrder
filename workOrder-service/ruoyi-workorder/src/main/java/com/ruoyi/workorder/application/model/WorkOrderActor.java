package com.ruoyi.workorder.application.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Immutable snapshot of the authenticated operator used by the work-order module. */
public class WorkOrderActor
{
    private final Long userId;
    private final String username;
    private final String displayName;
    private final String phone;
    private final Long deptId;
    private final String deptName;
    private final Set<String> permissions;

    public WorkOrderActor(Long userId, String username, String displayName, String phone,
            Long deptId, String deptName, Set<String> permissions)
    {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.phone = phone;
        this.deptId = deptId;
        this.deptName = deptName;
        this.permissions = permissions == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<String>(permissions));
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public Long getDeptId() { return deptId; }
    public String getDeptName() { return deptName; }
    public Set<String> getPermissions() { return permissions; }

    public boolean hasPermission(String permission)
    {
        return permissions.contains("*:*:*") || permissions.contains(permission);
    }

    public String primaryRole()
    {
        if (hasPermission("workorder:order:list")) return "DISPATCHER";
        if (hasPermission("workorder:order:assigned:list")) return "ENGINEER";
        return "REPORTER";
    }
}
