package com.comsysto.gradle
import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.language.cpp.plugins.CppPlugin
import org.gradle.nativeplatform.NativeLibrarySpec

class CroLaBeFraCppPlugin extends CppPlugin {

    @Override
    void apply(Project project) {
        super.apply(project)

        project.model {
            components {
                hayai(NativeLibrarySpec) {
                    sources {
                        cpp {
                            source {
                                srcDir "build/lib/hayai"
                                include "*.cpp"
                            }
                            exportedHeaders {
                                srcDir "build/lib/hayai"
                                include "*.hpp"
                            }
                        }
                    }
                }
            }
        }

        project.tasks.create(
                [
                        name  : 'downloadHayai',
                        type  : Download,
                        action: {
                            println 'Downloaded Hayai...'
                        }
                ],
                {
                    description "Download Hayai HEAD revision from Github."
                    def destFile = new File(project.buildDir, 'tmp/hayai.zip')
                    outputs.dir destFile
                    src 'https://github.com/bensteinert/hayai/archive/master.zip'
                    dest destFile
                }
        )

        project.tasks.create(
                [
                        name     : 'extractHayai',
                        dependsOn: 'downloadHayai',
                        type     : Copy,
                        action   : {
                            println 'Extracted Hayai...'
                        }
                ],
                {
                    description "Unpack Hayai library"
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
                        name     : 'installHayaiLib',
                        dependsOn: 'extractHayai',
                        type     : Copy,
                        action   : {
                            println 'Installed Hayai to project lib folder'
                        }
                ],
                {
                    mustRunAfter 'clean'
                    description "Installing Hayai to build lib directory"
                    def dest = new File(project.projectDir, 'lib/hayai')
                    def hayaiTmp = new File(project.buildDir, 'tmp/hayai/hayai-master')
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
                        type     : Copy,
                        action   : {
                            println 'Installed Hayai to project build folder'
                        }
                ],
                {
                    mustRunAfter 'clean'
                    description "Installing Hayai to build lib directory"
                    def dest = new File(project.buildDir, 'lib/hayai')
                    def hayaiTmp = new File(project.buildDir, 'tmp/hayai/hayai-master/src')
                    inputs.dir hayaiTmp
                    outputs.dir dest
                    from hayaiTmp
                    into dest
                    include '*.cpp'
                    include '*.hpp'
                }
        )

    }
}
