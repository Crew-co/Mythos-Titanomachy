import org.gradle.api.tasks.Copy

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.11"
}

group = property("group") as String
version = property("version") as String

val foliaApiVersion = property("foliaApiVersion") as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    // Local dev without any GitHub token at all:
    //   host:  ./gradlew publishApiLocally
    //   core:  ./gradlew publishCoreLocally
    mavenLocal()

    // The host's addon-api.
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/${property("hostRepo")}")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
    // MythosCore's own API — roles, spirits, eras, powers, events.
    maven {
        name = "MythosCore"
        url = uri("https://maven.pkg.github.com/${property("coreRepo")}")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("dev.folia:folia-api:$foliaApiVersion")

    // The host's api: AddonBase, AddonContext, @Command.
    compileOnly("${property("hostGroup")}:mythos-addon-api:${property("hostApiVersion")}")

    // MythosCore's api: Mythos, RoleService, SpiritService, EraService, the events.
    // compileOnly, ALWAYS. At runtime these classes come out of the loaded MythosCore
    // addon (declared in addon.yml `depends:`). Shade it and every `instanceof` breaks.
    compileOnly("${property("coreGroup")}:mythos-core:${property("coreVersion")}")

    compileOnly(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveBaseName.set(providers.gradleProperty("addonName").orElse(project.name))
        archiveClassifier.set("")
        // Nothing to relocate: every dependency here is compileOnly, provided by the
        // host at runtime. If you ever add a REAL library, relocate it — two addons
        // shipping different versions of the same lib will otherwise collide.
    }
    build { dependsOn(shadowJar) }
    jar { enabled = false }
}

tasks.register<Copy>("deployAddon") {
    group = "deployment"
    description = "Builds the addon and copies it into your test server's addons folder."
    dependsOn(tasks.shadowJar)

    val target = providers.gradleProperty("testServerPath").orNull
    onlyIf {
        if (target == null) logger.lifecycle("Set testServerPath in ~/.gradle/gradle.properties to use deployAddon.")
        target != null
    }
    from(tasks.shadowJar)
    if (target != null) into("$target/plugins/${property("hostPluginName")}/addons")
}
