package org.example;

import org.apache.commons.io.IOUtils;


import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 传递给DiffDetail对象，为最终的可视化渲染提供必要的数据。
 */
public class JarComparator {

    private final JarDecompiler decompiler = new JarDecompiler();
    private final SourceCodeDiff diff = new SourceCodeDiff();

    public ComparisonResult compare(File oldJar, File newJar) {
        try {
            if (Arrays.equals(calculateHash(oldJar), calculateHash(newJar))) {
                System.out.println(oldJar.getName() + " [状态: 未变更 (文件哈希值相同)]");
                return new ComparisonResult(oldJar.getName(), oldJar, newJar, ComparisonResult.Status.UNCHANGED);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            String errorMessage = getStackTraceAsString(e);
            System.err.println("警告: 无法计算文件哈希值 " + oldJar.getName() + "。将继续进行深度比较。\n错误详情:\n" + errorMessage);
        }

        System.out.println(oldJar.getName() + " [状态: 可能有变更 (正在进行深度扫描...)]");
        ComparisonResult result = new ComparisonResult(oldJar.getName(), oldJar, newJar, ComparisonResult.Status.MODIFIED);

        try (ZipFile oldZip = new ZipFile(oldJar); ZipFile newZip = new ZipFile(newJar)) {
            Map<String, ZipEntry> oldEntries = getEntriesMap(oldZip);
            Map<String, ZipEntry> newEntries = getEntriesMap(newZip);

            for (Map.Entry<String, ZipEntry> oldEntry : oldEntries.entrySet()) {
                String entryName = oldEntry.getKey();
                if (!entryName.endsWith(".class")) continue;

                ZipEntry newEntry = newEntries.get(entryName);
                if (newEntry == null) {
                    result.addDifference("  - 删除的类: " + formatClassName(entryName));
                } else {
                    byte[] oldClassBytes = IOUtils.toByteArray(oldZip.getInputStream(oldEntry.getValue()));
                    byte[] newClassBytes = IOUtils.toByteArray(newZip.getInputStream(newEntry));

                    if (!Arrays.equals(calculateHash(oldClassBytes), calculateHash(newClassBytes))) {
                        // 这个信息将在ReportGenerator中根据DiffDetail的内容动态生成
                        decompileAndDiff(oldClassBytes, newClassBytes, entryName, result);
                    }
                }
            }

            for (String newEntryName : newEntries.keySet()) {
                if (newEntryName.endsWith(".class") && !oldEntries.containsKey(newEntryName)) {
                    result.addDifference("  + 新增的类: " + formatClassName(newEntryName));
                }
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            result.addDifference("错误：在处理JAR包 " + newJar.getName() + " 时发生顶层异常。");
            result.addDifference(getStackTraceAsString(e));
        }

        // 判断是否有差异的逻辑，基于DiffDetail列表
        if (!result.hasDifferences()) {
            System.out.println(oldJar.getName() + " [状态: 未变更 (类文件无差异)]");
            return new ComparisonResult(oldJar.getName(), oldJar, newJar, ComparisonResult.Status.UNCHANGED);
        }

        return result;
    }

    /**
     * 将完整的上下文传递给DiffDetail。
     */
    private void decompileAndDiff(byte[] oldClassBytes, byte[] newClassBytes, String entryName, ComparisonResult result) {
        String oldSource = null;
        String newSource = null;
        try {
            System.out.println("    -> 正在反编译和比对 " + formatClassName(entryName) + "...");

            oldSource = decompiler.decompile(oldClassBytes, entryName);
            newSource = decompiler.decompile(newClassBytes, entryName);

            // 调用SourceCodeDiff的compare方法，负责判断差异类型和返回一个临时的内容
            DiffDetail diffDetail = diff.compare(oldSource, newSource, formatClassName(entryName));

            // 重新构建一个包含了所有信息的完整对象,让SourceCodeDiff的职责更单一（只负责计算和判断），而数据封装的职责在这一层完成。
            result.addDiffDetail(new DiffDetail(
                    diffDetail.getClassName(),
                    diffDetail.getType(),
                    diffDetail.getDiffContent(),
                    oldSource,
                    newSource
            ));

        } catch (DecompilationException e) {
            String stackTrace = getStackTraceAsString(e);
            String indentedStackTrace = "      " + stackTrace.replaceAll("\n", "\n      ");
            result.addDiffDetail(new DiffDetail(
                    formatClassName(entryName),
                    DiffDetail.DiffType.ERROR,
                    List.of("      [反编译失败] " + formatClassName(entryName), indentedStackTrace),
                    (oldSource != null) ? oldSource : "/* Old source unavailable */",
                    (newSource != null) ? newSource : "/* New source unavailable */"
            ));
        }
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private Map<String, ZipEntry> getEntriesMap(ZipFile zipFile) {
        Map<String, ZipEntry> entries = new HashMap<>();
        Enumeration<? extends ZipEntry> enu = zipFile.entries();
        while (enu.hasMoreElements()) {
            ZipEntry entry = enu.nextElement();
            if (!entry.isDirectory()) {
                entries.put(entry.getName(), entry);
            }
        }
        return entries;
    }

    private byte[] calculateHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        return digest.digest();
    }

    private byte[] calculateHash(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    private String formatClassName(String entryName) {
        return entryName.replace(".class", "").replace('/', '.');
    }
}