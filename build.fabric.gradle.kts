plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.18.2"
    id("ploceus") version "1.18.1"
}

version = "$version+mc${sc.current.version}"
group = property("maven_group") as String
base.archivesName = "${property("archives_base_name")}-fabric"

val standalone: SourceSet = sourceSets.create("standalone")
val docs: SourceSet = sourceSets.create("docs") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

repositories {
    maven("https://maven.ornithemc.net/releases")
    maven("https://jitpack.io/") {
        content { includeGroup("io.github.nevalackin") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings("net.ornithemc:feather-gen2:${property("deps.feather")}")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    implementation("io.github.nevalackin:radbus:${property("deps.radbus")}")
    include("io.github.nevalackin:radbus:${property("deps.radbus")}")

    ploceus.dependOsl(property("deps.osl") as String)
}

loom {
    runConfigs.all {
        runDirectory = rootProject.file("run")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    withSourcesJar()
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = if (name == "compileStandaloneJava") 8 else 25
    }

    processResources {
        val modVersion = project.version.toString()
        inputs.property("version", modVersion)

        filesMatching("fabric.mod.json") { expand("version" to modVersion) }
        filesMatching("mixins.illusion.json") { expand("java" to "JAVA_25") }
        exclude("mcmod.info")
    }

    jar {
        from(standalone.output)
        manifest {
            attributes("Main-Class" to "org.codeberg.illusionclient.Main")
        }

        val archivesName = base.archivesName
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${archivesName.get()}" }
        }
    }

    register<JavaExec>("docs") {
        group = "documentation"
        description = "Generates the module documentation page."

        val output = rootProject.layout.buildDirectory.file("docs/index.html")
        val runDir = layout.buildDirectory.dir("docs-run")
        val docsVersion = project.version.toString()
        val channel = providers.gradleProperty("channel").orElse("stable")

        inputs.property("version", docsVersion)
        inputs.property("channel", channel)
        outputs.file(output)

        classpath = docs.runtimeClasspath
        mainClass = "org.codeberg.illusionclient.docs.DocsGenerator"
        workingDir = runDir.get().asFile
        argumentProviders.add(CommandLineArgumentProvider {
            listOf(output.get().asFile.absolutePath, docsVersion, channel.get())
        })

        doFirst { runDir.get().asFile.mkdirs() }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to the root `build/libs/`."

        from(named<AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs"))
    }
}
