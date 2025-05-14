#!/usr/bin/env sh
gcc output.s -o output && ./output || echo $?