<?php

/**
 * master server script for coordinating NebulaDSS network nodes
 * keeps records of all nodes currently online: locations, latencies
 *
 * run on your standard PHP5 and SQLite3 or MySQL compatible webserver
 *
 * if you are using MySQL be sure to give a user read/write/create table
 * permissions on the database you intend to use, same goes for
 * filesystem perms on the SQLite3 DB file
 */

/**
 * enumerator emulation for tracking what db we are going to use.
 * probably should not edit this, but has to be up here because
 * of how PHP sequentially loads variables
 */
class DbType { //extends SplEnum { //not enabled in most dists yet
    //const __default = self::SQLite3; //not enable in most dists yet
    const SQLite3 = 1;
    const MySQL = 2;
}

//load in the database configuration
require('config.php');

//load in the database helper functions: open/close
require('dbfunctions.php');

//misc global internal constants
$myGlobalSqlResourceObject = false;
$kMaxFetchOnlineNodes = 20;
$kMinOnlineSessions = 3;

//node storage field names
$kUUIDStr = 'uuid';
$kWebStr = 'http';
$kAddrStr = 'address';
$kOnlineStr = 'online';
//Uptime field names
$kOnlineTimeStr = 'onlineTimeInt';
$kOfflineTimeStr = 'offlineTimeInt';
//file storage field names
$kFileNameStr = 'filename';
$kNameSpaceStr = 'namespace';
$kVersionStr = 'version';

//main logic constants
$kIPv4Str = 'ipv4';
$kOperationNat = 'nat';
$kOperationGetOnlineStr = 'online';
$kOperationGetOfflineStr = 'offline';
$kOperationPingStr = 'ping';
$kOperationPingDownStr = 'down';

//file operations
$kOperationGet = 'get';
$kOperationPut = 'put';


//entry point of main logic
if (isset($_GET['opt']) && ($_GET['opt'] != '')) {
    $opt = $_GET['opt'];
    if ($opt == $kOperationNat) { //NAT discovery
        $address = $_SERVER["REMOTE_ADDR"];
        if (isset($_GET[$kIPv4Str]) && ($_GET[$kIPv4Str] == $address)) {
            //do something when there is a match?
        }
        echo "opt=nat\nipv4=$address\n";
    } elseif ($opt == $kOperationGetOnlineStr) { //get a list of online nodes
        open();
        getOnlineNodes();
        close();
    } elseif ($opt == $kOperationGetOfflineStr) { //get a list of offline nodes
        open();
        getOfflineNodes();
        close();
    } elseif ($opt == $kOperationPingStr) { //allow a Node to notify that it is online or offline
        if (isset($_GET[$kWebStr]) && ($_GET[$kWebStr] != '') && isset($_GET[$kUUIDStr]) && ($_GET[$kUUIDStr] != '')) {
            $aUUID = $_GET[$kUUIDStr];
            $address = $_SERVER["REMOTE_ADDR"];
            $aWebPort = $_GET[$kWebStr];
            open();
            if (isset($_GET[$kOperationPingDownStr]) && ($_GET[$kOperationPingDownStr] != '')) { //set node entry offline
                setNodeOffline($aUUID, $address, $aWebPort);
            } else { //add node entry/set node online
                setNodeOnline($aUUID, $address, $aWebPort);
            }
            close();
        }
    } elseif ($opt == $kOperationPut) { //notify master server that a certain file exists on a certain node
        if ((isset($_GET[$kFileNameStr]) && ($_GET[$kFileNameStr] != ''))
                && (isset($_GET[$kFileNameStr]) && ($_GET[$kFileNameStr] != ''))
                && (isset($_GET[$kNameSpaceStr]) && ($_GET[$kNameSpaceStr] != ''))
                && (isset($_GET[$kVersionStr]) && ($_GET[$kVersionStr] != ''))
                && (isset($_GET[$kUUIDStr]) && ($_GET[$kUUIDStr] != ''))) { //have params for SQL
            open();
            putFile($_GET[$kUUIDStr], $_GET[$kNameSpaceStr], $_GET[$kFileNameStr], $_GET[$kVersionStr]);
            close();
        } else {
            echo "$kGlobalDebugStrFlag - '$opt' does not have necessary parameters present in your GET request";
        }
    } elseif ($opt == $kOperationGet) { //grab newline delimited dump of nodes with file (URLs)
        if ((isset($_GET[$kFileNameStr]) && ($_GET[$kFileNameStr] != ''))
                && (isset($_GET[$kFileNameStr]) && ($_GET[$kFileNameStr] != ''))
                && (isset($_GET[$kNameSpaceStr]) && ($_GET[$kNameSpaceStr] != ''))
                && (isset($_GET[$kVersionStr]) && ($_GET[$kVersionStr] != ''))) { //have params for SQL
            //do query
        } else {
            echo "$kGlobalDebugStrFlag - '$opt' does not have necessary parameters present in your GET request";
        }
    } elseif ($kGlobalDebugIsOn) { //opt not found
        echo "$kGlobalDebugStrFlag - '$opt' is not a recognized operation";
    }
} elseif ($kGlobalDebugIsOn) {
    echo "$kGlobalDebugStrFlag - no 'opt' parameter in your GET request, nothing to do";
}

