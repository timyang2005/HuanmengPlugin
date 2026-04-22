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
        // LightNovelReader Plugin API — GitHub Packages
        maven {
            url = uri("https://maven.pkg.github.com/dmzz-yyhyy/LightNovelReader")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull ?: ""
            }
        }
        // 备用：JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "HuanmengPlugin"
include(":plugin")
