package com.jarpatch.service;

import com.jarpatch.model.ErrorGuideItem;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品错误排查向导服务。
 * <p>
 * 系统控制器读取本服务提供的发布级排查清单，覆盖 JDK、CFR、编译、签名、路径长度和端口冲突；
 * 文案集中维护，避免前端散落不可追踪的处理建议。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ErrorGuideService {

    private static final List<ErrorGuideItem> GUIDE_ITEMS = List.of(
            new ErrorGuideItem("JDK", "JDK 配置或版本不匹配", "提示未找到 javac、版本无法识别或低于原包目标版本",
                    List.of("在设置中确认 JDK 目录指向完整 JDK，而不是 JRE",
                            "执行 javac -version，确认版本与原包目标 Java 版本一致或更高"),
                    List.of("重新选择有效 JDK 目录并保存",
                            "Java 8 原包可使用 JDK 8；使用更高 JDK 时系统会严格添加 --release")),
            new ErrorGuideItem("CFR", "CFR 反编译失败", "导入阶段提示 CFR 缺失、进程退出或部分源码未生成",
                    List.of("确认后端包内 BOOT-INF/lib 包含 cfr 依赖",
                            "查看任务持久化日志中的具体 class 路径和 CFR 输出"),
                    List.of("重新构建完整后端发布包",
                            "对混淆或损坏 class 保留只读查看，不把无法反编译的源码当作可编译源码")),
            new ErrorGuideItem("COMPILE", "Java 编译失败", "javac 返回语法、依赖、包名或目标版本错误",
                    List.of("先查看任务日志中的 javac 完整错误",
                            "确认修改源码所属主包或嵌套 Jar 分组正确",
                            "确认目标 class 版本检测依据与当前源码对应"),
                    List.of("只修改错误指向的源码或依赖选择后重新编译",
                            "不要通过改高目标版本或自动改源码绕过编译错误")),
            new ErrorGuideItem("SIGNATURE", "签名包导出受阻", "原包含 META-INF 签名且已有源码、资源或 class 修改",
                    List.of("在导出差异中确认所有真实修改",
                            "确认目标运行环境是否强制校验原签名"),
                    List.of("取消导出并使用原包保持签名",
                            "明确选择移除失效签名后导出，再按组织流程重新签名")),
            new ErrorGuideItem("PATH", "路径过长或无权限", "导入、编译或导出提示路径无法创建、访问被拒绝",
                    List.of("确认工作区和导出目录具有读写权限",
                            "检查 Windows 完整路径是否过长，目录名是否包含受限字符"),
                    List.of("把项目工作区或导出目录改到较短且可写的绝对路径",
                            "不要覆盖原包，也不要选择工作区 original 目录")),
            new ErrorGuideItem("PORT", "本地后端端口冲突", "桌面端等待健康检查超时或检测到其他实例占用 18765",
                    List.of("确认是否已有 JarPatch Studio 实例运行",
                            "检查占用 127.0.0.1:18765 的进程身份"),
                    List.of("先正常退出已有实例后重新启动",
                            "若占用者不是 JarPatch Studio，结束或调整该程序后再启动"))
    );

    /**
     * 返回不可变错误排查清单。
     *
     * @return 错误向导条目
     */
    public List<ErrorGuideItem> list() {
        return GUIDE_ITEMS;
    }
}
