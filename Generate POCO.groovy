import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

typeMapping = [
        (~/(?i)^bit$/)                                    : "bool",
        (~/(?i)^tinyint$/)                                : "byte",
        (~/(?i)^uniqueidentifier|uuid$/)                  : "Guid",
        (~/(?i)^int|integer|number$/)                     : "int",
        (~/(?i)^bigint$/)                                 : "long",
        (~/(?i)^varbinary|image$/)                        : "byte[]",
        (~/(?i)^double|float|real$/)                      : "double",
        (~/(?i)^decimal|money|numeric|smallmoney$/)       : "decimal",
        (~/(?i)^datetimeoffset$/)                         : "DateTimeOffset",
        (~/(?i)^datetime|datetime2|timestamp|date|time$/) : "DateTime",
        (~/(?i)^char$/)                                   : "char",
        (~/(?i)^text\[\]$/)                                   : "text[]",
        (~/(?i)^int\[\]$/)                                   : "int[]"
]

notNullableTypes = [ ]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = csharpName(table.getName())
    def fields = calcFields(table)
    def fks = calcForeignKeys(table)
    new File(dir, className + ".cs").withPrintWriter { out -> generate(out, className, fields, fks) }
}

def generate(out, className, fields, fks) {
    out.println "using System;"
    out.println "#nullable enable"
    out.println "namespace PARENTNAMESPACE"
    out.println "{"
    out.println "   public class $className"
    out.println "   {"

    fields.each() {
        out.println "      /// <summary>"
        out.println "      /// "
        out.println "      /// </summary>"
        out.println "      /// <example></example>"
        out.println "      public ${it.type} ${it.name} { get; set; }"
        out.println ""
    }

    fks.each() {
        out.println "       /// <summary>"
        out.println "       /// Foreign key to ${it.name} on column(s) ${it.columns}"
        out.println "       /// </summary>"
        out.println "       /// <example></example>"
        out.println "       public ${it.name} ${it.name} { get; set; }"
        out.println ""
    }
    out.println "   }"
    out.println "}"
    out.println "#nullable restore"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }?.value ?: "string"
        def nullable = col.isNotNull() || typeStr in notNullableTypes ? "" : "?"
        fields += [[
                           name : csharpName(col.getName()),
                           type : typeStr + nullable]]
    }
}

// https://gist.github.com/SoapCanFly/a4d4e5450e2855155fd7ccdf85f61321
def calcForeignKeys(table) {
    DasUtil.getForeignKeys(table).reduce([]) {fks, key->
        fks += [[
            name: key.getRefTableName(),
             columns: key.getColumnsRef().names().join(",") ]]
    }
}

def csharpName(str) {
    com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it) }
            .join("")
}
