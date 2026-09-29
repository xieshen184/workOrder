package com.ruoyi.web.controller.system;

import java.util.Map;
import javax.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.PasswordPolicy;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.common.utils.bean.BeanValidators;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.framework.web.service.ProfilePasswordAttemptService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ProfilePasswordAttemptService profilePasswordAttemptService;

    @Autowired
    private Validator validator;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(StringUtils.trim(user.getNickName()));
        currentUser.setEmail(StringUtils.trim(user.getEmail()));
        currentUser.setPhonenumber(StringUtils.trim(user.getPhonenumber()));
        currentUser.setSex(user.getSex());
        if (StringUtils.isEmpty(currentUser.getNickName()))
        {
            return error("用户昵称不能为空");
        }
        if (StringUtils.isEmpty(currentUser.getPhonenumber())
                || !currentUser.getPhonenumber().matches("^1[3-9]\\d{9}$"))
        {
            return error("请输入正确的11位手机号码");
        }
        if (!("0".equals(currentUser.getSex()) || "1".equals(currentUser.getSex())
                || "2".equals(currentUser.getSex())))
        {
            return error("请选择正确的性别");
        }
        // 对合并后的完整用户执行实体约束，既校验邮箱/XSS，又不会因请求未携带只读账号而误报。
        BeanValidators.validateWithException(validator, currentUser);
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        if (params == null)
        {
            return error("密码参数不能为空");
        }
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = getLoginUser();
        String userName = loginUser.getUsername();
        String password = loginUser.getPassword();

        // 前端校验只能改善体验；服务端必须再次执行完整策略，防止接口被绕过。
        if (StringUtils.isEmpty(oldPassword))
        {
            return error("旧密码不能为空");
        }
        if (!PasswordPolicy.isValid(newPassword))
        {
            return error("新密码必须为8至20位，并且同时包含字母和数字");
        }
        if (!profilePasswordAttemptService.isAllowed(userName))
        {
            long retryAfterSeconds = profilePasswordAttemptService.getRetryAfterSeconds(userName);
            long retryAfterMinutes = Math.max(1L, (retryAfterSeconds + 59L) / 60L);
            return error("旧密码连续错误次数过多，请在" + retryAfterMinutes + "分钟后重试")
                    .put("retryAfterSeconds", retryAfterSeconds);
        }
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            long failures = profilePasswordAttemptService.recordFailure(userName);
            long remaining = Math.max(profilePasswordAttemptService.getMaxRetryCount() - failures, 0L);
            long retryAfterSeconds = remaining == 0L
                    ? profilePasswordAttemptService.getRetryAfterSeconds(userName) : 0L;
            String message = remaining > 0L
                    ? "修改密码失败，旧密码错误，还可尝试" + remaining + "次"
                    : "旧密码连续错误次数过多，请稍后重试";
            return error(message)
                    .put("remainingAttempts", remaining)
                    .put("retryAfterSeconds", retryAfterSeconds);
        }
        // 旧密码通过后立即清除失败记录；后续若只是新密码不合规，不应消耗旧密码次数。
        profilePasswordAttemptService.clear(userName);
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userName, newPassword) > 0)
        {
            // 修改成功后让当前令牌立即失效，客户端必须使用新密码重新登录。
            tokenService.delLoginUser(loginUser.getToken());
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION);
            if (userService.updateUserAvatar(loginUser.getUsername(), avatar))
            {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}
