<?php

//load in the database configuration
require('dbconfig.php');

/**
 * open a connection to the SQL database and create tables if they do not exist
 * @global DbType $myDbType
 * @global SQL_LINK $myGlobalSqlResourceObject
 * @global string $myDbPath
 * @global string $myDbName
 * @global string $myNodesTable
 * @global string $kUUIDStr
 * @global string $kAddrStr
 * @global string $kWebStr
 * @global string $kOnlineStr
 * @global string $myUptimeTable
 * @global string $kOnlineTimeStr
 * @global string $kOfflineTimeStr
 * @global string $myFilesTable
 * @global string $kNameSpaceStr
 * @global string $kFileNameStr
 * @global string $kVersionStr 
 */
function open() {
    global $myDbType, $myGlobalSqlResourceObject, $myDbPath, $myDbName; //GLOBAL DB INFO
    global $myNodesTable, $kUUIDStr, $kAddrStr, $kWebStr, $kOnlineStr; //Node
    global $myUptimeTable, $kOnlineTimeStr, $kOfflineTimeStr; //Uptime info
    global $myFilesTable, $kNameSpaceStr, $kFileNameStr, $kVersionStr; //Files

    $aSqlNodesTableStr = "CREATE TABLE IF NOT EXISTS $myNodesTable "
            . "(id INTEGER PRIMARY KEY ASC, $kUUIDStr TEXT, $kAddrStr TEXT, "
            . "$kWebStr INTEGER, $kOnlineStr INTEGER)";
    $aSqlUptimeTableStr = "CREATE TABLE IF NOT EXISTS $myUptimeTable (id "
            . "INTEGER PRIMARY KEY ASC, $kUUIDStr TEXT, $kOnlineTimeStr "
            . "INTEGER, $kOfflineTimeStr INTEGER)";
    $aSqlFilesTableStr = "CREATE TABLE IF NOT EXISTS $myFilesTable (id "
            . "INTEGER PRIMARY KEY ASC, $kUUIDStr TEXT, $kNameSpaceStr TEXT, "
            . "$kFileNameStr TEXT, $kVersionStr INTEGER)";

    if ($myDbType == DbType::SQLite3) {
        $myGlobalSqlResourceObject = new SQLite3("$myDbPath/$myDbName");
        $myGlobalSqlResourceObject->exec($aSqlNodesTableStr);
        $myGlobalSqlResourceObject->exec($aSqlUptimeTableStr);
        $myGlobalSqlResourceObject->exec($aSqlFilesTableStr);
    } elseif ($myDbType == DbType::MySQL) {
        $myGlobalSqlResourceObject = mysql_connect($myDbHost, $myDbUser, $myDbPass);
        mysql_select_db($myDbName, $myGlobalSqlResourceObject);
        mysql_query($aSqlNodesTableStr, $myGlobalSqlResourceObject)
                or die(mysql_errno());
        mysql_query($aSqlUptimeTableStr, $myGlobalSqlResourceObject)
                or die(mysql_errno());
        mysql_query($aSqlFilesTableStr, $myGlobalSqlResourceObject)
                or die(mysql_errno());
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
