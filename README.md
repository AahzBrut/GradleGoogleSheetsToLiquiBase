Usage:

Add buildSrc to project's root folder

Add to build script:
tasks.register<CsvToLbScriptTask>("processCsv") {
    description = "Generates LiquiBase scripts from google sheets."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    author = "author_name"
    spreadSheetId = "UUID of Google sheet"
    lbsRoot = "root path to store lb script files"
    googleApiKeyPath = Path("path to file with Google API key")
}
