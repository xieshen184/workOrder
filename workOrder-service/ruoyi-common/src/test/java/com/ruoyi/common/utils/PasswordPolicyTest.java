package com.ruoyi.common.utils;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 个人中心密码策略契约测试。
 */
public class PasswordPolicyTest
{
    @Test
    public void acceptsPasswordContainingLettersAndDigitsWithinLengthRange()
    {
        assertTrue(PasswordPolicy.isValid("repair2026"));
        assertTrue(PasswordPolicy.isValid("Abc12345!"));
    }

    @Test
    public void rejectsMissingRequiredCharacterGroups()
    {
        assertFalse(PasswordPolicy.isValid("12345678"));
        assertFalse(PasswordPolicy.isValid("abcdefgh"));
        assertFalse(PasswordPolicy.isValid("维修密码2026"));
    }

    @Test
    public void rejectsInvalidLengthWhitespaceAndNull()
    {
        assertFalse(PasswordPolicy.isValid(null));
        assertFalse(PasswordPolicy.isValid("abc1234"));
        assertFalse(PasswordPolicy.isValid("abc 1234"));
        assertFalse(PasswordPolicy.isValid("abc123456789012345678"));
    }
}
