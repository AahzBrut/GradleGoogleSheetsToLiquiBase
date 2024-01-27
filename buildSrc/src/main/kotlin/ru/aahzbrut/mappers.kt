package ru.aahzbrut

import ru.aahzbrut.data.Column
import ru.aahzbrut.data.ColumnTypes
import ru.aahzbrut.data.Sheet
import java.nio.file.Path
import kotlin.io.path.Path


fun List<List<String>>.toSheet(sheetName: String, authorName: String, rootPath: Path): Sheet {
    val subPath = this[0][0]
    val columns = mutableListOf<Column>()
    val descriptionOffset = this[2].indexOf("RU_NAME")
    for (index in 0 until this[0].size){
        columns += Column(
            index,
            decodeColumnType(descriptionOffset, index),
            this[2][index],
            this[1][index]
        )
    }
    return Sheet(
        Path(rootPath.toString(), subPath),
        authorName,
        sheetName,
        columns,
        this.drop(3).map { row -> row.map { it.replace("|", ",") } }
    )
}

private fun List<List<String>>.decodeColumnType(
    descriptionOffset: Int,
    index: Int
) = if (descriptionOffset in 1..index) {
    ColumnTypes.TRANSLATION
} else if (this[1][index].isBlank()) {
    ColumnTypes.SIMPLE
} else {
    ColumnTypes.REFERENCE
}

fun String.camelToSnakeCase(): String {
    val pattern = "(?<=.)[A-Z]".toRegex()
    return this.replace(pattern, "_$0").lowercase()
}
