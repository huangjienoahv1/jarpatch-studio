package com.jarpatch.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JarPatch Studio 通用常量。
 * <p>
 * 该类集中管理接口文案、文件后缀、目录名称、数据库默认值和业务阈值，避免业务代码
 * 在方法体内散落展示文案、状态值和魔法数字。控制器、服务和仓储层通过该类共享固定契约。
 * </p>
 *
 * @author 黄杰
 */
public final class JarPatchConstants {

    public static final String PRODUCT_NAME = "JarPatch Studio";
    public static final String DEFAULT_AUDITOR = "admin";
    public static final String UTF_8 = "UTF-8";
    public static final String WORKSPACE_ORIGINAL_DIR = "original";
    public static final String WORKSPACE_EXTRACTED_DIR = "extracted";
    public static final String WORKSPACE_SOURCE_DIR = "sources";
    public static final String WORKSPACE_COMPILED_DIR = "compiled";
    public static final String WORKSPACE_EXPORT_DIR = "exports";
    public static final String SOURCE_NESTED_JAR_DIR = "nested-jars";
    public static final String TREE_SOURCE_PREFIX = "sources/";
    public static final String TREE_EXTRACTED_PREFIX = "extracted/";
    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
    public static final String SPRING_BOOT_CLASSES_DIR = "BOOT-INF/classes";
    public static final String SPRING_BOOT_LIB_DIR = "BOOT-INF/lib";
    public static final String WAR_CLASSES_DIR = "WEB-INF/classes";
    public static final String WAR_LIB_DIR = "WEB-INF/lib";
    public static final String JAVA_EXTENSION = "java";
    public static final String CLASS_EXTENSION = "class";
    public static final String JAR_EXTENSION = "jar";
    public static final String WAR_EXTENSION = "war";
    public static final String ZIP_SEPARATOR = "/";
    public static final int BUFFER_SIZE = 8192;
    public static final int EMPTY_SIZE = 0;
    public static final int ONE_HUNDRED_PERCENT = 100;
    public static final int DEFAULT_HTTP_PORT = 18765;
    public static final int SEARCH_MAX_RESULTS = 200;
    public static final int SEARCH_PREVIEW_MAX_LENGTH = 220;

    public static final String MESSAGE_SUCCESS = "操作成功";
    public static final String MESSAGE_FAILED = "操作失败";
    public static final String MESSAGE_PROJECT_NOT_FOUND = "项目不存在";
    public static final String MESSAGE_TASK_NOT_FOUND = "任务不存在";
    public static final String MESSAGE_TASK_CANCELLED = "任务已取消";
    public static final String MESSAGE_TASK_TYPE_EMPTY = "任务类型不能为空";
    public static final String MESSAGE_JDK_HOME_REQUIRED = "请输入 JDK 安装目录";
    public static final String MESSAGE_JDK_HOME_INVALID = "该目录下未找到可用的 javac";
    public static final String MESSAGE_JDK_CONFIG_INVALID = "已保存的 JDK 路径不可用，请重新配置";
    public static final String MESSAGE_FILE_NOT_EDITABLE = "该文件不允许编辑";
    public static final String MESSAGE_FILE_BINARY_READONLY = "二进制文件只支持查看，不支持编辑";
    public static final String MESSAGE_FILE_SIGNATURE_READONLY = "签名文件只支持查看，不支持编辑";
    public static final String MESSAGE_FILE_OUT_OF_WORKSPACE = "文件路径不在项目工作区内";
    public static final String MESSAGE_UNSUPPORTED_PACKAGE = "仅支持 jar 和 war 文件";
    public static final String MESSAGE_JDK_NOT_FOUND = "未检测到可用 JDK，请先配置 JDK 路径或系统环境变量 JAVA_HOME";
    public static final String MESSAGE_NO_MODIFIED_JAVA = "没有需要编译的 Java 修改文件";
    public static final String MESSAGE_SEARCH_KEYWORD_EMPTY = "搜索关键词不能为空";
    public static final String MESSAGE_INVALID_NESTED_JAR_SOURCE_PATH = "嵌套 Jar 源码路径不正确";
    public static final String MESSAGE_PROJECT_HISTORY_DELETED = "项目历史已删除";
    public static final String SETTING_KEY_JDK_HOME = "jdk.home";

    public static final Set<String> EDITABLE_RESOURCE_EXTENSIONS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    "properties", "yml", "yaml", "xml", "json", "txt", "html", "css", "js"
            )));

    public static final Set<String> SIGNATURE_EXTENSIONS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    "sf", "rsa", "dsa", "ec"
            )));

    private JarPatchConstants() {
    }
}
