package com.ftl.listener.test;

import blazing.chain.LZSEncoding;
import com.ftl.common.utils.FtlDateUtils;

import java.io.*;
import java.text.ParseException;
import java.util.Date;

public class PlainTest {
    public static void main(String[] args) throws ParseException, IOException {
//        Date date = FtlDateUtils.DATE_FORMAT("dd/MM/yyyy - HH:mm:ss").parse("09/05/2023 - 13:40:09.716");
//        System.out.println(date);
        File file = new File("data-fake.txt");
        boolean isExist = file.exists();
        File rawData = new File("kafka.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(rawData));

        if (!rawData.exists()) {
            rawData.createNewFile();
        }
        int count = 0;
        if (isExist) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String cat = "push queue Kafka: ";
                int index = line.indexOf(cat);
                String data = line.substring(index + cat.length()).trim();
                if(data.startsWith("N4")) {
                    //
                    count ++;
                    //System.out.println(decompressedBase64);
                    try {
                        System.out.println(data);
                        Thread.sleep(10);
                        String decompressedBase64 = LZSEncoding.decompressFromBase64(data);
                        bufferedWriter.write(decompressedBase64);
                        bufferedWriter.newLine();
                    }
                    catch (Exception e){
                        System.out.println(e);
                    }

                }
            }
            bufferedReader.close();
            bufferedWriter.close();
            System.out.println(count);
        }

    }
}
