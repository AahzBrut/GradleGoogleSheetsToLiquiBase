package ru.aahzbrut.data

import ru.aahzbrut.camelToSnakeCase
import java.nio.file.Path


data class Sheet(
    val path: Path,
    val authorName: String,
    val name: String,
    val columns: List<Column>,
    val data: List<List<String>>
) {
    val hasDescription: Boolean get() = columns.any { it.type == ColumnTypes.TRANSLATION }
    val dictionaryPath: Path get() = path.resolve("${tableName.lowercase()}.xml")
    val descriptionPath: Path get() = path.resolve("${tableName.lowercase()}_description.xml")
    val tableName: String get() = name.camelToSnakeCase().uppercase()
    val descriptionTableName: String get() = "${name.camelToSnakeCase().uppercase()}_DESCRIPTION"
    val translationColumns: List<Column> get() = columns.filter { it.type == ColumnTypes.TRANSLATION }
    val ordinalColumns: List<Column> get() = columns.filter { it.type != ColumnTypes.TRANSLATION }
}
