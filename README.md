# JAR 包深度比对工具 (JAR Deep Comparator)

![Java](https://img.shields.io/badge/Java-11+-orange.svg)![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)![License](https://img.shields.io/badge/License-MIT-green.svg)

一款强大、易用的Java桌面应用程序，用于深度、智能地比对两个文件夹内所有JAR包的差异，并生成直观、带有行内高亮的HTML可视化报告。

## 🌟 项目亮点

*   **深度反编译比对**：不仅仅是文件哈希值的比较，工具会深入到JAR包内部，对`.class`文件进行反编译，在源代码层面进行比对，精确捕捉每一个逻辑变更。
*   **智能差异过滤**：(不完善,但我懒得继续优化了)能够智能识别并过滤掉由编译器自动生成的、与业务逻辑无关的“噪音”差异（如`access$`合成方法、`$FF: synthetic method`注释等），并将它们与真正的逻辑变更分开报告。
*   **可视化报告**：生成的HTML报告采用业界领先的“修改行拆分与行内高亮”模式，以极其直观的方式展示代码变更，让差异“一目了然”。
*   **用户友好的图形界面**：通过简洁的图形界面选择文件夹，无需记忆和输入复杂的命令行参数，对所有用户都非常友好。
*   **自包含与跨平台**：采用Java编写，并通过内嵌资源的方式管理模板，生成的JAR包具有良好的跨平台性。可进一步打包成包含JRE的独立可执行程序，在没有Java环境的电脑上也能运行。

## ✨ 功能特性

*   **文件夹级比对**：自动扫描并配对两个指定文件夹中的所有JAR包。
*   **状态识别**：清晰地识别出JAR包的三种状态：**新增 (ADDED)**、**删除 (DELETED)** 和 **修改 (MODIFIED)**。
*   **类级差异分析**：对于被修改的JAR包，能够分析出内部哪些类是新增、删除或修改的。
*   **源代码级差异呈现**：
    *   对于被修改的类，提供源代码级别的差异比对。
    *   采用`+` / `-`号清晰标记新增和删除的行。
    *   对发生变更的行，提供**行内单词级高亮**，精确展示修改的具体字符。
*   **分类报告系统**：
    *   **主报告 (`main_report.html`)**: 只展示真正的代码逻辑变更和反编译错误，聚焦于需要开发者关注的核心内容。
    *   **非逻辑性差异报告 (`non_logical_report.html`)**: 单独收录所有由编译器行为导致的、可忽略的差异，保持主报告的整洁。

## 🚀 快速开始

### 使用要求

*   已安装Java运行环境 (JRE) 11或更高版本。

### 使用步骤

1.  **获取程序**：下载最新的可执行`JAR`文件（例如`jar-deep-comparator-1.0-SNAPSHOT.jar`）。
2.  **运行程序**：通过以下命令运行程序：
    ```bash
    java -jar jar-deep-comparator-1.0-SNAPSHOT.jar
    ```
    或者，在许多桌面环境中，您可以直接双击该JAR文件来启动。
3.  **选择文件夹**：
    *   程序启动后，会弹出一个欢迎对话框。点击“确定”。
    *   接着会弹出第一个文件选择器，请选择**旧版本 (V1)** 的JAR包所在的文件夹。
    *   随后会弹出第二个文件选择器，请选择**新版本 (V2)** 的JAR包所在的文件夹。
4.  **等待分析**：选择完毕后，程序会自动在后台进行深度比对和分析。您可以在控制台看到实时的进度信息。
5.  **查看报告**：
    *   分析完成后，会弹出一个成功对话框，告知您HTML报告的保存路径。
    *   在您选择的**新版本文件夹的上一级目录**中，会生成两份以时间戳命名的HTML报告文件。
    *   用您喜欢的浏览器打开`main_report_... .html`文件，即可查看最核心的差异报告。

## 🛠️ 开发与构建

本项目使用Apache Maven进行构建和依赖管理。

### 依赖库

*   **`org.apache.commons:commons-io`**: 用于简化文件操作。
*   **`org.jetbrains.java.decompiler:java-decompiler-engine`**: JetBrains出品的强大Java反编译器 (FernFlower)。
*   **`org.bitbucket.cowwoc:diff-match-patch`**: Google出品的顶级文本差异比对库。
*   **`io.github.java-diff-utils:java-diff-utils`**: 用于辅助实现差异比对逻辑。

### 构建项目

1.  克隆本仓库到本地。
2.  确保您已安装Apache Maven和JDK 11+。
3.  在项目根目录下，运行以下命令来编译和打包：
    ```bash
    mvn clean package
    ```
4.  命令执行成功后，会在`target/`目录下找到一个包含了所有依赖的、可直接运行的 fat-jar 文件，例如 `jar-deep-comparator-1.0-SNAPSHOT.jar`。


## 📄 许可证
部分代码由ai辅助生成
本项目采用 [MIT License](LICENSE) 开源。