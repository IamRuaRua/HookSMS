pluginManagement {
    repositories {
        val useAliyun = System.getenv("USE_ALIYUN_MIRRORS")?.toBooleanStrictOrNull()
            ?: (System.getenv("CI") == null && System.getenv("GITHUB_ACTIONS") == null)
        if (useAliyun) {
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val useAliyun = System.getenv("USE_ALIYUN_MIRRORS")?.toBooleanStrictOrNull()
            ?: (System.getenv("CI") == null && System.getenv("GITHUB_ACTIONS") == null)
        if (useAliyun) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        maven { url = uri("https://api.xposed.info/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "HookSMS"
include(":app")
