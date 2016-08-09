package com.envelopes.apps.labelprinter;

import com.google.gson.Gson;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by Manu on 7/8/2016.
 */
public class LabelHelper {
    public static String LABEL_PRINTER_HOME = "/usr/local/LabelPrinter/";
    public static String LABEL_SERVER_END_POINT = "https://envelopes.com/";
    public static String DEFAULT_LABEL_TYPE = "LABEL";
    private static String API_KEY = "b1a6fcad-20e3-4dc4-9347-d266fa012bee";
    private static String ENV = "PROD";
    public static String CONFIG_FILE_LOCATION = LABEL_PRINTER_HOME + "labelPrinter.properties";
    public static String LABEL_PRINTER_CACHE_LOCATION = LABEL_PRINTER_HOME + "ProductLabels/";
    public static String LABEL_PRINTER_DIRECTORY_GENERATION_PATH1 = LABEL_PRINTER_CACHE_LOCATION + "packLabels/";
    public static String LABEL_PRINTER_DIRECTORY_GENERATION_PATH2 = LABEL_PRINTER_CACHE_LOCATION + "miniLabels/";
    public static String GET_FILE_FROM_SERVER_END_POINT = LABEL_SERVER_END_POINT + "envelopes/control/serveLabelForStream?filePath=/uploads/productLabels/";
    public static String GET_LABEL_DATA_END_POINT = LABEL_SERVER_END_POINT + "envelopes/control/getLabelData?";
    public static String GET_LABEL_DATA_END_POINT2 = LABEL_SERVER_END_POINT + "envelopes/control/getLabelData2?";
    public static int MAX_COPIES = 100;
    protected static String[] fileTypes = {".pdf", ".png"};

    public static String initializeLabelPrinter() throws Exception {
        File testFile = new File(LABEL_PRINTER_DIRECTORY_GENERATION_PATH1);
        if(!testFile.exists()) {
            new File(LABEL_PRINTER_DIRECTORY_GENERATION_PATH1).mkdirs();
        }

        testFile = new File(LABEL_PRINTER_DIRECTORY_GENERATION_PATH2);
        if(!testFile.exists()) {
            new File(LABEL_PRINTER_DIRECTORY_GENERATION_PATH2).mkdirs();
        }
        checkConfigFile();
        return "";
    }

    protected static void checkConfigFile() throws Exception {
        File configFile = new File(CONFIG_FILE_LOCATION);
        if(!configFile.exists()) {
            FileOutputStream fileOut = new FileOutputStream(configFile);
            Properties properties = new Properties();

            properties.setProperty("labelPrinterHome", LABEL_PRINTER_HOME);
            properties.setProperty("labelServerEndPoint", LABEL_SERVER_END_POINT);
            properties.setProperty("defaultLabelType", DEFAULT_LABEL_TYPE);
            properties.setProperty("maxCopies", Integer.toString(MAX_COPIES));
            properties.setProperty("version", ENV);
            properties.store(fileOut, "Label Printer Configuration Properties");
            fileOut.close();
        } else {
            Properties properties = new Properties();
            InputStream inputStream = new FileInputStream(new File(CONFIG_FILE_LOCATION));
            properties.load(inputStream);

            if(properties.containsKey("labelPrinterHome")) {
                LABEL_PRINTER_HOME = properties.getProperty("labelPrinterHome");
            }

            if(properties.containsKey("labelServerEndPoint")) {
                LABEL_SERVER_END_POINT = properties.getProperty("labelServerEndPoint");
            }

            if(properties.containsKey("maxCopies")) {
                MAX_COPIES = Integer.parseInt(properties.getProperty("maxCopies"));
            }

            if(properties.containsKey("defaultLabelType")) {
                DEFAULT_LABEL_TYPE = properties.getProperty("defaultLabelType");
            }

            if(properties.containsKey("version")) {
                ENV = properties.getProperty("version");
            }

            updateConstants();
        }
    }

    protected static void updateConstants() {
        CONFIG_FILE_LOCATION = LABEL_PRINTER_HOME + "labelPrinter.properties";
        LABEL_PRINTER_CACHE_LOCATION = LABEL_PRINTER_HOME + "ProductLabels/";
        LABEL_PRINTER_DIRECTORY_GENERATION_PATH1 = LABEL_PRINTER_CACHE_LOCATION + "PackLabels/";
        LABEL_PRINTER_DIRECTORY_GENERATION_PATH2 = LABEL_PRINTER_CACHE_LOCATION + "MiniLabels/";
        GET_FILE_FROM_SERVER_END_POINT = LABEL_SERVER_END_POINT + "envelopes/control/serveLabelForStream?filePath=/uploads/productLabels/";
        GET_LABEL_DATA_END_POINT = LABEL_SERVER_END_POINT + "envelopes/control/getLabelData?";
        if(!ENV.equalsIgnoreCase("PROD")) {
            disableSslVerification();
        }
    }

