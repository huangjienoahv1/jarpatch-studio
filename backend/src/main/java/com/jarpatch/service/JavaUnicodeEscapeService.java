package com.jarpatch.service;

import org.springframework.stereotype.Service;

/**
 * Java 中文 Unicode 转义还原服务。
 * <p>
 * CFR 反编译后常会把中文字符串显示为“反斜杠 u 加四位十六进制”形式。该服务只还原中文、中文标点和全角字符
 * 对应的 Unicode 转义，避免把控制字符、换行符等 Java 语法敏感转义错误转换。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class JavaUnicodeEscapeService {

    private static final int UNICODE_ESCAPE_LENGTH = 6;
    private static final int HEX_RADIX = 16;

    /**
     * 还原文本中的中文 Unicode 转义。
     *
     * @param content 原始文本
     * @return 已还原中文的文本
     */
    public String decodeChineseEscapes(String content) {
        if (content == null || content.indexOf("\\u") < 0) {
            return content;
        }
        StringBuilder result = new StringBuilder(content.length());
        int index = 0;
        while (index < content.length()) {
            if (isUnicodeEscapeStart(content, index)) {
                String hex = content.substring(index + 2, index + UNICODE_ESCAPE_LENGTH);
                char decoded = (char) Integer.parseInt(hex, HEX_RADIX);
                if (isChineseReadableCharacter(decoded)) {
                    result.append(decoded);
                    index += UNICODE_ESCAPE_LENGTH;
                    continue;
                }
            }
            result.append(content.charAt(index));
            index++;
        }
        return result.toString();
    }

    /**
     * 判断当前位置是否为合法 Java Unicode 转义开头。
     *
     * @param content 文本内容
     * @param index   当前下标
     * @return 是合法转义开头时返回 true
     */
    private boolean isUnicodeEscapeStart(String content, int index) {
        if (index + UNICODE_ESCAPE_LENGTH > content.length()) {
            return false;
        }
        if (content.charAt(index) != '\\' || content.charAt(index + 1) != 'u') {
            return false;
        }
        for (int i = index + 2; i < index + UNICODE_ESCAPE_LENGTH; i++) {
            if (!isHex(content.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符是否为十六进制字符。
     *
     * @param value 待判断字符
     * @return 是十六进制字符时返回 true
     */
    private boolean isHex(char value) {
        return (value >= '0' && value <= '9')
                || (value >= 'a' && value <= 'f')
                || (value >= 'A' && value <= 'F');
    }

    /**
     * 判断字符是否属于中文可读字符范围。
     *
     * @param value 待判断字符
     * @return 是中文、中文标点或全角字符时返回 true
     */
    private boolean isChineseReadableCharacter(char value) {
        return (value >= '\u3400' && value <= '\u4dbf')
                || (value >= '\u4e00' && value <= '\u9fff')
                || (value >= '\uf900' && value <= '\ufaff')
                || (value >= '\u3000' && value <= '\u303f')
                || (value >= '\uff00' && value <= '\uffef');
    }
}
