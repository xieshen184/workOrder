package com.ruoyi.workorder.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Generates a stable public order number from the create idempotency identity. */
public class WorkOrderNumberGenerator
{
    public String generate(Long userId, String idempotencyKey)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((userId + ":" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder("WO");
            for (int i = 0; i < 15; i++) value.append(String.format("%02X", hash[i] & 0xff));
            return value.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
