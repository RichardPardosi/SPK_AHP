package model;




public class KriteriaModel {
    private int idKriteria;
    private String namaKriteria;
    private double bobotKriteria;
    
    public KriteriaModel() {
    }

    public KriteriaModel(int idKriteria, String namaKriteria, double bobotKriteria) {
        this.idKriteria = idKriteria;
        this.namaKriteria = namaKriteria;
        this.bobotKriteria = bobotKriteria;
    }

    public int getIdKriteria() {
        return idKriteria;
    }

    public void setIdKriteria(int idKriteria) {
        this.idKriteria = idKriteria;
    }

    public String getNama() {
        return namaKriteria;
    }

    public void setNama(String namaKriteria) {
        this.namaKriteria = namaKriteria;
    }

    public double getBobot() {
        return bobotKriteria;
    }

    public void setBobot(double bobotKriteria) {
        this.bobotKriteria = bobotKriteria;
    }
    private void showError(String message) {
        System.err.println(message); // Atau pakai JOptionPane.showMessageDialog(null, message);
    }
}

