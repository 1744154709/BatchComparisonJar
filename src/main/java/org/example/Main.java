package org.example;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 入口。
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("无法设置Nimbus外观，将使用默认外观。");
        }

        System.out.println("--- JAR包深度比对工具开始初始化 ---");
        // 弹出欢迎和说明对话框
        JOptionPane.showMessageDialog(null,
                "欢迎使用JAR包深度比对工具。\n\n您将需要依次选择两个文件夹：\n1. 旧版本 (V1) 的文件夹。\n2. 新版本 (V2) 的文件夹。\n\n程序将对这两个文件夹内的所有JAR包进行深度比对。",
                "欢迎", JOptionPane.INFORMATION_MESSAGE);

        // 通过图形界面获取文件夹路径
        File oldDir = selectDirectory("请选择旧版本 (V1) 的JAR包所在文件夹");
        if (oldDir == null) {
            System.out.println("操作已取消。程序退出。");
            return;
        }

        File newDir = selectDirectory("请选择新版本 (V2) 的JAR包所在文件夹");
        if (newDir == null) {
            System.out.println("操作已取消。程序退出。");
            return;
        }

        System.out.println("准备比较以下文件夹:");
        System.out.println("  旧文件夹: " + oldDir.getAbsolutePath());
        System.out.println("  新文件夹: " + newDir.getAbsolutePath());
        System.out.println("----------------------------------------");

        try {
            // 核心的比较逻辑
            FolderComparator folderComparator = new FolderComparator(oldDir, newDir);
            List<ComparisonResult> results = folderComparator.compare();
            System.out.println("\n比较流程已完成，正在生成HTML报告...");

            // 报告生成器
            ReportGenerator reportGenerator = new ReportGenerator();
            Map<String, String> reports = reportGenerator.generate(results, oldDir.getName(), newDir.getName());

            // 不同报告写入到不同的HTML文件中
            Path mainReportPath = writeReportToFile("main_report", reports.get("main_report"), newDir);
            Path nonLogicalReportPath = writeReportToFile("non_logical_report", reports.get("non_logical_report"), newDir);

            String successMessage = "报告生成完毕！\n\n"
                    + "您现在可以用浏览器打开以下文件查看详细报告：\n\n"
                    + "▶ 核心差异报告:\n" + mainReportPath.toAbsolutePath() + "\n\n"
                    + "▶ 非逻辑性差异报告:\n" + nonLogicalReportPath.toAbsolutePath() + "\n";
            System.out.println("\n--- 报告生成完毕 ---");
            System.out.println(successMessage);
            JOptionPane.showMessageDialog(null, successMessage, "操作成功", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            System.err.println("\n在程序执行过程中发生严重错误:");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "发生严重错误，详情请查看控制台日志。\n错误: " + e.getMessage(), "程序错误", JOptionPane.ERROR_MESSAGE);
        }

        System.out.println("\n--- 程序执行结束 ---");
    }

    /**
     * 打开一个图形化的文件夹选择对话框，并返回用户选择的文件夹。
     * @param title 对话框的标题
     * @return 用户选择的文件夹File对象，如果取消则返回null
     */
    private static File selectDirectory(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    /**
     * 将HTML报告内容写入一个带时间戳的文件，并返回生成的文件路径。
     * @param reportType 报告类型，用于构成文件名 (如 "main_report")
     * @param reportContent 要写入的HTML报告全文
     * @param referenceDir 用于确定报告保存位置的参考目录
     * @return 生成的文件的Path对象
     * @throws IOException
     */
    private static Path writeReportToFile(String reportType, String reportContent, File referenceDir) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        if (reportContent == null || reportContent.isEmpty()) {
            System.out.println("报告类型 '" + reportType + "' 内容为空，跳过文件生成。");
            return Paths.get(referenceDir.getParent(), "empty_report.tmp");
        }

        String fileName = "jar_comparison_" + reportType + "_" + timestamp + ".html";

        // 将报告生成在用户选择的新版本文件夹的父目录下
        Path filePath = Paths.get(referenceDir.getParent(), fileName);

        Files.writeString(filePath, reportContent, StandardCharsets.UTF_8);

        return filePath;
    }
}