import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.tinyremapper.IMappingProvider
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    id("xyz.wagyourtail.unimined") version "1.4.1"
    id("xyz.wagyourtail.jvmdowngrader") version "1.3.4"
    id("com.gradleup.shadow") version "9.3.2"
}

version = "$version+mc${sc.current.version}"
group = property("maven_group") as String
base.archivesName = "${property("archives_base_name")}-forge"

val standalone: SourceSet = sourceSets.create("standalone")

val shade: Configuration = configurations.create("shade")
val devMixin: Configuration = configurations.create("devMixin")
val devRealms: Configuration = configurations.create("devRealms")
configurations.implementation { extendsFrom(shade) }

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") {
        content { includeGroup("net.fabricmc") }
    }
    maven("https://maven.cleanroommc.com/") {
        content { includeGroup("zone.rong") }
    }
    maven("https://jitpack.io/") {
        content { includeGroup("io.github.nevalackin") }
    }
}

val remapDevRealms = tasks.register("remapDevRealms") {
    val input = devRealms
    val unimined = layout.projectDirectory.dir(".gradle/unimined/local")
    val output = layout.buildDirectory.file("devRealms/realms-feather.jar")
    inputs.files(input)
    outputs.file(output)

    doLast {
        val local = unimined.asFile
        val mappings = local.resolve("mappings").walk().filter { it.extension == "umf" }.maxBy { it.lastModified() }
        val minecraft = local.resolve("minecraft").walk().filter { it.name.endsWith("-searge.jar") }.maxBy { it.lastModified() }

        val provider = IMappingProvider { acceptor ->
            var owner: String? = null
            mappings.forEachLine { line ->
                val parts = line.split('\t')
                when {
                    parts[0] == "c" -> owner = parts[2].takeIf { it.startsWith("net/minecraft/realms/") }
                    owner == null || parts.size < 6 -> {}
                    parts[1] == "m" -> {
                        val (name, desc) = parts[3].split(';', limit = 2)
                        acceptor.acceptMethod(IMappingProvider.Member(owner, name, desc), parts[5].substringBefore(';'))
                    }
                    parts[1] == "f" -> acceptor.acceptField(IMappingProvider.Member(owner, parts[3], null), parts[5])
                }
            }
        }

        val remapper = TinyRemapper.newRemapper().withMappings(provider).ignoreFieldDesc(true).build()
        val out = output.get().asFile.apply { delete() }
        try {
            OutputConsumerPath.Builder(out.toPath()).build().use { consumer ->
                val jar = input.singleFile.toPath()
                consumer.addNonClassFiles(jar, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readClassPath(minecraft.toPath())
                remapper.readInputs(jar)
                remapper.apply(consumer)
            }
        } finally {
            remapper.finish()
        }
    }
}

unimined.minecraft {
    version(sc.current.version)

    mappings {
        ornitheGenVersion = 2
        calamus()
        feather(property("deps.feather") as String)
        searge()

        stubs("searge", "feather") {
            c("net/minecraft/entity/item/EntityMinecart", "net/minecraft/entity/vehicle/MinecartEntity") {
                m("func_174898_m;()D", "getVanillaMaxSpeed")
            }
            c("net/minecraft/util/RegistryNamespaced", "net/minecraft/util/registry/IdRegistry") {
                m("func_148757_b;(Ljava/lang/Object;)I", "getIdForObject")
            }
            c("net/minecraft/util/RegistryNamespacedDefaultedByKey", "net/minecraft/util/registry/DefaultedIdRegistry") {
                m("func_148757_b;(Ljava/lang/Object;)I", "getIdForObject")
            }
        }

        stubs("searge", "feather") {
            c("net/minecraft/client/resources/FallbackResourceManager", "net/minecraft/client/resource/manager/FallbackResourceManager") {
                f("field_110540_a", "field_110540_a")
            }
            c("net/minecraft/client/resources/SimpleReloadableResourceManager", "net/minecraft/client/resource/manager/SimpleReloadableResourceManager") {
                f("field_110548_a", "field_110548_a")
            }
            c("net/minecraft/client/resources/AbstractResourcePack", "net/minecraft/client/resource/pack/CustomResourcePack") {
                f("field_110597_b", "field_110597_b")
            }
            c("net/minecraft/tileentity/TileEntity", "net/minecraft/block/entity/BlockEntity") {
                f("field_145855_i", "field_145855_i")
            }
            c("net/minecraft/client/gui/GuiSelectWorld", "net/minecraft/client/gui/screen/menu/world/SelectWorldScreen") {
                f("field_146634_i", "field_146634_i")
            }
            c("net/minecraft/server/MinecraftServer", "net/minecraft/server/MinecraftServer") {
                f("field_71316_v", "field_71316_v")
            }
            c("net/minecraft/world/storage/SaveHandler", "net/minecraft/world/storage/AlphaWorldStorage") {
                f("field_75771_c", "field_75771_c")
            }
        }
    }

    minecraftForge {
        loader(property("deps.forge") as String)
        mixinConfig("mixins.illusion.json")
    }

    runs.config("client") {
        workingDir = rootProject.file("run")

        val devJar = tasks.named<Jar>("shadeDowngradedApi").flatMap { it.archiveFile }
        dependsOn(devJar)
        classpath = files(devJar) + shade.filter { !it.name.startsWith("mixinextras") } + devMixin +
            files(remapDevRealms) +
            (sourceSets.main.get().runtimeClasspath - sourceSets.main.get().output - shade)
                .filter { !it.name.startsWith("realms-") }
        jvmArgs("-noverify")
    }
}

dependencies {
    shade("io.github.nevalackin:radbus:${property("deps.radbus")}")
    shade("it.unimi.dsi:fastutil:${property("deps.fastutil")}")
    shade("com.google.code.gson:gson:${property("deps.gson")}")

    shade("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")

    compileOnly("net.fabricmc:sponge-mixin:${property("deps.mixin")}")
    devMixin("zone.rong:mixinbooter:${property("deps.mixinbooter")}") { isTransitive = false }
    devRealms("com.mojang:realms:1.7.59") { isTransitive = false }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    withSourcesJar()
}

jvmdg {
    downgradeTo = JavaVersion.VERSION_1_8
}

tasks {
    matching { it.javaClass.name.startsWith("xyz.wagyourtail.unimined") }.configureEach {
        notCompatibleWithConfigurationCache("Unimined does not support the configuration cache")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = if (name == "compileStandaloneJava") 8 else 25
    }

    processResources {
        val modVersion = project.version.toString()
        inputs.property("version", modVersion)

        filesMatching("mcmod.info") { expand("version" to modVersion) }
        filesMatching("mixins.illusion.json") { expand("java" to "JAVA_8") }
        exclude("fabric.mod.json")
    }

    jar {
        archiveClassifier = "dev"
    }

    shadowJar {
        enabled = false
    }

    downgradeJar {
        inputFile = jar.flatMap { it.archiveFile }
        archiveClassifier = "downgraded"
    }

    shadeDowngradedApi {
        archiveClassifier = "downgraded-api"
        shadePath = { "org/codeberg/illusionclient/lib/jvmdg/" + it.substringBefore(".").substringBeforeLast("-").replace(Regex("[.;\\[/]"), "-") }
    }

    val remapJar = named<Jar>("remapJar") {
        (this as RemapJarTask).inputFile = shadeDowngradedApi.flatMap { it.archiveFile }
        archiveClassifier = "remapped"
    }

    val bundleJar = register<ShadowJar>("bundleJar") {
        group = "build"
        archiveClassifier = ""
        configurations = listOf(shade)

        from(remapJar.map { zipTree(it.archiveFile) })
        from(standalone.output)

        relocate("it.unimi.dsi.fastutil", "org.codeberg.illusionclient.lib.fastutil")
        relocate("com.google.gson", "org.codeberg.illusionclient.lib.gson")
        relocate("com.llamalad7.mixinextras", "org.codeberg.illusionclient.lib.mixinextras")
        minimize {
            exclude(dependency("io.github.nevalackin:radbus:.*"))
            exclude(dependency("io.github.llamalad7:mixinextras-common:.*"))
        }
        exclude("META-INF/maven/**", "META-INF/LICENSE*", "META-INF/NOTICE*", "**/module-info.class")

        manifest {
            attributes(
                "Main-Class" to "org.codeberg.illusionclient.Main",
                "MixinConfigs" to "mixins.illusion.json",
            )
        }

        val archivesName = base.archivesName
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${archivesName.get()}" }
        }
    }

    assemble {
        dependsOn(bundleJar)
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to the root `build/libs/`."

        from(bundleJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs"))
    }
}
