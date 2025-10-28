/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Kategori;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import testsqlite.DatabaseHelper;

/**
 * KategoriDAO: CRUD untuk tabel data_kategori
 * - memastikan tabel dibuat jika belum ada
 */
public class KategoriDAO {

    public KategoriDAO() throws SQLException {
        ensureTable();
    }

    private void ensureTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS data_kategori ("
                + "id_kategori VARCHAR(10) PRIMARY KEY, "
                + "nama_kategori VARCHAR(50) NOT NULL)";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /**
     * Ambil semua kategori (ordered by id)
     */
    public List<Kategori> findAll() throws SQLException {
        ensureTable();
        List<Kategori> list = new ArrayList<>();
        String sql = "SELECT id_kategori, nama_kategori FROM data_kategori ORDER BY id_kategori";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Kategori k = new Kategori();
                k.setIdKategori(rs.getString("id_kategori"));
                k.setNamaKategori(rs.getString("nama_kategori"));
                list.add(k);
            }
        }
        return list;
    }

    /**
     * Cari berdasarkan id_kategori
     */
    public Kategori findById(String idKategori) throws SQLException {
        ensureTable();
        String sql = "SELECT id_kategori, nama_kategori FROM data_kategori WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idKategori);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Kategori(rs.getString("id_kategori"), rs.getString("nama_kategori"));
                }
            }
        }
        return null;
    }
    
    //cari berdasarkan nama 
       public Kategori findByName(String namaKategori) throws SQLException {
        ensureTable();
        String sql = "SELECT id_kategori, nama_kategori FROM data_kategori WHERE nama_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaKategori);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Kategori(rs.getString("id_kategori"), rs.getString("nama_kategori"));
                }
            }
        }
        return null;
    }

    /**
     * Insert kategori baru. Throws SQLException jika PK duplicate atau error lain.
     */
    public void insert(Kategori k) throws SQLException {
        ensureTable();
        String sql = "INSERT INTO data_kategori (id_kategori, nama_kategori) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, k.getIdKategori());
            ps.setString(2, k.getNamaKategori());
            ps.executeUpdate();
        }
    }

    /**
     * Update nama kategori (berdasarkan id_kategori).
     */
    public void update(Kategori k) throws SQLException {
        ensureTable();
        String sql = "UPDATE data_kategori SET nama_kategori = ? WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, k.getNamaKategori());
            ps.setString(2, k.getIdKategori());
            ps.executeUpdate();
        }
    }

    /**
     * Delete kategori berdasarkan id_kategori.
     * Jika ada foreign key (mis. barang referensi), akan melempar SQLException.
     */
    public void delete(String idKategori) throws SQLException {
        ensureTable();
        String sql = "DELETE FROM data_kategori WHERE id_kategori = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idKategori);
            ps.executeUpdate();
        }
    }
}
