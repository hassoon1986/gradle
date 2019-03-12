/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.kotlin.dsl.tooling.builders.r54

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.test.fixtures.file.LeaksFileHandles

import org.gradle.kotlin.dsl.tooling.builders.AbstractKotlinScriptModelCrossVersionTest

import static org.junit.Assert.assertThat


@TargetGradleVersion(">=5.4")
class KotlinSettingsScriptModelCrossVersionSpec extends AbstractKotlinScriptModelCrossVersionTest {

    def "can fetch classpath of settings script"() {

        given:
        withBuildSrc()

        and:
        def settingsDependency = withFile("settings-dependency.jar")
        def settings = withSettings("""
            buildscript {
                dependencies {
                    classpath(files("${normalizedPathOf(settingsDependency)}"))
                }
            }
        """)

        and:
        def projectDependency = withFile("project-dependency.jar")
        file("build.gradle") << """
            buildscript {
                dependencies {
                    classpath(files("${normalizedPathOf(projectDependency)}"))
                }
            }
        """

        when:
        def classPath = canonicalClassPathFor(projectDir, settings)

        then:
        assertContainsBuildSrc(classPath)
        assertContainsGradleKotlinDslJars(classPath)
        assertIncludes(classPath, settingsDependency)
        assertExcludes(classPath, projectDependency)
    }

    def "can fetch classpath of settings script plugin"() {

        given:
        withBuildSrc()
        withDefaultSettings()

        and:
        def settingsDependency = withFile("settings-dependency.jar", "")
        def settings = withFile("my.settings.gradle.kts", """
            buildscript {
                dependencies {
                    classpath(files("${normalizedPathOf(settingsDependency)}"))
                }
            }
        """)

        and:
        def projectDependency = withFile("project-dependency.jar", "")
        withFile("build.gradle", """
            buildscript {
                dependencies {
                    classpath(files("${normalizedPathOf(projectDependency)}"))
                }
            }
        """)

        when:
        def classPath = canonicalClassPathFor(projectDir, settings)

        then:
        assertContainsBuildSrc(classPath)
        assertContainsGradleKotlinDslJars(classPath)
        assertIncludes(classPath, settingsDependency)
        assertExcludes(classPath, projectDependency)
    }

    @LeaksFileHandles("Kotlin compiler daemon on buildSrc jar")
    def "sourcePath includes buildSrc source roots"() {

        given:
        withKotlinBuildSrc()
        def settings = withSettings("""include(":sub")""")

        expect:
        assertThat(
            sourcePathFor(settings),
            matchesProjectsSourceRoots(withMainSourceSetJavaKotlinIn("buildSrc")))
    }

    @LeaksFileHandles("Kotlin compiler daemon on buildSrc jar")
    def "sourcePath includes buildSrc project dependencies source roots"() {

        given:
        def sourceRoots = withMultiProjectKotlinBuildSrc()
        def settings = withSettings("""include(":sub")""")

        expect:
        assertThat(
            sourcePathFor(settings),
            matchesProjectsSourceRoots(sourceRoots))
    }
}
