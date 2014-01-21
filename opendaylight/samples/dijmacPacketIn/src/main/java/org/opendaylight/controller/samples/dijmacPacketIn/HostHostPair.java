
package org.opendaylight.controller.samples.dijmacPacketIn;

import java.io.Serializable;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

/**
 * Class that represent a pair of {Host, Host}, the intent of it
 * is to be used as a key in the database kept by MacSwitching module
 * where for every Packet
 */
public class HostHostPair implements Serializable {
    private static final long serialVersionUID = 1L;
    private HostNodeConnector srcHost;
    private HostNodeConnector dstHost;
    public HostHostPair(HostNodeConnector dst, HostNodeConnector src) {
        setsrcHost(src);
        setdstHost(dst);
    }

    public HostNodeConnector getsrcHost() {
        return srcHost;
    }
    public HostNodeConnector getdstHost() {
        return dstHost;
    }

    public void setsrcHost(HostNodeConnector srcHost) {
        this.srcHost = srcHost;
    }
    public void setdstHost(HostNodeConnector dstHost) {
        this.dstHost = dstHost;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((srcHost == null) ? 0 : srcHost.hashCode());
        result = prime * result + ((dstHost == null) ? 0 : dstHost.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HostHostPair other = (HostHostPair) obj;
        if (srcHost == null) {
            if (other.srcHost != null)
                return false;
        } else if (!srcHost.equals(other.srcHost))
            return false;
        if (dstHost == null) {
            if (other.dstHost != null)
                return false;
        } else if (!dstHost.equals(other.dstHost))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HostHostPair [srcHost=" + srcHost + ", dstHost=" + dstHost + "]";
    }
}
