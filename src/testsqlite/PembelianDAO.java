package testsqlite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PembelianDAO - CRUD sederhana untuk tabel data_pembelian & detail_pembelian
 */
public class PembelianDAO {

    public PembelianDAO() throws SQLException {
        ensureTables();
    }

    private void ensureTables() throws SQLException {
        String createHeader = "CREATE TABLE IF NOT EXISTS data_pembelian ("
                + "id_pembelian TEXT PRIMARY KEY, "
                + "tgl_pembelian TEXT NOT NULL, "
                + "payment_method TEXT NOT NULL DEFAULT 'CASH', "
                + "total_harga INTEGER NOT NULL"
                + ");";

        String createDetail = "CREATE TABLE IF NOT EXISTS detail_pembelian ("
                + "id_detail_pembelian INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "harga_beli INTEGER, "
                + "stok INTEGER, "
                + "subtotal INTEGER, "
                + "id_supplier INTEGER, "
                + "id_pembelian TEXT, "
                + "id_barang INTEGER, "
                + "FOREIGN KEY (id_pembelian) REFERENCES data_pembelian(id_pembelian)"
                + ");";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement st = conn.createStatement()) {
            try { st.execute("PRAGMA foreign_keys = ON;"); } catch (Throwable ignore) {}
            st.execute(createHeader);
            st.execute(createDetail);
        }
    }

    public boolean exists(String idPembelian) throws SQLException {
        String sql = "SELECT 1 FROM data_pembelian WHERE id_pembelian = ? LIMIT 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPembelian);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Insert pembelian header + details dalam satu transaksi.
     * Akan melempar SQLException bila id_pembelian sudah ada.
     */
public void insertPembelianWithDetails(Pembelian p) throws SQLException {
    String insertHeader = "INSERT INTO data_pembelian(id_pembelian, tgl_pembelian, payment_method, total_harga) VALUES (?, ?, ?, ?)";
    String insertDetail = "INSERT INTO detail_pembelian(harga_beli, stok, subtotal, id_supplier, id_pembelian, id_barang) VALUES (?, ?, ?, ?, ?, ?)";
    Connection conn = null;
    try {
        conn = DatabaseHelper.getConnection();
        conn.setAutoCommit(false);
        try (PreparedStatement psHeader = conn.prepareStatement(insertHeader)) {
            // gunakan id_pembelian yang sudah ada di model (string)
            psHeader.setString(1, p.getIdPembelian());
            psHeader.setString(2, p.getTglPembelian());
            psHeader.setString(3, p.getPaymentMethod() == null ? "CASH" : p.getPaymentMethod());
            // total_harga di modelmu adalah int
            psHeader.setInt(4, p.getTotalHarga() == null ? 0 : p.getTotalHarga());
            psHeader.executeUpdate();
        }

        try (PreparedStatement psDetail = conn.prepareStatement(insertDetail)) {
            if (p.getDetails() != null) {
                for (DetailPembelian d : p.getDetails()) {
                    // harga_beli, stok, subtotal => INTEGER
                    if (d.getHargaBeli() == null) psDetail.setNull(1, Types.INTEGER); else psDetail.setInt(1, d.getHargaBeli());
                    if (d.getStok() == null) psDetail.setNull(2, Types.INTEGER); else psDetail.setInt(2, d.getStok());
                    if (d.getSubtotal() == null) psDetail.setNull(3, Types.INTEGER); else psDetail.setInt(3, d.getSubtotal());
                    if (d.getIdSupplier() == null) psDetail.setNull(4, Types.INTEGER); else psDetail.setInt(4, d.getIdSupplier());
                    psDetail.setString(5, p.getIdPembelian()); // id_pembelian sebagai TEXT
                    if (d.getIdBarang() == null) psDetail.setNull(6, Types.INTEGER); else psDetail.setInt(6, d.getIdBarang());
                    psDetail.addBatch();
                }
                psDetail.executeBatch();
            }
        }

        conn.commit();
    } catch (SQLException ex) {
        if (conn != null) try { conn.rollback(); } catch (Throwable ignore) {}
        throw ex;
    } finally {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (Throwable ignore) {}
            try { conn.close(); } catch (Throwable ignore) {}
        }
    }
}


/** Generate next ID pembelian berdasarkan tanggal hari ini (di dalam connection & transaksi yang sama). */
private String generateNewIdPembelian(Connection conn) throws SQLException {
    String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
    String defaultId = today + "0001";
    String sql = "SELECT id_pembelian FROM data_pembelian WHERE id_pembelian LIKE ? ORDER BY id_pembelian DESC LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, today + "%");
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String lastId = rs.getString(1);
                if (lastId != null && lastId.length() >= 12) { // ddMMyyyy + 4 digits = 12
                    String seq = lastId.substring(8); // 4-digit seq
                    try {
                        int lastNum = Integer.parseInt(seq);
                        return today + String.format("%04d", lastNum + 1);
                    } catch (NumberFormatException ignore) { }
                }
            }
        }
    }
    return defaultId;
}


    public List<Pembelian> findAllPembelian() throws SQLException {
        List<Pembelian> out = new ArrayList<>();
        String sql = "SELECT id_pembelian, tgl_pembelian, payment_method, total_harga FROM data_pembelian ORDER BY tgl_pembelian DESC, id_pembelian DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pembelian p = new Pembelian();
                p.setIdPembelian(rs.getString("id_pembelian"));
                p.setTglPembelian(rs.getString("tgl_pembelian"));
                p.setPaymentMethod(rs.getString("payment_method"));
                p.setTotalHarga(rs.getInt("total_harga"));
                out.add(p);
            }
        }
        return out;
    }

    public List<DetailPembelian> findDetailsByPembelian(String idPembelian) throws SQLException {
        List<DetailPembelian> out = new ArrayList<>();
        String sql = "SELECT id_detail_pembelian, harga_beli, stok, subtotal, id_supplier, id_pembelian, id_barang FROM detail_pembelian WHERE id_pembelian = ? ORDER BY id_detail_pembelian";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPembelian);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetailPembelian d = new DetailPembelian();
                    d.setIdDetailPembelian(rs.getInt("id_detail_pembelian"));
                    d.setHargaBeli(rs.getInt("harga_beli"));
                    d.setStok(rs.getInt("stok"));
                    d.setSubtotal(rs.getInt("subtotal"));
                    int sup = rs.getInt("id_supplier"); d.setIdSupplier(rs.wasNull() ? null : sup);
                    d.setIdPembelian(rs.getString("id_pembelian"));
                    int ib = rs.getInt("id_barang"); d.setIdBarang(rs.wasNull() ? null : ib);
                    out.add(d);
                }
            }
        }
        return out;
    }

    public void deletePembelian(String idPembelian) throws SQLException {
        String delDetail = "DELETE FROM detail_pembelian WHERE id_pembelian = ?";
        String delHeader = "DELETE FROM data_pembelian WHERE id_pembelian = ?";
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pd = conn.prepareStatement(delDetail);
                 PreparedStatement ph = conn.prepareStatement(delHeader)) {
                pd.setString(1, idPembelian);
                pd.executeUpdate();

                ph.setString(1, idPembelian);
                ph.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (Throwable t) {}
                throw ex;
            } finally {
                try { conn.setAutoCommit(true); } catch (Throwable ignore) {}
            }
        }
    }
}
