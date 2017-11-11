/**
 * Purpose - send a slack payload to bot-database informing users of database update requirements
**/

// CONFIG
var SLACK_URL = "https://hooks.slack.com/services/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"
var BOT_NAME = "Progress Tracker"
var BOT_AVATAR = ":clock:"
var ROW_HEADER = 4
var CACHE_KEY = "changed-rows-t4"

// COLUMN DATA CHANGE
var COLUMN_CHANGE_DATA_DOC_LINKS = 7
var COLUMN_CHANGE_DATA_ACTUAL_START = 12
var COLUMN_CHANGE_DATA_ACTUAL_END = 13

// COLUMN OUTPUT NAME
var COLUMN_STORE = 15
var COLUMN_TASK_DESCRIPTION = 3
var COLUMN_ASSIGNED_TO = 5
var COLUMN_STATUS = 4
var COLUMN_STORE_ID = 1
var COLUMN_PLAN_START = 9
var COLUMN_PLAN_END = 10

// SIMPLE VALUE
var EMPTY_STRING = ""

// Cache keys
var KEY_STORE_NAME = "StoreName"
var KEY_STORE_ID = "StoreID"
var KEY_TASK_DESCRIPTION = "TaskDescription"
var KEY_ASSIGNED_TO = "AssignedTo"
var KEY_STATUS = "Status"
var KEY_OLD_VALUE = "OldValue"
var KEY_PLAN_START = "PlanStart"
var KEY_PLAN_END = "PlanEnd"


function ceta_db_column_edit(event){
  Logger.log("ceta_db_column_edit with event = %s", event)

  // get this spread sheet
  var ceta_spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  // get the sheets and range from the spreadsheet
  var ceta_sheet = event.source.getActiveSheet();
  var ceta_range = event.source.getActiveRange();
  Logger.log("ceta_spreadsheet = %s ceta_sheet = %s ceta_range = %s", ceta_spreadsheet, ceta_sheet, ceta_range)

  // get the cell thingy
  var active_cell = ceta_sheet.getActiveCell();
  var active_row = active_cell.getRow();
  var active_column = active_cell.getColumn();
  Logger.log("active_cell = %s active_row = %s active_column = %s", active_cell, active_row, active_column)

  // If header row then exit
  if (active_row <= ROW_HEADER) {
    Logger.log("active_row <= ROW_HEADER")
    return;
  }

  // if not the db column get out
  if (active_column != COLUMN_CHANGE_DATA_DOC_LINKS &&
      active_column != COLUMN_CHANGE_DATA_ACTUAL_START &&
      active_column != COLUMN_CHANGE_DATA_ACTUAL_END) {
    Logger.log("active_column != COLUMN_CHANGE_DATA_CHANGE")
    return;
  }

  // get the revision
  // var dataChangeKey = ceta_sheet.getRange(ROW_HEADER, active_column).getValue();
  // Logger.log("dataChangeKey = %s", dataChangeKey)
  var dataChangeKey = active_column

  // Get the changes in the cell
  var dataChangeValue = ceta_sheet.getRange(active_row, active_column).getValue();
  Logger.log("dataChangeValue = %s", dataChangeValue)

  // if its nothing then lets not bother (they're probably deleting stuff)
  if (dataChangeValue == EMPTY_STRING) {
    Logger.log("dataChangeValue == empty")
    return;
  }

  // get the logged in user (we can only get email I thinks)
  var activeUser = Session.getActiveUser();
  Logger.log("activeUser = %s", activeUser)
  var current_user = activeUser.getEmail();
  Logger.log("current_user = %s", current_user)

  // check if can get current_user
  if (current_user == EMPTY_STRING) {
    // at least put something in
    current_user = "An unknown user";
  }

  // get value
  var storeName = ceta_sheet.getRange(active_row, COLUMN_STORE).getValue();
  var storeId = ceta_sheet.getRange(active_row, COLUMN_STORE_ID).getValue();
  var taskDescription = ceta_sheet.getRange(active_row, COLUMN_TASK_DESCRIPTION).getValue();
  var assignedTo = ceta_sheet.getRange(active_row, COLUMN_ASSIGNED_TO).getValue()
  var status = ceta_sheet.getRange(active_row, COLUMN_STATUS).getValue()
  var planStart = Utilities.formatDate(ceta_sheet.getRange(active_row, COLUMN_PLAN_START).getValue(), "GMT+7", "dd/MM/yyyy")
  var planEnd = Utilities.formatDate(ceta_sheet.getRange(active_row, COLUMN_PLAN_END).getValue(), "GMT+7", "dd/MM/yyyy")

  // sanity value
  if (active_column == COLUMN_CHANGE_DATA_ACTUAL_START || active_column == COLUMN_CHANGE_DATA_ACTUAL_END) {
    dataChangeValue = Utilities.formatDate(dataChangeValue, "GMT+7", "dd/MM/yyyy")
  }

  saveChangesIntoCache(active_row, dataChangeKey, dataChangeValue, event.oldValue, storeName, storeId, taskDescription, assignedTo, status, planStart, planEnd)
}

