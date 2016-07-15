package com.envelopes.apps.labelprinter;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Manu on 7/8/2016.
 */
public class LabelHelper {
    public static String LABEL_PRINTER_HOME = "C:/LabelPrinter/";
    public static String LABEL_SERVER_END_POINT = "http://192.168.1.155/";
    public static final String CONFIG_FILE_LOCATION = LABEL_PRINTER_HOME + "labelPrinter.properties";
    public static final String LABEL_PRINTER_CACHE_LOCATION = LABEL_PRINTER_HOME + "ProductLabels/";
    public static final String LABEL_PRINTER_DIRECTORY_GENERATION_PATH = LABEL_PRINTER_CACHE_LOCATION + "PackLabels/";
    public static final String GET_FILE_FROM_SERVER_END_POINT = LABEL_SERVER_END_POINT + "admin/control/serveLabelForStream?filePath=/uploads/productLabels/";
    public static final String GET_LABEL_DATA_END_POINT = LABEL_SERVER_END_POINT + "admin/control/getLabelData?";
    protected static String[] fileTypes = {".pdf", ".png"};

    public static String initializeLabelPrinter() {
        File testFile = new File(LABEL_PRINTER_DIRECTORY_GENERATION_PATH);
        if(!testFile.exists()) {
            new File(LABEL_PRINTER_DIRECTORY_GENERATION_PATH).mkdirs();
        }
        checkConfigFile();
        return "";
    }

