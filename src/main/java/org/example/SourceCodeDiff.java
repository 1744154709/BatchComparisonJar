package org.example;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.example.DiffDetail;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceCodeDiff {

    // 过滤规则保持不变
    private static final Pattern ACCESS_METHOD_PATTERN = Pattern.compile("access\\$\\d+");
    private static final String NORMALIZED_ACCESS_METHOD = "access$normalized";
    private static final Pattern SYNTHETIC_COMMENT_PATTERN = Pattern.compile("^\\s*//\\s*\\$FF: synthetic method\\s*$\\n?", Pattern.MULTILINE);

    private final DiffMatchPatch dmp = new DiffMatchPatch();

    /**
     * 计算两个源码字符串的差异。
     */
    public LinkedList<DiffMatchPatch.Diff> diff(String oldSource, String newSource) {
        // 输入为null，也不会导致后续操作失败
        String safeOldSource = (oldSource != null) ? oldSource : "";
        String safeNewSource = (newSource != null) ? newSource : "";

        LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(safeOldSource, safeNewSource);
        dmp.diffCleanupSemantic(diffs);
        return diffs;
    }

    /**
     * 应用预定义的过滤规则来“净化”源代码字符串。
     */
    public String applyAllFilters(String source) {
        if (source == null) {
            return "";
        }

        String result = SYNTHETIC_COMMENT_PATTERN.matcher(source).replaceAll("");
        result = safeReplaceAll(result, ACCESS_METHOD_PATTERN, NORMALIZED_ACCESS_METHOD);
        return result;
    }
    /**
     * 比较两个源代码字符串，并返回一个DiffDetail对象，包含差异类型和详细信息。
     *
     * @param oldSource 旧版本的源代码
     * @param newSource 新版本的源代码
     * @param className 类名，用于报告中标识
     * @return DiffDetail对象，包含差异类型和详细信息
     */
    public DiffDetail compare(String oldSource, String newSource, String className) {
        String normalizedOldSource = applyAllFilters(oldSource);
        String normalizedNewSource = applyAllFilters(newSource);

        LinkedList<DiffMatchPatch.Diff> diffs = diff(normalizedOldSource, normalizedNewSource);

        if (diffs.size() == 1 && diffs.getFirst().operation == DiffMatchPatch.Operation.EQUAL) {
            return new DiffDetail(className, DiffDetail.DiffType.NON_LOGICAL_CHANGE, List.of("非逻辑性差异，细节将在报告中渲染。"), oldSource, newSource);
        } else {
            return new DiffDetail(className, DiffDetail.DiffType.LOGICAL_CHANGE, List.of("逻辑性差异，细节将在报告中渲染。"), oldSource, newSource);
        }
    }


    private String safeReplaceAll(String input, Pattern pattern, String replacement) {
        String safeReplacement = Matcher.quoteReplacement(replacement);
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, safeReplacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}