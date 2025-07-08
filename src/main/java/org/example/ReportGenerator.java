package org.example;

import org.apache.commons.io.IOUtils;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.example.ComparisonResult;
import org.example.DiffDetail;
import org.example.SourceCodeDiff;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生成带有行内高亮、交互友好的HTML报告。
 */
public class ReportGenerator {

    private final SourceCodeDiff diffEngine = new SourceCodeDiff();
    private final DiffMatchPatch dmp = new DiffMatchPatch();


    public Map<String, String> generate(List<ComparisonResult> results, String oldDirName, String newDirName) throws IOException {
        Map<String, String> reports = new HashMap<>();
        String template = loadTemplate("difftemplate.html");

        // 生成主报告
        String mainReportHtml = fillTemplate(template, "详细差异报告 (主报告)", results, oldDirName, newDirName, true, List.of(DiffDetail.DiffType.LOGICAL_CHANGE, DiffDetail.DiffType.ERROR));

        // 生成非逻辑性差异报告
        String nonLogicalReportHtml = fillTemplate(template, "非逻辑性差异报告 (编译器生成)", results, oldDirName, newDirName, false, List.of(DiffDetail.DiffType.NON_LOGICAL_CHANGE));

        reports.put("main_report", mainReportHtml);
        reports.put("non_logical_report", nonLogicalReportHtml);
        return reports;
    }

    private String generateDetailedHtmlContent(List<ComparisonResult> allResults, List<DiffDetail.DiffType> typesToInclude) {
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;

        List<ComparisonResult> relevantResults = allResults.stream()
                .filter(r -> r.getDiffDetails().stream().anyMatch(d -> typesToInclude.contains(d.getType())))
                .collect(Collectors.toList());

        for (ComparisonResult result : relevantResults) {
            List<DiffDetail> filteredDetails = result.getDiffDetails().stream()
                    .filter(d -> typesToInclude.contains(d.getType()))
                    .collect(Collectors.toList());

            if (!filteredDetails.isEmpty()) {
                hasContent = true;
                sb.append("<div class='jar-details'>");
                sb.append(String.format("<div class='jar-header'>JAR: %s | 状态: %s</div>", escapeHtml(result.getJarName()), result.getStatus()));

                for (DiffDetail detail : filteredDetails) {
                    sb.append(String.format("<h3 class='class-header'>类: %s (%s)</h3>", escapeHtml(detail.getClassName()), detail.getType()));
                    if (detail.getType() == DiffDetail.DiffType.ERROR) {
                        sb.append("<pre class='diff-delete'>");
                        detail.getDiffContent().forEach(line -> sb.append(escapeHtml(line)).append("\n"));
                        sb.append("</pre>");
                    } else {
                        sb.append(generateVisualInlineDiff(detail.getOldSource(), detail.getNewSource()));
                    }
                }
                sb.append("</div>");
            }
        }
        if (!hasContent) return "<p>未发现此类别的差异。</p>";
        return sb.toString();
    }

    private String generateVisualInlineDiff(String oldText, String newText) {
        String safeOldText = (oldText != null) ? oldText : "";
        String safeNewText = (newText != null) ? newText : "";
        LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(safeOldText, safeNewText);
        dmp.diffCleanupSemantic(diffs);

        StringBuilder html = new StringBuilder("<pre>");
        int oldLineNum = 1;
        int newLineNum = 1;

        for (int i = 0; i < diffs.size(); i++) {
            DiffMatchPatch.Diff currentDiff = diffs.get(i);

            if (currentDiff.operation == DiffMatchPatch.Operation.DELETE
                    && i + 1 < diffs.size()
                    && diffs.get(i + 1).operation == DiffMatchPatch.Operation.INSERT) {

                DiffMatchPatch.Diff nextDiff = diffs.get(i + 1);
                LinkedList<DiffMatchPatch.Diff> wordDiffs = dmp.diffMain(currentDiff.text, nextDiff.text);
                dmp.diffCleanupSemantic(wordDiffs);

                String deletedLineContent = buildHighlightedLine(wordDiffs, DiffMatchPatch.Operation.DELETE);
                String insertedLineContent = buildHighlightedLine(wordDiffs, DiffMatchPatch.Operation.INSERT);

                html.append("<div class='diff-line diff-delete'><span class='line-num'>").append(oldLineNum++).append("</span><span class='diff-op'>-</span><span class='diff-content'>").append(deletedLineContent).append("</span></div>");
                html.append("<div class='diff-line diff-insert'><span class='line-num'>").append(newLineNum++).append("</span><span class='diff-op'>+</span><span class='diff-content'>").append(insertedLineContent).append("</span></div>");

                i++;
            } else {
                String[] lines = currentDiff.text.split("\n", -1);
                for (int j = 0; j < lines.length; j++) {
                    String line = lines[j];
                    if (j == lines.length - 1 && line.isEmpty()) continue;

                    switch (currentDiff.operation) {
                        case INSERT:
                            html.append("<div class='diff-line diff-insert'><span class='line-num'>").append(newLineNum++).append("</span><span class='diff-op'>+</span><span class='diff-content'>").append(escapeHtml(line)).append("</span></div>");
                            break;
                        case DELETE:
                            html.append("<div class='diff-line diff-delete'><span class='line-num'>").append(oldLineNum++).append("</span><span class='diff-op'>-</span><span class='diff-content'>").append(escapeHtml(line)).append("</span></div>");
                            break;
                        case EQUAL:
                            html.append("<div class='diff-line'><span class='line-num'>").append(oldLineNum++).append("</span><span class='diff-op'> </span><span class='diff-content'>").append(escapeHtml(line)).append("</span></div>");
                            newLineNum++;
                            break;
                    }
                }
            }
        }
        html.append("</pre>");
        return html.toString();
    }
    /**
     * 根据二次比对的结果，构建带有完整上下文和行内高亮标记的单行HTML内容。
     */
    private String buildHighlightedLine(LinkedList<DiffMatchPatch.Diff> wordDiffs, DiffMatchPatch.Operation targetOperation) {
        StringBuilder sb = new StringBuilder();
        for (DiffMatchPatch.Diff wordDiff : wordDiffs) {
            // 对于目标行，我们拼接EQUAL和目标操作(DELETE或INSERT)的部分
            if (wordDiff.operation == DiffMatchPatch.Operation.EQUAL) {
                sb.append(escapeHtml(wordDiff.text));
            } else if (wordDiff.operation == targetOperation) {
                String highlightClass = (targetOperation == DiffMatchPatch.Operation.INSERT) ? "highlight-insert" : "highlight-delete";
                sb.append("<span class='").append(highlightClass).append("'>").append(escapeHtml(wordDiff.text)).append("</span>");
            }
        }
        return sb.toString();
    }
    /**
     * 填充HTML模板的通用辅助方法，逻辑更清晰。
     */
    private String fillTemplate(String baseTemplate, String reportTitle, List<ComparisonResult> results, String oldDirName, String newDirName, boolean includeSummary, List<DiffDetail.DiffType> typesToInclude) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String now = LocalDateTime.now().format(formatter);

