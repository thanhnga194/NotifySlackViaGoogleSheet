// CONFIG
var CACHE_KEY = "changed-rows-t2"

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
function saveChangesIntoCache(rowNumber, key, value) {
  Logger.log("saveValueToCache with rowNumber = %s, key = %s, value = %s", rowNumber, key, value)
  // GET JSON in cache,
  // if cache has no data, then let it empty
  // then covernt to object
  var cache = CacheService.getScriptCache()
  var changedRows = cache.get(CACHE_KEY)
  if (changedRows == null) {
    changedRows = "{}"
  }
  var changedRowsObject = JSON.parse(changedRows)
  Logger.log("changedRowsObject first = %s", changedRowsObject)

  // PUSH key/pair into JSON
  // form data first
  var changedRow = changedRowsObject[rowNumber]
  if (changedRow == null) {
    changedRow = {}
  }
  Logger.log("changedRow first = %s", changedRow)
  changedRow[key] = value
  Logger.log("changedRow later = %s", changedRow)
  changedRowsObject[rowNumber] = changedRow
  Logger.log("changedRowsObject later = %s", changedRowsObject)

  // STORE json by stringify
  var changeRowsObjectStringify = JSON.stringify(changedRowsObject)
  Logger.log("changeRowsObjectStringify = %s", changeRowsObjectStringify)

  cache.put(CACHE_KEY, changeRowsObjectStringify)
}