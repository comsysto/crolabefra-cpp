# crolabefra-cpp
Gradle Extension to integrate Hayai micro benchmarks for C/C++ code into  _CroLaBeFra (Cross-Language-Benchmarking-Framework)_ - see https://github.com/bensteinert/crolabefra-setup-poc for a detailed POC and use case.

## Usage

    plugins {
        id "com.comsysto.gradle.crolabefra.cpp" version "0.2.0"
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

Gradle additionally installs the Hayai library source to {PROJECT_ROOT}/lib for development of benchmarks.
    

