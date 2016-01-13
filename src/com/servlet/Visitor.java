package com.servlet;

import com.edgars.db.HBaseSQLManager;
import com.edgars.table.Table;
import json.JSONArray;
import json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class Visitor identifies visitor and saves data to HBase.
 * <p/>
 * Created by Edgars on 03/11/2015.
 * Email: edgars_fjodorovs@inbox.lv
 */
public class Visitor extends Table {

    /**
     * HBaseSQLManager to work with static class.
     */
    HBaseSQLManager hBaseSQLManager = new HBaseSQLManager();

    /**
     * ResultSet is what SELECT returns.
     */
    ResultSet resultSet = null;

    /**
     * Visitor's User Agent.
     */
    private String userAgent;

    /**
     * Visitor's HTTP_ACCEPT Headers.
     */
    private JSONArray headers;

    /**
     * Visitor's Screen Size and Color Depth.
     */
    private JSONArray screen;

    /**
     * Visitor's Time Zone.
     */
    private int timezone;

    /**
     * Are Cookies Enabled?.
     */
    private boolean enabledCookies;

    /**
     * Visitor's Browser Plugin Details.
     */
    private JSONArray plugins;

    /**
     * Visitor's System Fonts.
     */
    private JSONArray fonts;

    /**
     * md5Data is generated from visitor's data above.
     */
    private String md5Data;

    /**
     * md5Id will be generated from ID of visitor from HBase.
     */
    private String md5Id;

    /**
     * Empty constructor.
     */
    public Visitor() {
    }

    /**
     * Creating Visitor object with collected data.
     *
     * @param json JSONObject with visitor's data.
     */
    public Visitor(JSONObject json) {
        /**
         * This string is made of all information about visitor.
         */
        String userDataStringForMD5 = "";

        String userAgent = json.getString("userAgent");
        userDataStringForMD5 += userAgent;
        this.userAgent = userAgent;

        JSONArray headers = json.getJSONArray("headers");
        JSONArray headersArray = new JSONArray();
        for (int i = 0; i < headers.length(); i++) {
            String head = headers.getString(i).replace("\"", "").replace("{", "").replace("}", "");
            String[] splitLine = head.split(":");
            userDataStringForMD5 += splitLine[1];
            headersArray.put(splitLine[1]);
        }
        this.headers = headers;

        JSONArray screen = json.getJSONArray("screen");
        JSONArray screenArray = new JSONArray();
        for (int i = 0; i < screen.length(); i++) {
            String scr = screen.getString(i).replace("\"", "").replace("{", "").replace("}", "");
            String[] splitLine = scr.split(":");
            userDataStringForMD5 += splitLine[1];
            screenArray.put(splitLine[1]);
        }
        this.screen = screen;

        int timezone = json.getInt("timezone");
        userDataStringForMD5 += timezone;
        this.timezone = timezone;

        boolean enabledCookies = json.getBoolean("enabledCookies");
        userDataStringForMD5 += enabledCookies;
        this.enabledCookies = enabledCookies;

        JSONArray plugins = json.getJSONArray("plugins");
        JSONArray pluginsArray = new JSONArray();
        for (int i = 0; i < plugins.length(); i++) {
            String plugin = plugins.getString(i).replace("\"", "").replace("{", "").replace("}", "");
            String[] splitLine = plugin.split(":");
            userDataStringForMD5 += splitLine[1];
            pluginsArray.put(splitLine[1]);
        }
        this.plugins = plugins;

        JSONArray fonts = json.getJSONArray("fonts");
        JSONArray fontsArray = new JSONArray();
        for (int i = 0; i < fonts.length(); i++) {
            String font = fonts.getString(i).replace("\"", "").replace("{", "").replace("}", "");
            String[] splitLine = font.split(":");
            userDataStringForMD5 += splitLine[1];
            fontsArray.put(splitLine[1]);
        }
        this.fonts = fonts;

        this.md5Id = json.getString("cookiesId");

        this.md5Data = createMD5(userDataStringForMD5);
    }

