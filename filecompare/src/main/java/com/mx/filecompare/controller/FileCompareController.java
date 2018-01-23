package com.mx.filecompare.controller;

import com.mx.filecompare.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class FileCompareController {

    Util util = new Util();

    @RequestMapping("/getFile")
    @ResponseBody
    public Map<String, String> fileDiff(@RequestParam("path1") String path1, @RequestParam("path2") String path2){
        Map<String, String> result = new HashMap<String, String>();
        String t1 = "";
        String t2 = "";
        try {
            t1 = util.readTxt(path1, "\n");
            t2 = util.readTxt(path2, "\n");
            result.put("t1", t1);
            result.put("t2", t2);
            result.put("status", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
        }
        return result;
    }

    @RequestMapping("/upload")
    @ResponseBody
    public Map<String, String> fileUpload(MultipartFile file){
        InputStream is = null;
        Map<String, String> result = new HashMap<String, String>();
        if(file.isEmpty()){
            result.put("status", "fail");
            result.put("desc", "empty");
        } else{
            String fileName = file.getOriginalFilename();
            String path =  "/uploadFile/"+fileName ;
            try {
                is = file.getInputStream();
                Util.inputStreamToFile(is, path);
                result.put("status", "ok");
                result.put("desc", path);
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                result.put("status", "fail");
                result.put("desc", e.getMessage());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                result.put("status", "fail");
                result.put("desc", e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                result.put("status", "fail");
                result.put("desc", e.getMessage());
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
