<#-- @ftlvariable name="" type="ru.aahzbrut.data.Sheet" -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet author="${authorName}" id="${tableName}-001">
<#list data as row>
        <insert tableName="${tableName}">
    <#list ordinalColumns as column>
        <#if row[column.ordinal]?has_content>
            <#if column.isSimple()>
            <column name="${column.name}" value="${row[column.ordinal]}"/>
            <#elseif column.isReference()>
            <column name="${column.name}" valueComputed="SELECT ${column.refTableName}_ID FROM ${r"${default-schema}"}.${column.refTableName} WHERE CODE = '${row[column.ordinal]}'"/>
            </#if>
        </#if>
    </#list>
        </insert>
</#list>
    </changeSet>

</databaseChangeLog>
