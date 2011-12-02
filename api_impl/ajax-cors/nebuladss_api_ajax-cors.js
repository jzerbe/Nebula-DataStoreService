/*
 * this vanilla JavaScript library abstracts away the basic HTTP operations to give
 * the developer an easier set of functions to work with
 *
 * @author Jason Zerbe
 * @link https://github.com/jzerbe/Nebula-DataStoreService
 */

var myControlServerBaseStr = "http://www-users.cs.umn.edu/~zerbe/nebula_dss/master_server.php";

function nebulaDSS_fileExists(theNameSpaceStr, theFileNameStr) {
}

function nebulaDSS_getFile(theNameSpaceStr, theFileNameStr) {
}

function nebulaDSS_setFile(theNameSpaceStr, theFileNameStr, theLatencyMax, theBandwidthMin, theGroupKey, theFileObjPath) {
}

function nebulaDSS_private_ServerExists(theData) {
    var aDataStr = new String(theData);
    return (aDataStr.indexOf("http://") > -1);
}
