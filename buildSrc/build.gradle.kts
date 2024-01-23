plugins {
    `kotlin-dsl`
}

val jacksonVersion = "2.16.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}
