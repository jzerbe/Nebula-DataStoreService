/*
 * this JavaScript library abstracts away the basic NebulaDSS HTTP operations
 * to give the developer an easier set of functions to work with
 *
 * 1) asynchronous triggers are done with vanilla jQuery
 *
 * 2) functions require Cross-Origin Resource Sharing (CORS) [http://enable-cors.org/]
 * supported in: Internet Explorer 8+, Firefox 3.5+, Safari 4+, and Chrome
 *
 * 3) setFile functionality requires BlobBuilder support [http://caniuse.com/blobbuilder]
 *
 * @author Jason Zerbe
 * @link https://github.com/jzerbe/Nebula-DataStoreService
 */

var myControlServerBaseStr = "http://www-users.cs.umn.edu/~zerbe/nebula_dss/master_server.php";

/**
 * check to see if a file exists on the NebulaDSS
 *
 * @param theTriggerElement - a valid jQuery dom element: document, "#foo", body
 * @param theTriggerType - a string representation of the trigger handler that has been bound
 * @param theNameSpaceStr
 * @param theFileNameStr
 *
 * example usage:
 * $('#foo').bind('nebulaDSS_fileExists', function(event, param1) {
 *      if (param1) {
 *          alert("file exists");
 *      }
 * });
 * nebulaDSS_fileExists('#foo', 'nebulaDSS_fileExists', 'test-namespace', 'test-file');
 */
function nebulaDSS_fileExists(theTriggerElement, theTriggerType, theNameSpaceStr, theFileNameStr) {
    var aRequestUrlStr = myControlServerBaseStr + "?opt=get&namespace="
    + theNameSpaceStr + "&filename=" + theFileNameStr;
    var request = nebulaDSS_private_createCORSRequest("get", aRequestUrlStr);
    if (request){
        request.onload = function(){
            var aSingleHostExists = nebulaDSS_private_ServerExists(request.responseText);
            $(theTriggerElement).trigger(theTriggerType, [aSingleHostExists]);
        };
        request.send();
    } else {
        $(theTriggerElement).trigger(theTriggerType, [false]);
    }
}

/**
 * get the contens of a file stored on the NebulaDSS
 * function will use XHR2, if it is supported, otherwise degrades gracefully
 *
 * @param theTriggerElement - a valid jQuery dom element: document, "#foo", body
 * @param theTriggerType - a string representation of the trigger handler that has been bound
 * @param theNameSpaceStr
 * @param theFileNameStr
 * @param theResponseType - XHR2 responseType - "text", "arraybuffer", "blob", or "document"
 *
 * example usage:
 * $('#foo').bind('nebulaDSS_getFile', function(event, param1, param2) {
 *      if (param1) {
 *          alert("file contents = " + param2);
 *      }
 * });
 * nebulaDSS_getFile('#foo', 'nebulaDSS_getFile', 'test-namespace', 'test-file.txt', 'text');
 *
 * for more on responseType:
 * http://www.html5rocks.com/en/tutorials/file/xhr2/#toc-response
 */
function nebulaDSS_getFile(theTriggerElement, theTriggerType, theNameSpaceStr, theFileNameStr, theResponseType) {
    var aRequestUrlStr = myControlServerBaseStr + "?opt=get&namespace="
    + theNameSpaceStr + "&filename=" + theFileNameStr;
    var aMasterServerRequest = nebulaDSS_private_createCORSRequest("get", aRequestUrlStr);
    if (aMasterServerRequest){
        aMasterServerRequest.onload = function(){
            var data = aMasterServerRequest.responseText;
            if (nebulaDSS_private_ServerExists(data)) {
                var aHostUrlStrArray = data.split(/\r\n|\r|\n/);
                if (aHostUrlStrArray.length > 0) { //grab data from the first host (if exists)
                    var aDataRequest = nebulaDSS_private_createCORSRequest("get", aHostUrlStrArray[0]);
                    if (aDataRequest){
                        aDataRequest.responseType = theResponseType;
                        aDataRequest.onload = function(){
                            if (typeof(aDataRequest.response) != "undefined") {
                                $(theTriggerElement).trigger(theTriggerType, [true, aDataRequest.response]);
                            } else if (typeof(aDataRequest.responseText) != "undefined") {
                                $(theTriggerElement).trigger(theTriggerType, [true, aDataRequest.responseText]);
                            } else {
                                $(theTriggerElement).trigger(theTriggerType, [false, 'no data response']);
                            }
                        };
                        aDataRequest.send();
                    } else {
                        $(theTriggerElement).trigger(theTriggerType, [false, 'no xhr object for data request']);
                    }
                } else {
                    $(theTriggerElement).trigger(theTriggerType, [false, 'malformed host information']);
                }
            } else {
                $(theTriggerElement).trigger(theTriggerType, [false, 'no matching data hosts']);
            }
        };
        aMasterServerRequest.send();
    } else {
        $(theTriggerElement).trigger(theTriggerType, [false, 'no xhr object for master server request']);
    }
}

/**
 * create a file on the NebulaDSS
 *
 * @param theTriggerElement - a valid jQuery dom element: document, "#foo", body
 * @param theTriggerType - a string representation of the trigger handler that has been bound
 * @param theLatencyMax - an integer value for the maximum latency in milliseconds accepted
 * @param theBandwidthMin - float value of least bandwidth for hosts used
 * @param theNameSpaceStr
 * @param theFileNameStr
 * @param theGroupKey - all files with same UUID string should go on same host - SHA1(namespace + filename + time)
 * @param theFileObjContents - the actual file contents to push to the DSS
 *
 * for SHA1 output equivalent to php/python --> http://phpjs.org/functions/sha1:512
 *
 * example usage:
 * $('#foo').bind('nebulaDSS_setFile', function(event, param1, param2) {
 *      if (param1) {
 *          alert("upload worked - message = " + param2);
 *      } else {
 *          alert("upload failed - message = " + param2);
 *      }
 * });
 * nebulaDSS_setFile('#foo', 'nebulaDSS_setFile', 500, 0.25, 'test-namespace', 'test-file', 'myUUIDkey', '/home/user/file');
 */
