plugins {
    `kotlin-dsl`
}

val jacksonVersion = "2.16.1"
val freeMarkerVersion = "2.3.32"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.freemarker:freemarker:$freeMarkerVersion")
}
