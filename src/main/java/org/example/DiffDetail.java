package org.example;

import java.util.List;

/**
 * 封装了单个类文件差异的详细信息，并能够区分差异类型。
 */
public class DiffDetail {

    public enum DiffType {
        /** 真正的代码逻辑变更 */
        LOGICAL_CHANGE,
        /** 非逻辑性、可忽略的差异，如编译器生成的access$方法 */
        NON_LOGICAL_CHANGE,
        /** 反编译失败或其他处理错误 */
        ERROR
    }

    private final String className;
    private final DiffType type;
    private final List<String> diffContent;
    private final String oldSource;

    private final String newSource;

    public String getOldSource() {
        return oldSource;
    }

    public String getNewSource() {
        return newSource;
    }

    public DiffDetail(String className, DiffType type, List<String> diffContent, String oldSource, String newSource) {
        this.className = className;
        this.type = type;
        this.diffContent = diffContent;
        this.oldSource = oldSource;
        this.newSource = newSource;
    }

    public String getClassName() {
        return className;
    }

    public DiffType getType() {
        return type;
    }

    public List<String> getDiffContent() {
        return diffContent;
    }
}