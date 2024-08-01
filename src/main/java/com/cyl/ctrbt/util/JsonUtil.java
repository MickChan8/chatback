package com.cyl.ctrbt.util;

import org.springframework.util.StringUtils;

public class JsonUtil {

    public static String formatJsonStr(String strBefore){
        String str1 = StringUtils.replace(strBefore, "\r\n", "\n");
        String str2 =  StringUtils.replace(StringUtils.replace(str1, "\n```", ""),
                "```json\n", "");
        return str2;
    }
}
