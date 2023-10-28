package com.nju.bysj.softwaremodularisation.structure.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MyClassLoader extends ClassLoader {
    private String[] targetPath;

    public MyClassLoader() {
        super();
    }

    public MyClassLoader(String[] targetPath){
        this.targetPath = targetPath;
    }

    @Override
    protected Class<?> findClass(String fullClassName) throws ClassNotFoundException {
        Class<?> aClass = null;
        String fullClassFile = fullClassName.replace('.', '\\').concat(".class");
        byte[] classData = null;
        for (int i = 0; i < targetPath.length; i++) {
//            System.out.println(targetPath[i] + fullClassFile);
            if ((classData = getData(targetPath[i] + fullClassFile)) != null) {
                break;
            }
        }

        if (classData == null) {
            throw new ClassNotFoundException("lack class: " + fullClassName);
        }
        aClass = defineClass(fullClassName, classData, 0, classData.length);
        return aClass;
    }

    private byte[] getData(String path) {
        File file = new File(path);
        if (file.exists()){
            FileInputStream in = null;
            ByteArrayOutputStream out = null;
            try {
                in = new FileInputStream(file);
                out = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int size;
                while ((size = in.read(buffer)) != -1) {
                    out.write(buffer, 0, size);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
            return out.toByteArray();
        }else{
            return null;
        }
    }
}