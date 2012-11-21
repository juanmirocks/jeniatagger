# jeniatagger

Java port of the [GENIA Tagger (C++)](http://www.nactem.ac.uk/tsujii/GENIA/tagger/).


## Why?

The original C++ program contains [several issues](https://github.com/jmcejuela/geniatagger#known-issues). The most important problem is that the tagger uses big dictionaries that have to be reloaded each time the program is called (taking on a modern machine ~15 seconds). With this port if you run on the JVM, the dictionaries are loaded only once and kept in memory until the JVM finishes. Therefore, subsequent calls do not have any loading overhead and so run much faster. With this port it is also possible to specify exactly which analyses you want, namely, from the original, `base form`, `pos tag`, `shallow parsing`, and `named-entity recognition`. Thus you can make the program run more efficiently both in time and memory.


## State of the port

* It is known that for some instances jenia's output for named-entity recognition is not the same as the original's.
* All other analyses (`base form`, `pos tag`, and `shallow parsing`) have been thoroughly and successfully tested.
* The built-in tokenizer has not been implemented yet. For now, you have to use your own. See the [original README](https://github.com/jmcejuela/jeniatagger/ORIGINAL_GENIATAGGER_README). Contributions are welcome.
* Besides some few improvements and refactoring, for now the java code resembles almost exactly the original's.


## Installation

For now,

    git clone https://github.com/jmcejuela/jeniatagger.git
    cd jeniatagger

    mvn -Dmaven.test.skip=true install #install locally in your m2 repository
    mvn assembly:single #create a single executable jar and run the program from the command line [as the original](https://github.com/jmcejuela/jeniatagger/ORIGINAL_GENIATAGGER_README).


## How-to use it

TODO


## The original code

See [my fork](https://github.com/jmcejuela/geniatagger) of the C++ code.