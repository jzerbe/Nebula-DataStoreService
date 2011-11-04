<?php

/**
 * file stores all the logic and database constants
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

//configure the script output
$kGlobalDebugIsOn = true;
$kGlobalDebugStrFlag = 'DEBUG';

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
?>