package com.iustu.vertxagent.dubbo.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Author : Alex
 * Date : 2018/6/9 9:56
 * Description :
 */
public class AgentRequest {
    private static AtomicLong atomicLong = new AtomicLong();
    private long id;
    private String interfaceName;
    private String method;
    private String parameterTypesString;
    private String parameter;

    public AgentRequest() {
        id = atomicLong.getAndIncrement();
    }

    public AgentRequest(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getParameterTypesString() {
        return parameterTypesString;
    }

    public void setParameterTypesString(String parameterTypesString) {
        this.parameterTypesString = parameterTypesString;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
}
