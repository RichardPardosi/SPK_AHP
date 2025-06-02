/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author richard
 */
public class produkModel {
    public int idProduk;
    public String namaProduk;
    public Double kualitas;
    public Double penjualan;
    public Double stok;
    public Double rating;

    public produkModel(int idProduk, String namaProduk, Double kualitas, Double penjualan, Double stok, Double rating) {
    this.idProduk = idProduk;
    this.namaProduk = namaProduk;
    this.kualitas = kualitas;
    this.penjualan = penjualan;
    this.stok = stok;
    this.rating = rating;
    
    }
    
    public int getIdProduk() {
        return idProduk;
    }

    public void setIdProduk(int idProduk) {
        this.idProduk = idProduk;
    }

    public String getNamaProduk() {
        return namaProduk;
    }

    public void setNamaProduk(String namaProduk) {
        this.namaProduk = namaProduk;
    }

    public Double getKualitas() {
        return kualitas;
    }

    public void setKualitas(Double kualitas) {
        this.kualitas = kualitas;
    }

    public Double getPenjualan() {
        return penjualan;
    }

    public void setPenjualan(Double penjualan) {
        this.penjualan = penjualan;
    }

    public Double getStok() {
        return stok;
    }

    public void setStok(Double stok) {
        this.stok = stok;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    
    
    
    
    
    
    
}
