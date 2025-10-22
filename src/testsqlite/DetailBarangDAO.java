package testsqlite;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetailBarangDAO {

    public List<DetailBarang> findAll() throws SQLException {
        List<DetailBarang> list = new ArrayList<>();
        String sql = "SELECT id_detail_barang, barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian FROM detail_barang ORDER BY id_detail_barang";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DetailBarang d = mapRowToDetail(rs);
                list.add(d);
            }
        }
        return list;
    }

    public DetailBarang findById(int idDetail) throws SQLException {
        String sql = "SELECT id_detail_barang, barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian FROM detail_barang WHERE id_detail_barang = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToDetail(rs);
            }
        }
        return null;
    }

    /**
     * Ambil semua detail untuk satu barang (id_barang)
     */
    public List<DetailBarang> findByBarangId(int idBarang) throws SQLException {
        List<DetailBarang> list = new ArrayList<>();
        String sql = "SELECT id_detail_barang, barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian FROM detail_barang WHERE id_barang = ? ORDER BY id_detail_barang";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBarang);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRowToDetail(rs));
            }
        }
        return list;
    }

    /**
     * Insert detail_barang. Mengembalikan id generated (atau -1 jika gagal)
     * Note: kolom id_supplier sudah dihapus dari schema, jadi tidak diset di sini.
     */
    public int insert(DetailBarang d) throws SQLException {
        String sql = "INSERT INTO detail_barang (barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getBarcode());
            ps.setInt(2, d.getStok());
            ps.setString(3, d.getHargaJual() == null ? BigDecimal.ZERO.toPlainString() : d.getHargaJual().toPlainString());
            ps.setString(4, d.getTanggalExp());
            ps.setInt(5, d.getIdBarang());

            if (d.getIdDetailPembelian() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, d.getIdDetailPembelian());
            }

            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Update detail_barang
     */
    public void update(DetailBarang d) throws SQLException {
        String sql = "UPDATE detail_barang SET barcode=?, stok=?, harga_jual=?, tanggal_exp=?, id_barang=?, id_detail_pembelian=? WHERE id_detail_barang=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getBarcode());
            ps.setInt(2, d.getStok());
            ps.setString(3, d.getHargaJual() == null ? BigDecimal.ZERO.toPlainString() : d.getHargaJual().toPlainString());
            ps.setString(4, d.getTanggalExp());
            ps.setInt(5, d.getIdBarang());

            if (d.getIdDetailPembelian() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, d.getIdDetailPembelian());
            }

            ps.setInt(7, d.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Delete detail_barang
     */
    public void delete(int idDetail) throws SQLException {
        String sql = "DELETE FROM detail_barang WHERE id_detail_barang = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetail);
            ps.executeUpdate();
        }
    }

    /**
     * Kurangi stok untuk id_detail_barang. Throw SQLException jika stok tidak cukup.
     * Mengeksekusi dalam koneksi tersendiri; jika kamu memanggilnya dalam transaksi, gunakan versi yang menerima Connection.
     */
    public void decreaseStock(int idDetail, int qty) throws SQLException {
        try (Connection conn = DatabaseHelper.getConnection()) {
            decreaseStock(conn, idDetail, qty);
        }
    }

    /**
     * Versi decreaseStock yang menerima Connection (berguna saat processSale berada dalam satu transaksi)
     */
    public void decreaseStock(Connection conn, int idDetail, int qty) throws SQLException {
        // baca stok saat ini
        String sel = "SELECT stok FROM detail_barang WHERE id_detail_barang = ?";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setInt(1, idDetail);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Detail barang tidak ditemukan: " + idDetail);
                int stok = rs.getInt("stok");
                if (stok < qty) throw new SQLException("Stok tidak cukup (id=" + idDetail + "). Tersedia: " + stok + ", diminta: " + qty);
            }
        }
        // update stok
        String upd = "UPDATE detail_barang SET stok = stok - ? WHERE id_detail_barang = ?";
        try (PreparedStatement ps2 = conn.prepareStatement(upd)) {
            ps2.setInt(1, qty);
            ps2.setInt(2, idDetail);
            ps2.executeUpdate();
        }
    }

    /**
     * Ambil first available detail_barang (dipakai demo transaksi)
     * Mengembalikan null jika tidak ada.
     */
    public DetailBarang getFirstAvailableDetail() throws SQLException {
        String sql = "SELECT id_detail_barang, barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian FROM detail_barang WHERE stok > 0 LIMIT 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapRowToDetail(rs);
        }
        return null;
    }

    // helper mapping
    private DetailBarang mapRowToDetail(ResultSet rs) throws SQLException {
        DetailBarang d = new DetailBarang();
        d.setId(rs.getInt("id_detail_barang"));
        d.setBarcode(rs.getString("barcode"));
        d.setStok(rs.getInt("stok"));
        String hargaStr = rs.getString("harga_jual");
        d.setHargaJual((hargaStr == null || hargaStr.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(hargaStr));
        d.setTanggalExp(rs.getString("tanggal_exp"));
        d.setIdBarang(rs.getInt("id_barang"));


        // id_detail_pembelian nullable
        int idDet = rs.getInt("id_detail_pembelian");
        if (rs.wasNull()) {
            try {
                d.setIdDetailPembelian(null);
            } catch (NoSuchMethodError | Exception ignore) {}
        } else {
            try {
                d.setIdDetailPembelian(idDet);
            } catch (NoSuchMethodError | Exception ignore) {}
        }

        return d;
    }
}
