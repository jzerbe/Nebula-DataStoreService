<?php

//configure your db
$myDbType = new DbType(DbType::SQLite3);
$myDbHost = 'localhost'; //not needed for SQLite3
$myDbUser = 'NebulaDSS'; //not needed for SQLite3
$myDbPass = 'NebulaDSS'; //not needed for SQLite3
$myDbPath = 'db'; //not needed for MySQL, no trailing '/', relative to this file
$myDbName = 'NebulaDSS'; //will be the filename of db in SQLite3
//table names
$myNodesTable = 'Nodes'; //for storing address/port/uuid of Node
$myUptimeTable = 'Uptime'; //stores length of online seesions
$myFilesTable = 'Files'; //stores filename, namespace, version, and Node uuid

//configure the script output
$kGlobalDebugIsOn = true;
$kGlobalDebugStrFlag = 'DEBUG';
?>
