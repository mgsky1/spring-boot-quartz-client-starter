package cn.acmsmu.mgsky1.quartz.handler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import cn.acmsmu.mgsky1.quartz.model.QuartzHttpInvokerModel;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

/**
 * @Desc: https://blog.csdn.net/qq_34813134/article/details/127026138
 * @Author: huangzhiyuan
 * @CreateDate: 2023/11/12 15:44
 * @Modify:
 */
public class StandaloneHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private Map<String, QuartzHttpInvokerModel> invokerModelMap;

    public StandaloneHandler(List<QuartzHttpInvokerModel> modelList) {
        super();
        invokerModelMap = CollUtil.toMap(modelList, new HashMap<>(), QuartzHttpInvokerModel::getPath);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String url = request.uri();
        QuartzHttpInvokerModel model = invokerModelMap.get(url);
        if (model != null) {
            //请求内容
            String body = request.content().toString(CharsetUtil.UTF_8);
            //请求Type
            String m = request.method().name();
            if (!"POST".equals(m.toUpperCase())) {
                responseError(ctx);
                return;
            }
            if (JSONUtil.isJsonObj(body) || NumberUtil.isNumber(body)) {
                Method method = model.getMethod();
                Object p = null;
                if (method.getParameterCount() != 0) {
                    p = JSON.parseObject(body, method.getParameters()[0].getType());
                }
                Object obj = model.getObj();
                Object result = ReflectUtil.invoke(obj, method, p);
                responseOK(ctx, JSON.toJSONString(result));
            } else {
                responseError(ctx);
            }
        } else {
            responseError(ctx);
        }
    }

    private void responseOK(ChannelHandlerContext ctx, String result) {
        response(ctx, "{\"status\":\"OK\", \"data\":"+result+"}");
    }
    private void responseError(ChannelHandlerContext ctx) {
        response(ctx, "{\"status\":\"Error\"}");
    }

    private void response(ChannelHandlerContext ctx, String result){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK, Unpooled.wrappedBuffer(result.getBytes()));
        response.headers().set("Content-Type", "application/json; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