function checkCacheToSendToSlack(event) {
  Logger.log("checkCacheToSendToSlack with event = %s", event)

  var cache = CacheService.getScriptCache()
  var changedRows = cache.get(CACHE_KEY)
  Logger.log("changedRows = %s", changedRows)
  var changedRowsObject = JSON.parse(changedRows)

  for (var key in changedRowsObject) {
    // each row will send slack notification
    var changedRow = changedRowsObject[key]
    Logger.log("key = %s changedRow = %s", key, changedRow)

    var storeName = changedRow[KEY_STORE_NAME]
    var storeId = changedRow[KEY_STORE_ID]
    var taskDescription = changedRow[KEY_TASK_DESCRIPTION]
    var assignedTo = changedRow[KEY_ASSIGNED_TO]
    var status = changedRow[KEY_STATUS]
    var planStart = changedRow[KEY_PLAN_START]
    var planEnd = changedRow[KEY_PLAN_END]

    // build fields changed
    var fieldActualStart = null
    var fieldActualEnd = null
    var fieldDocLink = null

    actualStart = changedRow[COLUMN_CHANGE_DATA_ACTUAL_START]
    actualEnd = changedRow[COLUMN_CHANGE_DATA_ACTUAL_END]
    docLink = changedRow[COLUMN_CHANGE_DATA_DOC_LINKS]
    if (actualStart != null) {
      fieldActualStart = {
                      "title": "Actual Start            <=>      Plan Start",
                      "value": Utilities.formatString("%s                       %s", actualStart, planStart),
                      "short": false
                  }
    };
    if (actualEnd != null) {
      fieldActualEnd = {
                      "title": "Actual End            <=>      Plan End",
                      "value": Utilities.formatString("%s                       %s", actualEnd, planEnd),
                      "short": false
                  }
    };
    if (docLink != null) {
      fieldDocLink = {
                      "title": "Doc Links",
                      "value": docLink,
                      "short": false
                  }
    };

    // send slack notificaiton with format
    var payload = {
      "icon_emoji": BOT_AVATAR,
      "username": BOT_NAME,
      "attachments": [
          {
              "fallback": "Required plain-text summary of the attachment.",
              "color": "#36a64f",
              "title":  Utilities.formatString("[%s %s - %s] %s (PIC: %s)", status, storeId, storeName, taskDescription, assignedTo),
              "title_link": "https://docs.google.com/spreadsheets/d/1hKiinJXluVB1N-9z92Hv8YQYJgojkYwzOnE-dKmGHdY/edit?pli=1#gid=149195960",
              "fields": [
                  fieldActualStart,
                  fieldActualEnd,
                  fieldDocLink
              ],
              "image_url": "http://my-website.com/path/to/image.jpg",
              "thumb_url": "http://example.com/path/to/thumb.png",
          }
      ]
    };
    Logger.log("payload = %s", payload)

    // the URL payload
    var options = {
       "method" : "post",
       "contentType" : "application/json",
       "payload" : JSON.stringify(payload),
       "muteHttpExceptions" : true
    };

    // send to Slack
    var response = UrlFetchApp.fetch(SLACK_URL, options);
    Logger.log("response = %s", response)
  }

  // clear cache
  Logger.log("cache.remove(%s)", CACHE_KEY)
  cache.remove(CACHE_KEY)
}

// Sample data cache
// {
//   {
//     "1" : {
//       "ActualStart": value,
//       "ActualEnd": actualEndValue,
//       "DocLink": docLinkValue
//     },
//     "2"
//     ...........
//   }
// }
function saveChangesIntoCache(rowNumber, key, value, oldValue, storeName, storeId, taskDescription, assignedTo, status, planStart, planEnd) {
  Logger.log("saveValueToCache rowNumber = %s key = %s value = %s oldValue = %s storeName = %s storeId = %s taskDescription = %s assignedTo = %s status = %s planStart = %s planEnd = %s",
             rowNumber, key, value, oldValue, storeName, storeId, taskDescription, assignedTo, status, planStart, planEnd)
  // GET JSON in cache,
  // if cache has no data, then let it empty
  // then covernt to object
  var cache = CacheService.getScriptCache();
  var changedRows = cache.get(CACHE_KEY)
  if (changedRows == null) {
    changedRows = "{}"
  }
  var changedRowsObject = JSON.parse(changedRows)
  Logger.log("changedRowsObject first = %s", changedRowsObject)

  // PUSH key/pair into JSON
  // form data first
  var changedRow = changedRowsObject[rowNumber];
  if (changedRow == null) {
    changedRow = {}
  }
  Logger.log("changedRow first = %s", changedRow)

  // update value
  changedRow[key] = value;
  changedRow[KEY_OLD_VALUE] = oldValue
  changedRow[KEY_STORE_NAME] = storeName
  changedRow[KEY_STORE_ID] = storeId;
  changedRow[KEY_TASK_DESCRIPTION] = taskDescription;
  changedRow[KEY_ASSIGNED_TO] = assignedTo
  changedRow[KEY_STATUS] = status
  changedRow[KEY_PLAN_START] = planStart;
  changedRow[KEY_PLAN_END] = planEnd

  Logger.log("changedRow later = %s", changedRow);
  changedRowsObject[rowNumber] = changedRow;
  Logger.log("changedRowsObject later = %s", changedRowsObject);

  // STORE json by stringify
  var changeRowsObjectStringify = JSON.stringify(changedRowsObject)
  Logger.log("changeRowsObjectStringify = %s", changeRowsObjectStringify)

  cache.put(CACHE_KEY, changeRowsObjectStringify)
}