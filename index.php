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

//configure your db
$myDbType = new DbType(DbType::SQLite3);
$myDbHost = 'localhost'; //not needed for SQLite3
$myDbUser = 'NebulaDSS'; //not needed for SQLite3
$myDbPass = 'NebulaDSS'; //not needed for SQLite3
$myDbName = 'NebulaDSS';
$myNodesTable = 'Nodes';
$myFilesTable = 'Files';

//configure the output
$kGlobalDebugIsOn = true;
$kGlobalDebugStrFlag = 'DEBUG';

//misc global internal constants
$myGlobalSqlResourceObject = false;
$kMaxBootStrapNodes = 20;
$kFileNameStr = 'filename';
$kNameSpaceStr = 'namespace';
$kLatencyMaxStr = 'latency_max';
$kBandwidthMinStr = 'bandwidth_min';

//node operation and db names
$kUUIDStr = 'uuid';
$kWebStr = 'http';
$kAddrStr = 'address';
$kIPv4Str = 'ipv4';
$kOperationNat = 'nat';
$kOperationBootStrapStr = 'bootstrap';
$kOperationPingStr = 'ping';
$kOperationRemove = 'remove';
//file operations
$kOperationGet = 'get';
$kOperationPut = 'put';


//entry point of main logic
if (isset($_GET['opt']) && ($_GET['opt'] != '')) {
    $opt = $_GET['opt'];
    if ($opt == $kOperationNat) {
        $address = $_SERVER["REMOTE_ADDR"];
        if (isset($_GET[$kIPv4Str]) && ($_GET[$kIPv4Str] == $address)) {
            //do something?
        }
        echo "opt=nat\nipv4=$address\n";
    } elseif ($opt == $kOperationBootStrapStr) {
        open();
        getBootStrapNodes();
        close();
    } elseif ($opt == $kOperationPingStr) {
        if (isset($_GET[$kWebStr]) && ($_GET[$kWebStr] != '') && isset($_GET[$kUUIDStr]) && ($_GET[$kUUIDStr] != '')) {
            $aUUID = $_GET[$kUUIDStr];
            $address = $_SERVER["REMOTE_ADDR"];
            $aWebPort = $_GET[$kWebStr];
            open();
            if (isset($_GET[$kOperationRemove]) && ($_GET[$kOperationRemove] != '')) { //remove node entry
                removeBootStrapNode($aUUID, $address, $aWebPort);
            } else { //add node entry
                addBootStrapNode($aUUID, $address, $aWebPort);
            }
            close();
        }
    } elseif ($opt == $kOperationGet) {
        if ((isset($_GET[$kFileNameStr]) && ($_GET[$kFileNameStr] != ''))
                && (isset($_GET[$kFileNameStr]) && ($_GET[$kFileNameStr] != ''))
                && (isset($_GET[$kNameSpaceStr]) && ($_GET[$kNameSpaceStr] != ''))
                && (isset($_GET[$kLatencyMaxStr]) && ($_GET[$kLatencyMaxStr] != ''))
                && (isset($_GET[$kBandwidthMinStr]) && ($_GET[$kBandwidthMinStr] != ''))) { //have params for SQL
        } else {
            echo "$kGlobalDebugStrFlag - necessary parameters not present in your GET request";
        }
    } elseif ($kGlobalDebugIsOn) {
        echo "$kGlobalDebugStrFlag - '$opt' is not a recognized operation";
    }
} elseif ($kGlobalDebugIsOn) {
    echo "$kGlobalDebugStrFlag - no 'opt' parameter in your GET request, nothing to do";
}

//dump newline delimited list of available file locations "mirrors"
function getFileUrls($theFileNameStr, $theNameSpaceStr, $theLatencyMax, $theBandwidthMin) {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable;
    $aSqlStr = "SELECT $kAddrStr, $kWebStr FROM $myNodesTable ORDER BY id DESC";
}

/**
 * dump newline delimited bootsrap node list from the database
 * @global DbType $myDbType
 * @global boolean $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @global int $kMaxBootStrapNodes
 */
function getBootStrapNodes() {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable, $kMaxBootStrapNodes, $kAddrStr, $kWebStr;
    $aSqlStr = "SELECT $kAddrStr, $kWebStr FROM $myNodesTable ORDER BY id DESC LIMIT $kMaxBootStrapNodes";
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
 * insert address, port, and uuid into database representing datastore node
 * @global DbType $myDbType
 * @global boolean $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @param string $uuid
 * @param string $address
 * @param int $aWebPort
 */
function addBootStrapNode($uuid, $address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable;
    $aSqlStr = "INSERT INTO $myNodesTable VALUES(NULL, '$uuid', '$address', $aWebPort)";
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->exec($aSqlStr);
    } elseif ($myDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

/**
 * remove a bootstrap node address from the database
 * @global DbType $myDbType
 * @global boolean $myGlobalSqlResourceObject
 * @global string $myNodesTable
 * @param string $uuid 
 * @param string $address
 * @param int $aWebPort
 */
function removeBootStrapNode($uuid, $address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject, $myNodesTable, $kUUIDStr, $kAddrStr, $kWebStr;
    $aSqlStr = "DELETE FROM $myNodesTable WHERE $kUUIDStr='$uuid' AND $kAddrStr='$address' AND $kWebStr=$aWebPort";
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->exec($aSqlStr);
    } elseif ($myDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

/**
 * open connection to db and create table if does not already exist
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myDbName
 * @global string $myNodesTable
 * @global string $myDbHost
 * @global string $myDbUser
 * @global string $myDbPass 
 */
function open() {
    global $myDbType, $myGlobalSqlResourceObject, $myDbName, $myNodesTable, $myDbHost, $myDbUser, $myDbPass, $kUUIDStr, $kAddrStr, $kWebStr;
    $aSqlStr = "CREATE TABLE IF NOT EXISTS $myNodesTable (id INTEGER PRIMARY KEY ASC, $kUUIDStr TEXT, $kAddrStr TEXT, $kWebStr INTEGER)";
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject = new SQLite3($myDbName);
        $myGlobalSqlResourceObject->exec($aSqlStr);
    } elseif ($myDbType == DbType::MySQL) {
        $myGlobalSqlResourceObject = mysql_connect($myDbHost, $myDbUser, $myDbPass);
        mysql_select_db($myDbName, $myGlobalSqlResourceObject);
        mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

/**
 * close up connection to the database
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject 
 */
function close() {
    global $myDbType, $myGlobalSqlResourceObject;
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->close();
    } elseif ($myDbType == DbType::MySQL) {
        mysql_close($myGlobalSqlResourceObject);
    }
}

?>
