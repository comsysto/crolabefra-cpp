package com.comsysto.gradle

import de.undercouch.gradle.tasks.download.Download
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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
        def repository = 'bensteinert'
        def hayaiTag = 'crolabefra-cpp-0.2'

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
                    binaries.all {
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
                        name: 'downloadHayai',
                        type: Download
                ],
                {
                    description 'Download Hayai HEAD revision from Github.'
                    def destFile = new File(project.buildDir, 'tmp/hayai.zip')
                    outputs.dir destFile
                    src "https://github.com/${repository}/hayai/archive/${hayaiTag}.zip"
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

        def crolabefraCpp = project.tasks.create(
                [
                        name     : 'runCppBenchmarks',
                        group    : 'crolabefra',
                        dependsOn: ['installHayai', 'assemble'],
                        type     : Exec
                ],
                {
                    mustRunAfter 'assemble'
                    description 'Executes assembled Hayai Cpp benchmarks'
                    workingDir './build/exe/hayaiRunner'
                    commandLine Os.isFamily(Os.FAMILY_WINDOWS) ? 'hayaiRunner.exe' : './hayaiRunner'
                }
        )



        crolabefraCpp.doLast {
            File file = new File(project.buildDir, 'exe/hayaiRunner/result.json')
            if (file.exists()) {

                def json = file.withReader { reader ->
                    new JsonSlurper().parse(reader)
                }

                def mappedResultList = []
                json.each { benchmark ->
                    def Map mappedResult = [:]

                    mappedResult.group = benchmark.group
                    mappedResult.name = benchmark.name

                    mappedResult.numberOfIterationsPerRun = Integer.valueOf(benchmark.numberOfIterationsPerRun)
                    mappedResult.averageTime = Double.valueOf(benchmark.averagePerRun) / 1_000_000d
                    mappedResult.fastestTime = Double.valueOf(benchmark.fastestRun) / 1_000_000d
                    mappedResult.slowestTime = Double.valueOf(benchmark.slowestRun) / 1_000_000d
                    mappedResult.numberOfRuns = Integer.valueOf(benchmark.numberOfRuns)
                    mappedResult.totalTime = Double.valueOf(benchmark.totalTime) / 1_000_000d
                    mappedResult.unit = 'ms'

                    mappedResultList.add(mappedResult)
                }


                File destFile = new File(project.buildDir, 'results/crolabefra-cpp.json')
                destFile.getParentFile().mkdirs()
                if (destFile.exists()){
                    destFile.delete()
                }
                destFile.createNewFile();

                // for now, map values though as they are
                destFile.withWriter('UTF-8', {writer ->
                    writer.write(JsonOutput.prettyPrint(JsonOutput.toJson(mappedResultList)))
                })

                // check whether mothership is reachable
                def rootProject = project.getRootProject()
                if (rootProject.getTasksByName('crolabefra', false)) {
                    println('Mothership is there :)')
                    // write mapped results back to dest file
                    File rootDestFile = new File(rootProject.buildDir, 'results/mothership/data/crolabefra-cpp.js')
                    rootDestFile.getParentFile().mkdirs()
                    if (rootDestFile.exists()) {
                        rootDestFile.delete()
                    }
                    rootDestFile.createNewFile();
                    rootDestFile.withWriter('UTF-8', { writer ->
                        writer.write("crolabefra.data.cpp = ")
                        writer.write(JsonOutput.prettyPrint(JsonOutput.toJson(mappedResultList)))
                    })
                } else {
                    println('No mothership :(')
                }
            }
        }
    }
}
