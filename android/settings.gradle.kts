pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "secure-digital-certificate-wallet"

include(
    ":app",
    ":core",
    ":core-security",
    ":core-network",
    ":core-database",
    ":core-ui",
    ":feature-auth",
    ":feature-wallet",
    ":feature-certificate",
    ":feature-scanner",
    ":feature-verification",
    ":feature-profile"
)
