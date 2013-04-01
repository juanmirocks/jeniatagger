# jeniatagger

Java port of the [GENIA Tagger (C++)](http://www.nactem.ac.uk/tsujii/GENIA/tagger/).


## Why?

The original C++ program contains [several issues](https://github.com/jmcejuela/geniatagger#known-issues). The most important problem is that the tagger uses big dictionaries that have to be reloaded each time the program is called (taking on a modern machine ~15 seconds). With this port if you run on the JVM, the dictionaries are loaded only once and kept in memory until the JVM finishes. Therefore, subsequent calls do not have any loading overhead and so run much faster. With this port it is also possible to specify exactly which analyses you want, namely, to choose from the original, `base form`, `pos tag`, `shallow parsing`, and `named-entity recognition`. Thus you can make the program run more efficiently both in time and memory.


## State of the port

* The output of the original tagger and jenia's has been thoroughly and successfully tested to be the same.
* It is known that when unicode characters appear in an instance, some few analyses differ from the original's. In fact, the original c++ code did not handle unicode well. It used std::string which makes a unicode character of >= 2 bytes appear in a string as multiple characters and thus wrongnly increment the size of the containing string. This leads to unexpected behavior in the same original code. Therefore comparison tests are difficult to carry for unicode.
* The built-in tokenizer has not been implemented yet. For now, you have to use your own. See the [original README](https://github.com/jmcejuela/jeniatagger/ORIGINAL_GENIATAGGER_README). Contributions are welcome.
* Besides some few improvements and refactoring, for now the java code resembles almost exactly the original's.


## Installation

First of all you need to download the [jeniatagger models](http://sourceforge.net/projects/jeniatagger/files/models.zip/download). Take note of the local path where you download it.

### To use from the command line

Download the [runnable jar](http://sourceforge.net/projects/jeniatagger/files/jeniatagger-0.4.0-jar-with-dependencies.jar/download).

### To use from Java or a JVM language

    git clone https://github.com/jmcejuela/jeniatagger.git
    cd jeniatagger
    mvn install -DskipTests # install it in your local maven repository
    # mvn assembly:single # create a runnable jar if you wanna use your source modifications from the command line

## Use

### To use from the command line

Having followed the installation instructions:

`java -jar <runnable_jar_path> --models <models_path> --nt <some_file>`

(without a given file the program will analyze sentences directly from the command line input)

Each sentence is written in a new line. Empty lines are ignored. The words in the sentence must be already tokenized so that the sentence is a list of space-seprated tokens. For instance:

    Degenerin / Epithelial Sodium Channel ( DEG / ENaC )
    Drosophila TRPA channel Pyrexia : Pyx - A and Pyx - B
    ...

### To use from Java or a JVM langauge

The API hasn't been formally defined yet. For now see [JeniaTagger](https://github.com/jmcejuela/jeniatagger/blob/develop/src/main/java/com/jmcejuela/bio/jenia/JeniaTagger.java)


## The original Genia Tagger

* [Webpage](http://www.nactem.ac.uk/tsujii/GENIA/tagger/)
* [README](https://github.com/jmcejuela/jeniatagger/blob/develop/ORIGINAL_GENIATAGGER_README)
* [Code](https://github.com/jmcejuela/geniatagger) (my fork on github)
