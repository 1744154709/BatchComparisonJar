package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 封装一对JAR文件比较结果的数据模型。
 */
public class ComparisonResult {

    /**
     * 定义JAR包的比较状态
     */
    public enum Status {
        /** 仅在旧文件夹中存在 */
        DELETED,
        /** 仅在新文件夹中存在 */
        ADDED,
        /** 两个文件夹中都存在，但内容有差异 */
        MODIFIED,
        /** 两个文件夹中都存在，且内容完全相同 */
        UNCHANGED
    }

    private final String jarName;
    private final File oldJarFile;
    private final File newJarFile;
    private final Status status;
    private final List<DiffDetail> diffDetails;

    /**
     * 构造函数
     * @param jarName    被比较的JAR包的文件名
     * @param oldJarFile 旧文件夹中的JAR文件对象（可能为null）
     * @param newJarFile 新文件夹中的JAR文件对象（可能为null）
     * @param status     比较状态
     */
    public ComparisonResult(String jarName, File oldJarFile, File newJarFile, Status status) {
        this.jarName = jarName;
        this.oldJarFile = oldJarFile;
        this.newJarFile = newJarFile;
        this.status = status;
        this.diffDetails = new ArrayList<>();
    }

    /**
     * @param simpleDifference 一条简单的差异描述字符串
     */
    public void addDifference(String simpleDifference) {
        this.diffDetails.add(new DiffDetail(
                "General Info", // 类名可以设为一个通用标签
                DiffDetail.DiffType.LOGICAL_CHANGE,
                Collections.singletonList(simpleDifference),
                null,
                null
        ));
    }

    /**
     * 用于添加包含完整源码上下文的详细差异对象。
     * @param detail 一个完整的DiffDetail对象
     */
    public void addDiffDetail(DiffDetail detail) {
        this.diffDetails.add(detail);
    }


    public String getJarName() {
        return jarName;
    }

    public File getOldJarFile() {
        return oldJarFile;
    }

    public File getNewJarFile() {
        return newJarFile;
    }

    public Status getStatus() {
        return status;
    }

    public List<DiffDetail> getDiffDetails() {
        return diffDetails;
    }

    @Override
    public String toString() {
        return "ComparisonResult{" +
                "jarName='" + jarName + '\'' +
                ", status=" + status +
                ", differences=" + diffDetails.size() +
                '}';
    }

    /**
     * 判断此比较结果中是否包含任何类型的差异。
     * @return 如果差异列表不为空，则返回true
     */
    public boolean hasDifferences() {
        return !diffDetails.isEmpty();
    }
}