package org.opendaylight.controller.flowmanager.internal;

public class L2FlowManagerUnit {
    private short idleTimeout;
    private short hardTimeout;

    public L2FlowManagerUnit(){
        this.idleTimeout = 10;
        this.hardTimeout = 20;
    }
    public L2FlowManagerUnit(short idleTimeout,short hardTimeout){
        this.idleTimeout = idleTimeout;
        this.hardTimeout = hardTimeout;
    }

    public void setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public short getIdleTimeout() {
        return this.idleTimeout;
    }
    public void setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    public short getHardTimeout() {
        return this.hardTimeout;
    }
}
