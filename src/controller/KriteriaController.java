package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JFrame;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.KriteriaModel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author richard
 */

public class KriteriaController {
    private Connection conn;
   
    private JFrame parent;

    public KriteriaController(JFrame parent) {
        this.parent = parent;
    }
    
    public class DBConnection {
        private final String URL = "jdbc:mysql://localhost:3306/db_tugasakhir"; // Ganti dengan nama database kamu
        private final String USER = "root"; // Ganti jika bukan root
        private final String PASSWORD = ""; // Ganti jika ada password

        public Connection getConnection() {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Koneksi gagal: " + e.getMessage());
                return null;
            }
        }
    }

    // Ambil semua kriteria
    public List<KriteriaModel> getAllKriteria() {
        List<KriteriaModel> kriteriaList = new ArrayList<>();
        String sql = "SELECT * FROM tb_kriteria";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        KriteriaModel kriteria = new KriteriaModel(
                                rs.getInt("idkriteria"),
                                rs.getString("namaKriteria"),
                                rs.getDouble("bobotKriteria")
                        );
                        kriteriaList.add(kriteria);
                    }
                }
        } catch (SQLException e) {
            return null;
        }
        return kriteriaList;
    }

    // Update bobot kriteria
    public boolean updateBobotKriteria(int idKriteria, double bobotBaru) {
        String sql = "UPDATE tb_kriteria SET bobotKriteria = ? WHERE idKriteria = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, bobotBaru);
            stmt.setInt(2, idKriteria);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            
            return false;
        }
    }

    // Reset semua bobot
    public boolean resetSemuaBobot() {
        String sql = "UPDATE tb_kriteria SET bobotKriteria = NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
           
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
           
            return false;
        }
    }    
    
    public double[][] getMatrix() {
        List<KriteriaModel> list = getAllKriteria();
        int n = list.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            double bobotI = list.get(i).getBobot();
            for (int j = 0; j < n; j++) {
                double bobotJ = list.get(j).getBobot();
                if (bobotJ == 0 || bobotI == 0) {
                    matrix[i][j] = 1.0;
                } else {
                    matrix[i][j] = bobotI / bobotJ;
                }
            }
        }
        return matrix;
    }
}
