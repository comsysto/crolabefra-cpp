# crolabefra-cpp
Gradle Extension to integrate Hayai micro benchmarks for C/C++ code into  _CroLaBeFra (Cross-Language-Benchmarking-Framework)_ - see https://github.com/bensteinert/crolabefra-setup-poc for a detailed POC and use case.

Tested with gradle up to version 2.13

## CroLaBeFra integration
This gradle plugin is part of a toolset which is instrumented with a ['mothership'](https://github.com/comsysto/crolabefra-mothership) plugin, that should be applied to a surrounding root project. Check also the POC project mentioned above!

## Usage

    plugins {
        id "com.comsysto.gradle.crolabefra.cpp" version "0.3.1"
    }
       
    crolabefra {
        projectToBenchmark = '[project folder to take into account]'
        outputLibraryName = '[nativeLibrarySpec name to link in]'
        benchmarksPath = '[folder where to find benchmark source]'
    }
    
This plugin can of course also be used without any integration with a cross-language setup!

    $ gradle runCppBenchmarks
    
 - downloads Hayai (currently [this fork](https://github.com/bensteinert/hayai/tree/crolabefra-cpp-0.2)) from github
 - defines the needed Gradle library specs
 - assembles the benchmarks executable
 - executes the given set of Hayai benchmarks assembled in one executable
 
Furthermore, with 
    
    $ gradle installHayaiToLib 

you can additionally install the Hayai library source to {PROJECT_ROOT}/lib for development of benchmarks.
