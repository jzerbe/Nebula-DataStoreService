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
public class NebulaUtilities implements ProgramConstants {

    private static NebulaUtilities nu_singleInstance = null;
    private boolean nu_DebugOn = false;
    private boolean nu_isLocalHostBehindNATCache = false;
    private int nu_TaskTimerSeconds = kDefaultTaskTimerSeconds;

    protected NebulaUtilities(boolean theDebugOn) {
        nu_DebugOn = theDebugOn;
    }

    public static NebulaUtilities getInstance() {
        if (nu_singleInstance == null) {
            nu_singleInstance = new NebulaUtilities(false);
        }
        return nu_singleInstance;
    }

    public static NebulaUtilities getInstance(boolean theDebugOn) {
        if (nu_singleInstance == null) {
            nu_singleInstance = new NebulaUtilities(theDebugOn);
        }
        return nu_singleInstance;
    }

    /**
     * check if this host's address is able to be routed on the WAN
     * @return boolean - is behind a NAT, needs port forwarding
     */
    public boolean isLocalHostBehindNAT() {
        if (nu_isLocalHostBehindNATCache) {
            return nu_isLocalHostBehindNATCache;
        }

        String aIPv4AddrStr = getLocalIPAddress(IpAddressType.IPv4);
        if (aIPv4AddrStr == null) {
            return false;
        }

        if (nu_DebugOn) {
            System.out.println("LocalIPv4 Address = " + aIPv4AddrStr);
        }

        String isValidExternalAddressStr = "ipv4=" + aIPv4AddrStr;
        String aURLConnectionParamStr = "opt=nat" + "&" + isValidExternalAddressStr;
        ArrayList<String> returnArrayList = MasterServer.getInstance().returnServerMethod(aURLConnectionParamStr);
        if (returnArrayList.contains(isValidExternalAddressStr)) {
            return false;
        } else {
            nu_isLocalHostBehindNATCache = true;
            return true;
        }
    }

    /**
     * set up how often periodic latency and bandwidth checks should run
     * based on returned master server settings
     */
    protected void setPeriodTaskTime() {
        String aOperationPeriodicTask = "schedule";
        String aURLConnectionParamStr = "opt=" + aOperationPeriodicTask;
        ArrayList<String> returnArrayList = MasterServer.getInstance().returnServerMethod(aURLConnectionParamStr);
        if (returnArrayList.contains(aOperationPeriodicTask)) {
            for (String returnElement : returnArrayList) {
                if (returnElement.contains(aOperationPeriodicTask)) {
                    String returnElementArray[] = returnElement.split(kMasterServerReturnStringSplitStr);
                    nu_TaskTimerSeconds = Integer.getInteger(returnElementArray[1]);
                }
            }
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
