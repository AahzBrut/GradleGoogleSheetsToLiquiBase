package ru.aahzbrut

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedWriter
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import kotlin.io.path.*
import kotlin.math.min


@Suppress("kotlin:S1192")
abstract class CsvToLbScriptTask : DefaultTask() {
    @Input
    var author = ""
    @Input
    var spreadSheetId = ""
    @Input
    var lbsRoot = ""
    @InputFile
    var googleApiKeyPath = Path("")

    @TaskAction
    fun process() {
        val apiKey = googleApiKeyPath.readText()

        val request =
            HttpRequest.newBuilder(URI("https://sheets.googleapis.com/v4/spreadsheets/$spreadSheetId?fields=sheets(properties(title%2CsheetId))&key=$apiKey"))
                .GET().build()
        val result = HttpClient
            .newBuilder()
            .build()
            .send(request, BodyHandlers.ofString())

        val objectMapper = ObjectMapper()
        val resulBody = objectMapper.readValue<Map<String, Any>>(result.body())

        resulBody["sheets"]?.let { sheets ->
            (sheets as List<*>).forEach { properties ->
                (properties as Map<*, *>).forEach {
                    val sheet = it.value as Map<*, *>
                    val sheetId = sheet["sheetId"].toString()
                    val title = sheet["title"] as String

                    logger.lifecycle("Processing sheet: $title")

                    val sheetRequest =
                        HttpRequest.newBuilder(URI("https://docs.google.com/spreadsheets/d/$spreadSheetId/export?format=csv&id=$spreadSheetId&gid=$sheetId"))
                            .GET().build()
                    val sheetResult = HttpClient
                        .newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build()
                        .send(sheetRequest, BodyHandlers.ofString())

                    val contents = sheetResult.body().split("\r?\n|\r".toRegex())
                    processDictionary(title.camelToSnakeCase().uppercase(), contents)
                }
            }
        }
    }

    private fun processDictionary(tableName: String, contents: List<String>) {
        val fileSubPath = contents[0].split(",")[0]
        val bodyTypesDst = Path("$lbsRoot$fileSubPath/${tableName.lowercase()}.xml")
        if (!bodyTypesDst.exists()) {
            bodyTypesDst.parent.createDirectories()
            bodyTypesDst.createFile()
        }
        logger.lifecycle("Writing script file: ${bodyTypesDst.name}")
        val writer = bodyTypesDst.bufferedWriter()
        val refTableNames = contents[1].split(",")
        val columnNames = contents[2].split(",")
        val offset = columnNames.indexOf("RU_NAME")
        writer.write("$HEADER\n")
        writer.write("    <changeSet author=\"$author\" id=\"$tableName-001\">\n")
        contents.drop(3).forEach { line ->
            val columns = line.split(",")
            writer.write("        <insert tableName=\"$tableName\">\n")
            processRow(refTableNames, writer, columns, offset, columnNames)
            writer.write("        </insert>\n")
        }
        writer.write("    </changeSet>\n")
        writer.write(FOOTER)
        writer.flush()
        writer.close()
        if (offset > 0) processDescriptions(tableName, contents)
    }

    private fun processRow(
        refTableNames: List<String>,
        writer: BufferedWriter,
        columns: List<String>,
        offset: Int,
        columnNames: List<String>
    ) {
        if (refTableNames[0].isBlank()) {
            writer.write("            <column name=\"CODE\" value=\"${columns[0]}\"/>\n")
        } else {
            writer.write("            <column name=\"${columnNames[0]}\" valueComputed=\"SELECT ${refTableNames[0]}_ID FROM \${default-schema}.${refTableNames[0]} WHERE CODE = '${columns[0]}'\"/>\n")
        }
        for (i in 1 until min(columns.size, if (offset == -1) columns.size else offset)) {
            if (columns[i].isNotBlank()) {
                if (refTableNames[i].isNotBlank()) {
                    writer.write("            <column name=\"${columnNames[i]}\" valueComputed=\"SELECT ${refTableNames[i]}_ID FROM \${default-schema}.${refTableNames[i]} WHERE CODE = '${columns[i]}'\"/>\n")
                } else {
                    writer.write("            <column name=\"${columnNames[i]}\" value=\"${columns[i]}\"/>\n")
                }
            }
        }
    }

    private fun processDescriptions(tableName: String, contents: List<String>) {
        val fileSubPath = contents[0].split(",")[0]
        val bodyTypesDst = Path("$lbsRoot$fileSubPath/${tableName.lowercase()}_description.xml")
        logger.lifecycle("Writing script file: ${bodyTypesDst.name}")
        val writer = bodyTypesDst.bufferedWriter()
        val offset = contents[2].split(",").indexOf("RU_NAME")
        writer.write("$HEADER\n")
        writer.write("    <changeSet author=\"$author\" id=\"${tableName}_DESCRIPTION-001\">\n")
        contents.drop(3).forEach { line ->
            val columns = line.split(",")
            writer.write("        <insert tableName=\"${tableName}_DESCRIPTION\">\n")
            writer.write("            <column name=\"${tableName}_ID\" valueComputed=\"SELECT ${tableName}_ID FROM \${default-schema}.$tableName WHERE CODE = '${columns[0]}'\"/>\n")
            writer.write("            <column name=\"LANGUAGE_ID\" valueComputed=\"SELECT LANGUAGE_ID FROM \${default-schema}.LANGUAGE WHERE CODE = 'RU'\"/>\n")
            writer.write("            <column name=\"NAME\" value=\"${columns[offset].replace("|", ",")}\"/>\n")
            writer.write(
                "            <column name=\"DESCRIPTION\" value=\"${
                    columns[offset + 1].replace(
                        "|",
                        ","
                    )
                }\"/>\n"
            )
            writer.write("        </insert>\n")
            writer.write("        <insert tableName=\"${tableName}_DESCRIPTION\">\n")
            writer.write("            <column name=\"${tableName}_ID\" valueComputed=\"SELECT ${tableName}_ID FROM \${default-schema}.$tableName WHERE CODE = '${columns[0]}'\"/>\n")
            writer.write("            <column name=\"LANGUAGE_ID\" valueComputed=\"SELECT LANGUAGE_ID FROM \${default-schema}.LANGUAGE WHERE CODE = 'EN'\"/>\n")
            writer.write("            <column name=\"NAME\" value=\"${columns[offset + 2].replace("|", ",")}\"/>\n")
            writer.write(
                "            <column name=\"DESCRIPTION\" value=\"${
                    columns[offset + 3].replace(
                        "|",
                        ","
                    )
                }\"/>\n"
            )
            writer.write("        </insert>\n")
        }
        writer.write("    </changeSet>\n")
        writer.write(FOOTER)
        writer.flush()
        writer.close()
    }

    private fun String.camelToSnakeCase(): String {
        val pattern = "(?<=.)[A-Z]".toRegex()
        return this.replace(pattern, "_$0").lowercase()
    }
}