    /**
     * String to MD5 string converter.
     *
     * @param str String to convert
     * @return Converted MD5 string.
     */
    private static String createMD5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Boolean identify() throws IOException, SQLException {
        Boolean result = false;
        try {
            System.out.println("getMd5Id: " + getMd5Id());
            resultSet = hBaseSQLManager.executeSqlGetString(
                    "SELECT MD5_ID FROM VISITOR WHERE MD5_ID ='" + getMd5Id() + "'");
            if (resultSet.next()) result = true;
            System.out.println("identify result: " + result);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (hBaseSQLManager.statement != null) try {
                hBaseSQLManager.statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("identify result: " + result);
        return result;
    }

    public void getVisitorItemIgnoreList() {

    }

    public void getVisitorPurchaseList() {

    }

    /**
     * Method which saves visitor. There are listed 4 cases:
     * 1. Visitor's cookiesId & cookiesData are equal to visitor's MD5_ID & MD5_DATA into HBase.
     * 2. Visitor's cookiesId are equal to MD5_ID from HBase, but DATA is not equal. The we update visitor.
     * 3. Visitor's cookies does not have cookiesId or cookiesData. If visitor's md5Data is found into HBase, then we assign visitor to
     * existing one.
     * 4.  Visitor's cookies does not have cookiesId or cookiesData. Create new visitor if nothing similar found into Hbase.
     */
    @Override
    public void save() {
        //JSONObject response = new JSONObject();
        String cookiesId = getMd5Id();
        String cookiesData = getMd5Data();
        String md5IdSearch = "";
        String md5DataSearch = "";
        int visitorId = 0;

        // CASE: WHEN THERE ARE COOKIES BUT THERE ARE NO USER IN DB!!!

        /**
         * This if checks if visitor has cookies.
         */
        if (!cookiesId.equals("")) {
            try {
                resultSet = hBaseSQLManager.executeSqlGetString(
                        "SELECT * FROM VISITOR WHERE MD5_ID ='" + cookiesId + "'");
                if (resultSet.next()) {
                    md5IdSearch = resultSet.getString("MD5_ID");
                    md5DataSearch = resultSet.getString("MD5_DATA");
                    visitorId = resultSet.getInt("ID");
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (hBaseSQLManager.statement != null) try {
                    hBaseSQLManager.statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (cookiesId.equals(md5IdSearch)) {
/*                try {
                    resultSet = hBaseSQLManager.executeSqlGetString(
                            "SELECT * FROM VISITOR WHERE MD5_ID = '" + md5IdSearch + "'");
                    if (resultSet.next()) {
                        md5DataSearch = resultSet.getString("MD5_DATA");
                        visitorId = resultSet.getInt("ID");
                        response.put("visitorId!!!", visitorId);
                    }
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (hBaseSQLManager.statement != null) hBaseSQLManager.statement.close();
                }*/
                if (cookiesData.equals(md5DataSearch)) {
                    // Everything is okay, visitor didn't change anything. OK
/*                    response.put("message:", "Everything is OK!");
                    response.put("cookiesId", getMd5Id());
                    response.put("cookiesData", getMd5Data());*/
                } else {
                        /* Seems user changed something. We'll update visitor. OK */
                    try {
                        hBaseSQLManager.executeSqlGetIdOnUpdate(
                                "UPSERT INTO VISITOR(ID, USER_AGENT, HEADERS, " +
                                        "TIMEZONE, SCREEN, ENABLED_COOKIES, PLUGINS, FONTS, MD5_ID, MD5_DATA) " +
                                        "VALUES(" + visitorId + ", '" + getUserAgent() + "', '" + getHeaders() + "', " +
                                        getTimezone() +
                                        ", '" + getScreen() + "', '" + isEnabledCookies() + "'" + ", '" +
                                        getPlugins() +
                                        "', '" + getFonts() + "', '" + getMd5Id() + "', '" + getMd5Data() + "')");
                    } catch (SQLException | ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (hBaseSQLManager.statement != null) try {
                            hBaseSQLManager.statement.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
 /*                   response.put("message", "Visitor data updated!");
                    response.put("cookiesId", getMd5Id());
                    response.put("cookiesData", getMd5Data());*/
                }
            }
        }

        /**
         * This if checks if visitor does not have cookies.
         */
        if (cookiesId.equals("")) {
            String md5id = "";
            try {
                resultSet = hBaseSQLManager.executeSqlGetString(
                        "SELECT MD5_DATA FROM VISITOR WHERE MD5_DATA = '" + getMd5Data() + "'");
                if (resultSet.next()) md5DataSearch = resultSet.getString("MD5_DATA");
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (hBaseSQLManager.statement != null) try {
                    hBaseSQLManager.statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (md5DataSearch.equals(getMd5Data())) {
                try {
                    resultSet = hBaseSQLManager.executeSqlGetString("SELECT MD5_ID FROM VISITOR WHERE MD5_DATA = '" +
                            md5DataSearch + "'");
                    if (resultSet.next()) md5id = resultSet.getString("MD5_ID");
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (hBaseSQLManager.statement != null) try {
                        hBaseSQLManager.statement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                setMd5Id(md5id);
/*                response.put("message", "Data assigned to existing visitor!");
                response.put("cookiesId", getMd5Id());
                response.put("cookiesData", getMd5Data());*/
            } else {
                try {
                    hBaseSQLManager.executeSqlGetIdOnUpdate(
                            "UPSERT INTO VISITOR(ID, USER_AGENT, TIMEZONE, HEADERS, SCREEN, ENABLED_COOKIES, PLUGINS," +
                                    " FONTS, MD5_DATA) " +
                                    "VALUES(NEXT VALUE FOR VISITOR.VISITOR_SEQUENCE, '" + getUserAgent() +
                                    "', " + getTimezone() + ", '" + getHeaders() + "', '" + getScreen() +
                                    "', '" + isEnabledCookies() + "'" + ", '" + getPlugins() + "', '"
                                    + getFonts() + "', '" + getMd5Data() + "')");
                    try {
                        resultSet = hBaseSQLManager.executeSqlGetString("SELECT ID FROM VISITOR WHERE MD5_DATA = '" +
                                getMd5Data() + "'");
                        if (resultSet.next()) visitorId = resultSet.getInt("ID");
                    } catch (SQLException | ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (hBaseSQLManager.statement != null) hBaseSQLManager.statement.close();
                    }
                    md5id = createMD5(Integer.toString(visitorId));
                    setMd5Id(md5id);
                    hBaseSQLManager.executeSqlGetIdOnUpdate(
                            "UPSERT INTO VISITOR(ID, MD5_ID) VALUES(" + visitorId + ", '" + getMd5Id() + "')");
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (hBaseSQLManager.statement != null) try {
                        hBaseSQLManager.statement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
/*                response.put("message", "New visitor created!");
                response.put("cookiesId", getMd5Id());
                response.put("cookiesData", getMd5Data());*/
            }
        }

        try {
            try {
                hBaseSQLManager.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void delete() {

    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public JSONArray getHeaders() {
        return headers;
    }

    public void setHeaders(JSONArray headers) {
        this.headers = headers;
    }

    public JSONArray getScreen() {
        return screen;
    }

    public void setScreen(JSONArray screen) {
        this.screen = screen;
    }

    public int getTimezone() {
        return timezone;
    }

    public void setTimezone(int timezone) {
        this.timezone = timezone;
    }

    public boolean isEnabledCookies() {
        return enabledCookies;
    }

    public void setEnabledCookies(boolean enabledCookies) {
        this.enabledCookies = enabledCookies;
    }

    public JSONArray getPlugins() {
        return plugins;
    }

    public void setPlugins(JSONArray plugins) {
        this.plugins = plugins;
    }

    public JSONArray getFonts() {
        return fonts;
    }

    public void setFonts(JSONArray fonts) {
        this.fonts = fonts;
    }

    public String getMd5Id() {
        return md5Id;
    }

    public void setMd5Id(String md5Id) {
        this.md5Id = md5Id;
    }

    public String getMd5Data() {
        return md5Data;
    }

    public void setMd5Data(String md5Data) {
        this.md5Data = md5Data;
    }

    public HBaseSQLManager gethBaseSQLManager() {
        return hBaseSQLManager;
    }

    public void sethBaseSQLManager(HBaseSQLManager hBaseSQLManager) {
        this.hBaseSQLManager = hBaseSQLManager;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

}
