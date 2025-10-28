/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import testsqlite.DatabaseHelper;

public class SupplierDAO {
    public List<Supplier> findAll() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT id_supplier, nama_supplier, alamat_supplier, notelp_supplier FROM data_supplier ORDER BY id_supplier";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Supplier s = new Supplier();
                s.setIdSupplier(rs.getInt("id_supplier"));
                s.setNamaSupplier(rs.getString("nama_supplier"));
                s.setAlamatSupplier(rs.getString("alamat_supplier"));
                s.setNotelpSupplier(rs.getString("notelp_supplier"));
                list.add(s);
            }
        }
        return list;
    }

    public Supplier findById(int id) throws SQLException {
        String sql = "SELECT id_supplier, nama_supplier, alamat_supplier, notelp_supplier FROM data_supplier WHERE id_supplier = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Supplier(rs.getInt("id_supplier"), rs.getString("nama_supplier"),
                            rs.getString("alamat_supplier"), rs.getString("notelp_supplier"));
                }
            }
        }
        return null;
    }

    public int insert(Supplier s) throws SQLException {
        String sql = "INSERT INTO data_supplier (nama_supplier, alamat_supplier, notelp_supplier) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getNamaSupplier());
            ps.setString(2, s.getAlamatSupplier());
            ps.setString(3, s.getNotelpSupplier());
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) {
                    int id = g.getInt(1);
                    s.setIdSupplier(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void update(Supplier s) throws SQLException {
        String sql = "UPDATE data_supplier SET nama_supplier=?, alamat_supplier=?, notelp_supplier=? WHERE id_supplier=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNamaSupplier());
            ps.setString(2, s.getAlamatSupplier());
            ps.setString(3, s.getNotelpSupplier());
            ps.setInt(4, s.getIdSupplier());
            ps.executeUpdate();
        }
    }

    public void delete(int idSupplier) throws SQLException {
        String sql = "DELETE FROM data_supplier WHERE id_supplier = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSupplier);
            ps.executeUpdate();
        }
    }
}
