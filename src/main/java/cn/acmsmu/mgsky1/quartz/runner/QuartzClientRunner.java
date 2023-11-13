package cn.acmsmu.mgsky1.quartz.runner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

import cn.acmsmu.mgsky1.quartz.annotation.QuartzClient;
import cn.acmsmu.mgsky1.quartz.config.QuartzStarterConfig;
import cn.acmsmu.mgsky1.quartz.handler.StandaloneHandler;
import cn.acmsmu.mgsky1.quartz.meta.ClientTypeEnum;
import cn.acmsmu.mgsky1.quartz.model.QuartzHttpInvokerModel;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.ClassScanner;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

/**
 * @Desc:
 * @Author: huangzhiyuan
 * @CreateDate: 2023/11/11 23:00
 * @Modify:
 */
@Component
public class QuartzClientRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(QuartzClientRunner.class);

    private static final String LOGGER_PREFIX = "【quartz-client】";

    private static final Integer DEFAULT_PORT = 9100;

    @Autowired
    private QuartzStarterConfig starterConfig;

    @Autowired
    private ApplicationContext applicationContext;

    private ChannelFuture channelFuture = null;

    private EventLoopGroup bossGroup = null;

    private EventLoopGroup workerGroup = null;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!ClientTypeEnum.STANDALONE.equalByCode(starterConfig.getType())) {
            logger.error("{}: 暂时不支持其他模式", LOGGER_PREFIX);
            return;
        }
        logger.info("{}: 定时任务配置为:{}", LOGGER_PREFIX, JSON.toJSON(starterConfig));
        // 初始化Netty服务端
        ServerBootstrap serverBootstrap = initNettyServerBootStrap();
        // 将目标方法注册为netty服务端的一个控制器
        List<QuartzHttpInvokerModel> invokerModels = getTargetInvokerModel(starterConfig.getPackageScan());
        if (CollUtil.isEmpty(invokerModels)) {
            return;
        }

        // 启动Netty服务
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new HttpServerCodec());// http 编解码
                pipeline.addLast("httpAggregator", new HttpObjectAggregator(100 * 1024 * 1024)); // http 消息聚合器                                                                     512*1024为接收的最大contentlength
                pipeline.addLast(new HttpServerExpectContinueHandler());
                if (CollUtil.isNotEmpty(invokerModels)) {
                    pipeline.addLast(new StandaloneHandler(invokerModels));
                }
            }
        });
        channelFuture = serverBootstrap
            .bind(Optional.ofNullable(starterConfig.getPort()).orElse(DEFAULT_PORT)).sync();
        logger.info("{}: 定时任务初始化完毕", LOGGER_PREFIX);
    }

    private List<QuartzHttpInvokerModel> getTargetInvokerModel(String packageName) throws Exception{
        if (StrUtil.isEmpty(packageName)) {
            return Collections.emptyList();
        }
        logger.info("{}: 要扫描的定时任务的包名为：{}", LOGGER_PREFIX, packageName);

        List<QuartzHttpInvokerModel> quartzHttpInvokerModels = new ArrayList<>();

        try {
            Set<Class<?>> classes = ClassScanner.scanPackage(packageName);
            if (CollUtil.isNotEmpty(classes)) {
                for (Class clazz : classes) {
                    List<QuartzHttpInvokerModel> mList = getTargetInvokerModel(clazz);
                    if (CollUtil.isNotEmpty(mList)) {
                        quartzHttpInvokerModels.addAll(mList);
                    }
                }
            }
            return quartzHttpInvokerModels;
        } catch (Exception e) {
            logger.error("{}: 初始化时发生异常:", LOGGER_PREFIX, e);
            throw e;
        }
    }

    private List<QuartzHttpInvokerModel> getTargetInvokerModel(Class clazz) {
        Method[] publicMethods = ReflectUtil.getPublicMethods(clazz);
        if (publicMethods.length == 0) {
            return Collections.emptyList();
        }

        List<QuartzHttpInvokerModel> methodList = new ArrayList<>();
        for (Method method : publicMethods) {
            QuartzClient annotation = method.getAnnotation(QuartzClient.class);
            Integer parameterCount = method.getParameterCount();
            if (annotation != null && (parameterCount == 1 || parameterCount == 0)) {
                Object obj = applicationContext.getBean(clazz);
                methodList.add(new QuartzHttpInvokerModel("/" + annotation.value(), obj, method));
                logger.info("{}: 扫描到定时任务：{}", LOGGER_PREFIX, annotation.value());
            }
        }
        return methodList;
    }

    private ServerBootstrap initNettyServerBootStrap() {
        // code by GPT-3.5
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true);
        return serverBootstrap;
    }

    @PreDestroy
    public void destroy() throws Exception{
        if (channelFuture != null) {
            channelFuture.channel().closeFuture().sync();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