    protected static void checkConfigFile() {
        File configFile = new File(CONFIG_FILE_LOCATION);
        if(!configFile.exists()) {
            try {
                FileOutputStream fileOut = new FileOutputStream(configFile);
                Properties properties = new Properties();

                properties.setProperty("labelPrinterHome", "C:/LabelPrinter/");
                properties.setProperty("labelServerEndPoint", "http://192.168.1.155/");
                properties.store(fileOut, "Label Printer Configuration Properties");
                fileOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Properties properties = new Properties();
                InputStream inputStream = new FileInputStream(new File(CONFIG_FILE_LOCATION));
                properties.load(inputStream);
                if(properties.containsKey("labelPrinterHome")) {
                    LABEL_PRINTER_HOME = properties.getProperty("labelPrinterHome");
                }

                if(properties.containsKey("labelServerEndPoint")) {
                    LABEL_SERVER_END_POINT = properties.getProperty("labelServerEndPoint");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static String getOrderDetails(String orderId) {

        return "";
    }

    public static LabelObject getLabelForProductId(String productId, boolean useCache) throws LabelNotFoundException {

        String productIdWithQty = "";
        String cartonQty = "", boxQty = "", packQty = "";
        long lastModifiedOnServer = 0;
        LabelObject labelObject = null;
        Map<String, Object> result;
        String[] labelPath;
        try {
            result = getJSON(GET_LABEL_DATA_END_POINT + "productId=" + productId);
            if((boolean)result.get("success")) {
                Map<String, String> labelData = (Map<String, String>)result.get("labelData");
                if (labelData.containsKey("PRODUCT_ID") && !labelData.get("PRODUCT_ID").isEmpty()) {
                    productId = labelData.get("PRODUCT_ID");
                    productIdWithQty = productId;
                    lastModifiedOnServer = new Long(labelData.get("LAST_MODIFIED"));
                    if (labelData.containsKey("PACK_QTY") && !(packQty =  labelData.get("PACK_QTY")).isEmpty()) {
                        productIdWithQty += "-" + packQty;
                        if (labelData.containsKey("BOX_QTY")) {
                            boxQty = labelData.get("BOX_QTY");
                        }
                        if (labelData.containsKey("CARTON_QTY")) {
                            cartonQty = labelData.get("CARTON_QTY");
                        }
                    }
                }
            } else {
                throw new LabelNotFoundException("An error occurred while retrieving the label from the server for the given ProductId : " + productId, null);
            }

            if(!packQty.isEmpty()) {
                int _packQty = Integer.parseInt(packQty);
                if(!boxQty.isEmpty()) {
                    int _boxQty = Integer.parseInt(boxQty);
                    if(_packQty >= _boxQty && _packQty % _boxQty == 0) {
                        packQty = "";
                        productIdWithQty = productId;
                    }
                } else if(!cartonQty.isEmpty()) {
                    int _cartonQty = Integer.parseInt(cartonQty);
                    if(_packQty >= _cartonQty && _packQty % _cartonQty == 0) {
                        packQty = "";
                        productIdWithQty = productId;
                    }
                }
            }

            boolean packLabel = !packQty.isEmpty();
            labelObject = new LabelObject(productIdWithQty);

            labelPath = LabelHelper.getLabel(productIdWithQty, !isCacheExpired(productId, lastModifiedOnServer, useCache), packLabel);

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

    protected static String[] getLabel(String productId, boolean useCache, boolean packLabel) throws Exception {
        return getLabelFromLocal(productId, useCache, packLabel);
    }

    protected static String[] getLabelFromLocal(String productId, boolean useCache, boolean packLabel) throws Exception {
        String[] labelPath = new String[fileTypes.length];
        boolean ignoreCache = shouldIgnoreCache(productId, useCache, packLabel);
        for(int i = 0; i < fileTypes.length; i ++) {
            labelPath[i] = getLabelFromLocal(productId, fileTypes[i], !ignoreCache, packLabel);
        }
        return labelPath;
    }

    protected static String getLabelFromLocal(String productId, String fileType, boolean useCache, boolean packLabel) throws Exception {
        File label = new File(LABEL_PRINTER_CACHE_LOCATION + (packLabel ? "PackLabels/" : "")  + productId + fileType);
        if(label.exists() && useCache) {
            return label.getAbsolutePath();
        } else {
            return getLabelFileFromServer(productId, fileType, packLabel);
        }
    }

    protected static boolean shouldIgnoreCache(String productId, boolean useCache, boolean packLabel) {
        boolean ignoreCache = !useCache;
        if(ignoreCache) {
            return ignoreCache;
        }
        for(int i = 0; i < fileTypes.length; i ++) {
            ignoreCache = !new File(LABEL_PRINTER_CACHE_LOCATION + (packLabel ? "PackLabels/" : "") + productId + fileTypes[i]).exists();
            if(ignoreCache) {
                return ignoreCache;
            }
        }
        return ignoreCache;
    }

    protected static boolean isCacheExpired(String productId, long lastModifiedTimeOnServer, boolean useCache) {

        boolean cacheExpired = !useCache;
        File labelFile = new File(LABEL_PRINTER_CACHE_LOCATION + productId + ".pdf");
        if(labelFile.lastModified() < lastModifiedTimeOnServer) {
            cacheExpired = true;
        }
        return cacheExpired;
    }

    protected static String getLabelFileFromServer(String productId, String fileType, boolean packLabel) throws Exception {
        URL url = new URL(GET_FILE_FROM_SERVER_END_POINT + (packLabel ? "packLabels/" : "") + productId  + fileType);
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(LABEL_PRINTER_CACHE_LOCATION + (packLabel ? "PackLabels/" : "") + productId + fileType), StandardCopyOption.REPLACE_EXISTING);
        in.close();
        File downloadedFile;
        if((downloadedFile = new File(LABEL_PRINTER_CACHE_LOCATION + (packLabel ? "PackLabels/" : "") + productId + fileType)).length() <= 0) {
            downloadedFile.delete();
            return "";
        }

        return downloadedFile.getAbsolutePath();
    }

    public static boolean isOrderId(String id) {
        return id != null && (id.toUpperCase().startsWith("ENV") || id.toUpperCase().startsWith("AE"));
    }

    public static Map<String, Object> getJSON(String targetURL) throws Exception {
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

}
