/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package mainMenu;

import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import koneksi.koneksi;

/**
 *
 * @author richard
 */
class Produk {
    String nama;
    double kualitas, penjualan, stok, rating;
    double skor;

    public Produk(String nama, double kualitas, double penjualan, double stok, double rating) {
        this.nama = nama;
        this.kualitas = kualitas;
        this.penjualan = penjualan;
        this.stok = stok;
        this.rating = rating;
    }
}

public class alternatif extends javax.swing.JPanel {
//    public static void main(String[] args) {
//        List<Produk> produkList = new ArrayList<>();
//
//        // 1. Ambil data produk dari database
//        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db_tugasakhir", "root", "");
//             Statement stmt = conn.createStatement()) {
//
//            ResultSet rs = stmt.executeQuery("SELECT * FROM tb_dataproduk");
//            while (rs.next()) {
//                produkList.add(new Produk(
//                        rs.getString("namaProduk"),
//                        rs.getDouble("kualitas"),
//                        rs.getDouble("penjualan"),
//                        rs.getDouble("stok"),
//                        rs.getDouble("rating")
//                ));
//            }
//
//            // 2. Cari nilai max dan min
//            double maxKualitas = produkList.stream().mapToDouble(p -> p.kualitas).max().orElse(1);
//            double maxPenjualan = produkList.stream().mapToDouble(p -> p.penjualan).max().orElse(1);
//            double minStok = produkList.stream().mapToDouble(p -> p.stok).min().orElse(1); // stok = cost
//            double maxRating = produkList.stream().mapToDouble(p -> p.rating).max().orElse(1);
//
//            // 3. Ambil bobot dari tb_kriteria
//            Map<String, Double> bobot = new HashMap<>();
//            ResultSet rsk = stmt.executeQuery("SELECT * FROM tb_kriteria");
//            while (rsk.next()) {
//                bobot.put(rsk.getString("namaKriteria").toLowerCase(), rsk.getDouble("bobotKriteria"));
//            }
//
//            // 4. Hitung skor akhir
//            for (Produk p : produkList) {
//                double normKualitas = p.kualitas / maxKualitas;
//                double normPenjualan = p.penjualan / maxPenjualan;
//                double normStok = minStok / p.stok; // karena cost
//                double normRating = p.rating / maxRating;
//
//                p.skor = (normKualitas * bobot.get("kualitas")) +
//                         (normPenjualan * bobot.get("penjualan")) +
//                         (normStok * bobot.get("stok")) +
//                         (normRating * bobot.get("rating"));
//            }
//
//            // 5. Urutkan berdasarkan skor
//            produkList.sort((a, b) -> Double.compare(b.skor, a.skor));
//
//            // 6. Tampilkan hasil
//            System.out.printf("%-20s %-10s %-10s\n", "Nama Produk", "Skor", "Peringkat");
//            int peringkat = 1;
//            for (Produk p : produkList) {
//                System.out.printf("%-20s %-10.4f %-10d\n", p.nama, p.skor, peringkat++);
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

    
    

    /**
     * Creates new form alternatif
     */
    public alternatif() {
        initComponents();
        tampilData();
        
        
        DefaultTableCellRenderer head_render = new DefaultTableCellRenderer();
        head_render.setBackground(new Color(0,102,0)); // Warna latar belakang header
        head_render.setForeground(Color.WHITE); // Warna teks header putih

        tbl_ranking.getTableHeader().setDefaultRenderer(head_render);
        tbl_ranking.getTableHeader().setOpaque(false);
        tbl_ranking.setRowHeight(25);
    }
    
