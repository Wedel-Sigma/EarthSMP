plugins {
    java
    id("com.gradleup.shadow") version "9.4.3"
}

group = "io.github.wedelsigma.earthsmp"
version = "1.8.5"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.230-stable")
    implementation("org.xerial:sqlite-jdbc:3.54.1.0")
    compileOnly("xyz.jpenilla:squaremap-api:1.4.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.yaml:snakeyaml:2.4")
    testImplementation("org.xerial:sqlite-jdbc:3.54.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Paper API is compileOnly for the deployed plugin, but JUnit tests also
// reference Bukkit types through CoreCaptureZone and need it on their classpath.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Only the shaded JAR is deployable. The plain one lacks the SQLite driver.
tasks.jar {
    archiveBaseName.set("EarthSMP-Core")
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveBaseName.set("EarthSMP-Core")
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// A successful build now also validates plugin.yml and tests SQLite war migrations.
tasks.test {
    useJUnitPlatform()
    systemProperty("earthsmp.version", project.version.toString())
}
