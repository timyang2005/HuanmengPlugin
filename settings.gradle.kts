pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LightNovelReader Plugin API — 官方公共 Maven 仓库
        maven { url = uri("https://maven.nariko.org/release") }
        // 备用：JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "HuanmengPlugin"
include(":plugin")
