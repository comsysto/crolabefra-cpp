package com.comsysto.gradle
import de.undercouch.gradle.tasks.download.Download
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.language.cpp.plugins.CppPlugin
import org.gradle.language.cpp.tasks.CppCompile
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibrarySpec

class CroLaBeFraCppPlugin extends CppPlugin {

    @Override
    void apply(Project project) {
        super.apply(project)
        def extension = project.extensions.create('crolabefra', CroLaBeFraPluginExtension)
        def hayaiTag = 'crolabefra-cpp-0.1'
        project.model {
            components {
                hayai(NativeLibrarySpec) {
                    sources {
                        cpp {
                            source {
                                srcDir 'build/lib/hayai'
                                include '*.cpp'
                            }
                            exportedHeaders {
                                srcDir 'build/lib/hayai'
                                include '*.hpp'
                            }
                            builtBy(project.tasks.getByName('installHayai'));
                        }
                    }
                    binaries.all{
                        tasks.withType(CppCompile) {
                            dependsOn 'installHayai'
                        }
                    }
                }
                hayaiRunner(NativeExecutableSpec) {
                    sources {
                        cpp {
                            source {
                                srcDir extension.benchmarksPath
                                include '*.cpp'
                            }
                        }
                    }
                    binaries.all {
                        lib library: 'hayai', linkage: 'static'
                        lib project: ':' + extension.projectToBenchmark, library: extension.outputLibraryName, linkage: 'static'
                    }
                }
            }
        }

        project.tasks.create(
                [
                        name  : 'downloadHayai',
                        type  : Download
                ],
                {
                    description 'Download Hayai HEAD revision from Github.'
                    def destFile = new File(project.buildDir, 'tmp/hayai.zip')
                    outputs.dir destFile
                    src "https://github.com/bensteinert/hayai/archive/${hayaiTag}.zip"
                    dest destFile
                }
        )

        project.tasks.create(
                [
                        name     : 'extractHayai',
                        dependsOn: 'downloadHayai',
                        type     : Copy
                ],
                {
                    description 'Unpack Hayai library'
                    mustRunAfter 'downloadHayai'
                    def dest = new File(project.buildDir, 'tmp/hayai')
                    def hayaiZipSrc = new File(project.buildDir, 'tmp/hayai.zip')
                    inputs.file hayaiZipSrc
                    outputs.dir dest
                    from project.zipTree(hayaiZipSrc)
                    into dest
                }
        )

        project.tasks.create(
                [
                        name     : 'installHayaiToLib',
                        group    : 'crolabefra support',
                        dependsOn: 'extractHayai',
                        type     : Copy
                ],
                {
                    mustRunAfter 'extractHayai'
                    description 'Installing Hayai to the project lib directory for usage with other tools'
                    def dest = new File(project.projectDir, 'lib/hayai')
                    def hayaiTmp = new File(project.buildDir, "tmp/hayai/hayai-${hayaiTag}")
                    inputs.dir hayaiTmp
                    outputs.dir dest
                    from hayaiTmp
                    into dest
                }
        )

        project.tasks.create(
                [
                        name     : 'installHayai',
                        dependsOn: 'extractHayai',
                        type     : Copy
                ],
                {
                    mustRunAfter 'clean'
                    description 'Installing Hayai source to build directory for usage with plugin'
                    def dest = new File(project.buildDir, 'lib/hayai')
                    def hayaiTmp = new File(project.buildDir, "tmp/hayai/hayai-${hayaiTag}/src")
                    inputs.dir hayaiTmp
                    outputs.dir dest
                    from hayaiTmp
                    into dest
                    include '*.cpp'
                    include '*.hpp'
                }
        )

        project.tasks.create(
                [
                        name     : 'runCppBenchmarks',
                        group    : 'crolabefra',
                        dependsOn: ['installHayai','assemble'],
                        type     : Exec
                ],
                {
                    mustRunAfter 'assemble'
                    description 'Executes assembled Hayai Cpp benchmarks'
                    workingDir './build/binaries/hayaiRunnerExecutable'
                    commandLine Os.isFamily(Os.FAMILY_WINDOWS) ? 'hayaiRunner.exe' : './hayaiRunner'
                }
        )

    }
}
