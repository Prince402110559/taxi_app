package com.example.myapplication;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class connectionDB {
    public static void main(String[] args){
        String url = "jdbc:mysql://localhost:3306/testDB";
        String user = "Root";
        String password = "Marumbine.04";

        try{
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("connection successful");
            conn.close();

        }catch(SQLException e){
            System.out.println("connection failed " + e.getMessage());
        }

    }

}
