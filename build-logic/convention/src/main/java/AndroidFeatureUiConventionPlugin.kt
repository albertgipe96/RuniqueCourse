import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.example.convention.ExtensionType
import com.example.convention.addUiDependencies
import com.example.convention.configureAndroidCompose
import com.example.convention.configureBuildTypes
import com.example.convention.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.kotlin

class AndroidFeatureUiConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.run {
            pluginManager.run {
                apply("runique.android.library.compose")
            }

            dependencies {
                addUiDependencies(target)
            }
        }
    }
}