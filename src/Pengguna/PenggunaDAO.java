/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Pengguna;

/**
 *
 * @author COMPUTER
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import testsqlite.DatabaseHelper;

public class PenggunaDAO {

    public PenggunaDAO() throws SQLException {
        ensureTable();
    }

    private void ensureTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS data_pengguna ("
                + "id_pengguna TEXT PRIMARY KEY, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password TEXT, "
                + "alamat TEXT, "
                + "jabatan TEXT, "
                + "nama_lengkap TEXT, "
                + "hak_akses INTEGER DEFAULT 0, "
                + "email TEXT, "
                + "notelp_pengguna TEXT"
                + ");";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement st = conn.createStatement()) {
            try { st.execute("PRAGMA foreign_keys = ON;"); } catch (Throwable ignore) {}
            st.execute(sql);
        }
    }
    
    // inside PenggunaDAO class
public Pengguna findByUsername(String username) throws SQLException {
    String sql = "SELECT id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna FROM data_pengguna WHERE username = ?";
    try (Connection conn = DatabaseHelper.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Pengguna p = new Pengguna();
                p.setIdPengguna(rs.getString("id_pengguna"));
                p.setUsername(rs.getString("username"));
                p.setPassword(rs.getString("password"));
                p.setAlamat(rs.getString("alamat"));
                p.setJabatan(rs.getString("jabatan"));
                p.setNamaLengkap(rs.getString("nama_lengkap"));
                int ha = rs.getInt("hak_akses"); p.setHakAkses(rs.wasNull() ? null : ha);
                p.setEmail(rs.getString("email"));
                p.setNotelpPengguna(rs.getString("notelp_pengguna"));
                return p;
            }
        }
    }
    return null;
}


    private String generateNewIdPengguna(Connection conn) throws SQLException {
        // pattern: USR001, USR002, ...
        String todayLike = "USR%";
        String sql = "SELECT id_pengguna FROM data_pengguna WHERE id_pengguna LIKE ? ORDER BY id_pengguna DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, todayLike);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String last = rs.getString(1); // e.g. "USR012"
                    if (last != null && last.length() > 3) {
                        String seq = last.substring(3);
                        try {
                            int num = Integer.parseInt(seq);
                            return "USR" + String.format("%03d", num + 1);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return "USR" + String.format("%03d", 1);
    }

    public String insert(Pengguna p) throws SQLException {
        if (p == null) throw new IllegalArgumentException("Pengguna tidak boleh null");

        String insert = "INSERT INTO data_pengguna (id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // generate id on same connection to avoid race
                if (p.getIdPengguna() == null || p.getIdPengguna().trim().isEmpty()) {
                    p.setIdPengguna(generateNewIdPengguna(conn));
                }

                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, p.getIdPengguna());
                    ps.setString(2, p.getUsername());
                    ps.setString(3, p.getPassword());
                    ps.setString(4, p.getAlamat());
                    ps.setString(5, p.getJabatan());
                    ps.setString(6, p.getNamaLengkap());
                    if (p.getHakAkses() == null) ps.setNull(7, Types.INTEGER);
                    else ps.setInt(7, p.getHakAkses());
                    ps.setString(8, p.getEmail());
                    ps.setString(9, p.getNotelpPengguna());
                    ps.executeUpdate();
                }

                conn.commit();
                return p.getIdPengguna();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (Throwable t) {}
                throw ex;
            } finally {
                try { conn.setAutoCommit(true); } catch (Throwable ignore) {}
            }
        }
    }

    public void update(Pengguna p) throws SQLException {
        if (p == null || p.getIdPengguna() == null) throw new IllegalArgumentException("Pengguna atau id kosong");
        String sql = "UPDATE data_pengguna SET username=?, password=?, alamat=?, jabatan=?, nama_lengkap=?, hak_akses=?, email=?, notelp_pengguna=? WHERE id_pengguna=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getUsername());
            ps.setString(2, p.getPassword());
            ps.setString(3, p.getAlamat());
            ps.setString(4, p.getJabatan());
            ps.setString(5, p.getNamaLengkap());
            if (p.getHakAkses() == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, p.getHakAkses());
            ps.setString(7, p.getEmail());
            ps.setString(8, p.getNotelpPengguna());
            ps.setString(9, p.getIdPengguna());
            ps.executeUpdate();
        }
    }

    public void delete(String idPengguna) throws SQLException {
        String sql = "DELETE FROM data_pengguna WHERE id_pengguna=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPengguna);
            ps.executeUpdate();
        }
    }

    public Pengguna findById(String idPengguna) throws SQLException {
        String sql = "SELECT id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna FROM data_pengguna WHERE id_pengguna=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPengguna);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pengguna p = new Pengguna();
                    p.setIdPengguna(rs.getString("id_pengguna"));
                    p.setUsername(rs.getString("username"));
                    p.setPassword(rs.getString("password"));
                    p.setAlamat(rs.getString("alamat"));
                    p.setJabatan(rs.getString("jabatan"));
                    p.setNamaLengkap(rs.getString("nama_lengkap"));
                    int ha = rs.getInt("hak_akses"); p.setHakAkses(rs.wasNull() ? null : ha);
                    p.setEmail(rs.getString("email"));
                    p.setNotelpPengguna(rs.getString("notelp_pengguna"));
                    return p;
                }
            }
        }
        return null;
    }

    public List<Pengguna> findAll() throws SQLException {
        List<Pengguna> out = new ArrayList<>();
        String sql = "SELECT id_pengguna, username, password, alamat, jabatan, nama_lengkap, hak_akses, email, notelp_pengguna FROM data_pengguna ORDER BY id_pengguna";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pengguna p = new Pengguna();
                p.setIdPengguna(rs.getString("id_pengguna"));
                p.setUsername(rs.getString("username"));
                p.setPassword(rs.getString("password"));
                p.setAlamat(rs.getString("alamat"));
                p.setJabatan(rs.getString("jabatan"));
                p.setNamaLengkap(rs.getString("nama_lengkap"));
                int ha = rs.getInt("hak_akses"); p.setHakAkses(rs.wasNull() ? null : ha);
                p.setEmail(rs.getString("email"));
                p.setNotelpPengguna(rs.getString("notelp_pengguna"));
                out.add(p);
            }
        }
        return out;
    }
}
