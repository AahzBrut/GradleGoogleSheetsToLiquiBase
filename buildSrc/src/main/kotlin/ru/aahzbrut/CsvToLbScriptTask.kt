package ru.aahzbrut

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import freemarker.template.Configuration
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText


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

        val configuration = Configuration(Configuration.VERSION_2_3_32)
        configuration.setDirectoryForTemplateLoading(Path("${project.rootDir}/buildSrc/src/main/resources/templates").toFile())
        configuration.defaultEncoding = "UTF-8"

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
                    processSheet(contents, title, configuration)
                }
            }
        }
    }

    private fun processSheet(contents: List<String>, title: String, configuration: Configuration) {
        val sheetData = contents.map { row -> row.split(",") }
        val sheetObject = sheetData.toSheet(title, author, Path(lbsRoot))
        if (!sheetObject.dictionaryPath.parent.exists()) {
            sheetObject.dictionaryPath.parent.createDirectories()
        }
        val tableTemplate = configuration.getTemplate("dictionary_template.ftlh")
        val tableOutput = sheetObject.dictionaryPath.toFile().bufferedWriter()
        tableTemplate.process(sheetObject, tableOutput)
        tableOutput.flush()
        tableOutput.close()

        if (sheetObject.hasDescription) {
            val descriptionTemplate = configuration.getTemplate("description_template.ftlh")
            val descriptionOutput = sheetObject.descriptionPath.toFile().bufferedWriter()
            descriptionTemplate.process(sheetObject, descriptionOutput)
            descriptionOutput.flush()
            descriptionOutput.close()
        }
    }
}
