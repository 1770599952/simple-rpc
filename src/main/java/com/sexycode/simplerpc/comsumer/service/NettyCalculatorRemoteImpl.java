package com.sexycode.simplerpc.comsumer.service;

import com.sexycode.simplerpc.provider.service.Calculator;
import com.sexycode.simplerpc.reuqest.CalculateRpcRequest;
import com.sexycode.simplerpc.utils.SerializeUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.net.InetSocketAddress;

public class NettyCalculatorRemoteImpl implements Calculator {
    public int add(int a, int b) {
        String host = "127.0.0.1";
        int port = 9090;
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4));
                            channel.pipeline().addLast(new LengthFieldPrepender(4));
                            channel.pipeline().addLast(new ClientHandler());

                        }
                    });
            ChannelFuture f = bootstrap.connect().sync();
            CalculateRpcRequest calculateRpcRequest = generateRequest(a, b);
            byte[] datas = new SerializeUtils().serialize(calculateRpcRequest);
            f.channel().writeAndFlush(new PooledByteBufAllocator().buffer(500).writeBytes(datas)).sync();
            f.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private CalculateRpcRequest generateRequest(int a, int b) {
        CalculateRpcRequest calculateRpcRequest = new CalculateRpcRequest();
        calculateRpcRequest.setA(a);
        calculateRpcRequest.setB(b);
        calculateRpcRequest.setMethod("add");
        return calculateRpcRequest;
    }
}

class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        Integer result = new SerializeUtils<Integer>().unSerialize(bytes);
        System.out.println(result);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}


