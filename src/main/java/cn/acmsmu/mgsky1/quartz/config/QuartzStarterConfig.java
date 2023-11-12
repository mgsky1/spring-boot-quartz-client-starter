package cn.acmsmu.mgsky1.quartz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2023/11/11 22:11
 * @Modify:
 */
@Component
@ConfigurationProperties(prefix = "quartz.client")
public class QuartzStarterConfig {
    /**
     * quartz客户端端口号
     */
    private Integer port;

    /**
     * quartz客户端类型
     */
    private String type;

    /**
     * 要扫描的包
     */
    private String packageScan;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPackageScan() {
        return packageScan;
    }

    public void setPackageScan(String packageScan) {
        this.packageScan = packageScan;
    }
}
