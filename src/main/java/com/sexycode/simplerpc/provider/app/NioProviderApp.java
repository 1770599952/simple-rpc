package com.sexycode.simplerpc.provider.app;

import com.sexycode.simplerpc.provider.service.Calculator;
import com.sexycode.simplerpc.provider.service.CalculatorImpl;
import com.sexycode.simplerpc.reuqest.CalculateRpcRequest;
import com.sexycode.simplerpc.utils.SerializeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioProviderApp {
    private static Logger log = LoggerFactory.getLogger(ProviderApp.class);

    private Selector selector;

    public static void main(String[] args) throws Exception {
        NioProviderApp nioProviderApp = new NioProviderApp();
        nioProviderApp.initServer(9090);
        nioProviderApp.run();
    }

    /**
     * @param port
     * @throws IOException
     */
    public void initServer(int port) throws Exception {

        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        serverChannel.configureBlocking(false);

        serverChannel.socket().bind(new InetSocketAddress(port));

        this.selector = Selector.open();

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务端启动成功！");
    }

    private void run() throws Exception {
        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                handle(key);
            }
        }
    }

    private static void handle(SelectionKey key) throws Exception {
        if (key.isAcceptable()) {
            try {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(key.selector(), SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }
        } else if (key.isReadable()) {
            SocketChannel sc = null;
            try {
                sc = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                buffer.clear();
                int len = sc.read(buffer);
                Thread.sleep(1000L);
                while (len != -1) {
                    len = sc.read(buffer);
                }
                if (buffer.capacity() > 0) {
                    InputStream sbs = new ByteArrayInputStream(buffer.array());
                    // 将请求反序列化
                    ObjectInputStream objectInputStream = new ObjectInputStream(sbs);
                    Object object = objectInputStream.readObject();

                    log.info("request is {}", object);

                    // 调用服务
                    int result = 0;
                    if (object instanceof CalculateRpcRequest) {
                        CalculateRpcRequest calculateRpcRequest = (CalculateRpcRequest) object;
                        if ("add".equals(calculateRpcRequest.getMethod())) {
                            Calculator calculator = new CalculatorImpl();
                            result = calculator.add(calculateRpcRequest.getA(), calculateRpcRequest.getB());
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }
                    SerializeUtils serializeUtils = new SerializeUtils();
                    ByteBuffer bufferToWrite = ByteBuffer.wrap(serializeUtils.serialize(new Integer(result)));
                    sc.write(bufferToWrite);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (sc != null) {
                    try {
                        sc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
