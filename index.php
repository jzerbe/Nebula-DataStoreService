<?php

/**
 * master server script for bootstrapping TomP2P DMM network nodes
 * keeps a record of the latest nodes that are up. run on your
 * standard PHP5 and SQLite3 or MySQL compatible webserver
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
$kDbType = new DbType(DbType::SQLite3);
$kDbHost = "localhost"; //not needed for SQLite3
$kDbUser = "NebulaDSS"; //not needed for SQLite3
$kDbPass = "NebulaDSS"; //not needed for SQLite3
$kDbName = "NebulaDSS";
$kDbTable = "bootstrap";

//configure the output
$kDebugIsOn = true;
$kDebugStrFlag = 'DEBUG';

//misc global internal constants
$kMaxBootStrapNodes = 20;
$GlobalSqlResource = false;
$kOperationBootStrap = 'bootstrap';
$kOperationPing = 'ping';

if (isset($_GET['opt']) && ($_GET['opt'] != '')) {
    $opt = $_GET['opt'];
    if ($opt == $kOperationBootStrap) {
        open();
        getBootStrapNodes();
        close();
    } elseif ($opt == $kOperationPing) {
        if ((isset($_GET['udpport']) && ($_GET['udpport'] != ''))
                && (isset($_GET['tcpport']) && ($_GET['tcpport'] != ''))) {
            $address = $_SERVER["REMOTE_ADDR"];
            $udpport = $_GET['udpport'];
            $tcpport = $_GET['tcpport'];
            open();
            if (isset($_GET['remove']) && ($_GET['remove'] != '')) { //remove ping entry
                removeBootStrapNode($address);
            } else { //add host entry
                addBootStrapNode($address, $udpport, $tcpport);
            }
            close();
        }
    } elseif ($kDebugIsOn) {
        echo "$kDebugStrFlag - '$opt' is not a recognized operation";
    }
} elseif ($kDebugIsOn) {
    echo "$kDebugStrFlag - no 'opt' parameter in your GET request, nothing to do";
}

//open connection to db and create table if does not already exist
function open() {
    global $kDbType, $GlobalSqlResource, $kDbName, $kDbTable, $kDbHost, $kDbUser, $kDbPass;
    if ($kDbType == DbType::SQLite3) {
        $GlobalSqlResource = new SQLite3($kDbName);
        $GlobalSqlResource->exec("CREATE TABLE IF NOT EXISTS $kDbTable (id INTEGER PRIMARY KEY ASC, udpport INTEGER, tcpport INTEGER, address TEXT)");
    } elseif ($kDbType == DbType::MySQL) {
        $GlobalSqlResource = mysql_connect($kDbHost, $kDbUser, $kDbPass);
        mysql_select_db($kDbName, $GlobalSqlResource);
    }
}

//get bootsrap node array from the database
function getBootStrapNodes() {
    global $kDbType, $GlobalSqlResource, $kDbTable, $kMaxBootStrapNodes;
    $aSqlStr = "SELECT udpport, address FROM $kDbTable ORDER BY id ASC LIMIT $kMaxBootStrapNodes";
    if ($kDbType == DbType::SQLite3) {
        $results = $GlobalSqlResource->query($aSqlStr);
        while ($row = $results->fetchArray(SQLITE3_ASSOC)) {
            if (!isset($row['address']))
                continue;
            echo $row['address'] . ':' . $row['udpport'] . "\n";
        }
    } elseif ($kDbType == DbType::MySQL) {
        $results = mysql_query($aSqlStr, $GlobalSqlResource) or die(mysql_errno($GlobalSqlResource));
        while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
            if (!isset($row['address']))
                continue;
            echo $row['address'] . ':' . $row['udpport'] . "\n";
        }
    }
}

//insert address and ports into database
function addBootStrapNode($address, $udpport, $tcpport) {
    global $kDbType, $GlobalSqlResource, $kDbTable;
    $aSqlStr = "INSERT INTO $kDbTable VALUES(NULL, $udpport, $tcpport, $address)";
    if ($kDbType == DbType::SQLite3) {
        $GlobalSqlResource->exec($aSqlStr);
    } elseif ($kDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $GlobalSqlResource) or die(mysql_errno());
    }
}

//remove a bootstrap node address from the database
function removeBootStrapNode($address) {
    global $kDbType, $GlobalSqlResource, $kDbTable;
    $aSqlStr = "DELETE FROM $kDbTable WHERE address='$address' LIMIT 1";
    if ($kDbType == DbType::SQLite3) {
        $GlobalSqlResource->exec($aSqlStr);
    } elseif ($kDbType == DbType::MySQL) {
        mysql_query($aSqlStr, $GlobalSqlResource) or die(mysql_errno());
    }
}

//close up connection to the database
function close() {
    global $kDbType, $GlobalSqlResource;
    if ($kDbType == DbType::SQLite3) {
        $GlobalSqlResource->close();
    } elseif ($kDbType == DbType::MySQL) {
        mysql_close($GlobalSqlResource);
    }
}

?>
