/*
 * this JavaScript library abstracts away the basic HTTP operations to give
 * the developer an easier set of functions to work with
 *
 * requires 1) vanilla jQuery for function triggers 2) jQuery form plugin for setFile
 *
 * this works via Cross-Origin Resource Sharing (CORS) [http://enable-cors.org/]
 * supported in: Internet Explorer 8+, Firefox 3.5+, Safari 4+, and Chrome
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
 *
 * @param theTriggerElement - a valid jQuery dom element: document, "#foo", body
 * @param theTriggerType - a string representation of the trigger handler that has been bound
 * @param theNameSpaceStr
 * @param theFileNameStr
 *
 * example usage:
 * $('#foo').bind('nebulaDSS_getFile', function(event, param1, param2) {
 *      if (param1) {
 *          alert("file contents = " + param2);
 *      }
 * });
 * nebulaDSS_getFile('#foo', 'nebulaDSS_getFile', 'test-namespace', 'test-file.txt');
 */
function nebulaDSS_getFile(theTriggerElement, theTriggerType, theNameSpaceStr, theFileNameStr) {
    var aRequestUrlStr = myControlServerBaseStr + "?opt=get&namespace="
    + theNameSpaceStr + "&filename=" + theFileNameStr + "&redir=true";
    var request = nebulaDSS_private_createCORSRequest("get", aRequestUrlStr);
    if (request){
        request.onload = function(){
            if (typeof(request.responseText) != "undefined") {
                $(theTriggerElement).trigger(theTriggerType, [true, request.responseText]);
            } else {
                $(theTriggerElement).trigger(theTriggerType, [false, 'no response']);
            }
        };
        request.send();
    } else {
        $(theTriggerElement).trigger(theTriggerType, [false, 'no xhr object']);
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
 * @param theGroupKey - all files with same UUID string should go on same host - SHA1(namespace + filename + time) ?
 * @param theFileObjPath - the local file path/handle for the multipart upload
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
 * nebulaDSS_setFile('#foo', 'nebulaDSS_setFile', 'test-namespace', 'test-file.txt', 500, 0.25, 'myUUIDkey', '/home/user/file');
 */
function nebulaDSS_setFile(theTriggerElement, theTriggerType, theLatencyMax, theBandwidthMin,
    theNameSpaceStr, theFileNameStr, theGroupKey, theFileObjPath) {
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
                        theGroupKey, theFileObjPath);
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
 * multipart/form-data submission via 3rd party jQuery form plugin
 * will not have any response - jQuery does not offer CORS support
 * @link http://jquery.malsup.com/form/
 * @link http://stackoverflow.com/questions/5938842/cross-domain-ajax-post-in-chrome
 */
function nebulaDSS_private_SubmitData(theTriggerElement, theTriggerType, theHostUrlStr,
    theNameSpaceStr, theFileNameStr,theGroupKey, theFileObjPath) {
    var aFileSubmitForm = $(document.createElement('form')).hide();
    var aFileSubmitFormId = theNameSpaceStr + '-' + theFileNameStr;
    aFileSubmitForm.attr('id', aFileSubmitFormId);
    aFileSubmitForm.append("<input type='text' name='namespace' value='"+theNameSpaceStr+"' />");
    aFileSubmitForm.append("<input type='text' name='filename' value='"+theFileNameStr+"' />");
    aFileSubmitForm.append("<input type='text' name='group-key' value='"+theGroupKey+"' />");
    aFileSubmitForm.append("<input type='file' name='file' value='"+theFileObjPath+"' />");

    var aSubmitFormOptions = { //form should be all set before options
        iframe: true,
        type: 'post',
        url: theHostUrlStr
    };
    aFileSubmitForm.ajaxForm(aSubmitFormOptions);

    aFileSubmitForm.ajaxSubmit(); //will post, but will not be able to see response (when XSS)

    $(theTriggerElement).trigger(theTriggerType, [true, 'data submitted']);
}

/**
 * check to see if there are any server entries in the string
 */
function nebulaDSS_private_ServerExists(theData) {
    var aDataStr = new String(theData);
    return (aDataStr.indexOf("http://") > -1);
}

/**
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
