package com.sexycode.simplerpc.provider.app;

import com.sexycode.simplerpc.provider.service.Calculator;
import com.sexycode.simplerpc.provider.service.CalculatorImpl;
import com.sexycode.simplerpc.reuqest.CalculateRpcRequest;
import com.sexycode.simplerpc.utils.SerializeUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
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
        new NettyProviderApp(9090).start();
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        final RequestServerHandler serverHandler = new RequestServerHandler();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1024 * 1024))
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(serverHandler);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

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
            SerializeUtils serializeUtils1 = new SerializeUtils();
            in.clear();
            in.writeBytes(serializeUtils1.serialize(new Integer(result)));
            ctx.write(in);
            ctx.flush();
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            System.out.println("收完数据");
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    .addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
