package com.sexycode.simplerpc.comsumer.app;

import com.sexycode.simplerpc.comsumer.service.NettyCalculatorRemoteImpl;
import com.sexycode.simplerpc.provider.service.Calculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyConsumerApp {
    private static Logger log = LoggerFactory.getLogger(ComsumerApp.class);

    public static void main(String[] args) {
        Calculator calculator = new NettyCalculatorRemoteImpl();
        int result = calculator.add(1, 2);
        log.info("result is {}", result);
    }
}
