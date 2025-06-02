/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package mainMenu;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.swing.table.DefaultTableCellRenderer;
import koneksi.koneksi;
import model.KriteriaModel;
import controller.KriteriaController;
import static java.lang.String.format;

/**
 *
 * @author richard
 */
public class kriteria extends javax.swing.JPanel {
    
    private Connection conn;
    private double[] priority;
    private JTextField[][] inputFields = new JTextField[4][4];
    private double[][] pairwiseComparisonMatrix;
    private double[][] normalizedMatrix;
    private double[] priorityVector;
    private double lambdaMax;
    private double ci;
    private double cr;
    private int n;
    private PreparedStatement pstmt;
    private String query;
    private ResultSet resultSet;
    
    
    private int idKriteria;
    private String namaKriteria;
    private double bobotKriteria;
    
    private KriteriaController kri; 
    private JFrame parent;
    private final double[] RI_TABLE = {0.0, 0.0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49};
    
    
    
    private KriteriaModel getterKriteria;
    private void showError(String message) {
        System.err.println(message); 
    }
    private void closeStatement() {
        try {
            if(pstmt != null){
                pstmt.close();
                pstmt = null;
            }
            if(resultSet != null){
                resultSet.close();
                resultSet = null;
            }   
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }
    
    
    private void updateDataBobot(double[] priority) {
        if (priority == null || priority.length == 0) {
            
            return;
        }
        List<KriteriaModel> kriteriaList = kri.getAllKriteria();
        if (kriteriaList != null && kriteriaList.size() == priority.length) {
            boolean success = true;
            for (int i = 0; i < priority.length; i++) {
                int idKriteria = kriteriaList.get(i).getIdKriteria();
                
                double bobotBaru = Double.parseDouble(String.format("%.3f", priority[i]));
                if (!kri.updateBobotKriteria(idKriteria, bobotBaru)) {
                    success = false;
                    break;
                }
            }
            if (success) {
                resetForm();
                tampilData();
                panelView();
            } else {
                JOptionPane.showMessageDialog(this, "Gagal mengupdate salah satu bobot kriteria.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Jumlah kriteria tidak sesuai.");
        }
    }
    
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
            showError("Gagal mengambil data karyawan: " + e.getMessage());
            return null;
        }
        return kriteriaList;
    }
 
    
    private void tampilkanRankingKriteria(String[] namaKriteria) {
        // Buat model data
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Kriteria");
        model.addColumn("bobotBaru");

        // Gabungkan nama kriteria dan bobotnya
        java.util.List<Object[]> data = new ArrayList<>();
        for (int i = 0; i < priorityVector.length; i++) {
            data.add(new Object[]{namaKriteria[i], priorityVector[i]});
        }

        // Urutkan dari prioritas tertinggi ke terendah
        data.sort((a, b) -> Double.compare((Double) b[1], (Double) a[1]));
        
        

        // Tambahkan ke model tabel
        for (Object[] row : data) {
            model.addRow(row);
        }

        // Buat JTable dan tampilkan di panel
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        rankKriteria.removeAll(); // bersihkan panel
        rankKriteria.setLayout(new java.awt.BorderLayout());
        rankKriteria.add(scrollPane, java.awt.BorderLayout.CENTER);
        rankKriteria.revalidate();
        rankKriteria.repaint();
    }

    
    public void setPairwiseComparisonMatrix(double[][] matrix) {
        this.pairwiseComparisonMatrix = matrix;
        this.n = matrix.length;
    }
   
    
    
    
    public void calculatePriorityVector() {
        // 1. Jumlah tiap kolom
        double[] columnSums = new double[n];
        for (int j = 0; j < n; j++) {
            columnSums[j] = 0.0;
            for (int i = 0; i < n; i++) {
                columnSums[j] += pairwiseComparisonMatrix[i][j];
            }
        }
        
        // 2. Normalisasi matriks perbandingan berpasangan
        normalizedMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                normalizedMatrix[i][j] = pairwiseComparisonMatrix[i][j] / columnSums[j];
            }
        }
        
        // 3. Hitung priority vector (rata-rata baris)
        priorityVector = new double[n];
        for (int i = 0; i < n; i++) {
            double rowSum = 0.0;
            for (int j = 0; j < n; j++) {
                rowSum += normalizedMatrix[i][j];
            }
            priorityVector[i] = rowSum / n;
        }
        
        // 4. Hitung lambda max dengan lebih presisi
        double[] weightedSum = new double[n];
        for (int i = 0; i < n; i++) {
            weightedSum[i] = 0.0;
            for (int j = 0; j < n; j++) {
                weightedSum[i] += pairwiseComparisonMatrix[i][j] * priorityVector[j];
            }
        }
        
