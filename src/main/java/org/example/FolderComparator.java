package org.example;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 负责比较两个文件夹内的所有JAR文件。
 */
public class FolderComparator {

    private final File oldDir;
    private final File newDir;
    private final JarComparator jarComparator;

    public FolderComparator(File oldDir, File newDir) {
        this.oldDir = oldDir;
        this.newDir = newDir;
        this.jarComparator = new JarComparator();
    }

    /**
     * 执行文件夹比较，并返回所有差异结果。
     * @return 一个包含所有差异的 ComparisonResult 列表。
     */
    public List<ComparisonResult> compare() {
        System.out.println("正在扫描JAR文件...");

        IOFileFilter jarFilter = new SuffixFileFilter(".jar");
        Collection<File> oldJarFiles = FileUtils.listFiles(oldDir, jarFilter, TrueFileFilter.INSTANCE);
        Collection<File> newJarFiles = FileUtils.listFiles(newDir, jarFilter, TrueFileFilter.INSTANCE);

        Map<String, File> oldJarMap = oldJarFiles.stream()
                .collect(Collectors.toMap(File::getName, Function.identity()));
        Map<String, File> newJarMap = newJarFiles.stream()
                .collect(Collectors.toMap(File::getName, Function.identity()));

        List<ComparisonResult> results = new ArrayList<>();

        for (Map.Entry<String, File> oldEntry : oldJarMap.entrySet()) {
            String jarName = oldEntry.getKey();
            File oldJar = oldEntry.getValue();
            File newJar = newJarMap.get(jarName);

            if (newJar != null) {
                System.out.println("正在比较 " + jarName + "...");
                ComparisonResult result = jarComparator.compare(oldJar, newJar);
                if (result.getStatus() != ComparisonResult.Status.UNCHANGED) {
                    results.add(result);
                }
            } else {
                System.out.println(jarName + " [状态: 已删除]");
                results.add(new ComparisonResult(jarName, oldJar, null, ComparisonResult.Status.DELETED));
            }
        }

        for (Map.Entry<String, File> newEntry : newJarMap.entrySet()) {
            String jarName = newEntry.getKey();
            if (!oldJarMap.containsKey(jarName)) {
                System.out.println(jarName + " [状态: 新增]");
                results.add(new ComparisonResult(jarName, null, newEntry.getValue(), ComparisonResult.Status.ADDED));
            }
        }

        return results;
    }
}