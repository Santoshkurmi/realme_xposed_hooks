package com.realme.modxposed;

public interface FileObserverListener {
        void onFileUpdated(String path);
        void onFileAttributesChanged(String path);
}

