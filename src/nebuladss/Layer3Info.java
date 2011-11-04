/*
 * singleton utility class for getting (IP) Network Layer information
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
public class Layer3Info implements ProgramConstants {

    private static Layer3Info l3i_singleInstance = null;
    private boolean l3i_DebugOn = false;
    private boolean l3i_isNAT_CachedBoolean = false;

    protected Layer3Info(boolean theDebugOn) {
        l3i_DebugOn = theDebugOn;
    }

    public static Layer3Info getInstance() {
        if (l3i_singleInstance == null) {
            l3i_singleInstance = new Layer3Info(false);
        }
        return l3i_singleInstance;
    }

    public static Layer3Info getInstance(boolean theDebugOn) {
        if (l3i_singleInstance == null) {
            l3i_singleInstance = new Layer3Info(theDebugOn);
        }
        return l3i_singleInstance;
    }

    /**
     * check if this host's address is able to be routed on the WAN
     * @return boolean - is behind a NAT, needs port forwarding
     */
    public boolean isHostBehindNAT() {
        if (l3i_isNAT_CachedBoolean) {
            return l3i_isNAT_CachedBoolean;
        }

        String aIPv4AddrStr = getValidIPAddress(IpAddressType.IPv4);
        if (aIPv4AddrStr == null) {
            return false;
        }

        if (l3i_DebugOn) {
            System.out.println("LocalIPv4 Address = " + aIPv4AddrStr);
        }

        String isValidExternalAddressStr = "ipv4=" + aIPv4AddrStr;
        String aURLConnectionParamStr = "opt=nat" + "&" + isValidExternalAddressStr;
        ArrayList<String> returnArrayList = HttpCmdClient.getInstance().returnServerMethod(
                HttpCmdClient.getInstance().getMasterSeverUrlStr(), aURLConnectionParamStr);
        if (returnArrayList.contains(isValidExternalAddressStr)) {
            return false;
        } else {
            l3i_isNAT_CachedBoolean = true;
            return true;
        }
    }

    /**
     * gets a valid IP Address from the local machine that is able to be routed
     * @return String
     */
    public String getValidIPAddress(IpAddressType theIpAddressType) {
        Enumeration<NetworkInterface> aNetworkInterfaceEnumeration = null;
        try {
            aNetworkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            Logger.getLogger(Layer3Info.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (aNetworkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface currentNetworkInterface = aNetworkInterfaceEnumeration.nextElement();
            try {
                if (currentNetworkInterface.isLoopback()) {
                    continue;
                }
            } catch (SocketException ex) {
                Logger.getLogger(Layer3Info.class.getName()).log(Level.SEVERE, null, ex);
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
