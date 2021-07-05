#!/bin/sh
mkdir -p build/i18n/pot/
mkdir -p build/i18n/srcFileList/
find src/ -type f > build/i18n/srcFileList/josm-plugin_noteSolver.txt
xgettext --from-code=UTF-8 --language=Java --add-comments --sort-output -k -ktrc:1c,2 -kmarktrc:1c,2 -ktr -kmarktr -ktrn:1,2 -ktrnc:1c,2,3 --files-from=build/i18n/srcFileList/josm-plugin_noteSolver.txt --output-dir=build/i18n/pot/
