<?php

/**
 * master server script for coordinating NebulaDSS network nodes
 * keeps a record of all the node uptime sessions, locations, latencies
 * run on your standard PHP5 and SQLite3 or MySQL compatible webserver
 *
 * if you are using MySQL be sure to create the database and give
 * a user read/write permissions to the database
 */

/**
 * enumerator emulation for tracking what db we are going to use.
 * probably should not edit this, but has to be up here because
 * of how PHP sequentially loads variables
 */
class DbType extends SplEnum {
    const __default = self::SQLite3;

    const SQLite3 = 1;
    const MySQL = 2;
}

//configure your db
$myDbType = new DbType(DbType::SQLite3);
$myDbHost = "localhost"; //not needed for SQLite3
$myDbUser = "NebulaDSS"; //not needed for SQLite3
$myDbPass = "NebulaDSS"; //not needed for SQLite3
$myDbName = "NebulaDSS";
$myDbTable = "Nodes";

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

//operation and db names
$kWebStr = 'http';
$kAddrStr = 'address';
$kOperationBootStrapStr = 'bootstrap';
$kOperationPingStr = 'ping';
$kOperationGet = 'get';
$kOperationRemove = 'remove';


//entry point of main logic
if (isset($_GET['opt']) && ($_GET['opt'] != '')) {
    $opt = $_GET['opt'];
    if ($opt == $kOperationBootStrapStr) {
        open();
        getBootStrapNodes();
        close();
    } elseif ($opt == $kOperationPingStr) {
        if (isset($_GET[$kWebStr]) && ($_GET[$kWebStr] != '')) {
            $address = $_SERVER["REMOTE_ADDR"];
            $aWebPort = $_GET[$kWebStr];
            open();
            if (isset($_GET[$kOperationRemove]) && ($_GET[$kOperationRemove] != '')) { //remove ping entry
                removeBootStrapNode($address);
            } else { //add host entry
                addBootStrapNode($address, $aWebPort);
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

//open connection to db and create table if does not already exist
function open() {
    global $myDbType, $myGlobalSqlResourceObject, $myDbName, $myDbTable, $myDbHost, $myDbUser, $myDbPass;
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject = new SQLite3($myDbName);
        $myGlobalSqlResourceObject->exec("CREATE TABLE IF NOT EXISTS $myDbTable (id INTEGER PRIMARY KEY ASC, $kWebStr INTEGER, $kAddrStr TEXT)");
    } elseif ($myDbType == DbType::MySQL) {
        $myGlobalSqlResourceObject = mysql_connect($myDbHost, $myDbUser, $myDbPass);
        mysql_select_db($myDbName, $myGlobalSqlResourceObject);
    }
}

//dump newline delimited list of available file locations "mirrors"
function getFileUrls($theFileNameStr, $theNameSpaceStr, $theLatencyMax, $theBandwidthMin) {
    global $myDbType, $myGlobalSqlResourceObject, $myDbTable;
    $aSqlStr = "SELECT $kAddrStr, $kWebStr FROM $myDbTable ORDER BY id DESC";
}

/**
 * dump newline delimited bootsrap node list from the database
 * @global DbType $myDbType
 * @global boolean $myGlobalSqlResourceObject
 * @global string $myDbTable
 * @global int $kMaxBootStrapNodes 
 */
function getBootStrapNodes() {
    global $myDbType, $myGlobalSqlResourceObject, $myDbTable, $kMaxBootStrapNodes;
    $aSqlStr = "SELECT $kAddrStr, $kWebStr FROM $myDbTable ORDER BY id DESC LIMIT $kMaxBootStrapNodes";
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
 * insert address and port into database representing datastore node
 * @global DbType $myDbType
 * @global boolean $myGlobalSqlResourceObject
 * @global string $myDbTable
 * @param string $address
 * @param int $aWebPort 
 */
function addBootStrapNode($address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject, $myDbTable;
    $aSqlStr = "INSERT INTO $myDbTable VALUES(NULL, $aWebPort, $address)";
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->exec($aSqlStr);
    } elseif ($myDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

//remove a bootstrap node address from the database
function removeBootStrapNode($address, $aWebPort) {
    global $myDbType, $myGlobalSqlResourceObject, $myDbTable;
    $aSqlStr = "DELETE FROM $myDbTable WHERE $kAddrStr='$address' AND $kWebStr='$aWebPort' LIMIT 1";
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->exec($aSqlStr);
    } elseif ($myDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $myGlobalSqlResourceObject) or die(mysql_errno());
    }
}

//close up connection to the database
function close() {
    global $myDbType, $myGlobalSqlResourceObject;
    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject->close();
    } elseif ($myDbType == DbType::MySQL) {
        mysql_close($myGlobalSqlResourceObject);
    }
}

?>