    public static List<Map<String, String>> getLabelsForOrderIdOrProductId(String orderOrProductIdWithQty, boolean miniLabel) throws LabelNotFoundException {
        Map<String, Object> result;
        try {
            result = getJSON(GET_LABEL_DATA_END_POINT + "orderOrProductIdWithQty=" + orderOrProductIdWithQty + (miniLabel ? "&miniLabel=true" : ""));
            if((boolean)result.get("success")) {
                List<Map<String, String>> labelsData = (List<Map<String, String>>) result.get("data");
                for (Map<String, String> labelData : labelsData) {
                    boolean availableInCache = labelAvailableInCache(labelData) && cacheNotExpired(labelData);
                    if(!availableInCache) {
                        getLabelFileFromServer2(labelData);
                    }
                }
            } else {
                if(result.containsKey("data")) {
                    throw new LabelNotFoundException("No Label found for the given ProductId : " + orderOrProductIdWithQty, null);
                } else {
                    throw new LabelNotFoundException("An error occurred while retrieving the label(s) from the server for the given Id : " + orderOrProductIdWithQty, null);
                }
            }
        } catch(Exception e) {
            throw new LabelNotFoundException("An error occurred while retrieving the label(s) from the server for the given Id : " + orderOrProductIdWithQty, e);
        }

        return (List<Map<String, String>>)result.get("data");
    }

    protected static boolean cacheNotExpired(Map<String, String> labelData) {
        File labelFile = new File(LABEL_PRINTER_CACHE_LOCATION + labelData.get("labelPDFPath"));
        if(labelFile.exists() && labelFile.lastModified() >= new Long(labelData.get("lastModified"))) {
            labelFile = new File(LABEL_PRINTER_CACHE_LOCATION + labelData.get("labelPath"));
            if(labelFile.exists() && labelFile.lastModified() >= new Long(labelData.get("lastModified"))) {
                return true;
            }
        }
        return false;
    }

    protected static boolean labelAvailableInCache(Map<String, String> labelData) {
        return new File(LABEL_PRINTER_CACHE_LOCATION + labelData.get("labelPDFPath")).exists() && new File(LABEL_PRINTER_CACHE_LOCATION + labelData.get("labelPath")).exists();
    }

    protected static void getLabelFileFromServer2(Map<String, String> labelData, boolean... imageFileFlag) throws Exception {
        String relativeLabelPath = labelData.get(imageFileFlag != null && imageFileFlag.length == 1 && imageFileFlag[0] ? "labelPath" : "labelPDFPath");
        URL url = new URL(GET_FILE_FROM_SERVER_END_POINT + relativeLabelPath + "&ts=" + System.currentTimeMillis());
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(LABEL_PRINTER_CACHE_LOCATION + relativeLabelPath), StandardCopyOption.REPLACE_EXISTING);
        in.close();
        File downloadedFile;
        if((downloadedFile = new File(LABEL_PRINTER_CACHE_LOCATION + relativeLabelPath)).length() <= 0) {
            downloadedFile.delete();
            throw new LabelNotFoundException("An error occurred while retrieving label from the server for ProductID : " + labelData.get("productIdWithQty"));
        }
        if(imageFileFlag == null || imageFileFlag.length == 0) {
            getLabelFileFromServer2(labelData, true);
        }
    }

    public static Map<String, Object> getJSON(String targetURL) throws Exception {
        if(targetURL.contains("?")) {
            targetURL += "&apiToken=" + API_KEY;
        } else {
            targetURL += "?apiToken=" + API_KEY;
        }
        StringBuilder response = new StringBuilder();
        URL restServiceURL = new URL(targetURL);

        HttpURLConnection httpConnection = (HttpURLConnection) restServiceURL.openConnection();
        httpConnection.setRequestMethod("GET");
        httpConnection.setRequestProperty("Accept", "application/json");

        if (httpConnection.getResponseCode() != 200) {
            throw new RuntimeException("HTTP GET Request Failed with Error code : "
                    + httpConnection.getResponseCode());
        }
        BufferedReader responseBuffer = new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));

        String output;
