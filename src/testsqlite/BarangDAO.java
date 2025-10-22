package testsqlite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BarangDAO {
    public List<Barang> findAll() throws SQLException {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT b.id, b.nama, b.id_kategori, k.nama_kategori " +
                "FROM barang b LEFT JOIN data_kategori k ON b.id_kategori = k.id_kategori ORDER BY b.nama";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Barang b = new Barang();
                b.setId(rs.getInt("id"));
                b.setNama(rs.getString("nama"));
                b.setIdKategori(rs.getString("id_kategori"));
                b.setNamaKategori(rs.getString("nama_kategori"));
                list.add(b);
            }
        }
        return list;
    }

    public void insert(Barang b) throws SQLException {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO barang (nama, id_kategori) VALUES (?, ?)")) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getIdKategori());
            ps.executeUpdate();
        }
    }

    public void update(Barang b) throws SQLException {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE barang SET nama=?, id_kategori=? WHERE id=?")) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getIdKategori());
            ps.setInt(3, b.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM barang WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