/**
 * record that a certain node now contains a copy of a certain file
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global type $myFilesTable
 * @global string $kUUIDStr
 * @global string $kNameSpaceStr
 * @global string $kFileNameStr
 * @global string $kVersionStr
 * @param string $theNodeUUID
 * @param string $theNameSpaceStr
 * @param string $theFileNameStr
 * @param int $theVersionInt 
 */
function putFile($theNodeUUID, $theNameSpaceStr, $theFileNameStr, $theVersionInt) {
    global $myDbType, $myGlobalSqlResourceObject; //GLOBAL DB INFO
    global $myFilesTable, $kUUIDStr, $kNameSpaceStr, $kFileNameStr, $kVersionStr; //File table specific

    $aSqlCountStr = "SELECT COUNT($kUUIDStr) FROM $myFilesTable "
            + " WHERE $kUUIDStr='$theNodeUUID' AND "
            + "$kNameSpaceStr='$theNameSpaceStr' AND "
            + "$kFileNameStr='$theFileNameStr' AND $kVersionStr=$theVersionInt";
    $aSqlInsertStr = "INSERT INTO $myFilesTable VALUES(NULL, '$theNodeUUID', "
            + "'$theNameSpaceStr', '$theFileNameStr', $theVersionInt)";

    if ($myDbType == DbType::SQLite3) {
        $count = $myGlobalSqlResourceObject->querySingle($aSqlCountStr);
        if ($count < 1) {
            $myGlobalSqlResourceObject->exec($aSqlInsertStr);
        }
    } elseif ($myDbType == DbType::MySQL) {
        $result = mysql_query($aSqlCountStr, $myGlobalSqlResourceObject) or die(mysql_errno());
        $count = mysql_result($result, 0);
        if ($count < 1) {
            mysql_query($aSqlInsertStr, $myGlobalSqlResourceObject) or die(mysql_errno());
        }
    }
}

/**
 * dump newline delimited online node list from the database ordered by uptime
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global int $kMaxFetchOnlineNodes
 * @global string $kAddrStr
 * @global string $kWebStr
 * @global string $kOnlineStr
 * @global string $myUptimeTable
 * @global string $kUUIDStr
 * @global int $kMinOnlineSessions
 * @global string $kOnlineTimeStr
 * @global string $kOfflineTimeStr
 */
function getOnlineNodes() {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable, $kMaxFetchOnlineNodes, $kAddrStr, $kWebStr, $kOnlineStr, $myUptimeTable, $kUUIDStr, $kMinOnlineSessions, $kOnlineTimeStr, $kOfflineTimeStr;
    $aSqlStr = "SELECT Up.$kUUIDStr AS UptimeUUID, Up.$kOnlineTimeStr AS OnlineTimeStamp, Up.$kOfflineTimeStr AS OfflineTimeStamp, OfflineTimeStamp - OnlineTimeStamp AS UptimeDiff FROM $myNodesTable N, $myUptimeTable Up WHERE Up.$kUUIDStr = N.$kUUIDStr AND N.$kOnlineStr=1";
    //select from this "Temp" select
    if ($myDbType == DbType::SQLite3) {
        $results = $myGlobalSqlResourceObject->query($aSqlStr);
        while ($row = $results->fetchArray(SQLITE3_ASSOC)) {
            if (!isset($row[$kAddrStr]))
                continue;
            echo $row[$kAddrStr] . ':' . $row[$kWebStr] . "\n";
        }
    } elseif ($myDbType == DbType::MySQL) {
        $results = mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno($myGlobalSqlResourceObject));
        while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
            if (!isset($row[$kAddrStr]))
                continue;
            echo $row[$kAddrStr] . ':' . $row[$kWebStr] . "\n";
        }
    }
}