    private void hitungAlternatif() {
        List<Produk> produkList = new ArrayList<>();

        try (Connection conn = koneksi.getKoneksi(); Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM tb_dataproduk");
            while (rs.next()) {
                produkList.add(new Produk(
                        rs.getString("namaProduk"),
                        rs.getDouble("kualitas"),
                        rs.getDouble("penjualan"),
                        rs.getDouble("stok"),
                        rs.getDouble("rating")
                ));
            }

            double maxKualitas = produkList.stream().mapToDouble(p -> p.kualitas).max().orElse(1);
            double maxPenjualan = produkList.stream().mapToDouble(p -> p.penjualan).max().orElse(1);
            double minStok = produkList.stream().mapToDouble(p -> p.stok).min().orElse(1);
            double maxRating = produkList.stream().mapToDouble(p -> p.rating).max().orElse(1);

            Map<String, Double> bobot = new HashMap<>();
            ResultSet rsk = stmt.executeQuery("SELECT * FROM tb_kriteria");
            while (rsk.next()) {
                bobot.put(rsk.getString("namaKriteria").toLowerCase(), rsk.getDouble("bobotKriteria"));
            }

            for (Produk p : produkList) {
                double normKualitas = p.kualitas / maxKualitas;
                double normPenjualan = p.penjualan / maxPenjualan;
                double normStok = minStok / p.stok;
                double normRating = p.rating / maxRating;

                p.skor = (normKualitas * bobot.get("kualitas")) +
                         (normPenjualan * bobot.get("penjualan")) +
                         (normStok * bobot.get("stok")) +
                         (normRating * bobot.get("rating"));
            }

            produkList.sort((a, b) -> Double.compare(a.skor, b.skor));

            // Simpan ke tb_ranking
            Statement clearStmt = conn.createStatement();
            clearStmt.executeUpdate("DELETE FROM tb_ranking");

            PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO tb_ranking (namaProduk, skorProduk, peringkatProduk) VALUES (?, ?, ?)");

            int peringkat = 1;
            for (Produk p : produkList) {
                insertStmt.setString(1, p.nama);
                insertStmt.setDouble(2, p.skor);
                insertStmt.setInt(3, peringkat++);
                insertStmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(null, "Perhitungan dan penyimpanan berhasil!");
            tampilData(); // refresh table

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal menghitung: " + e.getMessage());
        }
    }
    
    
    private void tampilData() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Nama Produk");
        model.addColumn("Skor");
        model.addColumn("Peringkat");

        try {
            String sql = "SELECT namaProduk, skorProduk, peringkatProduk FROM tb_ranking ORDER BY peringkatProduk ASC";

            Connection conn = koneksi.getKoneksi();
            Statement stm = conn.createStatement();
            ResultSet res = stm.executeQuery(sql);

            while (res.next()) {
                model.addRow(new Object[]{
                    res.getString("namaProduk"),
                    String.format("%.3f", res.getDouble("skorProduk")),
                    res.getInt("peringkatProduk")
                });
            }

            tbl_ranking.setModel(model);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal menampilkan data: " + e.toString());
        }
    }
    
    private void hapusData() {
        try {
            Connection conn = koneksi.getKoneksi();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM tb_ranking");
            JOptionPane.showMessageDialog(null, "Data berhasil dihapus.");
            tampilData(); // refresh table setelah dihapus
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal menghapus data: " + e.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        panelCustom2 = new custom.panelCustom();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_ranking = new javax.swing.JTable();
        btn_hitung = new javax.swing.JButton();
        btn_hapus = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setLayout(new java.awt.CardLayout());

        jPanel2.setBackground(new java.awt.Color(0, 102, 0));
        jPanel2.setPreferredSize(new java.awt.Dimension(1718, 773));

        panelCustom2.setBackground(new java.awt.Color(255, 255, 255));
        panelCustom2.setPreferredSize(new java.awt.Dimension(1718, 922));
        panelCustom2.setRoundBottomLeft(50);
        panelCustom2.setRoundBottomRight(50);
        panelCustom2.setRoundTopLeft(50);
        panelCustom2.setRoundTopRight(50);

        tbl_ranking.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tbl_ranking);

        btn_hitung.setText("Hitung Peringkat");
        btn_hitung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_hitungActionPerformed(evt);
            }
        });

        btn_hapus.setText("Hapus Data");
        btn_hapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_hapusActionPerformed(evt);
            }
        });

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("MENGHITUNG PERINGKAT");

        javax.swing.GroupLayout panelCustom2Layout = new javax.swing.GroupLayout(panelCustom2);
        panelCustom2.setLayout(panelCustom2Layout);
        panelCustom2Layout.setHorizontalGroup(
            panelCustom2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCustom2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCustom2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1138, Short.MAX_VALUE)
                    .addGroup(panelCustom2Layout.createSequentialGroup()
                        .addGroup(panelCustom2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelCustom2Layout.createSequentialGroup()
                                .addComponent(btn_hitung)
                                .addGap(18, 18, 18)
                                .addComponent(btn_hapus))
                            .addComponent(jLabel1))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panelCustom2Layout.setVerticalGroup(
            panelCustom2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCustom2Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 338, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCustom2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_hitung)
                    .addComponent(btn_hapus))
                .addContainerGap(460, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelCustom2, javax.swing.GroupLayout.DEFAULT_SIZE, 1150, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelCustom2, javax.swing.GroupLayout.DEFAULT_SIZE, 910, Short.MAX_VALUE)
        );

        add(jPanel2, "card2");
    }// </editor-fold>//GEN-END:initComponents

    private void btn_hitungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_hitungActionPerformed
        // TODO add your handling code here:
        hitungAlternatif();
    }//GEN-LAST:event_btn_hitungActionPerformed

    private void btn_hapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_hapusActionPerformed
        // TODO add your handling code here:
        hapusData();
    }//GEN-LAST:event_btn_hapusActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_hapus;
    private javax.swing.JButton btn_hitung;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private custom.panelCustom panelCustom2;
    private javax.swing.JTable tbl_ranking;
    // End of variables declaration//GEN-END:variables
}
