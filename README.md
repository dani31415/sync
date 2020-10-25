## Build status and tests

[![Build Status](https://travis-ci.org/dani31415/sync.svg?branch=master)](https://travis-ci.org/dani31415/sync)

## Description 

Command line tool for two-way synchronization of the files of two folders. Optimized for raw images and large video files.

The motivation of this project was to make room in a laptop to have more space to store images and videos. After synchronized to the backup device, it is safe to remove the unused files (since are *archived* in the backcup).

The syncrhonization works fine with external USB devices and shared network folders.

## Dependencies

This project depends only on Java SDK and [ant](https://ant.apache.org) for the build proces.

## Rules for the synchronization

* Simmetric syncronization: files are copied or deleted from both folders.
* If a folder is removed from one target folder, it is moved to "ARCHIVED" in the other target folder. A file is removed when was previouly synchronized and does not exist anymore in one of the folders.
* If a file is removed from one target folder, it is moved to ".deleted" in the other target folder. A file is removed when was previouly synchronized and does not exist anymore in one of the folders.
* If a file/folder is created in one target folder, it is copied to the other target folder.
* Renaming files and folders are obtimized.

