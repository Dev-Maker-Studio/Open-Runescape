package com.jagex.game.runetek5.utils;

import javax.swing.*;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.window.WindowMode;

import java.awt.*;
import java.io.File;

public class DirectoryPicker implements Runnable {

    public File pickedDirectory;

    public boolean fileChoserOpen;

    public final String diaglogTitle;

    public final boolean ready;

    public DirectoryPicker(String diaglogTitle) {
        this.diaglogTitle = diaglogTitle;
        this.ready = true;
        (new Thread(this)).start();
    }

    @Override
    public void run() {
        Container container = WindowMode.getActiveContainer();
        Frame mainFrame = null;
        if (GameShell.engineFrame != null) {
            mainFrame = GameShell.engineFrame;
        } else {
            while (container != null) {
                if (container instanceof Frame) {
                    mainFrame = (Frame) container;
                    break;
                }
                container = container.getParent();
            }
        }
        if (mainFrame == null) {
            throw new RuntimeException("");
        } 
        JFileChooser fileChoser = new JFileChooser("");
        fileChoser.setDialogTitle(this.diaglogTitle);
        fileChoser.setFileFilter(new DirectoryFileFilter(this, this));
        fileChoser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChoser.setAcceptAllFileFilterUsed(false);
        int state = fileChoser.showOpenDialog(mainFrame);
        if (state == 0) {
            this.pickedDirectory = fileChoser.getSelectedFile();
        }
        this.fileChoserOpen = true;
    }

    public boolean fileChoserOpen() {
        return this.fileChoserOpen;
    }

    public boolean ready() {
        return this.ready;
    }

    public File getPickedDirectory() {
        return this.pickedDirectory;
    }
}
