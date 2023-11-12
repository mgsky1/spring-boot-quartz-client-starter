package cn.acmsmu.mgsky1.quartz.model;

import java.lang.reflect.Method;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2023/11/12 15:47
 * @Modify:
 */
public class QuartzHttpInvokerModel {

    private String path;

    private Object obj;

    private Method method;

    public QuartzHttpInvokerModel(String path, Object obj, Method method) {
        this.path = path;
        this.obj = obj;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
