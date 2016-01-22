package com.servlet;


import com.edgars.algorithm.Algorithm;
import com.edgars.algorithm.MostPopular;
import com.edgars.algorithm.Random;
import com.edgars.db.HBaseSQLManager;
import com.edgars.table.PrintBook;
import json.JSONArray;
import json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Edgars on 03/11/2015.
 * Email: edgars_fjodorovs@inbox.lv
 */
public class RecommendationServlet extends HttpServlet {


    ResultSet resultSet = null;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("json");

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        JSONObject json = new JSONObject(sb.toString());
        Visitor visitor = new Visitor(json);

        try {
            if (visitor.identify()) {
                VisitorLog visitorLog = new VisitorLog(json);
                visitorLog.setVisitorMd5Id(visitor.getMd5Id());
                visitorLog.save();
                visitor.save();
            } else {
                visitor.save();
                VisitorLog visitorLog = new VisitorLog(json);
                visitorLog.setVisitorMd5Id(visitor.getMd5Id());
                visitorLog.save();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JSONObject responseJSON = new JSONObject();
        responseJSON.put("cookiesId", visitor.getMd5Id());
        responseJSON.put("cookiesData", visitor.getMd5Data());

        response.getWriter().println(responseJSON);
        response.setStatus(200);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("json");
        String md5Id = request.getParameter("id");
        HBaseSQLManager hBaseSQLManager = new HBaseSQLManager();
        int id = 0;
        try {
            resultSet = hBaseSQLManager.executeSqlGetString(
                    "SELECT ID FROM VISITOR WHERE MD5_ID = '" + md5Id + "'"
            );
            while (resultSet.next()) {
                id = resultSet.getInt("ID");
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

        Visitor visitor = new Visitor();
        visitor.setMd5Id(md5Id);
        Algorithm algorithm = null;

        try {
            if (visitor.identify()) {
                try {
                    algorithm = new MostPopular(id, 3);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    algorithm = new Random(id, 3);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // MAKE FROM IT SHOW RECOMMENDATION METHOD OR SMTH
            List list = algorithm.getRecommendation();
            JSONArray jsonArray = new JSONArray();

            for (int i = 0; i < list.size(); i++) {
                PrintBook book = new PrintBook(list.get(i).toString());
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("title", book.getTitle());
                jsonObject.put("img", book.getImg());
                jsonObject.put("price", book.getPrice());
                jsonObject.put("url", book.getUrl());

                jsonArray.put(jsonObject);
            }

            JSONObject bookJSONObject = new JSONObject();
            bookJSONObject.put("book", jsonArray);

            response.getWriter().println(bookJSONObject);

            try {
                hBaseSQLManager.close();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        response.setStatus(200);

    }
}
