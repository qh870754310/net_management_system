package com.sjsz.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * HttpServletResponse帮助类
 * Created by zhengda.li on 2017/2/13 10:09.
 */
public class ResponseHelper {
    /**
     * 信息输出
     *
     * @param response HttpServletResponse对象
     * @param obj      需要输出的对象
     */
    public static void write(HttpServletResponse response, Object obj) {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.println(obj.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}
