package com.ruoyi.common.utils;

/**
 * 个人密码强度规则。
 *
 * <p>该类只判断密码本身，不读取用户、缓存或配置，便于移动端与服务端对齐规则，
 * 也避免把强度判断散落在控制器中。</p>
 */
public final class PasswordPolicy
{
    public static final int MIN_LENGTH = 8;

    public static final int MAX_LENGTH = 20;

    private PasswordPolicy()
    {
    }

    /**
     * 新密码必须为 8 至 20 位，并且同时包含字母和数字。
     * 允许使用其它可见字符增强强度，但不能用空白字符占位。
     *
     * @param password 待校验的明文密码
     * @return 是否满足个人中心的新密码规则
     */
    public static boolean isValid(String password)
    {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH)
        {
            return false;
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int index = 0; index < password.length(); index++)
        {
            char current = password.charAt(index);
            if (Character.isWhitespace(current))
            {
                return false;
            }
            // 与小程序端保持一致：此处的“字母”限定为常用 ASCII 英文字母。
            hasLetter = hasLetter || (current >= 'A' && current <= 'Z') || (current >= 'a' && current <= 'z');
            hasDigit = hasDigit || Character.isDigit(current);
        }
        return hasLetter && hasDigit;
    }
}
