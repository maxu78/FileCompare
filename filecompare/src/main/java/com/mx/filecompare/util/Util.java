package com.mx.filecompare.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Slf4j
public class Util {

    //从filePath+fileName字符串分离出filePath和fileName
    public static String[] splitFilePath(String filePath){
        File file = new File(filePath);
        String fileName = file.getName();
        String dirPath = filePath.trim().replace(fileName, "");
        return new String[] {dirPath, fileName};
    }

    public boolean downLoadFromFTP(String host, String username, String password, String localFilePath, String ftpFilePath) throws Exception{
        return downLoadFromFTP(host, 21, username, password, localFilePath, ftpFilePath);
    }

    public boolean downLoadFromFTP(String host, int port, String username, String password, String localFilePath, String ftpFilePath) throws Exception{
        boolean success = false;
        FTPClient ftp = null;
        OutputStream is = null;
        try{
            ftp = new FTPClient();
            ftp.connect(host, port);//连接FTP服务器
            String logStr = ftp.getReplyString();
            log.info("[connect]"+logStr);

            ftp.login(username, password);//登录
            logStr = ftp.getReplyString();
            log.info("[logStrin]"+logStr);

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return success;
            }

            log.info("[workingdirectory]"+ftp.printWorkingDirectory());
            String[] fileInfo = splitFilePath(ftpFilePath);
            log.info(Arrays.toString(fileInfo));
            String path = fileInfo[0];
            String fileName = fileInfo[1];
            ftp.changeWorkingDirectory(path);
            logStr = ftp.getReplyString();
            log.info("[changeworkingdirectory]"+logStr);

            FTPFile[] fs = ftp.listFiles();
            for(FTPFile ff:fs){
                if(ff.getName().equals(fileName)){
                    log.info("[localFilePath]"+localFilePath);
                    createFile(localFilePath);
                    is = new FileOutputStream(localFilePath);
                    ftp.retrieveFile(ff.getName(), is);
                }
            }
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            is.close();
            ftp.logout();
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    success = false;
                }
            }
        }
        return success;
    }

    public static String createFile(String filePath) throws Exception{
        String result = "";
        File file = new File(filePath);
        File pFile = file.getParentFile();
        if(!pFile.exists()){
            pFile.mkdirs();
        }else{
            result = "exist";
        }
        file.createNewFile();
        result = "ok";
        return result;
    }

    //调用后台Http接口
    public String getDomainInfo(String httpurl) throws Exception {
        URL url = new URL(httpurl);
        URLConnection context = url.openConnection();
        InputStream ins = context.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
        StringBuilder sb=new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        in.close();
        return sb.toString();
    }

    //输出到指定path文件中
    public void print(String filePath, String code) throws Exception {
        try {
            File tofile = new File(filePath);
            if(!tofile.exists()){
                tofile.mkdirs();
                tofile.createNewFile();
            }
            FileWriter fw = new FileWriter(tofile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            pw.println(code);
            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    //把inputStream写入到文件中
    public static void inputStreamToFile(InputStream in, String filePath) throws Exception {
        File file = new File(filePath);
        String res = createFile(filePath);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            int ch = 0;
            while ((ch = in.read()) != -1) {
                os.write(ch);
            }
            os.flush();
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //压缩某个文件
    public void compressFile(String inFileName, String outFileName, boolean isDelete) throws Exception {
        log.info("in: "+inFileName+"\t out:"+outFileName);
        File f = null;
        FileInputStream in = null;
        try {
            f = new File(inFileName);
            in = new FileInputStream(f);
        }catch (FileNotFoundException e) {
            log.info("Could not find the inFile..."+inFileName);
        }

        GZIPOutputStream out = null;
        try {
            out = new GZIPOutputStream(new FileOutputStream(outFileName));
        }catch (IOException e) {
            log.info("Could not find the outFile..."+outFileName);
        }
        byte[] buf = new byte[10240];
        int len = 0;
        try {
            while (((in.available()>10240)&& (in.read(buf)) > 0)) {
                out.write(buf);
            }
            len = in.available();
            in.read(buf, 0, len);
            out.write(buf, 0, len);
            in.close();
            log.info("Completing the GZIP file..."+outFileName);
            out.flush();
            out.close();
        }catch (IOException e) {
            e.printStackTrace();
            log.info("exception!!!!!!!!");
            throw e;
        }
        if(isDelete && f != null){
            f.delete();
        }
    }

    //压缩sourcePath路径的文件到zipPath文件中
    public void zipFile(String sourcePath, String zipPath, boolean isDelete) throws Exception {
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipPath);
            zos = new ZipOutputStream(fos);
            if(isDelete){
                clearFiles(sourcePath);
            }
//          zos.setEncoding("gbk");//此处修改字节码方式。
            writeZip(new File(sourcePath), "", zos);
        } catch (FileNotFoundException e) {
            System.out.println("创建ZIP文件失败");
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                System.out.println("创建ZIP文件失败");
                e.printStackTrace();
            }

        }
    }

    private void writeZip(File file, String parentPath, ZipOutputStream zos) throws Exception {

        if(file.exists()){
            if(file.isDirectory()){//处理文件夹
                parentPath+=file.getName()+File.separator;
                File [] files=file.listFiles();
                if(files.length != 0){
                    for(File f:files){
                        writeZip(f, parentPath, zos);
                    }
                }
                else{       //空目录则创建当前目录
                    try {
                        zos.putNextEntry(new ZipEntry(parentPath));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }else{
                FileInputStream fis=null;
                try {
                    fis=new FileInputStream(file);
                    ZipEntry ze = new ZipEntry(parentPath + file.getName());
                    zos.putNextEntry(ze);
                    byte [] content=new byte[1024];
                    int len;
                    while((len=fis.read(content))!=-1){
                        zos.write(content,0,len);
                        zos.flush();
                    }
                } catch (FileNotFoundException e) {
                    System.out.println("创建ZIP文件失败");
                    e.printStackTrace();
                    throw e;
                } catch (IOException e) {
                    System.out.println("创建ZIP文件失败");
                    e.printStackTrace();
                    throw e;
                }finally{
                    try {
                        if(fis!=null){
                            fis.close();
                        }
                    }catch(IOException e){
                        System.out.println("创建ZIP文件失败");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //压缩指定文件
    public void ZipFiles(String[] srcfilePath, String zipFilePath, boolean isDelete) throws Exception {
        File[] srcFile = new File[srcfilePath.length];
        for(int i=0; i<srcfilePath.length; i++){
            srcFile[i] = new File(srcfilePath[i]);
        }
        File zipFile = new File(zipFilePath);
        byte[] buf = new byte[1024];
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
            for (int i = 0; i < srcFile.length; i++) {
                FileInputStream in = new FileInputStream(srcFile[i]);
                out.putNextEntry(new ZipEntry(srcFile[i].getName()));
                String str = srcFile[i].getName();
                System.out.println("压缩文件"+str+"中...");
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
            System.out.println("压缩完成...");
            if(isDelete){
                for (int i = 0; i < srcFile.length; i++) {
                    srcFile[i].delete();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    //删除文件和目录
    public void clearFiles(String workspaceRootPath){
        File file = new File(workspaceRootPath);
        if(file.exists()){
            deleteFile(file);
        }
    }
    private void deleteFile(File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            for(int i=0; i<files.length; i++){
                deleteFile(files[i]);
            }
        }
        file.delete();
    }

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {}
            // 定义 BufferedReader输入流来读取URL的响应-设置编码
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义 BufferedReader输入流来读取URL的响应-设置编码
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    //调用webservice
    public static String invokeSoap(String url, String input, String soapAction, String timeOut) throws Exception{
        System.out.println(input);
        HttpClient httpClient = new HttpClient();
        String soapResponseData = "";
        PostMethod postMethod = null;
        int timeout = 300000;
        try{
            try{
                timeout = Integer.parseInt(timeOut);
            } catch(Exception e){
                System.out.println("超时时间"+timeout+"转int失败... 置为默认值5min");
            }
            httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
            postMethod = new PostMethod(url);
            byte[] b = input.getBytes("utf-8");
            InputStream is = new ByteArrayInputStream(b, 0, b.length);
            RequestEntity re = new InputStreamRequestEntity(is, b.length, "application/soap+xml; charset=utf-8");
            postMethod.setRequestEntity(re);
            postMethod.setRequestHeader("SOAPAction", soapAction);
            int statusCode = httpClient.executeMethod(postMethod);
            if(statusCode == 200) {
                System.out.println("调用成功!");
                InputStream inputStream = postMethod.getResponseBodyAsStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                StringBuffer stringBuffer = new StringBuffer();
                String str= "";
                while((str = br.readLine()) != null){
                    stringBuffer.append(str );
                }
                soapResponseData = stringBuffer.toString();
                soapResponseData = soapResponseData.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
                System.out.println("返回:" + soapResponseData);
            }
            else {
                System.out.println("调用失败!错误码：" + statusCode);
                soapResponseData = statusCode+"";
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            if(postMethod != null){
                postMethod.releaseConnection();
            }
        }
        return soapResponseData;
    }

    //读取properties文件
    public synchronized static Map<String, String> readProperty(String filePath) throws Exception{
        Map<String, String> result = new HashMap<String, String>();
        Properties prop = new Properties();
        InputStream in = null;
        try{
            //读取属性文件
            in = new BufferedInputStream (new FileInputStream(filePath));
            prop.load(in);     ///加载属性列表
            Iterator<String> it = prop.stringPropertyNames().iterator();
            while(it.hasNext()){
                String key = it.next();
                String value = prop.getProperty(key);
                result.put(key, value);
            }

        } catch(Exception e){
            e.printStackTrace();
            throw e;
        } finally{
            if(in != null){
                in.close();
            }
        }
        return result;
    }

    //写入properties文件
    public synchronized void storeProperty(Map<String, String> map, String filePath, boolean isDeleteOrg) throws Exception{
        File file = new File(filePath);
        if(isDeleteOrg && file.exists()){
            file.delete();
        }
        FileOutputStream oFile = null;
        Properties prop = new Properties();
        try{
            ///保存属性到b.properties文件
            oFile = new FileOutputStream(filePath, true);//true表示追加打开
            for(Map.Entry<String, String> entry : map.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();
                prop.setProperty(key, value);
            }
            //如果comments不为空，保存后的属性文件第一行会是#comments,表示注释信息；如果为空则没有注释信息。
            prop.store(oFile, null);

        } catch(Exception e){
            e.printStackTrace();
            throw e;
        } finally{
            if(oFile != null){
                oFile.close();
            }
        }

    }

    /**
     *
     * @param path 路径
     * @param seprate 分隔符
     * @return
     * @throws Exception
     */
    public String readTxt(String path, String seprate) throws Exception{
        File file = new File(path);
        StringBuilder sb = new StringBuilder();
        String s = "";
        BufferedReader br = null;
        try{
            br = new BufferedReader(new FileReader(file));
            while((s = br.readLine()) != null) {
                sb.append(s + seprate);
            }
        } catch(Exception e){
            e.printStackTrace();
            throw e;
        } finally{
            if(br != null){
                br.close();
            }
        }
        return sb.toString();
    }

    //解析只有一个节点的xml
    public Map<String, Map<String, String>> analyzeXml(String xml) throws Exception{
        Map<String, Map<String, String>> result = new HashMap<String, Map<String,String>>();
        Map<String, String> map = new HashMap<String, String>();
        Document doc = null;
        doc = DocumentHelper.parseText(xml);
        Element root = doc.getRootElement();
        String rootName = root.getName();
        log.info("根节点名称: "+rootName);
        Iterator<Element> it = root.elementIterator();
        while(it.hasNext()){
            Element e = (Element) it.next();
            String name = e.getName();
            String value = e.getTextTrim();
            map.put(name, value);
        }
        result.put(rootName, map);
        return result;
    }

    // 获取两个时间字符串中间的年月[minDate, maxDate)
    public static List<String> getMonthTimeBetween(String minDate, String maxDate, String type) throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat(type);// 格式化为年月

        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();

        min.setTime(sdf.parse(minDate));
        min.set(min.get(Calendar.YEAR), min.get(Calendar.MONTH), 1);

        max.setTime(sdf.parse(maxDate));
        max.set(max.get(Calendar.YEAR), max.get(Calendar.MONTH), 1);

        Calendar curr = min;
        while (curr.before(max)) {
            result.add(sdf.format(curr.getTime()));
            curr.add(Calendar.MONTH, 1);
        }
        return result;
    }

    // 获取两个时间字符串中间的年月日[minDate, maxDate)
    public static List<String> getDateTimeBetween(String minDate, String maxDate, String type) throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat(type);// 格式化为年月

        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();

        min.setTime(sdf.parse(minDate));
        min.set(min.get(Calendar.YEAR), min.get(Calendar.MONTH), min.get(Calendar.DATE));
        max.setTime(sdf.parse(maxDate));
        max.set(max.get(Calendar.YEAR), max.get(Calendar.MONTH), max.get(Calendar.DATE));

        Calendar curr = min;
        while (curr.before(max)) {
            result.add(sdf.format(curr.getTime()));
            curr.add(Calendar.DATE, 1);
        }
        return result;
    }

    //获取Date时间后type(年月日)后i个单位(i可以为负数)
    public static Date getRoundDate(Date date, String type, int i){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if("MILLISECOND".equals(type)){
            cal.add(Calendar.MILLISECOND, i);
        }
        if("SECOND".equals(type)){
            cal.add(Calendar.SECOND, i);
        }
        if("MINUTE".equals(type)){
            cal.add(Calendar.MINUTE, i);
        }
        if("HOUR".equals(type)){
            cal.add(Calendar.HOUR_OF_DAY, i);
        }
        if("DATE".equals(type)){
            cal.add(Calendar.DATE, i);
        }
        if("MONTH".equals(type)){
            cal.add(Calendar.MONTH, i);
        }
        if("YEAR".equals(type)){
            cal.add(Calendar.YEAR, i);
        }
        return cal.getTime();
    }

    //把日期转化成type类型的字符串("yyyy-MM-dd HH:mm:ss:SSS")
    public static String dateToString(String type, Date date) throws Exception{
        SimpleDateFormat sdf = new SimpleDateFormat(type);
        return sdf.format(date);
    }

    //把type类型的字符串转化成日期("yyyy-MM-dd HH:mm:ss:SSS")
    public static Date stringToDate(String type, String dateString) throws Exception{
        SimpleDateFormat sdf = new SimpleDateFormat(type);
        return sdf.parse(dateString);
    }

    //调用perl
    public Map<String, String> invoke(String cmd) throws Exception{
        log.info("cmd: "+cmd);
        String detail = "";
        String error = "";
        String exitValue = "";
        Map<String, String> result = new HashMap<String, String>();
        InputStream in = null;
        InputStream err = null;
        Process pro  = null;
        String charSet = "UTF-8";
        try {
            String os = System.getProperty("os.name");
            log.info("OSVersion: "+os);
            if(os.toLowerCase().startsWith("win")){
                pro = Runtime.getRuntime().exec(cmd);//windows端执行时使用这句
                charSet = "GBK";
            }else if(os.toLowerCase().startsWith("linux")){
                String[] cmds = {"/bin/sh","-c",cmd};//Linux端执行时使用这句
                pro = Runtime.getRuntime().exec(cmds); //Linux端执行时使用这句
            }
            in = pro.getInputStream();
            err = pro.getErrorStream();
            CountDownLatch threadSignal = new CountDownLatch(2);
            ReadThread inThread = new ReadThread(in, charSet, threadSignal, "normal");
            ReadThread errThread = new ReadThread(err, charSet, threadSignal, "error");
            new Thread(inThread).start();
            new Thread(errThread).start();
            pro.waitFor();
            exitValue = String.valueOf(pro.exitValue());
            log.info("执行结果: "+exitValue+"\t 0为正常,其余为异常");
            try {
                threadSignal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            detail = inThread.getOutStr().toString();
            error = errThread.getOutStr().toString();
            result.put("status", exitValue);
            result.put("detail", detail);
            result.put("error", error);
        } catch (Exception e) {
            log.info("执行命令报错: "+e.getMessage());
            e.printStackTrace();
        } finally{
            if(pro != null){
                pro.destroy();
                pro = null;
            }
        }
        return result;
    }

    //把一个数组按照size进行分割
    public static List<List<String>> subList(List<String> l, int size){
        List<List<String>> list = new ArrayList<List<String>>();
        int num = l.size();
        int in = num/size;
        int j = 0;
        for(int i=0; i<=num; i++){
            if(i != 0 && i % size == 0){
                List<String> tempList = l.subList(i - size, i);
                list.add(tempList);
                j++;
            }
        }
        if(j == in){
            List<String> tempList = l.subList(in*size, num);
            if(tempList != null && tempList.size()>0){
                list.add(tempList);
            }

        }

        return list;
    }

    public static String trim(String str){
        if(str == null || "null".equalsIgnoreCase(str)){
            return "";
        }
        return str.trim();
    }

    //origin 待补全字符, sup补全字符, num 补齐至多少位, pos 在前面补齐还是在后面补齐
    public static String supplement(String origin, String sup, int num, String pos){
        StringBuilder sb = new StringBuilder();
        int index = (num - origin.length()) / sup.length();
        int remainder = (num - origin.length()) % sup.length();
        for(int i=0; i<index; i++){
            sb.append(sup);
        }
        for(int i=0; i<remainder; i++){
            sb.append(sup.charAt(i));
        }
        if("suffix".equals(pos)){
            return origin+sb.toString();
        }else{
            return sb.toString()+origin;
        }
    }

    public String dealNull(String str){
        if(str == null){
            return "";
        }
        return str.trim();
    }

    public class ReadThread implements Runnable{
        private StringBuilder outStr;
        private BufferedReader reader;
        private CountDownLatch threadsSignal;
        private String type;

        public ReadThread(){

        }
        public ReadThread(InputStream in, String charSet, CountDownLatch threadsSignal, String type){
            this.outStr = new StringBuilder();
            this.type = type;
            this.threadsSignal = threadsSignal;
            try {
                this.reader = new BufferedReader(new InputStreamReader(in, charSet));
            } catch (UnsupportedEncodingException e) {
                this.reader = null;
                e.printStackTrace();
            }
        }

        public void run() {
            System.out.println("Thread "+type+" is called...");
            // TODO Auto-generated method stub
            String line = null;
            try{
                while((line = reader.readLine()) != null){
                    System.out.println(type+"########"+line+"\n");
                    if(line.endsWith("\n")){
                        outStr.append(line);
                    }else{
                        outStr.append(line+"\n");
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            } finally{
                if (reader != null){
                    try{
                        reader.close();
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
                threadsSignal.countDown();
                System.out.println("Thread "+type+" is end...");
            }
        }

        public StringBuilder getOutStr(){
            return outStr;
        }

    }

}
