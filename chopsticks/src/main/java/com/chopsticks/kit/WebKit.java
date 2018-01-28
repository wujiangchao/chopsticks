package com.chopsticks.kit;

import com.chopsticks.mvc.http.Request;

public class WebKit {

    public static final String UNKNOWN_MAGIC = "unknown";

    /**
     * Get the client IP address by request
     *
     * @param request Request instance
     * @return return ip address
     */
    public static String ipAddress(Request request) {
        String ipAddress = request.header("x-forwarded-for");
        if (StringKit.isBlank(ipAddress) || UNKNOWN_MAGIC.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.header("Proxy-Client-IP");
        }
        if (StringKit.isBlank(ipAddress) || UNKNOWN_MAGIC.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.header("WL-Proxy-Client-IP");
        }
        if (StringKit.isBlank(ipAddress) || UNKNOWN_MAGIC.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.header("X-Real-IP");
        }
        if (StringKit.isBlank(ipAddress) || UNKNOWN_MAGIC.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.header("HTTP_CLIENT_IP");
        }
        if (StringKit.isBlank(ipAddress) || UNKNOWN_MAGIC.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.header("HTTP_X_FORWARDED_FOR");
        }
        return ipAddress;
    }

}
