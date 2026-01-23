import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.gradleup.shadow") version "9.2.0"
    id("maven-publish")
    java
    application
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("jvm") version "2.2.21"
}

group = "darkan"
version = "2.0.0"
description = "Darkan Client"

application {
    mainClass.set("com.darkan.bot.BotKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17)
	}
}

repositories {
    mavenLocal()
    mavenCentral()
}

// LWJGL3 version and native classifier configuration
val lwjglVersion = "3.3.3"
val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else if (arch.startsWith("ppc"))
                "natives-linux-ppc64le"
            else if (arch.startsWith("riscv"))
                "natives-linux-riscv64"
            else
                "natives-linux"
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64"))
                "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            else
                "natives-windows-x86"
        else -> throw Error("Unrecognized or unsupported platform: $name")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.github.classgraph:classgraph:4.8.177")
    implementation("com.github.weisj:darklaf-core:3.0.2")

    // LWJGL3 - Modern OpenGL bindings (for rendering modernization)
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-stb")  // For image loading
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)

    // JOML - Java OpenGL Math Library (for matrix/vector operations)
    implementation("org.joml:joml:1.10.5")

    //kotlinscript support
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.2.21")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitLab"
            url = uri("https://gitlab.com/api/v4/projects/42378995/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    jar {
        manifest {
            attributes(mutableMapOf("Main-Class" to "com.darkan.bot.BotKt"))
        }
    }

    shadowJar {
        archiveBaseName.set("darkan-client-shaded")
    }
}

tasks {
	build {
		dependsOn(shadowJar)
	}
}

// Task to run the rendering test application
tasks.register<JavaExec>("runRenderTest") {
    group = "application"
    description = "Run the RuneTek5 Rendering Engine Test Application"
    mainClass.set("com.darkan.rendertest.RenderingTestAppKt")
    classpath = sourceSets["main"].runtimeClasspath

    // Allow passing cache path as argument: ./gradlew runRenderTest --args="path/to/cache"
    // Default cache path is ~/.darkan/cache
}