function nebulaDSS_setFile(theTriggerElement, theTriggerType, theLatencyMax, theBandwidthMin,
    theNameSpaceStr, theFileNameStr, theGroupKey, theFileObjContents) {
    var aRequestUrlStr = myControlServerBaseStr + "?opt=online"
    + "&latency_max=" + theLatencyMax + "&bandwidth_min=" + theBandwidthMin
    + "&group_key=" + theGroupKey;
    var request = nebulaDSS_private_createCORSRequest("get", aRequestUrlStr);
    if (request){
        request.onload = function(){
            var data = request.responseText;
            if (nebulaDSS_private_ServerExists(data)) {
                var aHostUrlStrArray = data.split(/\r\n|\r|\n/);
                if (aHostUrlStrArray.length > 0) {
                    nebulaDSS_private_SubmitData(theTriggerElement, theTriggerType,
                        aHostUrlStrArray[0], theNameSpaceStr, theFileNameStr,
                        theFileObjContents);
                } else {
                    $(theTriggerElement).trigger(theTriggerType, [false, 'malformed host information']);
                }
            } else {
                $(theTriggerElement).trigger(theTriggerType, [false, 'no matching hosts']);
            }
        };
        request.send();
    } else {
        $(theTriggerElement).trigger(theTriggerType, [false, 'no xhr object']);
    }
}

/**
 * multipart/form-data submission via CORS and BlobBuilder API
 *
 * @param theTriggerElement - a valid jQuery dom element: document, "#foo", body
 * @param theTriggerType - a string representation of the trigger handler that has been bound
 * @param theHostUrlStr - what DSS node will we be POSTing to?
 * @param theNameSpaceStr
 * @param theFileNameStr
 * @param theFileObjContents - the actual file contents to push to the DSS
 *
 * for inspiration see:
 * http://www.html5rocks.com/en/tutorials/file/xhr2/#toc-send-blob
 */
function nebulaDSS_private_SubmitData(theTriggerElement, theTriggerType, theHostUrlStr,
    theNameSpaceStr, theFileNameStr, theFileObjContents) {
    var aBlobBuilderSupported = nebulaDSS_private_initBlobBuilderAPI();
    if (aBlobBuilderSupported) {
        var aBlobBuilder = new BlobBuilder();
        aBlobBuilder.append(theFileObjContents);

        var aFormData = new FormData();
        if (typeof(aFormData) != "undefined") {
            aFormData.append('namespace', theNameSpaceStr);
            aFormData.append('filename', theFileNameStr);
            aFormData.append('file', aBlobBuilder.getBlob('application/octet-stream'));

            var request = nebulaDSS_private_createCORSRequest("post", theHostUrlStr);
            if (request){
                request.onload = function(){
                    $(theTriggerElement).trigger(theTriggerType, [true, 'data submitted']);
                };
                request.send(aFormData);
            } else {
                $(theTriggerElement).trigger(theTriggerType, [false, 'no xhr object']);
            }
        } else {
            $(theTriggerElement).trigger(theTriggerType, [false, 'FormData API not supported']);
        }
    } else {
        $(theTriggerElement).trigger(theTriggerType, [false, 'BlobBuilder API not supported']);
    }
}

/**
 * check to see if there are any server entries in the string
 * @param theData - data string to check if it contains HTTP server path strings
 * @return boolean - are there any HTTP path strings in the data?
 */
function nebulaDSS_private_ServerExists(theData) {
    var aDataStr = new String(theData);
    return (aDataStr.indexOf("http://") > -1);
}

/**
 * create and return a CORS XHR request object
 *
 * @param method - "get" or "post"
 * @param url - the URL to work upon
 * @return xhr - a valid CORS XHR request object
 *
 * copied and renamed from:
 * http://www.nczonline.net/blog/2010/05/25/cross-domain-ajax-with-cross-origin-resource-sharing/
 */
function nebulaDSS_private_createCORSRequest(method, url){
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr){
        xhr.open(method, url, true);
    } else if (typeof XDomainRequest != "undefined"){
        xhr = new XDomainRequest();
        xhr.open(method, url);
    } else {
        xhr = null;
    }
    return xhr;
}

/**
 * prepare the HTML5 BlobBuilder API for generic use
 * eliminate need to use specialty DOM refrences
 *
 * @return boolean - is the BlobBuilder API supported?
 *
 * taken from:
 * http://msdn.microsoft.com/en-us/library/hh673542(v=vs.85).aspx#blobbuilder
 */
function nebulaDSS_private_initBlobBuilderAPI() {
    if (window.BlobBuilder) {
    // No change needed - the W3C standard API will be used by default.
    } else if (window.MSBlobBuilder) {
        window.BlobBuilder = window.MSBlobBuilder;
    } else if (window.WebKitBlobBuilder) {
        window.BlobBuilder = window.WebKitBlobBuilder;
    } else if (window.MozBlobBuilder) {
        window.BlobBuilder = window.MozBlobBuilder;
    } else {
        return false;
    }

    return true;
}
