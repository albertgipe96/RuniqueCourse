import com.android.build.api.dsl.LibraryExtension
import com.example.convention.ExtensionType
import com.example.convention.configureBuildTypes
import com.example.convention.configureKotlinAndroid
import com.example.convention.configureKotlinJvm
import com.example.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class JvmKtorConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.run {
            pluginManager.run {
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            dependencies {
                "implementation"(libs.findBundle("ktor").get())
            }
        }
    }
}