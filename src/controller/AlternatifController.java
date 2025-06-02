package controller;

import javax.swing.JOptionPane;
import java.sql.Connection;
import javax.swing.JFrame;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import koneksi.koneksi;



public class AlternatifController {
    private static Connection conn;

    private JFrame parent;

    public AlternatifController(JFrame parent) {
        this.parent = parent;
    }
    
    public void insertPeringkat(String idProduk, double score) {
        try {
            String sql = "INSERT INTO peringkat (idkaryawan, score) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, idProduk);
            stmt.setDouble(2, score);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    } 
    
    public List<Object[]> getPeringkat() {
       List<Object[]> data = new ArrayList<>();
       try {
           String sql = "SELECT p.idkaryawan, k.nama, k.jabatan, p.score FROM peringkat p JOIN karyawan k ON p.idkaryawan = k.idkaryawan ORDER BY p.score DESC";
           PreparedStatement stmt = conn.prepareStatement(sql);
           ResultSet rs = stmt.executeQuery();

           while (rs.next()) {
               data.add(new Object[]{
                   rs.getString("idkaryawan"),
                   rs.getString("nama"),
                   rs.getString("jabatan"),
                   rs.getDouble("score")
               });
           }
       } catch (SQLException e) {
           JOptionPane.showMessageDialog(null, "Gagal mengambil data peringkat: " + e.getMessage());
       }
       return data;
    }
    
    public void deleteAllPeringkat() {
        try {
            String sql = "DELETE FROM peringkat";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menghapus peringkat: " + e.getMessage());
        }
    }
}
