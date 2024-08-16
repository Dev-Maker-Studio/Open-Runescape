package com.jagex.game.runetek5.utils;

import javax.swing.filechooser.FileFilter;

import java.io.File;

/**
 * 
 * @author Brainpower - 28/01/2023
 *
 */
public class DirectoryFileFilter extends FileFilter {

    public final DirectoryPicker dummy;
    public final DirectoryPicker filePicker;

    public DirectoryFileFilter(DirectoryPicker dummy, DirectoryPicker filePicker) {
        this.dummy = dummy;
        this.filePicker = filePicker;
    }

    @Override
    public boolean accept(File pickedFile) {
        return this.filePicker.ready() && pickedFile.isDirectory() || !this.filePicker.ready();
    }

    @Override
    public String getDescription() {
        return null;
    }
}