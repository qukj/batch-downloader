package io.github.qukj.batchdownloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Scanner;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Downloader {

    @Value("${url}")
    private String url;

    @Value("${cookie}")
    private String cookie;

    @Value("${fileIdPath}")
    private String fileIdPath;

    @Value("${outputDir}")
    private String outputDir;

    private CloseableHttpClient httpClient= HttpClients.createDefault();
    
    public void download() {
        if(outputDir == null) {
            System.out.println("未指定下载目录");
            return;
        }
        File dir = new File(outputDir);
        if(!dir.exists()) {
            boolean success = dir.mkdirs();
            if(!success) {
                System.out.println("创建下载目录失败");
                return;
            } else {
                System.out.println("创建下载目录成功");
            }
        } else if(dir.isFile()) {
            System.out.println("指定的下载目录已存在，但类型是文件，无法下载");
            return;
        }
        
        System.out.println("开始下载");
     
        try (Scanner scanner = new Scanner(new FileReader(fileIdPath))) {
            while(scanner.hasNext()) {
                String fileId = scanner.nextLine();
                downloadOne(dir, fileId);
            }
        } catch (FileNotFoundException ex) {
        	System.out.println("读取文件ID失败");
            ex.printStackTrace();
        }
    }
    
    private void downloadOne(File outputDir, String fileId) {

        HttpGet httpget = new HttpGet(url.replace("{}", fileId));
        httpget.addHeader("Cookie", cookie);
        
        CloseableHttpResponse response = null;
        OutputStream out = null;
        
        try {
        	response = httpClient.execute(httpget);
            Header contentDis = response.getFirstHeader("Content-Disposition");
            String serverFileName = "";
            if(contentDis != null) {
                String value = contentDis.getValue();
                int index = value.lastIndexOf("=");
                if(index > 0) {
                    serverFileName = value.substring(index + 1);
                }
            } else {
                System.out.println(fileId + "下载失败，未返回Content-Disposition");
                return;
            }
            
            File outputFile = new File(outputDir, fileId + "_" + URLDecoder.decode(serverFileName, "utf-8"));
            out = new FileOutputStream(outputFile);
            
            response.getEntity().writeTo(out);
            
            System.out.println(fileId + "下载成功");
            
        } catch(Exception e) {
            System.out.println(fileId + "下载失败");
            e.printStackTrace();
        } finally {
        	if(response != null) {
        		try {
					response.close();
				} catch (IOException e) {
					System.out.println("关闭response失败");
					e.printStackTrace();
				}
        	}
        	if(out != null) {
        		try {
        			out.close();
				} catch (IOException e) {
					System.err.println("关闭out失败");
					e.printStackTrace();
				}
        	}
        }
    }
}