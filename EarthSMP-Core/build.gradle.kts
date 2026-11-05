plugins {
    java
}

group = "io.github.wedelsigma.earthsmp"
version = "0.1.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper 26.3 - stable API; dependency available from the PaperMC repository.
    compileOnly("io.papermc.paper:paper-api:26.3.build.230-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("EarthSMP-Core")
}
