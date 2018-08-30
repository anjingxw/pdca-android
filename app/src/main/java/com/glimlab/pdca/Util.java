package com.glimlab.pdca;

import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;

public class Util
{
    public static boolean deleteFile(String paramString)
    {
        File file = new File(paramString);
        if ((file.isFile()) && (file.exists())) {
            return file.delete();
        }
        return false;
    }

    public static String encodeBase64File(String paramString)
            throws Exception
    {
        File localObject = new File(paramString);
        FileInputStream fileInputStream = new FileInputStream((File)localObject);
        byte[] array = new byte[(int)((File)localObject).length()];
        fileInputStream.read(array);
        fileInputStream.close();
        return Base64.encodeToString(array, 0);
    }
}
