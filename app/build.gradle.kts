import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.google.devtools.ksp)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.google.protobuf)
	alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
	namespace = "com.cbkii.btandroidts"
	compileSdk = libs.versions.android.compilesdk.get().toInt()

	defaultConfig {
		applicationId = "com.cbkii.btandroidts"
		minSdk = libs.versions.android.minsdk.get().toInt()
		targetSdk = libs.versions.android.targetsdk.get().toInt()
		versionCode = 180007
		versionName = "19.0.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	flavorDimensions += "distribution"
	productFlavors {
		create("standard") {
			dimension = "distribution"
		}
		create("ts18Privileged") {
			dimension = "distribution"
		}
	}

	signingConfigs {
		val keySecretFile = rootProject.file("keystore.properties")
		if (!keySecretFile.exists()) return@signingConfigs

		val properties = Properties()
		keySecretFile.inputStream().use { properties.load(it) }
		val userHome = System.getProperty("user.home")
		val storeFileName = properties.getProperty("STORE_FILE_NAME")
		val keyStoreFolder = File(userHome, "keystore")
		if (!keyStoreFolder.exists()) return@signingConfigs

		val keyStoreFile = File(keyStoreFolder, storeFileName)
		if (!keyStoreFile.exists()) return@signingConfigs

		create("release") {
			storeFile = keyStoreFile
			keyAlias = properties.getProperty("KEY_ALIAS")
			keyPassword = properties.getProperty("KEY_PASSWORD")
			storePassword = properties.getProperty("STORE_PASSWORD")
		}
	}

	buildTypes {
		debug {
			applicationIdSuffix = ".debug"
			isMinifyEnabled = false
		}
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			multiDexEnabled = true
			signingConfig = signingConfigs.findByName("release")
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
		optIn.add("kotlin.time.ExperimentalTime")
		optIn.add("kotlin.uuid.ExperimentalUuidApi")
	}
}

composeCompiler {
//	featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
	metricsDestination = layout.buildDirectory.dir("compose_compiler")
	reportsDestination = layout.buildDirectory.dir("compose_compiler")
	stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.bundles.compose)
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.compose.destination.core)
	ksp(libs.compose.destination.ksp)
	implementation(libs.bundles.kotlinx)
	implementation(libs.androidx.core.splashscreen)
	implementation(platform(libs.koin.bom))
	implementation(libs.bundles.koin)
	implementation(libs.androidx.graphics.shapes)
	implementation(libs.androidx.datastore)
	implementation(libs.protobuf.javalite)
	implementation(libs.protobuf.kotlin.lite)

	testImplementation(libs.junit)
	testImplementation(libs.kotlinx.coroutines.test)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)

	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
}

protobuf {
	protoc {
		artifact = libs.protobuf.protoc.compiler.get().toString()
	}
	plugins {
		create("java") {
			artifact = libs.protobuf.protoc.gen.javalite.get().toString()
		}
	}
	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
				create("java") {
					option("lite")
				}
				create("kotlin") {
					option("lite")
				}
			}
		}
	}
}