/**
 * dump newline delimited offline node list from the database
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global int $kMaxFetchOnlineNodes
 * @global string $kAddrStr
 * @global string $kWebStr
 * @global string $kOnlineStr
 */
function getOfflineNodes() {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable, $kMaxFetchOnlineNodes, $kAddrStr, $kWebStr, $kOnlineStr;
    $aSqlStr = "SELECT $kAddrStr, $kWebStr FROM $myNodesTable WHERE $kOnlineStr=0 ORDER BY id DESC LIMIT $kMaxFetchOnlineNodes";
    if ($myDbType == DbType::SQLite3) {
        $results = $myGlobalSqlResourceObject->query($aSqlStr);
        while ($row = $results->fetchArray(SQLITE3_ASSOC)) {
            if (!isset($row[$kAddrStr]))
                continue;
            echo $row[$kAddrStr] . ':' . $row[$kWebStr] . "\n";
        }
    } elseif ($myDbType == DbType::MySQL) {
        $results = mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno($myGlobalSqlResourceObject));
        while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
            if (!isset($row[$kAddrStr]))
                continue;
            echo $row[$kAddrStr] . ':' . $row[$kWebStr] . "\n";
        }
    }
}

/**
 * insert node tuple into database or set an existing node tuple to be online
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global string $kUUIDStr
 * @global string $kAddrStr
 * @global string $kWebStr
 * @global string $kOnlineStr
 * @global string $myUptimeTable
 * @param type $uuid
 * @param type $address
 * @param type $aWebPort
 */
function setNodeOnline($uuid, $address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable, $kUUIDStr, $kAddrStr, $kWebStr, $kOnlineStr, $myUptimeTable;
    $aSqlCountStr = "SELECT COUNT($kUUIDStr) FROM $myNodesTable WHERE $kUUIDStr='$uuid' AND $kAddrStr='$address'";
    $aSqlUpdateStr = "UPDATE $myNodesTable SET $kOnlineStr=1 WHERE $kUUIDStr='$uuid' AND $kAddrStr='$address'";
    $aSqlInsertStr = "INSERT INTO $myNodesTable VALUES(NULL, '$uuid', '$address', $aWebPort, 1)";
    $aDateSortableStr = date("YmdHis");
    $aSqlUptimeStr = "INSERT INTO $myUptimeTable VALUE(NULL, '$uuid', $aDateSortableStr, 0)";
    if ($myDbType == DbType::SQLite3) {
        $count = $myGlobalSqlResourceObject->querySingle($aSqlCountStr);
        if ($count > 0) {
            $myGlobalSqlResourceObject->exec($aSqlUpdateStr);
        } else {
            $myGlobalSqlResourceObject->exec($aSqlInsertStr);
        }
        $myGlobalSqlResourceObject->exec($aSqlUptimeStr);
    } elseif ($myDbType == DbType::MySQL) {
        $result = mysql_query($aSqlCountStr, $myGlobalSqlResourceObject) or die(mysql_errno());
        $count = mysql_result($result, 0);
        if ($count > 0) {
            mysql_query($aSqlUpdateStr, $myGlobalSqlResourceObject) or die(mysql_errno());
        } else {
            mysql_query($aSqlInsertStr, $myGlobalSqlResourceObject) or die(mysql_errno());
        }
        mysql_query($aSqlUptimeStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

/**
 * set a node to be offline in the database
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global string $kUUIDStr
 * @global string $kAddrStr
 * @global string $kWebStr
 * @global string $kOnlineStr
 * @global string $myUptimeTable
 * @global string $kOnlineTimeStr
 * @global string $kOfflineTimeStr
 * @param type $uuid
 * @param type $address
 * @param type $aWebPort
 */
function setNodeOffline($uuid, $address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable, $kUUIDStr, $kAddrStr, $kWebStr, $kOnlineStr, $myUptimeTable, $kOnlineTimeStr, $kOfflineTimeStr;
    $aSqlStr = "UPDATE $myNodesTable SET $kOnlineStr=0 WHERE $kUUIDStr='$uuid' AND $kAddrStr='$address'";
    $aDateSortableStr = date("YmdHis");
    $aSqlUptimeStr = "UPDATE $myUptimeTable SET $kOfflineTimeStr=$aDateSortableStr WHERE $kUUIDStr='$uuid' AND $kOfflineTimeStr=0";
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->exec($aSqlStr);
    } elseif ($myDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

?>
