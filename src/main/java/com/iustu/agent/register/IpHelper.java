package com.iustu.agent.register;

import java.net.InetAddress;

public class IpHelper {

    public static String getHostIp() throws Exception {

        String ip = InetAddress.getLocalHost().getHostAddress();
        return ip;
    }
}
