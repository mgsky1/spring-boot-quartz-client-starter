package cn.acmsmu.mgsky1.quartz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import cn.acmsmu.mgsky1.quartz.runner.QuartzClientRunner;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2023/11/11 23:50
 * @Modify:
 */
@Configuration
@Import({QuartzClientRunner.class, QuartzStarterConfig.class})
public class QuartzStarterAutoConfig {
}
