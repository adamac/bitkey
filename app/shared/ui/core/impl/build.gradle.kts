import build.wallet.gradle.logic.extensions.targets

plugins {
  id("build.wallet.kmp")
  alias(libs.plugins.compose.runtime)
}

kotlin {
  targets(ios = true, jvm = true)

  sourceSets {
    commonMain {
      dependencies {
        api(projects.shared.composeRuntimePublic)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.shared.coroutinesTesting)
        implementation(libs.kmp.molecule.runtime)
        implementation(projects.shared.uiCoreTesting)
      }
    }
  }
}
