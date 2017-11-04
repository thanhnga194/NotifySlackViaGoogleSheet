/**
 * Purpose - send a slack payload to bot-database informing users of database update requirements
**/

// CONFIG
var SLACK_URL = "https://hooks.slack.com/services/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"
var BOT_NAME = "Progress Tracker"
var BOT_AVATAR = ":clock:"
var ROW_HEADER = 4
var CACHE_TIME = 3600 // = 60 * 60 seconds = 60 minutes

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

// SIMPLE VALUE
var EMPTY_STRING = ""

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
  var revision_range = ceta_sheet.getRange(ROW_HEADER, active_column);
  var revision_content = revision_range.getValue();
  Logger.log("revision_range = %s revision_content = %s", revision_range, revision_content)

  // Get the changes in the cell
  var db_changes_range = ceta_sheet.getRange(active_row, active_column);
  var db_changes_content = db_changes_range.getValue();

  Logger.log("db_changes_range = %s db_changes_content = %s", db_changes_range, db_changes_content)

  // if its nothing then lets not bother (they're probably deleting stuff)
  if (db_changes_content == EMPTY_STRING) {
    Logger.log("db_changes_content == empty")
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
  var store = ceta_sheet.getRange(active_row, COLUMN_STORE).getValue();
  var store_id = ceta_sheet.getRange(active_row, COLUMN_STORE_ID).getValue()
  var task_description = ceta_sheet.getRange(active_row, COLUMN_TASK_DESCRIPTION).getValue();
  var assigned_to = ceta_sheet.getRange(active_row, COLUMN_ASSIGNED_TO).getValue()
  var status = ceta_sheet.getRange(active_row, COLUMN_STATUS).getValue()

  // Sample output of notification
  // [🏁1002 - <STORE NAME>] Tender Hand-over to SD (PIC: SD)
  // Actual Start: 17/11/2017
  // Actual End: 17/11/2017
  // Doc Links: <links>


  // *FILL TITLE*
  var title = EMPTY_STRING
  // Get cache of latest user name.
  var cache = CacheService.getScriptCache();
  var latestUserName = cache.get("latest-user-name");
  Logger.log("latestUserName = %s", latestUserName)

  // If latest user name != nil && current user name != latest user name, then send title
  if (latestUserName != null && current_user != latestUserName) {
    Logger.log("latestUserName != null && current_user != latestUserName")
    title = Utilities.formatString("[%s %s - %s] %s (PIC: %s)", status, store_id, store, task_description, assigned_to)
  } else { // else don't send title, and store latest user name with current user name
    cache.put("latest-user-name", current_user)
  }
  Logger.log("title = %s", title)

  // *FILL CONTENT*
  var content = EMPTY_STRING
  if (active_column == COLUMN_CHANGE_DATA_ACTUAL_START || active_column == COLUMN_CHANGE_DATA_ACTUAL_END) {
    date = Utilities.formatDate(db_changes_content, "GMT+7", "dd/MM/yyyy");
    content = Utilities.formatString("*%s*: %s", revision_content, date)
  } else {
    content = Utilities.formatString("*%s*: %s", revision_content, db_changes_content)
  }
  Logger.log("content = %s", content)

  // *FILL OUTPUT*
  var output = EMPTY_STRING
  if (title != EMPTY_STRING) {
    output = Utilities.formatString("%s \n %s", title, content)
  } else {
    output = Utilities.formatString("%s", content)
  }
  Logger.log("output = %s", output)

  // generate the payload text object
  var payload = { "text": current_user + " just updated:\n" + output,
                  "icon_emoji": BOT_AVATAR,
                  "username": BOT_NAME
   };
  Logger.log("payload = %s", payload)

  //the URL payload
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