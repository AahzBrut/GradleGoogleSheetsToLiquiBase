<#-- @ftlvariable name="" type="ru.aahzbrut.data.Sheet" -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet author="${authorName}" id="${descriptionTableName}-001">
    <#list data as row>
        <insert tableName="${descriptionTableName}">
            <column name="${tableName}_ID" valueComputed="SELECT ${tableName}_ID FROM ${r"${default-schema}"}.${tableName} WHERE CODE = '${row[0]}'"/>
            <column name="LANGUAGE_ID" valueComputed="SELECT LANGUAGE_ID FROM ${r"${default-schema}"}.LANGUAGE WHERE CODE = 'RU'"/>
        <#list translationColumns as ruColumn>
            <#if ruColumn?counter < 3>
            <column name="${ruColumn.cleanName}" value="${row[ruColumn.ordinal]}"/>
            </#if>
        </#list>
        </insert>
        <insert tableName="${descriptionTableName}">
            <column name="${tableName}_ID" valueComputed="SELECT ${tableName}_ID FROM ${r"${default-schema}"}.${tableName} WHERE CODE = '${row[0]}'"/>
            <column name="LANGUAGE_ID" valueComputed="SELECT LANGUAGE_ID FROM ${r"${default-schema}"}.LANGUAGE WHERE CODE = 'EN'"/>
        <#list translationColumns as enColumn>
            <#if enColumn?counter &gt; 2>
            <column name="${enColumn.cleanName}" value="${row[enColumn.ordinal]}"/>
            </#if>
        </#list>
        </insert>
    </#list>
    </changeSet>

</databaseChangeLog>
