package com.ruoyi.workorder.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.stereotype.Component;

/** 本地私有附件存储；目录不挂载到若依公开的 /profile 静态资源路径。 */
@Component
public class LocalWorkOrderAttachmentStorage implements WorkOrderAttachmentStorage
{
    private static final int BUFFER_SIZE = 8192;

    private Path root()
    {
        return Paths.get(RuoYiConfig.getProfile(), "workorder-private").toAbsolutePath().normalize();
    }

    @Override
    public StoredObject store(InputStream input, String extension) throws IOException
    {
        LocalDate now = LocalDate.now();
        String suffix = extension == null || extension.length() == 0 ? "" : "." + extension.toLowerCase();
        String objectKey = now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/"
                + String.format("%02d", now.getDayOfMonth()) + "/" + IdUtils.fastSimpleUUID() + suffix;
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());

        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }

        long size = 0L;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (OutputStream output = Files.newOutputStream(target))
        {
            int read;
            while ((read = input.read(buffer)) >= 0)
            {
                if (read == 0) continue;
                output.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                size += read;
            }
        }
        return new StoredObject(objectKey, toHex(digest.digest()), size);
    }

    @Override
    public Path resolve(String objectKey)
    {
        Path root = root();
        Path resolved = root.resolve(objectKey).normalize();
        // 归一化后再次检查根目录，阻止 ../ 等路径穿越 objectKey。
        if (!resolved.startsWith(root))
        {
            throw new IllegalArgumentException("Illegal attachment object key");
        }
        return resolved;
    }

    @Override
    public void delete(String objectKey) throws IOException
    {
        Files.deleteIfExists(resolve(objectKey));
    }

    private String toHex(byte[] bytes)
    {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) value.append(String.format("%02x", item & 0xff));
        return value.toString();
    }
}
