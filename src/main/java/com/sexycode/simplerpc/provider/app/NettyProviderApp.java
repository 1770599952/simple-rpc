package com.sexycode.simplerpc.provider.app;

import com.sexycode.simplerpc.provider.service.Calculator;
import com.sexycode.simplerpc.provider.service.CalculatorImpl;
import com.sexycode.simplerpc.reuqest.CalculateRpcRequest;
import com.sexycode.simplerpc.utils.SerializeUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NettyProviderApp {
    private static Logger log = LoggerFactory.getLogger(ProviderApp.class);

    private final int port;

    public NettyProviderApp(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        new Thread(new Runnable() {
            public void run() {
                try {
                    new NettyProviderApp(9090).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        final RequestServerHandler serverHandler = new RequestServerHandler();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4));
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(4));
                            socketChannel.pipeline().addLast(serverHandler);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            // f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //    group.shutdownGracefully().sync();
        }

    }

    @ChannelHandler.Sharable
    private class RequestServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf in = (ByteBuf) msg;
            System.out.println("Server received:" + in.toString());
            SerializeUtils<CalculateRpcRequest> serializeUtils = new SerializeUtils();
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            CalculateRpcRequest calculateRpcRequest = serializeUtils.unSerialize(bytes);
            int result = 0;
            if ("add".equals(calculateRpcRequest.getMethod())) {
                Calculator calculator = new CalculatorImpl();
                result = calculator.add(calculateRpcRequest.getA(), calculateRpcRequest.getB());
            } else {
                throw new UnsupportedOperationException();
            }
            in.clear();
            SerializeUtils serializeUtils1 = new SerializeUtils();
            System.out.println(serializeUtils1.serialize(new Integer(result)));
            ctx.writeAndFlush(new PooledByteBufAllocator().buffer(500).writeBytes(serializeUtils1.serialize(new Integer(result)))).sync();
        }


        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            System.out.println("收完数据");
            ctx.close().addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
