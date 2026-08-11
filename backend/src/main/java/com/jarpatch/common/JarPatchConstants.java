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
    public static final String EMPTY_TEXT = "";
    public static final String MESSAGE_DETAIL_SEPARATOR = ": ";
    public static final String MESSAGE_LIST_SEPARATOR = "；";
    public static final String DEFAULT_AUDITOR = "admin";
    public static final String UTF_8 = "UTF-8";
    public static final String WORKSPACE_ORIGINAL_DIR = "original";
    public static final String WORKSPACE_EXTRACTED_DIR = "extracted";
    public static final String WORKSPACE_SOURCE_DIR = "sources";
    public static final String WORKSPACE_COMPILED_DIR = "compiled";
    public static final String WORKSPACE_EXPORT_DIR = "exports";
    public static final String WORKSPACE_BASELINE_DIR = "baseline";
    public static final String WORKSPACE_STAGING_PREFIX = ".staging-";
    public static final String WORKSPACE_IMPORTING_MARKER = ".importing";
    public static final String WORKSPACE_READY_MARKER = ".ready";
    public static final String SOURCE_NESTED_JAR_DIR = "nested-jars";
    public static final String COMPILE_TARGET_MAIN = "main-classes";
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
    public static final String ARCHIVE_ENTRY_SEPARATOR = "!/";
    public static final String EMPTY_JSON_ARRAY = "[]";
    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final int BUFFER_SIZE = 8192;
    public static final int EMPTY_SIZE = 0;
    public static final int ONE_HUNDRED_PERCENT = 100;
    public static final int DEFAULT_HTTP_PORT = 18765;
    public static final long BACKEND_SHUTDOWN_DELAY_MILLIS = 250L;
    public static final int SEARCH_MAX_RESULTS = 200;
    public static final int SEARCH_PREVIEW_MAX_LENGTH = 220;
    public static final long DEFAULT_MAX_EDITABLE_FILE_BYTES = 5L * 1024L * 1024L;
    public static final long MIN_MAX_EDITABLE_FILE_BYTES = 1024L;
    public static final long MAX_MAX_EDITABLE_FILE_BYTES = 100L * 1024L * 1024L;
    public static final long WORKSPACE_CLEANUP_CONFIRMATION_MINUTES = 10L;
    public static final String TASK_LOG_PROGRESS_FORMAT = "[%d%%] %s";

    public static final String MESSAGE_SUCCESS = "操作成功";
    public static final String MESSAGE_FAILED = "操作失败";
    public static final String MESSAGE_PROJECT_NOT_FOUND = "项目不存在";
    public static final String MESSAGE_TASK_NOT_FOUND = "任务不存在";
    public static final String MESSAGE_TASK_CANCELLED = "任务已取消";
    public static final String MESSAGE_TASK_TYPE_EMPTY = "任务类型不能为空";
    public static final String MESSAGE_TASK_INTERRUPTED = "后端进程异常中断，任务已自动标记为失败";
    public static final String MESSAGE_JDK_HOME_REQUIRED = "请输入 JDK 安装目录";
    public static final String MESSAGE_JDK_HOME_INVALID = "该目录下未找到可用的 javac";
    public static final String MESSAGE_JDK_CONFIG_INVALID = "已保存的 JDK 路径不可用，请重新配置";
    public static final String MESSAGE_JDK_VERSION_INVALID = "无法识别 javac 版本，请配置完整 JDK";
    public static final String MESSAGE_JDK_VERSION_TOO_LOW = "当前 JDK 版本低于原包目标 Java 版本，已阻止编译";
    public static final String MESSAGE_CLASS_VERSION_NOT_FOUND = "无法从原包定位目标 class 版本，已阻止编译";
    public static final String MESSAGE_CLASS_VERSION_CONFLICT = "本次编译文件的原始 class 版本不一致，请分开修改和编译";
    public static final String MESSAGE_JAVA_RELEASE_UNSUPPORTED = "原包目标 Java 版本不支持 --release 编译";
    public static final String MESSAGE_FILE_NOT_EDITABLE = "该文件不允许编辑";
    public static final String MESSAGE_FILE_BINARY_READONLY = "二进制文件只支持查看，不支持编辑";
    public static final String MESSAGE_FILE_SIGNATURE_READONLY = "签名文件只支持查看，不支持编辑";
    public static final String MESSAGE_FILE_OUT_OF_WORKSPACE = "文件路径不在项目工作区内";
    public static final String MESSAGE_FILE_CHANGED_EXTERNALLY = "文件已在其他位置发生变化，请重新打开后再保存";
    public static final String MESSAGE_FILE_ENCODING_UNSUPPORTED = "文件编码不是受支持的 UTF-8、UTF-16LE 或 UTF-16BE";
    public static final String MESSAGE_FILE_MIXED_LINE_ENDINGS = "文件包含混合换行格式，修改前请先统一换行格式";
    public static final String MESSAGE_UNSUPPORTED_PACKAGE = "仅支持 jar 和 war 文件";
    public static final String MESSAGE_JDK_NOT_FOUND = "未检测到可用 JDK，请先配置 JDK 路径或系统环境变量 JAVA_HOME";
    public static final String MESSAGE_NO_MODIFIED_JAVA = "没有需要编译的 Java 修改文件";
    public static final String MESSAGE_SEARCH_KEYWORD_EMPTY = "搜索关键词不能为空";
    public static final String MESSAGE_INVALID_NESTED_JAR_SOURCE_PATH = "嵌套 Jar 源码路径不正确";
    public static final String MESSAGE_PROJECT_HISTORY_DELETED = "项目历史已删除";
    public static final String MESSAGE_WORKSPACE_CLEANED = "工作区已清理，项目历史仍然保留";
    public static final String MESSAGE_WORKSPACE_ALREADY_CLEANED = "该项目工作区已经清理";
    public static final String MESSAGE_WORKSPACE_CLEANUP_CONFIRMATION_INVALID = "清理确认已失效或工作区内容已变化，请重新预览";
    public static final String MESSAGE_WORKSPACE_CLEANUP_TASK_RUNNING = "项目仍有运行中任务，不能清理工作区";
    public static final String MESSAGE_PROJECT_SETTING_JAVA_VERSION_MISMATCH = "项目目标 Java 版本来自原包 class，不能改为其他版本";
    public static final String MESSAGE_PROJECT_SETTING_EXPORT_DIRECTORY_INVALID = "默认导出目录必须是绝对目录路径";
    public static final String MESSAGE_PROJECT_SETTING_FILE_LIMIT_INVALID = "可编辑文件大小限制不在允许范围内";
    public static final String MESSAGE_PROJECT_SETTING_UI_PREFERENCES_INVALID = "界面偏好必须是 JSON 对象";
    public static final String MESSAGE_PROJECT_SETTING_NESTED_JARS_CORRUPTED = "项目嵌套 Jar 设置损坏";
    public static final String MESSAGE_FILE_TOO_LARGE_TO_EDIT = "文件超过项目设置的可编辑大小限制";
    public static final String MESSAGE_EXPORT_OVERWRITE_ORIGINAL = "导出路径不能覆盖输入原包或工作区原包";
    public static final String MESSAGE_EXPORT_ATOMIC_MOVE_REQUIRED = "当前文件系统不支持原子发布导出文件，请选择同一文件系统内的其他目录";
    public static final String MESSAGE_WORKSPACE_ATOMIC_MOVE_REQUIRED = "工作区文件系统不支持原子提交，已停止本次操作";
    public static final String MESSAGE_WORKSPACE_IMPORT_STATE_INVALID = "工作区导入状态无效";
    public static final String MESSAGE_ARCHIVE_TOO_LARGE = "压缩包大小超过允许上限";
    public static final String MESSAGE_ARCHIVE_ENTRY_LIMIT = "压缩包条目数量超过允许上限";
    public static final String MESSAGE_ARCHIVE_UNCOMPRESSED_LIMIT = "压缩包展开总大小超过允许上限";
    public static final String MESSAGE_ARCHIVE_ENTRY_TOO_LARGE = "压缩包内单个文件超过允许上限";
    public static final String MESSAGE_ARCHIVE_RATIO_LIMIT = "压缩包包含异常压缩比条目";
    public static final String MESSAGE_ARCHIVE_PATH_DEPTH_LIMIT = "压缩包条目路径层级超过允许上限";
    public static final String MESSAGE_ARCHIVE_DUPLICATE_ENTRY = "压缩包包含重复条目";
    public static final String MESSAGE_SIGNED_ARCHIVE_MODIFIED = "原包包含签名且已有修改；请选择移除失效签名，或取消导出";
    public static final String MESSAGE_SOURCE_NOT_COMPILED = "源码已有修改但没有当前编译产物，请先成功编译再导出";
    public static final String MESSAGE_EXPORT_VALIDATION_FAILED = "导出结构校验失败，目标文件未发布";
    public static final String MESSAGE_BACKEND_SHUTDOWN_ACCEPTED = "后端已接受安全退出请求";
    public static final String MESSAGE_SIGNATURE_POLICY_UNSUPPORTED = "不支持的签名策略";
    public static final String MESSAGE_FILE_TREE_BUILD_FAILED = "构建文件树失败: ";
    public static final String MESSAGE_SHA256_UNAVAILABLE = "当前 Java 运行时缺少 SHA-256";
    public static final String LOG_UNEXPECTED_EXCEPTION = "后端发生未预期异常";
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
