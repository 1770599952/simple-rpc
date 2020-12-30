package com.sexycode.simplerpc.utils;

import java.io.*;

public class SerializeUtils<T extends Serializable> {
    public byte[] serialize(T t) {
        byte[] bytes = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(t);
            bytes = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            System.out.println("Serialize error");
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    System.out.println("Close ObjectOutputStream error");
                }
            }
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                System.out.println("Close ObjectOutputStream error");
            }
        }
        return bytes;
    }

    public T unSerialize(byte[] bytes) {
        Object object = null;
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            object = objectInputStream.readObject();
        } catch (Exception e) {
            System.out.println("UnSerialize error");
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    System.out.println("Close ObjectInputStream error");
                }
            }
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e) {
                    System.out.println("Close ByteArrayInputStream error");
                }
            }
        }
        return (T) object;
    }
}
