/*
 * singleton utility class for managing system properties and connections
 */
package nebuladss;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jason Zerbe
 */
public class NebulaUtilities {

    private static NebulaUtilities nu_singleInstance = null;
    private boolean isLocalHostBehindNATCache = false;

    protected NebulaUtilities() {
    }

    public static NebulaUtilities getInstance() {
        if (nu_singleInstance == null) {
            nu_singleInstance = new NebulaUtilities();
        }
        return nu_singleInstance;
    }

    /**
     * check if this host's address is able to be routed on the WAN
     * @return boolean - is behind a NAT, needs port forwarding
     */
    public boolean isLocalHostBehindNAT() {
        if (isLocalHostBehindNATCache) {
            return isLocalHostBehindNATCache;
        }

        String aIPv4AddrStr = getLocalIPAddress(IpAddressType.IPv4);
        if (aIPv4AddrStr == null) {
            return false;
        }
        String isValidExternalAddressStr = "ipv4=" + aIPv4AddrStr;
        String aURLConnectionParamStr = "opt=nat" + "&" + isValidExternalAddressStr;
        ArrayList<String> returnArrayList = MasterServer.getInstance().returnServerMethod(aURLConnectionParamStr);
        if (returnArrayList.contains(isValidExternalAddressStr)) {
            return false;
        } else {
            isLocalHostBehindNATCache = true;
            return true;
        }
    }

    /**
     * gets a valid IP Address from the local machine that is able to be routed
     * @return String
     */
    public String getLocalIPAddress(IpAddressType theIpAddressType) {
        Enumeration<NetworkInterface> aNetworkInterfaceEnumeration = null;
        try {
            aNetworkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            Logger.getLogger(NebulaUtilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (aNetworkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface currentNetworkInterface = aNetworkInterfaceEnumeration.nextElement();
            try {
                if (currentNetworkInterface.isLoopback()) {
                    continue;
                }
            } catch (SocketException ex) {
                Logger.getLogger(NebulaUtilities.class.getName()).log(Level.SEVERE, null, ex);
            }

            for (InterfaceAddress currentNetworkInterfaceAddress : currentNetworkInterface.getInterfaceAddresses()) {
                InetAddress currentNetworkInterfaceInetAddress = currentNetworkInterfaceAddress.getAddress();

                if ((theIpAddressType.equals(IpAddressType.IPv4)) && (!(currentNetworkInterfaceInetAddress instanceof Inet4Address))) {
                    continue;
                } else if ((theIpAddressType.equals(IpAddressType.IPv6)) && (!(currentNetworkInterfaceInetAddress instanceof Inet6Address))) {
                    continue;
                }

                return currentNetworkInterfaceInetAddress.getHostAddress();
            }
        }

        return null;
    }

    /**
     * used in specifying what type of IP Address to grab
     */
    public enum IpAddressType {

        IPv4, IPv6
    }
}
