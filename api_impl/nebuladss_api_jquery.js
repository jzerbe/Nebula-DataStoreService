/*
 * this jQuery library abstracts away the basic HTTP operations to give
 * the developer an easier set of functions to work with
 *
 * nebulaDSS_setFile requires http://malsup.github.com/jquery.form.js
 *
 * @author Jason Zerbe
 * @link https://github.com/jzerbe/Nebula-DataStoreService
 * @see http://api.jquery.com/trigger/
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
    $.get(aRequestUrlStr, function(theReturnedData) {
        var aDataStr = new String(theReturnedData);
        var aSingleHostExists = (aDataStr.indexOf("http://") > -1);
        $(theTriggerElement).trigger(theTriggerType, [aSingleHostExists]);
    });
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
 * $('#foo').bind('nebulaDSS_getFile', function(event, param1) {
 *      alert("file contents = " + param1);
 * });
 * nebulaDSS_getFile('#foo', 'nebulaDSS_getFile', 'test-namespace', 'test-file.txt');
 */
function nebulaDSS_getFile(theTriggerElement, theTriggerType, theNameSpaceStr, theFileNameStr) {
    var aRequestUrlStr = myControlServerBaseStr + "?opt=get&namespace="
    + theNameSpaceStr + "&filename=" + theFileNameStr + "&redir=true";
    $.get(aRequestUrlStr, function(theReturnedData) {
        $(theTriggerElement).trigger(theTriggerType, [theReturnedData]);
    });
}

/**
 * create a file on the NebulaDSS
 *
 * @param theTriggerElement - a valid jQuery dom element: document, "#foo", body
 * @param theTriggerType - a string representation of the trigger handler that has been bound
 * @param theNameSpaceStr
 * @param theFileNameStr
 * @param theLatencyMax - an integer value for the maximum latency in milliseconds accepted
 * @param theBandwidthMin - float value of least bandwidth for hosts used
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
 * nebulaDSS_setFile('#foo', 'nebulaDSS_setFile', 'test-namespace', 'test-file.txt', 500, 0.25, 'myUUIDkey');
 */
function nebulaDSS_setFile(theTriggerElement, theTriggerType, theNameSpaceStr,
    theFileNameStr, theLatencyMax, theBandwidthMin, theGroupKey, theFileObjPath) {
    var aOnlineNodeRequestStr = myControlServerBaseStr + "?opt=online"
    + "&latency_max=" + theLatencyMax + "&bandwidth_min=" + theBandwidthMin;
    $.get(aOnlineNodeRequestStr, function(theReturnedData) {
        var aDataStr = new String(theReturnedData);
        var aSingleHostExists = (aDataStr.indexOf("http://") > -1);
        if (aSingleHostExists) {
            var aHostUrlStrArray = aDataStr.split(/\r\n|\r|\n/);
            if (aHostUrlStrArray.length > 0) {
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
                    url: aHostUrlStrArray[0]
                };
                aFileSubmitForm.ajaxForm(aSubmitFormOptions);

                aFileSubmitForm.ajaxSubmit();

                $(theTriggerElement).trigger(theTriggerType, [true, 'data submitted']);
            } else {
                $(theTriggerElement).trigger(theTriggerType, [false, 'malformed host information']);
            }
        } else {
            $(theTriggerElement).trigger(theTriggerType, [false, 'no matching hosts']);
        }
    });
}
