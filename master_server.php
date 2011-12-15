<?php

header("Access-Control-Allow-Origin: *"); //http://enable-cors.org/#how-php

/**
 * SQLite3 database and table setup
 */
$SQLite3_conn = new SQLite3('NebulaDSS.db');
$aSqlBoolRet = $SQLite3_conn->exec("PRAGMA foreign_keys = ON");
if (!$aSqlBoolRet) {
    die($SQLite3_conn->lastErrorMsg);
}

$aSqlCreateNodeTable = "CREATE TABLE IF NOT EXISTS Nodes "
        . "(id INTEGER PRIMARY KEY ASC, uuid TEXT, "
        . "address TEXT, http INTEGER, online INTEGER)";
$aSqlBoolRet = $SQLite3_conn->exec($aSqlCreateNodeTable);
if (!$aSqlBoolRet) {
    die($SQLite3_conn->lastErrorMsg);
}

$aSqlCreateFilesTable = "CREATE TABLE IF NOT EXISTS Files "
        . "(id INTEGER PRIMARY KEY ASC, uuid TEXT, "
        . "namespace TEXT, filename TEXT)";
$aSqlBoolRet = $SQLite3_conn->exec($aSqlCreateFilesTable);
if (!$aSqlBoolRet) {
    die($SQLite3_conn->lastErrorMsg);
}

$aSqlCreateGroupKeyTable = "CREATE TABLE IF NOT EXISTS GroupKeys "
        . "(id INTEGER PRIMARY KEY ASC, key TEXT, "
        . "node_id INTEGER, FOREIGN KEY(node_id) REFERENCES Nodes(id))";
$aSqlBoolRet = $SQLite3_conn->exec($aSqlCreateGroupKeyTable);
if (!$aSqlBoolRet) {
    die($SQLite3_conn->lastErrorMsg);
}

/**
 * the main logic
 */
if (isset($_GET['opt']) && ($_GET['opt'] != '')) {
    $opt = $_GET['opt'];
    if ($opt == 'address') {
        echo 'address=' . $_SERVER["REMOTE_ADDR"] . "\n";
    } elseif ($opt == 'get') {
        $aReturnArray = getFileHostRecords($SQLite3_conn, $_GET['namespace'], $_GET['filename']);
        if ($aReturnArray && (sizeof($aReturnArray) > 0)) {
            for ($i = 0; $i < sizeof($aReturnArray); $i++) {
                $aHostStr = 'http://' . $aReturnArray[$i]['address'] . ':' . $aReturnArray[$i]['http']
                        . '/files?namespace=' . $aReturnArray[$i]['namespace'] . '&filename='
                        . $aReturnArray[$i]['filename'];
                if (isset($_GET['redir']) && ($_GET['redir'] != '')) {
                    header("Location: $aHostStr");
                    die(); //paranoid coding - do not want disturb headers
                }
                echo $aHostStr . "\n";
            }
        }
    } elseif ($opt == 'latency') {
        echo "latency=true\n";
    } elseif ($opt == 'online-uuid') {
        if (getNodeIsOnline($SQLite3_conn, $_GET['uuid'])) {
            echo "online-uuid=" . $_GET['uuid'] . "\n";
        } else {
            echo "offline-uuid=" . $_GET['uuid'] . "\n";
        }
    } elseif ($opt == 'online') {
        $aReturnArray = getNodesOnline($SQLite3_conn);
        if ($aReturnArray) {
            for ($i = 0; $i < sizeof($aReturnArray); $i++) {
                echo 'http://' . $aReturnArray[$i]['address'] . ':' . $aReturnArray[$i]['http']
                . "/files\n";
            }
        }
    } elseif ($opt == 'ping') {
        if (isset($_GET['remove']) && ($_GET['remove'] != '')) {
            setNodeIsOffline($SQLite3_conn, $_GET['uuid']);
        } else {
            setNodeIsOnline($SQLite3_conn, $_GET['uuid'], $_SERVER["REMOTE_ADDR"], $_GET['http']);
        }
    } elseif ($opt == 'set') {
        addFileRecord($SQLite3_conn, $_GET['uuid'], $_GET['namespace'], $_GET['filename']);
    }
} else {
    echo "ERROR - no opt\n";
}

/**
 * tell the master server that a certain nodes has a certain file
 * @param SQLite3 $SQLite3_conn
 * @param string $theUUID
 * @param string $theNameSpace
 * @param string $theFileName
 */
function addFileRecord($SQLite3_conn, $theUUID, $theNameSpace, $theFileName) {
    $theUUID = filterSqlVariable($theUUID);
    $theNameSpace = filterSqlVariable($theNameSpace);
    $theFileName = filterSqlVariable($theFileName);

    $aResultCount = $SQLite3_conn->querySingle("SELECT COUNT(*) FROM Files "
            . "WHERE uuid='$theUUID' AND namespace='$theNameSpace' AND "
            . "filename='$theFileName'");

    if ($aResultCount == 0) {
        $SQLite3_conn->exec("INSERT INTO Files VALUES(NULL, '$theUUID', "
                . "'$theNameSpace', '$theFileName')");
    }
}

