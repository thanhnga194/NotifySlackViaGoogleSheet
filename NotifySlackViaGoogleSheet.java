/**
 * @author Chris Tate-Davies
 * @revision 0.0.1
 *
 * 10th May 2016
 * Purpose - send a slack payload to bot-database informing users of database update requirements
**/

var SLACK_URL = "https://hooks.slack.com/services/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"
var HEADER_ROW_NUMBER = 4
var COLUMN_CHANGE_DATA_DOC_LINKS = 7
var COLUMN_CHANGE_DATA_ACTUAL_START = 12
var COLUMN_CHANGE_DATA_ACTUAL_END = 13

function ceta_db_column_edit(event){
  Logger.log("ceta_db_column_edit")

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
  if (active_row <= HEADER_ROW_NUMBER) {
    Logger.log("active_row <= HEADER_ROW_NUMBER")
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
  var revision_range = ceta_sheet.getRange(HEADER_ROW_NUMBER, active_column);
  var revision_content = revision_range.getValue();
  Logger.log("revision_range = %s revision_content = %s", revision_range, revision_content)

  // Get the changes in the cell
  var db_changes_range = ceta_sheet.getRange(active_row, active_column);
  var db_changes_content = db_changes_range.getValue();

  Logger.log("db_changes_range = %s db_changes_content = %s", db_changes_range, db_changes_content)

  // if its nothing then lets not bother (they're probably deleting stuff)
  if (db_changes_content == "") return;

  // get the logged in user (we can only get email I thinks)
  var current_user = Session.getActiveUser().getEmail();
  Logger.log("current_user = %s", current_user)

  //if its blank (why?)
  if (current_user == "") {
    // at least put something in
    current_user = "An unknown user";
  }

  // generate the payload text object
  var payload = { "text" : current_user + " has just entered text into the db field for revision " + revision_content + " - Content is: ```" + db_changes_content + "```" };

  //the URL payload
  var options = {
     "method" : "post",
     "contentType" : "application/json",
     "payload" : JSON.stringify(payload),
     "muteHttpExceptions" : true
  };

  Logger.log("payload = %s", payload)

  //send that bugger
  var response = UrlFetchApp.fetch(SLACK_URL, options);

  Logger.log("response = %s", response)
  //we could check for response, but who cares?
}