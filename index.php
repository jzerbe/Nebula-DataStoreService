<?php

//enumerator emulation for tracking what db we are going to use
class DbType extends SplEnum {
    const __default = self::SQLite3;

    const SQLite3 = 1;
    const MySQL = 2;
}

//configure your db
$kDbType = new DbType(DbType::SQLite3);
$kDbName = "NebulaDSS";
$kDbTable = "bootstrap";

if ($kDbType == DbType::SQLite3) {
    $mySqLite3Db = new SQLite3($kDbName);
    $mySqLite3Db->exec("CREATE TABLE IF NOT EXISTS $kDbTable (id INTEGER PRIMARY KEY ASC, udpport INTEGER, tcpport INTEGER, ipv4address TEXT, ipv6address TEXT)");
}
?>