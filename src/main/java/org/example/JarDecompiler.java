package org.example;

import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.main.Fernflower; // 导入核心反编译器类

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

/**
 * 封装了JetBrains的反编译器,采用内存操作，直接与Fernflower核心API交互
 */
public class JarDecompiler {

    /**
     * 反编译单个.class文件的字节数组。
     *
     * @param classBytes .class文件的字节内容
     * @param entryName  JAR包中条目的名称，eg: "com/example/MyClass.class"
     * @return 反编译后的Java源代码字符串
     * @throws DecompilationException
     */
    public String decompile(byte[] classBytes, String entryName) throws DecompilationException {
        try {
            // 反编译器所需的核心组件
            // IBytecodeProvider: 从内存中提供字节码
            IBytecodeProvider bytecodeProvider = (externalPath, internalPath) -> classBytes;
            // 反编译结果保存在内存中
            InMemoryResultSaver resultSaver = new InMemoryResultSaver();
            // 配置选项
            Map<String, Object> options = new HashMap<>();
            options.put("dgs", "1");
            options.put("asc", "1");
            // 创建并配置核心反编译器 Fernflower
            Fernflower fernflower = new Fernflower(bytecodeProvider, resultSaver, options, new PrintStreamLogger(System.out));

            // 将需要反编译的“源”添加到反编译器上下文中
            fernflower.addSource(new File(entryName));

            // 执行反编译
            fernflower.decompileContext();

            // 从内存中获取结果
            String decompiledSource = resultSaver.getSourceCode();
            if (decompiledSource == null) {
                throw new DecompilationException("反编译完成，但未能从结果中获取源代码 for " + entryName);
            }
            return decompiledSource;

        } catch (Exception e) {
            throw new DecompilationException("反编译 " + entryName + " 时发生严重错误。", e);
        }
    }

    /**
     * IResultSaver的内存实现，用于捕获反编译后的源代码。
     */
    private static class InMemoryResultSaver implements IResultSaver {
        private String sourceCode;

        @Override
        public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
            this.sourceCode = content;
        }

        public String getSourceCode() {
            return this.sourceCode;
        }

        @Override
        public void saveFolder(String path) { }

        @Override
        public void copyFile(String source, String path, String entryName) { }

        @Override
        public void createArchive(String path, String archiveName, Manifest manifest) { }

        @Override
        public void saveDirEntry(String path, String archiveName, String entryName) { }

        @Override
        public void copyEntry(String source, String path, String archiveName, String entry) { }

        @Override
        public void closeArchive(String path, String archiveName) { }

        @Override
        public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
            this.sourceCode = content;
        }
    }
}