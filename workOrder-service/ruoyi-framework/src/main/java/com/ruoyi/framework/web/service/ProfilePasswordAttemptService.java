package com.ruoyi.framework.web.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;

/**
 * 个人中心旧密码校验的失败次数控制。
 *
 * <p>登录失败和修改密码失败使用不同缓存键；此处只向控制器暴露“是否允许、记录失败、
 * 清除记录”三个动作，控制器无需了解 Redis 键和过期策略。</p>
 */
@Component
public class ProfilePasswordAttemptService
{
    @Autowired
    private RedisCache redisCache;

    @Value(value = "${user.password.maxRetryCount:5}")
    private int maxRetryCount;

    @Value(value = "${user.password.lockTime:10}")
    private int lockTimeMinutes;

    public boolean isAllowed(String username)
    {
        Number failures = redisCache.getCacheObject(getCacheKey(username));
        return failures == null || failures.longValue() < maxRetryCount;
    }

    /**
     * 记录一次旧密码错误，并在首次失败时建立锁定窗口。
     *
     * @return 当前累计失败次数
     */
    public long recordFailure(String username)
    {
        String cacheKey = getCacheKey(username);
        long failures = redisCache.incrementCacheValue(cacheKey);
        if (failures == 1L)
        {
            redisCache.expire(cacheKey, lockTimeMinutes, TimeUnit.MINUTES);
        }
        return failures;
    }

    public void clear(String username)
    {
        redisCache.deleteObject(getCacheKey(username));
    }

    public int getMaxRetryCount()
    {
        return maxRetryCount;
    }

    public long getRetryAfterSeconds(String username)
    {
        long seconds = redisCache.getExpire(getCacheKey(username));
        return Math.max(seconds, 0L);
    }

    private String getCacheKey(String username)
    {
        return CacheConstants.PROFILE_PWD_ERR_CNT_KEY + username;
    }
}