/**
 * get an array of node information for all nodes that have the file
 * @param SQLite3 $SQLite3_conn
 * @param string $theNameSpace
 * @param string $theFileName
 * @return array
 */
function getFileHostRecords($SQLite3_conn, $theNameSpace, $theFileName) {
    $theNameSpace = filterSqlVariable($theNameSpace);
    $theFileName = filterSqlVariable($theFileName);

    $aSqlJoinSearch = "SELECT Files.uuid, Nodes.address, "
            . "Nodes.http, Files.namespace, "
            . "Files.filename FROM Files, Nodes "
            . "WHERE Files.uuid = Nodes.uuid "
            . "AND Files.namespace = '$theNameSpace' AND "
            . "Files.filename = '$theFileName' AND "
            . "Nodes.online = 1 LIMIT 20";
    $results = $SQLite3_conn->query($aSqlJoinSearch);
    if (!$results) {
        return false;
    }

    $i = 0;
    while ($row = $results->fetchArray(SQLITE3_ASSOC)) {
        $aReturnArray[$i]['uuid'] = $row['uuid'];
        $aReturnArray[$i]['address'] = $row['address'];
        $aReturnArray[$i]['http'] = $row['http'];
        $aReturnArray[$i]['namespace'] = $row['namespace'];
        $aReturnArray[$i]['filename'] = $row['filename'];
        $i++;
    }

    return $aReturnArray;
}

/**
 * return a 2-d array containing node information for all available nodes
 * @param SQLite3 $SQLite3_conn
 * @return array
 */
function getNodesOnline($SQLite3_conn) {
    $results = $SQLite3_conn->query("SELECT uuid, address, http FROM Nodes "
            . "WHERE online=1");
    if (!$results) {
        return false;
    }

    $i = 0;
    while ($row = $results->fetchArray(SQLITE3_ASSOC)) {
        $aReturnArray[$i]['uuid'] = $row['uuid'];
        $aReturnArray[$i]['address'] = $row['address'];
        $aReturnArray[$i]['http'] = $row['http'];
        $i++;
    }

    return $aReturnArray;
}

/**
 * check if a certain node is online based on its UUID
 * @param SQLite3 $SQLite3_conn
 * @param string $theUUID
 * @return boolean - is the node with $theUUID available?
 */
function getNodeIsOnline($SQLite3_conn, $theUUID) {
    $theUUID = filterSqlVariable($theUUID);

    $aResultCount = $SQLite3_conn->querySingle("SELECT COUNT(*) FROM Nodes "
            . "WHERE uuid='$theUUID' AND online=1");
    if ($aResultCount == 1) {
        return true;
    } else {
        return false;
    }
}

/**
 * set that a certain node is online with certain params
 * @param SQLite3 $SQLite3_conn
 * @param string $theUUID
 * @param string $theAddress
 * @param int $theWebPort
 * @return boolean - did the set work?
 */
function setNodeIsOnline($SQLite3_conn, $theUUID, $theAddress, $theWebPort) {
    $theUUID = filterSqlVariable($theUUID);
    if (!filter_var($theAddress, FILTER_VALIDATE_IP)) {
        return false; //http://www.w3schools.com/php/filter_validate_ip.asp
    }
    $theWebPort = filterSqlVariable($theWebPort);

    $aResultCount = $SQLite3_conn->querySingle("SELECT COUNT(*) FROM Nodes "
            . "WHERE uuid='$theUUID'");
    if ($aResultCount == 0) {
        return ($SQLite3_conn->exec("INSERT INTO Nodes VALUES(NULL, '$theUUID', "
                . "'$theAddress', '$theWebPort', 1)"));
    } elseif ($aResultCount == 1) {
        return ($SQLite3_conn->exec("UPDATE Nodes SET address='$theAddress', "
                . "http='$theWebPort', online=1 WHERE uuid='$theUUID'"));
    } else {
        return false;
    }
}

/**
 * set that a certain node with a certain UUID is offline
 * @param SQLite3 $SQLite3_conn
 * @param string $theUUID
 * @return boolean - did everything work properly?
 */
function setNodeIsOffline($SQLite3_conn, $theUUID) {
    $theUUID = filterSqlVariable($theUUID);

    $aResultCount = $SQLite3_conn->querySingle("SELECT COUNT(*) FROM Nodes "
            . "WHERE uuid='$theUUID'");
    if ($aResultCount == 1) {
        return ($SQLite3_conn->exec("UPDATE Nodes SET online=0 WHERE uuid='$theUUID'"));
    } else {
        return false;
    }
}

/**
 * minimal SQL filtering function to foil stupid attackers
 * only allows periods, dashes, and alphanumeric characters
 */
function filterSqlVariable($theDataVariable) {
    return (ereg_replace("[^A-Za-z0-9.-]", "", $theDataVariable));
}

?>
