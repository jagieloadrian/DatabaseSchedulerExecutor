plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = project.property("group") as String
version = project.property("version") as String

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.github.jsqlparser:jsqlparser:5.1")

    //jdbc drivers
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.10")
    implementation("com.microsoft.sqlserver:mssql-jdbc:13.6.0.jre11")
    implementation("com.oracle.database.jdbc:ojdbc11:23.8.0.25.04")

    //logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

    //testcontainers (E2E against real Postgres/MySQL)
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.anjo.MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.anjo.MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
