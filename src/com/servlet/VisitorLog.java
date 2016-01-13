package com.servlet;

import com.edgars.db.HBaseSQLManager;
import com.edgars.table.Table;
import json.JSONArray;
import json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Edgars on 10/12/2015.
 * Email: edgars_fjodorovs@inbox.lv
 */
public class VisitorLog extends Table {

    /**
     * HBaseSQLManager to work with static class.
     */
    HBaseSQLManager hBaseSQLManager = new HBaseSQLManager();
    private JSONArray cart;
    private String visitedUrl;
    private String ip;
    private int timestamp;
    private String visitorMd5Id;
    private String item;
    private ResultSet resultSet;
    private int bookId;

    /**
     * Empty constructor.
     */
    public VisitorLog(JSONObject json) {
        this.cart = json.getJSONArray("cart");
        this.visitedUrl = json.getString("url");
        this.ip = json.getString("ip");
        this.timestamp = json.getInt("timestamp");
    }

    /**
     * Method to save user activities into HBase.
     */
    @Override
    public void save() {
        for (int i = 0; i < cart.length(); i++) {
            String item = getCart().getString(i).replace("\"", "").replace("{", "").replace("}", "");
            setItem(item);

            try {
                resultSet = hBaseSQLManager.executeSqlGetString(
                        "SELECT ID FROM BOOK_STORE WHERE URL = '" + getItem() + "'");
                if (resultSet.next()) setBookId(resultSet.getInt("ID"));
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (hBaseSQLManager.statement != null) try {
                    hBaseSQLManager.statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            try {
                hBaseSQLManager.executeSqlGetIdOnUpdate(
                        "UPSERT INTO VISITOR_LOG (ID, IP_ADDRESS, LINK, TIMESTAMP, MD5_ID, CART) " +
                                "VALUES(NEXT VALUE FOR VISITOR_LOG.VISITOR_LOG_SEQUENCE, '" + getIp() +
                                "', '" + getVisitedUrl() + "', " + getTimestamp() + ", '" + getVisitorMd5Id() +
                                "', " + getBookId() + ")");
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (hBaseSQLManager.statement != null) try {
                    hBaseSQLManager.statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            try {
                hBaseSQLManager.close();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void delete() {

    }

    @Override
    public int getBookId() {
        return bookId;
    }

    @Override
    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public JSONArray getCart() {
        return cart;
    }

    public void setCart(JSONArray cart) {
        this.cart = cart;
    }

    public String getVisitedUrl() {
        return visitedUrl;
    }

    public void setVisitedUrl(String visitedUrl) {
        this.visitedUrl = visitedUrl;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getVisitorMd5Id() {
        return visitorMd5Id;
    }

    public void setVisitorMd5Id(String visitorMd5Id) {
        this.visitorMd5Id = visitorMd5Id;
    }

    public HBaseSQLManager gethBaseSQLManager() {
        return hBaseSQLManager;
    }

    public void sethBaseSQLManager(HBaseSQLManager hBaseSQLManager) {
        this.hBaseSQLManager = hBaseSQLManager;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }


}
