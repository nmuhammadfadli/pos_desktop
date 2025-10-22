/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuruDAO {

    public GuruDAO() throws SQLException {
        ensureTable();
    }

    private void ensureTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS data_guru ("
                + "id_guru INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nama_guru TEXT UNIQUE NOT NULL, "
                + "notelp_guru TEXT DEFAULT '0', "
                + "jabatan TEXT DEFAULT '')";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public List<Guru> findAll() throws SQLException {
        ensureTable();
        List<Guru> list = new ArrayList<>();
        String sql = "SELECT id_guru, nama_guru, notelp_guru, jabatan FROM data_guru ORDER BY nama_guru";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Guru g = new Guru();
                g.setIdGuru(rs.getInt("id_guru"));
                g.setNamaGuru(rs.getString("nama_guru"));
                g.setNotelpGuru(rs.getString("notelp_guru"));
                g.setJabatan(rs.getString("jabatan"));
                list.add(g);
            }
        }
        return list;
    }

    public Guru findById(int id) throws SQLException {
        ensureTable();
        String sql = "SELECT id_guru, nama_guru,  notelp_guru, jabatan FROM data_guru WHERE id_guru = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Guru g = new Guru();
                    g.setIdGuru(rs.getInt("id_guru"));
                    g.setNamaGuru(rs.getString("nama_guru"));
                    g.setNotelpGuru(rs.getString("notelp_guru"));
                    g.setJabatan(rs.getString("jabatan"));
                    return g;
                }
            }
        }
        return null;
    }

    public Guru findByName(String namaGuru) throws SQLException {
        ensureTable();
        String sql = "SELECT id_guru, nama_guru, notelp_guru, jabatan FROM data_guru WHERE nama_guru = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaGuru);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Guru g = new Guru();
                    g.setIdGuru(rs.getInt("id_guru"));
                    g.setNamaGuru(rs.getString("nama_guru"));
                    g.setNotelpGuru(rs.getString("notelp_guru"));
                    g.setJabatan(rs.getString("jabatan"));
                    return g;
                }
            }
        }
        return null;
    }

    public int insert(Guru g) throws SQLException {
        ensureTable();
        String sql = "INSERT INTO data_guru (nama_guru, notelp_guru, jabatan) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getNamaGuru());
            if (g.getNotelpGuru() == null) ps.setNull(2, Types.VARCHAR); else ps.setString(2, g.getNotelpGuru());
            if (g.getJabatan() == null) ps.setNull(3, Types.VARCHAR); else ps.setString(3, g.getJabatan());

            int affected = ps.executeUpdate();
            if (affected == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    g.setIdGuru(newId);
                    return newId;
                }
            }
        }
        return -1;
    }

    public void update(Guru g) throws SQLException {
        ensureTable();
        String sql = "UPDATE data_guru SET nama_guru=?, notelp_guru=?, jabatan=? WHERE id_guru=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, g.getNamaGuru());
            if (g.getNotelpGuru() == null) ps.setNull(2, Types.VARCHAR); else ps.setString(2, g.getNotelpGuru());
            if (g.getJabatan() == null) ps.setNull(3, Types.VARCHAR); else ps.setString(3, g.getJabatan());
            ps.setInt(4, g.getIdGuru());
            ps.executeUpdate();
        }
    }

    public void delete(int idGuru) throws SQLException {
        ensureTable();
        String sql = "DELETE FROM data_guru WHERE id_guru = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idGuru);
            ps.executeUpdate();
        }
    }
}