//        System.out.println("Output from Server:  \n");
        while ((output = responseBuffer.readLine()) != null) {
            response.append(output);
        }
        httpConnection.disconnect();
        return new Gson().fromJson(response.toString(), HashMap.class);
    }

    @Deprecated
    public static LabelObject getLabelForProductId(String productId, boolean useCache, boolean miniLabel) throws LabelNotFoundException {

        String productIdWithQty = "";
        String cartonQty = "", labelQty = "", packQty = "";
        long lastModifiedOnServer = 0;
        LabelObject labelObject = null;
        Map<String, Object> result;
        String[] labelPath;
        boolean packLabel = false;
        try {
            result = getJSON(GET_LABEL_DATA_END_POINT + "productId=" + productId + (miniLabel ? "&miniLabel=true" : ""));
            if((boolean)result.get("success")) {
                Map<String, String> labelData = (Map<String, String>)result.get("labelData");
                if (labelData.containsKey("PRODUCT_ID") && !labelData.get("PRODUCT_ID").isEmpty()) {
                    productId = labelData.get("PRODUCT_ID");
                    productIdWithQty = productId;
                    lastModifiedOnServer = new Long(labelData.get("LAST_MODIFIED"));
                    if(labelData.containsKey("SAMPLES_LABEL") && ((String)labelData.get("SAMPLES_LABEL")).equalsIgnoreCase("true")) {
                        productIdWithQty += "-SAMPLES";
                    } else if (labelData.containsKey("PACK_QTY") && !(packQty =  labelData.get("PACK_QTY")).isEmpty()) {
                        productIdWithQty += "-" + packQty;
                        packLabel = true;
                    } else if(labelData.containsKey("LABEL_QTY") && labelData.containsKey("CARTON_QTY") && !(labelQty = labelData.get("LABEL_QTY")).isEmpty() && !(cartonQty = labelData.get("CARTON_QTY")).isEmpty() && labelQty.equalsIgnoreCase(cartonQty)) {
                        productIdWithQty += "-" + labelQty;
                        packLabel = true;
                    }
                }
            } else {
                throw new LabelNotFoundException("An error occurred while retrieving the label from the server for the given ProductId : " + productId, null);
            }

            labelObject = new LabelObject(productIdWithQty, miniLabel);

            labelPath = LabelHelper.getLabel(productIdWithQty, !isCacheExpired(productId, lastModifiedOnServer, useCache), packLabel, miniLabel);

        } catch(Exception e) {
            throw new LabelNotFoundException("An error occurred while retrieving the label from the server for the given ProductId : " + productId, e);
        }
        if(labelPath[0].isEmpty() || labelPath[1].isEmpty()) {
            throw new LabelNotFoundException("No Label found for the given ProductId : " + productId, null);
        }
        labelObject.setLabelPDFPath(labelPath[0]);
        labelObject.setLabelPath(labelPath[1]);
        return labelObject;
    }

    @Deprecated
    protected static String[] getLabel(String productId, boolean useCache, boolean packLabel, boolean miniLabel) throws Exception {
        return getLabelFromLocal(productId, useCache, packLabel, miniLabel);
    }

    @Deprecated
    protected static String[] getLabelFromLocal(String productId, boolean useCache, boolean packLabel, boolean miniLabel) throws Exception {
        String[] labelPath = new String[fileTypes.length];
        boolean ignoreCache = shouldIgnoreCache(productId, useCache, packLabel, miniLabel);
        for(int i = 0; i < fileTypes.length; i ++) {
            labelPath[i] = getLabelFromLocal(productId, fileTypes[i], !ignoreCache, packLabel, miniLabel);
        }
        return labelPath;
    }

    @Deprecated
    protected static String getLabelFromLocal(String productId, String fileType, boolean useCache, boolean packLabel, boolean miniLabel) throws Exception {
        File label = new File(LABEL_PRINTER_CACHE_LOCATION + (miniLabel ? "MiniLabels/" : packLabel ? "PackLabels/" : "")  + productId + fileType);
        if(label.exists() && useCache) {
            return label.getAbsolutePath();
        } else {
            return getLabelFileFromServer(productId, fileType, packLabel, miniLabel);
        }
    }

    @Deprecated
    protected static boolean shouldIgnoreCache(String productId, boolean useCache, boolean packLabel, boolean miniLabel) {
        boolean ignoreCache = !useCache;
        if(ignoreCache) {
            return ignoreCache;
        }
        for(int i = 0; i < fileTypes.length; i ++) {
            ignoreCache = !new File(LABEL_PRINTER_CACHE_LOCATION + (miniLabel ? "MiniLabels/" : packLabel ? "PackLabels/" : "") + productId + fileTypes[i]).exists();
            if(ignoreCache) {
                return ignoreCache;
            }
        }
        return ignoreCache;
    }

    @Deprecated
    protected static boolean isCacheExpired(String productId, long lastModifiedTimeOnServer, boolean useCache) {

        boolean cacheExpired = !useCache;
        File labelFile = new File(LABEL_PRINTER_CACHE_LOCATION + productId + ".pdf");
        if(labelFile.lastModified() < lastModifiedTimeOnServer) {
            cacheExpired = true;
        }
        return cacheExpired;
    }

    @Deprecated
    protected static String getLabelFileFromServer(String productId, String fileType, boolean packLabel, boolean miniLabel) throws Exception {
        URL url = new URL(GET_FILE_FROM_SERVER_END_POINT + (miniLabel ? "miniLabels/" : packLabel ? "packLabels/" : "") + productId  + fileType + "&ts=" + System.currentTimeMillis());
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(LABEL_PRINTER_CACHE_LOCATION + (miniLabel ? "MiniLabels/" :packLabel ? "PackLabels/" : "") + productId + fileType), StandardCopyOption.REPLACE_EXISTING);
        in.close();
        File downloadedFile;
        if((downloadedFile = new File(LABEL_PRINTER_CACHE_LOCATION + (miniLabel ? "MiniLabels/" :packLabel ? "PackLabels/" : "") + productId + fileType)).length() <= 0) {
            downloadedFile.delete();
            return "";
        }

        return downloadedFile.getAbsolutePath();
    }

    @Deprecated
    public static boolean isOrderId(String id) {
        return id != null && (id.toUpperCase().startsWith("ENV") || id.toUpperCase().startsWith("AE"));
    }

    private static void disableSslVerification() {
        try
        {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

}
