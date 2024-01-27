package ru.aahzbrut.data

data class Column(
    val ordinal: Int,
    val type: ColumnTypes,
    val name: String,
    val refTableName: String,
) {
    val isSimple: Boolean get() = type == ColumnTypes.SIMPLE
    val isReference: Boolean get() = type == ColumnTypes.REFERENCE
    val isTranslation: Boolean get() = type == ColumnTypes.TRANSLATION
    val cleanName: String get() = name.substring(3)
}
