package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    static void insertBeer (Connection conn, int id, String name, String type) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO beers VALUES (?, ?, ?)");
        stmt.setInt(1, id);
        stmt.setString(2, name);
        stmt.setString(3, type);
        stmt.execute();
    }

    static void deleteBeer (Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM beers WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();

      /*  int updateId = 0;
        for (int i=0; i < selectBeers(conn).size(); i++) {
            int updateId = i;
            selectBeers(conn).get(i);

        }
        PreparedStatement stmt2 = conn.prepareStatement("UPDATE beers SET id = ?");
        stmt2.setInt(1, updateId);
        stmt2.execute();*/
    }

    static ArrayList<Beer> selectBeers (Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM beers");
        ArrayList<Beer> beers = new ArrayList();
        while (results.next()) {
            int id = results.getInt("id");
            String name = results.getString("name");
            String type = results.getString("type");
            Beer beer = new Beer(id, name, type);
            beers.add(beer);
        }
        return(beers);
    }

    static void editBeer (Connection conn, int id, String name, String type) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE beers SET id = ?, name = ?, type = ?");
        stmt.setInt(1, id);
        stmt.setString(2, name);
        stmt.setString(3, type);
        stmt.execute();

    }

    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS beers (id INT, name VARCHAR, type VARCHAR)");



        Spark.get(
                "/",
                ((request, response) -> {
                    ArrayList<Beer> beers = selectBeers(conn);
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        return new ModelAndView(new HashMap(), "not-logged-in.html");
                    }
                    HashMap m = new HashMap();
                    m.put("username", username);
                    m.put("beers", beers);
                    return new ModelAndView(m, "logged-in.html");
                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/login",
                ((request, response) -> {
                    String username = request.queryParams("username");
                    Session session = request.session();
                    session.attribute("username", username);
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/create-beer",
                ((request, response) -> {
                    ArrayList<Beer> beers = selectBeers(conn);
                    Beer beer = new Beer();
                    beer.id = beers.size() + 1;
                    beer.name = request.queryParams("beername");
                    beer.type = request.queryParams("beertype");
                    insertBeer(conn, beer.id, beer.name, beer.type);
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/delete-beer",
                ((request, response) -> {
                    String id = request.queryParams("beerid");
                    deleteBeer(conn, Integer.valueOf(id));
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/edit-beer",
                ((request, response) -> {
                    String id = request.queryParams("editBeerId");
                    int idNum = Integer.valueOf(id);
                    String editName = request.queryParams("editBeerName");
                    String editType = request.queryParams("editBeerType");
                    ArrayList<Beer> beers = selectBeers(conn);
                    for (Beer beer : beers) {
                        if (beer.id == idNum) {

                            editBeer(conn, beer.id, editName, editType);
                        }
                    }
                    response.redirect("/");
                    return "";
                })
        );
    }
}