//        double[] consistencyVector = new double[n];
//        for (int i = 0; i < n; i++) {
//            consistencyVector[i] = weightedSum[i] / priorityVector[i];
//        }
        
        lambdaMax = 0.0;
        for (int i = 0; i < n; i++) {
            lambdaMax += weightedSum[i];
        }
//        lambdaMax /= n;
        
        // 5. Hitung CI dengan presisi yang lebih tinggi
        ci = (lambdaMax - n) / (n - 1);
        
        // 6. Hitung CR dengan presisi yang lebih tinggi
        double ri = getRIValue(n);
        cr = ci / ri;
    }
    
    private double getRIValue(int size) {
        if (size <= 0) {
            return 0.0;
        }
        if (size <= RI_TABLE.length) {
            return RI_TABLE[size - 1];
        }
        return RI_TABLE[RI_TABLE.length - 1]; // Fallback to the largest value if size exceeds table
    }
    
    // Jumlah tiap kolom dari matriks perbandingan
    public double[] getColumnSums() {
        double[] sums = new double[n];
        for (int j = 0; j < n; j++) {
            sums[j] = 0.0;
            for (int i = 0; i < n; i++) {
                sums[j] += pairwiseComparisonMatrix[i][j];
            }
        }
        return sums;
    }
    
    // Matriks nilai kriteria (normalisasi)
    public double[][] getNormalizedMatrix() {
        return normalizedMatrix;
    }
    
    // Vektor prioritas (bobot)
    public double[] getPriorityVector() {
        return priorityVector;
    }
    
    // Jumlah tiap baris dari matriks nilai kriteria (harus selalu 1.000)
    public double[] getRowSums() {
        double[] sums = new double[n];
        for (int i = 0; i < n; i++) {
            sums[i] = 0.0;
            for (int j = 0; j < n; j++) {
                sums[i] += normalizedMatrix[i][j];
            }
        }
        return sums;
    }
    
    // Jumlah tiap kolom dari matriks nilai kriteria (untuk pemeriksaan, sumnya harus 1.0)
    public double[] getCriteriaColumnSum() {
        double[] sums = new double[n];
        for (int j = 0; j < n; j++) {
            sums[j] = 0.0;
            for (int i = 0; i < n; i++) {
                sums[j] += normalizedMatrix[i][j];
            }
        }
        return sums;
    }
    
    public double getLambdaMaxValue() {
        return lambdaMax;
    }
    
    public double getCIValue() {
        return ci;
    }
    
    public double getCRValue() {
        return cr;
    }
    
    // Ambil semua kriteria


    /**
     * Creates new form kriteria
     */
    public kriteria() {
        initComponents();
        
        KriteriaModel getterKriteria = new KriteriaModel();
        tampilData();
        resetForm();
        panelView();
        
        
        
        DefaultTableCellRenderer head_render = new DefaultTableCellRenderer();
        head_render.setBackground(new Color(0,102,0)); // Warna latar belakang header
        head_render.setForeground(Color.WHITE); // Warna teks header putih

        tbl_kriteria.getTableHeader().setDefaultRenderer(head_render);
        tbl_kriteria.getTableHeader().setOpaque(false);
        tbl_kriteria.setRowHeight(25);
        
        
    }
    
    private void resetForm() {
        for (int i = 0; i < inputFields.length; i++) {
            for (int j = 0; j < inputFields[i].length; j++) {
                if (inputFields[i][j] != null) {
                    inputFields[i][j].setText("");
                }
            }
        }
        priorityVector = null;
        normalizedMatrix = null;
        pairwiseComparisonMatrix = null;
    }
    
    private void panelView() {
        matriksKriteria.setVisible(false);
        rankKriteria.setVisible(true);
    }
    
    private void tampilData() {
        
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID kriteria");
        model.addColumn("Nama Kriteria");
        model.addColumn("Bobot Baru");

        try {
            String sql = "SELECT * FROM tb_kriteria";
            Connection conn = koneksi.getKoneksi();
            Statement stm = conn.createStatement();
            ResultSet res = stm.executeQuery(sql);
            
            while (res.next()) {
                model.addRow(new Object[]{
                    
                    res.getInt("idKriteria"),
                    res.getString("namaKriteria"),
                    String.format("%.3f", res.getDouble("bobotKriteria"))
                });
            }

            tbl_kriteria.setModel(model);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal menampilkan data: " + e.toString());
        }
    }
    
    private void setupFields() {
        // Input dari atas diagonal (segitiga atas)
        inputFields[0][1] = k1k2;
        inputFields[0][2] = k1k3;
        inputFields[0][3] = k1k4;
        inputFields[1][2] = k2k3;
        inputFields[1][3] = k2k4;
        inputFields[2][3] = k3k4;
    }
    
    public double[][] getComparisonMatrix() {
        int n = 4;
        double[][] matrix = new double[n][n];
        
        // Diagonal = 1 (selalu)
        for (int i = 0; i < n; i++) {
            matrix[i][i] = 1.0;
        }
        
        // Input dari atas diagonal (segitiga atas)
        try {
            matrix[0][1] = Double.parseDouble(k1k2.getText());
            matrix[0][2] = Double.parseDouble(k1k3.getText());
            matrix[0][3] = Double.parseDouble(k1k4.getText());
            matrix[1][2] = Double.parseDouble(k2k3.getText());
            matrix[1][3] = Double.parseDouble(k2k4.getText());
            matrix[2][3] = Double.parseDouble(k3k4.getText());
            
            // Segitiga bawah (kebalikan dari atas)
            matrix[1][0] = 1.0 / matrix[0][1];
            matrix[2][0] = 1.0 / matrix[0][2];
            matrix[3][0] = 1.0 / matrix[0][3];
            matrix[2][1] = 1.0 / matrix[1][2];
            matrix[3][1] = 1.0 / matrix[1][3];
            matrix[3][2] = 1.0 / matrix[2][3];
        } catch (NumberFormatException e) {
         
            return null;
        }
        return matrix;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        main_panel = new javax.swing.JPanel();
        rankKriteria = new javax.swing.JPanel();
        panelCustom4 = new custom.panelCustom();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_kriteria = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        matriksKriteria = new javax.swing.JPanel();
        panelCustom1 = new custom.panelCustom();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        k1k1 = new javax.swing.JTextField();
        k2k1 = new javax.swing.JTextField();
        k3k1 = new javax.swing.JTextField();
        k4k1 = new javax.swing.JTextField();
        k1k2 = new javax.swing.JTextField();
        k2k2 = new javax.swing.JTextField();
        k3k2 = new javax.swing.JTextField();
        k4k2 = new javax.swing.JTextField();
        k1k4 = new javax.swing.JTextField();
        k2k4 = new javax.swing.JTextField();
        k3k4 = new javax.swing.JTextField();
        k4k4 = new javax.swing.JTextField();
        k4k3 = new javax.swing.JTextField();
        k3k3 = new javax.swing.JTextField();
        k2k3 = new javax.swing.JTextField();
        k1k3 = new javax.swing.JTextField();
        jumk2 = new javax.swing.JTextField();
        jumk1 = new javax.swing.JTextField();
        jumk3 = new javax.swing.JTextField();
        jumk4 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        k1n4 = new javax.swing.JTextField();
        k3n3 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        k3n2 = new javax.swing.JTextField();
        k4n1 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        k1n3 = new javax.swing.JTextField();
        k4n3 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        k3n1 = new javax.swing.JTextField();
        k2n4 = new javax.swing.JTextField();
        k4n4 = new javax.swing.JTextField();
        k3n4 = new javax.swing.JTextField();
        k4n2 = new javax.swing.JTextField();
        k1n2 = new javax.swing.JTextField();
        k2n3 = new javax.swing.JTextField();
        k2n2 = new javax.swing.JTextField();
        k1n1 = new javax.swing.JTextField();
        k2n1 = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jumn1 = new javax.swing.JTextField();
        bobotn1 = new javax.swing.JTextField();
        jumn2 = new javax.swing.JTextField();
        bobotn2 = new javax.swing.JTextField();
        bobotn3 = new javax.swing.JTextField();
        jumn3 = new javax.swing.JTextField();
        jumn4 = new javax.swing.JTextField();
        bobotn4 = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        nilaici = new javax.swing.JTextField();
        lambdamax = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        nilaicr = new javax.swing.JTextField();
        validcr = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        btnHitung = new javax.swing.JButton();
        btnSimpan = new javax.swing.JButton();
        btnBatal = new javax.swing.JButton();

        setLayout(new java.awt.CardLayout());

        main_panel.setLayout(new java.awt.CardLayout());

        rankKriteria.setBackground(new java.awt.Color(0, 102, 0));
        rankKriteria.setLayout(new java.awt.CardLayout());

        panelCustom4.setBackground(new java.awt.Color(255, 255, 255));
        panelCustom4.setRoundBottomLeft(50);
        panelCustom4.setRoundBottomRight(50);
        panelCustom4.setRoundTopLeft(50);
        panelCustom4.setRoundTopRight(50);

        tbl_kriteria.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tbl_kriteria);

        jButton1.setText("Matriks");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        btnHapus.setText("Hapus");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCustom4Layout = new javax.swing.GroupLayout(panelCustom4);
        panelCustom4.setLayout(panelCustom4Layout);
        panelCustom4Layout.setHorizontalGroup(
            panelCustom4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCustom4Layout.createSequentialGroup()
                .addGap(124, 124, 124)
                .addGroup(panelCustom4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnHapus)
                    .addComponent(jButton1)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1288, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelCustom4Layout.setVerticalGroup(
            panelCustom4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCustom4Layout.createSequentialGroup()
                .addGap(84, 84, 84)
                .addComponent(jButton1)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 569, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnHapus)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        rankKriteria.add(panelCustom4, "card2");

        main_panel.add(rankKriteria, "card3");

        matriksKriteria.setBackground(new java.awt.Color(0, 102, 0));
        matriksKriteria.setLayout(new java.awt.CardLayout());

        panelCustom1.setBackground(new java.awt.Color(255, 255, 255));
        panelCustom1.setRoundBottomLeft(50);
        panelCustom1.setRoundBottomRight(50);
        panelCustom1.setRoundTopLeft(50);
        panelCustom1.setRoundTopRight(50);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("K1");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("K2");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("K3");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("K4");

        k2k1.setEnabled(false);

        k3k1.setEnabled(false);

        k4k1.setEnabled(false);

        k3k2.setEnabled(false);

        k4k2.setEnabled(false);

        k4k3.setEnabled(false);

        jumk2.setEnabled(false);

        jumk1.setEnabled(false);

        jumk3.setEnabled(false);

        jumk4.setEnabled(false);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Jumlah");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("K1");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("K2");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("K3");

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("K4");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel10.setText("Matriks Perbandingan Berpasangan");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(79, 79, 79)
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(79, 79, 79)
                                    .addComponent(k3k1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(k3k2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                                        .addComponent(jLabel2)
                                                        .addGap(18, 18, 18)
                                                        .addComponent(k2k1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                                        .addComponent(jLabel1)
                                                        .addGap(18, 18, 18)
                                                        .addComponent(k1k1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                    .addComponent(jLabel5)
                                                    .addGap(18, 18, 18)
                                                    .addComponent(jumk1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel4)
                                                .addComponent(jLabel3))
                                            .addGap(18, 18, 18)
                                            .addComponent(k4k1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(12, 12, 12)))
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(k2k2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(k1k2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(k4k2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jumk2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jumk3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jumk4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(k2k3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(k1k3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(k4k3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(k3k3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(k2k4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(k1k4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(k4k4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(k3k4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(74, 74, 74)
                        .addComponent(jLabel10)))
                .addContainerGap(112, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addComponent(jLabel10)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(jLabel9)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(k2k3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(k2k4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(k1k1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k1k2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k1k3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k1k4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(k2k1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k2k2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(k3k3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(k3k4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(k3k2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(k3k1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(k4k2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k4k1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(k4k4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(k4k3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jumk2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jumk1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jumk3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jumk4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("K2");

        k1n4.setEnabled(false);

        k3n3.setEnabled(false);

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("K1");

        k3n2.setEnabled(false);

        k4n1.setEnabled(false);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("K4");

        k1n3.setEnabled(false);

        k4n3.setEnabled(false);

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel13.setText("K3");

        k3n1.setEnabled(false);

        k2n4.setEnabled(false);

        k4n4.setEnabled(false);

        k3n4.setEnabled(false);

        k4n2.setEnabled(false);

        k1n2.setEnabled(false);

        k2n3.setEnabled(false);

        k2n2.setEnabled(false);

        k1n1.setEnabled(false);

        k2n1.setEnabled(false);

        jLabel15.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel15.setText("Matriks Nilai Kriteria");

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("Bobot");

        jLabel17.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("Jumlah");

        jumn1.setEnabled(false);

        bobotn1.setEnabled(false);

        jumn2.setEnabled(false);

        bobotn2.setEnabled(false);

        bobotn3.setEnabled(false);

        jumn3.setEnabled(false);

        jumn4.setEnabled(false);

        bobotn4.setEnabled(false);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(109, 109, 109)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(k1n1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k1n2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k1n3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k1n4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(k4n1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(k4n2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(k4n3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k4n4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(k2n1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k2n2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k2n3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k2n4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(k3n1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k3n2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k3n3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(k3n4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(41, 41, 41)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jumn1, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                    .addComponent(jumn2, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                    .addComponent(jumn3, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                    .addComponent(jumn4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bobotn1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE)
                    .addComponent(bobotn4)
                    .addComponent(bobotn3)
                    .addComponent(bobotn2))
                .addGap(62, 62, 62))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(202, 202, 202)
                .addComponent(jLabel15)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel15)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(jLabel16))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jumn1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bobotn1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jumn2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bobotn2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jumn3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bobotn3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(bobotn4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jumn4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(jLabel12)
                            .addComponent(jLabel13)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(k1n1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k1n2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k1n3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k1n4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(k2n3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(k2n4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(k2n1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(k2n2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(k3n3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k3n4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k3n2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k3n1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(k4n2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(k4n1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(k4n4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(k4n3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel18.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel18.setText("Hasil Perhitungan");

        jLabel19.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel19.setText("Lambda Max");

        nilaici.setEnabled(false);

        lambdamax.setEnabled(false);

        jLabel20.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel20.setText("Consistency Indeks (CI)");

        jLabel21.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel21.setText("Consistency Rasio (CR)");

        nilaicr.setEnabled(false);

        validcr.setEnabled(false);

        jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel22.setText("CR < 0.1");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(352, 352, 352)
                .addComponent(jLabel18)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(48, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel20)
                    .addComponent(jLabel19))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lambdamax)
                    .addComponent(nilaici, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(76, 76, 76)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel22)
                    .addComponent(jLabel21))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(nilaicr)
                    .addComponent(validcr, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(42, 42, 42))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel18)
                .addGap(38, 38, 38)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(lambdamax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel20)
                            .addComponent(nilaici, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel21)
                            .addComponent(nilaicr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(validcr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(101, Short.MAX_VALUE))
        );

        btnHitung.setText("Hitung");
        btnHitung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHitungActionPerformed(evt);
            }
        });

        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSimpanActionPerformed(evt);
            }
        });

        btnBatal.setText("Batal");
        btnBatal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBatalActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCustom1Layout = new javax.swing.GroupLayout(panelCustom1);
        panelCustom1.setLayout(panelCustom1Layout);
        panelCustom1Layout.setHorizontalGroup(
            panelCustom1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCustom1Layout.createSequentialGroup()
                .addGroup(panelCustom1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelCustom1Layout.createSequentialGroup()
                        .addGap(127, 127, 127)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCustom1Layout.createSequentialGroup()
                        .addGap(184, 184, 184)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelCustom1Layout.createSequentialGroup()
                        .addGap(500, 500, 500)
                        .addComponent(btnHitung)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSimpan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBatal)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelCustom1Layout.setVerticalGroup(
            panelCustom1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCustom1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCustom1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelCustom1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnHitung)
                    .addComponent(btnSimpan)
                    .addComponent(btnBatal))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        matriksKriteria.add(panelCustom1, "card2");

        main_panel.add(matriksKriteria, "card2");

        add(main_panel, "card2");
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        main_panel.removeAll();
        main_panel.repaint();
        main_panel.revalidate();

        main_panel.add(matriksKriteria);
        main_panel.repaint();
        main_panel.revalidate();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void btnHitungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHitungActionPerformed
        // TODO add your handling code here:
        try {
            // 1. Ambil nilai dari field input
            double[][] matrix = getComparisonMatrix();
            if (matrix == null) return;

            // 2. Hitung AHP
            kriteria ahp = new kriteria();
            ahp.setPairwiseComparisonMatrix(matrix);
            ahp.calculatePriorityVector();

            // 3. Ambil hasil perhitungan
            // Menyimpan sementara matriks untuk memudahkan penghitungan
            double[][] normMatrix = ahp.getNormalizedMatrix();
            double[] columnSums = ahp.getColumnSums();
            double[] rowSums = ahp.getRowSums();
            priority = ahp.getPriorityVector();
            double lambda = ahp.getLambdaMaxValue();
            double ci = ahp.getCIValue();
            double cr = ahp.getCRValue();
            
            

            // 4. Tampilkan Jumlah Kolom dari Matriks Perbandingan
            jumk1.setText(String.format("%.3f", columnSums[0]));
            jumk2.setText(String.format("%.3f", columnSums[1]));
            jumk3.setText(String.format("%.3f", columnSums[2]));
            jumk4.setText(String.format("%.3f", columnSums[3]));

            // 5. Tampilkan segitiga bawah matriks perbandingan (hasil dari 1/nilai)
            k2k1.setText(String.format("%.3f", matrix[1][0]));
            k3k1.setText(String.format("%.3f", matrix[2][0]));
            k3k2.setText(String.format("%.3f", matrix[2][1]));
            k4k1.setText(String.format("%.3f", matrix[3][0]));
            k4k2.setText(String.format("%.3f", matrix[3][1]));
            k4k3.setText(String.format("%.3f", matrix[3][2]));

            // 6. Tampilkan matriks nilai kriteria (hasil normalisasi)
            k1n1.setText(String.format("%.3f", normMatrix[0][0]));
            k1n2.setText(String.format("%.3f", normMatrix[0][1]));
            k1n3.setText(String.format("%.3f", normMatrix[0][2]));
            k1n4.setText(String.format("%.3f", normMatrix[0][3]));

            k2n1.setText(String.format("%.3f", normMatrix[1][0]));
            k2n2.setText(String.format("%.3f", normMatrix[1][1]));
            k2n3.setText(String.format("%.3f", normMatrix[1][2]));
            k2n4.setText(String.format("%.3f", normMatrix[1][3]));

            k3n1.setText(String.format("%.3f", normMatrix[2][0]));
            k3n2.setText(String.format("%.3f", normMatrix[2][1]));
            k3n3.setText(String.format("%.3f", normMatrix[2][2]));
            k3n4.setText(String.format("%.3f", normMatrix[2][3]));

            k4n1.setText(String.format("%.3f", normMatrix[3][0]));
            k4n2.setText(String.format("%.3f", normMatrix[3][1]));
            k4n3.setText(String.format("%.3f", normMatrix[3][2]));
            k4n4.setText(String.format("%.3f", normMatrix[3][3]));

            // 7. Tampilkan jumlah baris dari matriks nilai kriteria
            jumn1.setText(String.format("%.3f", rowSums[0]));
            jumn2.setText(String.format("%.3f", rowSums[1]));
            jumn3.setText(String.format("%.3f", rowSums[2]));
            jumn4.setText(String.format("%.3f", rowSums[3]));

            // 8. Tampilkan bobot prioritas (Rata-rata baris)
            
            bobotn1.setText(String.format("%.3f", priority[0]));
            bobotn2.setText(String.format("%.3f", priority[1]));
            bobotn3.setText(String.format("%.3f", priority[2]));
            bobotn4.setText(String.format("%.3f", priority[3]));

            // 9. Tampilkan hasil perhitungan lambda, CI, CR
            lambdamax.setText(String.format("%.3f", lambda));
            nilaici.setText(String.format("%.3f", ci));
            nilaicr.setText(String.format("%.3f", cr));

            // 10. Tampilkan validitas CR
            validcr.setText(cr <= 0.1 ? "Konsisten" : "Tidak Konsisten");
            
            
            System.out.println("idKriteria : " +idKriteria);
        } catch (NumberFormatException ex) {

        } catch (Exception e) {
            
        }
        
    }//GEN-LAST:event_btnHitungActionPerformed

    private void btnBatalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBatalActionPerformed
        // TODO add your handling code here:
        main_panel.removeAll();
        main_panel.repaint();
        main_panel.revalidate();

        main_panel.add(rankKriteria);
        main_panel.repaint();
        main_panel.revalidate();
    }//GEN-LAST:event_btnBatalActionPerformed

    private void btnSimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanActionPerformed
        // TODO add your handling code here:
            Connection conn = koneksi.getKoneksi();
            String sql = "UPDATE tb_kriteria SET bobotKriteria = ? WHERE idKriteria = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 4; i++) {
                    stmt.setDouble(1, priority[i]); // bobot dari AHP
                    stmt.setInt(2, i + 1); // asumsikan idKriteria adalah 1, 2, 3, 4
                    stmt.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Semua bobot berhasil disimpan.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat menyimpan data: " + e.getMessage());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Format input tidak valid: " + e.getMessage());
            }

            tampilData();

            main_panel.removeAll();
            main_panel.repaint();
            main_panel.revalidate();
            main_panel.add(rankKriteria);
            main_panel.repaint();
            main_panel.revalidate();

        
    }//GEN-LAST:event_btnSimpanActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        // TODO add your handling code here:
        int konfirmasi = JOptionPane.showConfirmDialog(this,
            "Apakah Anda yakin ingin menghapus semua nilai bobot?",
            "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION);

    if (konfirmasi == JOptionPane.YES_OPTION) {
        String sql = "UPDATE tb_kriteria SET bobotKriteria = NULL";
        try (Connection conn = koneksi.getKoneksi();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Nilai bobot berhasil dihapus.");
                // Kosongkan tampilan di form jika diperlukan:
                bobotn1.setText("");
                bobotn2.setText("");
                bobotn3.setText("");
                bobotn4.setText("");
                
                // Refresh tabel jika ada
                tampilData();
            } else {
                JOptionPane.showMessageDialog(this, "Tidak ada data yang diubah.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Terjadi kesalahan saat menghapus bobot: " + e.getMessage());
        }
    }
    }//GEN-LAST:event_btnHapusActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField bobotn1;
    private javax.swing.JTextField bobotn2;
    private javax.swing.JTextField bobotn3;
    private javax.swing.JTextField bobotn4;
    private javax.swing.JButton btnBatal;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnHitung;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jumk1;
    private javax.swing.JTextField jumk2;
    private javax.swing.JTextField jumk3;
    private javax.swing.JTextField jumk4;
    private javax.swing.JTextField jumn1;
    private javax.swing.JTextField jumn2;
    private javax.swing.JTextField jumn3;
    private javax.swing.JTextField jumn4;
    private javax.swing.JTextField k1k1;
    private javax.swing.JTextField k1k2;
    private javax.swing.JTextField k1k3;
    private javax.swing.JTextField k1k4;
    private javax.swing.JTextField k1n1;
    private javax.swing.JTextField k1n2;
    private javax.swing.JTextField k1n3;
    private javax.swing.JTextField k1n4;
    private javax.swing.JTextField k2k1;
    private javax.swing.JTextField k2k2;
    private javax.swing.JTextField k2k3;
    private javax.swing.JTextField k2k4;
    private javax.swing.JTextField k2n1;
    private javax.swing.JTextField k2n2;
    private javax.swing.JTextField k2n3;
    private javax.swing.JTextField k2n4;
    private javax.swing.JTextField k3k1;
    private javax.swing.JTextField k3k2;
    private javax.swing.JTextField k3k3;
    private javax.swing.JTextField k3k4;
    private javax.swing.JTextField k3n1;
    private javax.swing.JTextField k3n2;
    private javax.swing.JTextField k3n3;
    private javax.swing.JTextField k3n4;
    private javax.swing.JTextField k4k1;
    private javax.swing.JTextField k4k2;
    private javax.swing.JTextField k4k3;
    private javax.swing.JTextField k4k4;
    private javax.swing.JTextField k4n1;
    private javax.swing.JTextField k4n2;
    private javax.swing.JTextField k4n3;
    private javax.swing.JTextField k4n4;
    private javax.swing.JTextField lambdamax;
    private javax.swing.JPanel main_panel;
    private javax.swing.JPanel matriksKriteria;
    private javax.swing.JTextField nilaici;
    private javax.swing.JTextField nilaicr;
    private custom.panelCustom panelCustom1;
    private custom.panelCustom panelCustom4;
    private javax.swing.JPanel rankKriteria;
    private javax.swing.JTable tbl_kriteria;
    private javax.swing.JTextField validcr;
    // End of variables declaration//GEN-END:variables
}
