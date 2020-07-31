package org.jetbrains.dokka.gradle

import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.Platform
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GradleDokkaSourceSetBuilderTest {


    private val project = ProjectBuilder.builder().withName("root").build()

    @Test
    fun sourceSetId() {
        val sourceSet = GradleDokkaSourceSetBuilder("myName", project)
        assertEquals(
            DokkaSourceSetID(project, "myName"), sourceSet.sourceSetID,
            "Expected sourceSet.sourceSetID to match output of DokkaSourceSetID factory function"
        )

        assertEquals(
            ":/myName", sourceSet.sourceSetID.toString(),
            "Expected SourceSetId's string representation"
        )
    }

    @Test
    fun classpath() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.classpath.from(project.file("path/to/file.jar"))
        sourceSet.classpath.from(project.file("path/to/other.jar"))

        assertEquals(
            listOf(project.file("path/to/file.jar"), project.file("path/to/other.jar")), sourceSet.classpath.toList(),
            "Expected both file paths being present in classpath"
        )

        assertEquals(
            listOf(project.file("path/to/file.jar"), project.file("path/to/other.jar")),
            sourceSet.build().classpath.toList(),
            "Expected both file paths being present in built classpath"
        )
    }

    @Test
    fun moduleDisplayName() {
        val sourceSet = GradleDokkaSourceSetBuilder("myName", project)

        assertNull(
            sourceSet.moduleDisplayName.getSafe(),
            "Expected no ${GradleDokkaSourceSetBuilder::moduleDisplayName.name} being set by default"
        )

        assertEquals(
            "root", sourceSet.build().moduleDisplayName,
            "Expected project name being used for ${DokkaConfiguration.DokkaSourceSet::moduleDisplayName.name} " +
                    "after building source set with no ${GradleDokkaSourceSetBuilder::moduleDisplayName.name} being set"
        )

        sourceSet.moduleDisplayName by "displayName"

        assertEquals(
            "displayName", sourceSet.build().moduleDisplayName,
            "Expected previously set ${GradleDokkaSourceSetBuilder::displayName.name} to be present after build"
        )
    }

    @Test
    fun displayName() {
        val sourceSet = GradleDokkaSourceSetBuilder("myName", project)
        assertNull(
            sourceSet.displayName.getSafe(),
            "Expected no ${GradleDokkaSourceSetBuilder::displayName.name} being set by default"
        )

        assertEquals(
            "myName", sourceSet.build().displayName,
            "Expected source set name being used for ${DokkaConfiguration.DokkaSourceSet::displayName.name} " +
                    "after building source set with no ${GradleDokkaSourceSetBuilder::displayName.name} being set"
        )

        sourceSet.displayName by "displayName"

        assertEquals(
            "displayName", sourceSet.build().displayName,
            "Expected previously set ${GradleDokkaSourceSetBuilder::displayName.name} to be present after build"
        )
    }

    @Test
    fun `displayName default for sourceSet ending with Main`() {
        val sourceSet = GradleDokkaSourceSetBuilder("jvmMain", project)
        assertEquals(
            "jvm", sourceSet.build().displayName,
            "Expected 'Main' being stripped for source set display name after build"
        )
    }

    @Test
    fun `displayName default for sourceSet with platform specified`() {
        val sourceSet = GradleDokkaSourceSetBuilder("myName", project)
        sourceSet.platform.set(Platform.jvm.name)

        assertEquals(
            Platform.jvm.name, sourceSet.build().displayName,
            "Expected platform being used as fallback as source set display name after build"
        )
    }

    @Test
    fun sourceRoots() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.sourceRoots.from(project.file("root1"))
        sourceSet.sourceRoot(project.file("root2"))
        sourceSet.sourceRoot(project.file("root3").absolutePath)
        sourceSet.sourceRoot("root4")

        assertEquals(
            listOf("root1", "root2", "root3", "root4").map(project::file).toSet(),
            sourceSet.build().sourceRoots,
            "Expected all files being present"
        )

        sourceSet.build().sourceRoots.forEach { root ->
            assertTrue(
                root.startsWith(project.projectDir),
                "Expected all roots to be inside the projectDir\n" +
                        "projectDir: ${project.projectDir}\n" +
                        "root: ${root.absolutePath})"
            )
        }
    }

    @Test
    fun dependentSourceSets() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().dependentSourceSets, "Expected no dependent sourceSets by default")

        sourceSet.dependentSourceSets.add(DokkaSourceSetID(project, "s1"))
        sourceSet.dependsOn("s2")
        sourceSet.dependsOn(DokkaSourceSetID(project, "s3"))
        sourceSet.dependsOn(GradleDokkaSourceSetBuilder("s4", project))
        sourceSet.dependsOn(GradleDokkaSourceSetBuilder("s5", project).build())
        sourceSet.dependsOn(DefaultKotlinSourceSet(project, "s6"))
        sourceSet.dependsOn(DefaultAndroidSourceSet("s7", project, false))

        assertEquals(
            listOf(":/s1", ":/s2", ":/s3", ":/s4", ":/s5", ":/s6", ":/s7"),
            sourceSet.build().dependentSourceSets.map { it.toString() },
            "Expected all source sets being registered"
        )
    }

    @Test
    fun samples() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().samples, "Expected no default samples")
        sourceSet.samples.from(project.file("s1"))
        sourceSet.samples.from(project.file("s2"))
        assertEquals(
            setOf(project.file("s1"), project.file("s2")), sourceSet.build().samples,
            "Expected all samples being present after build"
        )
    }

    @Test
    fun includes() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().includes, "Expected no default includees")
        sourceSet.includes.from(project.file("i1"))
        sourceSet.includes.from(project.file("i2"))
        assertEquals(
            setOf(project.file("i1"), project.file("i2")), sourceSet.build().includes,
            "Expected all includes being present after build"
        )
    }

    @Test
    fun includeNonPublic() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(
            DokkaDefaults.includeNonPublic, sourceSet.build().includeNonPublic,
            "Expected default value for ${GradleDokkaSourceSetBuilder::includeNonPublic.name}"
        )

        sourceSet.includeNonPublic.set(!DokkaDefaults.includeNonPublic)
        assertEquals(
            !DokkaDefaults.includeNonPublic, sourceSet.build().includeNonPublic,
            "Expected flipped value for ${GradleDokkaSourceSetBuilder::includeNonPublic.name}"
        )
    }

    @Test
    fun reportUndocumented() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(
            DokkaDefaults.reportUndocumented, sourceSet.build().reportUndocumented,
            "Expected default value for ${GradleDokkaSourceSetBuilder::reportUndocumented.name}"
        )

        sourceSet.reportUndocumented.set(!DokkaDefaults.reportUndocumented)
        assertEquals(
            !DokkaDefaults.reportUndocumented, sourceSet.build().reportUndocumented,
            "Expected flipped value for ${GradleDokkaSourceSetBuilder::reportUndocumented.name}"
        )
    }

    @Test
    fun jdkVersion() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(
            DokkaDefaults.jdkVersion, sourceSet.build().jdkVersion,
            "Expected default value for ${GradleDokkaSourceSetBuilder::jdkVersion.name}"
        )

        sourceSet.jdkVersion.set(DokkaDefaults.jdkVersion + 1)
        assertEquals(
            DokkaDefaults.jdkVersion + 1, sourceSet.build().jdkVersion,
            "Expected increased value for ${GradleDokkaSourceSetBuilder::jdkVersion.name}"
        )
    }

    @Test
    fun sourceLinks() {
        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        assertEquals(emptySet(), sourceSet.build().sourceLinks, "Expected no default source links")

        sourceSet.sourceLinks.add(
            GradleSourceLinkBuilder(project).apply {
                this.lineSuffix by "ls1"
                this.path by "p1"
                this.url by "u1"
            })

        sourceSet.sourceLink {
            it.lineSuffix by ""
        }

    }
}
