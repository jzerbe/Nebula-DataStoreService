<?php

/**
 * master server script for coordinating NebulaDSS network nodes:
 * keeps records of all nodes currently online (locations, latencies, etc.)
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

//load in the database helper/setup functions: open/close
require('dbfunctions.php');

//how often (seconds) should periodic latency and bandwidth checks kick off?
$kTaskTimerSeconds = 180;

//misc global internal constants
$myGlobalSqlResourceObject = false;
$kMaxFetchNodes = 20;
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
$kOptStr = 'opt';
$kIPv4Str = 'ipv4';
$kOperationNat = 'nat';
$kOperationPeriodicTask = 'schedule';
$kOperationGetOnlineStr = 'online';
$kOperationGetOfflineStr = 'offline';
$kOperationPingStr = 'ping';
$kOperationPingDownStr = 'down';

//file operations
$kOperationGet = 'get';
$kOperationPut = 'put';


/** main logic follows - do not touch unless you know what you are doing */
if (isset($_GET[$kOptStr]) && ($_GET[$kOptStr] != '')) {
    $opt = $_GET[$kOptStr];
    if ($opt == $kOperationNat) { //NAT discovery
        $address = $_SERVER["REMOTE_ADDR"];
        if (isset($_GET[$kIPv4Str]) && ($_GET[$kIPv4Str] == $address)) {
            //do something when there is a match?
        }
        echo "$kOptStr=$kOperationNat\n$kIPv4Str=$address\n";
    } elseif ($opt == $kOperationPeriodicTask) { //how often should periodic checks run?
        echo "$kOptStr=$kOperationPeriodicTask\n$kOperationPeriodicTask=$kTaskTimerSeconds\n";
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
    echo "$kGlobalDebugStrFlag - no $kOptStr parameter in your GET request, nothing to do";
}


/** database functions follow - do not touch unless you know what you are doing */

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
 * newline dump of ($kMaxFetchNodes) up nodes (address:port) with high-probablity of staying up
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global string $kUUIDStr
 * @global string $kOnlineStr
 * @global int $kMaxFetchNodes
 * @global string $myUptimeTable
 * @global string $kOnlineTimeStr
 * @global string $kOfflineTimeStr 
 */
function getOnlineNodes() {
    global $myDbType, $myGlobalSqlResourceObject; //GLOBAL DB INFO
    global $myNodesTable, $kUUIDStr, $kOnlineStr, $kMaxFetchNodes; //Node
    global $myUptimeTable, $kOnlineTimeStr, $kOfflineTimeStr; //Uptime

    $aSqlStr = "SELECT Up.$kUUIDStr AS UptimeUUID, Up.$kOnlineTimeStr AS "
            + "OnlineTimeStamp, Up.$kOfflineTimeStr AS OfflineTimeStamp, "
            + "OfflineTimeStamp - OnlineTimeStamp AS UptimeDiff FROM "
            + "$myNodesTable N, $myUptimeTable Up WHERE Up.$kUUIDStr = "
            + "N.$kUUIDStr AND N.$kOnlineStr=1";
    //select from this "Temp" select ($kMaxFetchNodes)
    //TODO: create select query with outputs: $kAddrStr, $kWebStr

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
 * set the new/existing node to be online (add to/update the db)
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global string $kUUIDStr
 * @global string $kAddrStr
 * @global string $kOnlineStr
 * @global string $myUptimeTable
 * @param type $uuid
 * @param type $address
 * @param type $aWebPort 
 */
function setNodeOnline($uuid, $address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject; //GLOBAL DB INFO
    global $myNodesTable, $kUUIDStr, $kAddrStr, $kOnlineStr; //Node
    global $myUptimeTable; //Uptime

    $aSqlCountStr = "SELECT COUNT(*) FROM $myNodesTable WHERE "
            + "$kUUIDStr='$uuid' AND $kAddrStr='$address'"; //how many nodes exactly match?
    $aSqlUpdateStr = "UPDATE $myNodesTable SET $kOnlineStr=1 WHERE "
            + "$kUUIDStr='$uuid' AND $kAddrStr='$address'"; //set the node online
    $aSqlInsertStr = "INSERT INTO $myNodesTable VALUES(NULL, '$uuid', "
            + "'$address', $aWebPort, 1)"; //create a node entry if not seen before

    $aDateSortableStr = date("YmdHis"); //set the sortable date to be exactly now
    $aSqlUptimeStr = "INSERT INTO $myUptimeTable VALUE(NULL, '$uuid', "
            + "$aDateSortableStr, 0)"; //insert a new session start

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
 * newline dump of ($kMaxFetchNodes) offline nodes (address:port), ordered by creation
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global string $kAddrStr
 * @global string $kWebStr
 * @global string $kOnlineStr
 * @global int $kMaxFetchNodes 
 */
function getOfflineNodes() {
    global $myDbType, $myGlobalSqlResourceObject; //GLOBAL DB INFO
    global $myNodesTable, $kAddrStr, $kWebStr, $kOnlineStr, $kMaxFetchNodes; //Nodes

    $aSqlStr = "SELECT $kAddrStr, $kWebStr FROM $myNodesTable WHERE "
            + "$kOnlineStr=0 ORDER BY id DESC LIMIT $kMaxFetchNodes";

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
 * set the existing node to be online (update the db)
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global string $kUUIDStr
 * @global string $kAddrStr
 * @global string $kOnlineStr
 * @global string $myUptimeTable
 * @global string $kOfflineTimeStr
 * @param string $uuid
 * @param string $address
 * @param int $aWebPort 
 */
function setNodeOffline($uuid, $address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject;
    global $myNodesTable, $kUUIDStr, $kAddrStr, $kOnlineStr; //Node table
    global $myUptimeTable, $kOfflineTimeStr; //Uptime table

    $aSqlStr = "UPDATE $myNodesTable SET $kOnlineStr=0 WHERE $kUUIDStr='$uuid' "
            + "AND $kAddrStr='$address'";

    $aDateSortableStr = date("YmdHis");
    $aSqlUptimeStr = "UPDATE $myUptimeTable SET "
            + "$kOfflineTimeStr=$aDateSortableStr WHERE $kUUIDStr='$uuid' "
            + "AND $kOfflineTimeStr=0";

    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->exec($aSqlStr);
    } elseif ($myDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

?>