        // 替换通用元数据
        String report = baseTemplate.replace("${reportTitle}", escapeHtml(reportTitle))
                .replace("${reportTime}", now)
                .replace("${oldDir}", escapeHtml(oldDirName))
                .replace("${newDir}", escapeHtml(newDirName));

        // 根据需要决定是否填充摘要信息
        String summaryContent = includeSummary ? generateHtmlSummary(results) : "";
        report = report.replace("${summaryContent}", summaryContent);

        // 生成并填充核心的详细差异内容
        String detailedContent = generateDetailedHtmlContent(results, typesToInclude);
        report = report.replace("${detailedContent}", detailedContent);

        return report;
    }

    private String loadTemplate(String resourceName) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("无法找到资源文件: " + resourceName);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private String generateHtmlSummary(List<ComparisonResult> results) {
        long added = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.ADDED).count();
        long deleted = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.DELETED).count();
        long modified = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.MODIFIED).count();
        long unchanged = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.UNCHANGED).count();

        return String.format(
                "<p><strong>新增 (ADDED):</strong> %d</p>" +
                        "<p><strong>删除 (DELETED):</strong> %d</p>" +
                        "<p><strong>修改 (MODIFIED):</strong> %d</p>" +
                        "<p><strong>未变 (UNCHANGED):</strong> %d</p>",
                added, deleted, modified, unchanged
        );
    }


    /**
     * 生成带有行内高亮的HTML差异视图。
     */
    private String generateVisualHtmlDiff(String oldText, String newText) {
        String safeOldText = (oldText != null) ? oldText : "";
        String safeNewText = (newText != null) ? newText : "";

        LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(safeOldText, safeNewText);
        dmp.diffCleanupSemantic(diffs);

        StringBuilder html = new StringBuilder("<pre>");
        int oldLineNum = 1;
        int newLineNum = 1;

        for (DiffMatchPatch.Diff aDiff : diffs) {
            String text = aDiff.text;
            String[] lines = text.split("\n", -1);

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (i < lines.length - 1 || text.endsWith("\n")) { // 是一个完整的行
                    switch (aDiff.operation) {
                        case INSERT:
                            html.append("<div class='diff-line diff-insert'><span class='line-num'>").append(newLineNum++).append("</span><span class='diff-op'>+</span><span class='diff-content'>")
                                    .append(highlightWords(line, true)).append("</span></div>");
                            break;
                        case DELETE:
                            html.append("<div class='diff-line diff-delete'><span class='line-num'>").append(oldLineNum++).append("</span><span class='diff-op'>-</span><span class='diff-content'>")
                                    .append(highlightWords(line, false)).append("</span></div>");
                            break;
                        case EQUAL:
                            html.append("<div class='diff-line'><span class='line-num'>").append(oldLineNum++).append("</span><span class='diff-op'> </span><span class='diff-content'>")
                                    .append(escapeHtml(line)).append("</span></div>");
                            newLineNum++; // 相同行，两边行号都增加
                            break;
                    }
                } else { // 文本片段，不是完整的一行
                    html.append(highlightWords(line, aDiff.operation == DiffMatchPatch.Operation.INSERT));
                }
            }
        }
        html.append("</pre>");
        return html.toString();
    }

    // 一个简化的单词高亮逻辑
    private String highlightWords(String text, boolean isInsert) {
        String tag = isInsert ? "highlight-insert" : "highlight-delete";
        // 简单的实现，直接高亮整个片段
        return "<span class='" + tag + "'>" + escapeHtml(text) + "</span>";
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}