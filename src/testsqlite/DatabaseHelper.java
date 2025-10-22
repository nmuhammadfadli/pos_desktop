package testsqlite;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper: inisialisasi DB + CRUD helper untuk barang, voucher, detail_barang.
 * Java 8 compatible. Package: testsqlite
 */
public class DatabaseHelper {
    private static final String DB_PATH = "data/pos_app.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    // ------------------ init & connection ------------------
    public static void initDatabase() throws Exception {
        System.out.println("Working dir: " + System.getProperty("user.dir"));
        File dbFile = new File(DB_PATH);
        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        boolean existed = dbFile.exists();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // enable pragma early
            try { stmt.execute("PRAGMA foreign_keys = ON;"); } catch (Throwable t) {}
            try { stmt.execute("PRAGMA journal_mode = WAL;"); } catch (Throwable t) {}

            // ---------------- reference tables ----------------
            stmt.execute("CREATE TABLE IF NOT EXISTS data_kategori (" +
                    "id_kategori VARCHAR(10) PRIMARY KEY, " +
                    "nama_kategori TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS data_guru (" +
                    "id_guru INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama_guru TEXT UNIQUE NOT NULL, " +
                    "notelp_guru TEXT DEFAULT '0', " +
                    "jabatan TEXT DEFAULT '')");

            stmt.execute("CREATE TABLE IF NOT EXISTS data_supplier (" +
                    "id_supplier INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama_supplier TEXT NOT NULL, " +
                    "alamat_supplier TEXT, " +
                    "notelp_supplier TEXT)");

            // ---------------- main item table ----------------
            stmt.execute("CREATE TABLE IF NOT EXISTS barang (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama TEXT NOT NULL, " +
                    "id_kategori VARCHAR(10) NOT NULL, " +
                    "FOREIGN KEY(id_kategori) REFERENCES data_kategori(id_kategori))");

            // ---------------- pembelian tables (header + detail) ----------------
            stmt.execute("CREATE TABLE IF NOT EXISTS data_pembelian (" +
                    "id_pembelian TEXT PRIMARY KEY, " +
                    "tgl_pembelian TEXT NOT NULL, " +
                    "total_harga INTEGER NOT NULL DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS detail_pembelian (" +
                    "id_detail_pembelian INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "harga_beli INTEGER NOT NULL, " +
                    "stok INTEGER NOT NULL, " +
                    "subtotal INTEGER NOT NULL, " +
                    "id_supplier INTEGER NOT NULL, " +
                    "id_pembelian TEXT NOT NULL, " +
                    "id_detail_barang INTEGER NOT NULL, " +
                    "FOREIGN KEY(id_supplier) REFERENCES data_supplier(id_supplier), " +
                    "FOREIGN KEY(id_pembelian) REFERENCES data_pembelian(id_pembelian), " +
                    "FOREIGN KEY(id_detail_barang) REFERENCES detail_barang(id_detail_barang))");

            // ---------------- detail_barang (WITHOUT id_supplier) ----------------
            stmt.execute("CREATE TABLE IF NOT EXISTS detail_barang (" +
                    "id_detail_barang INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "barcode TEXT, " +
                    "stok INTEGER NOT NULL DEFAULT 0, " +
                    "harga_jual TEXT NOT NULL, " +
                    "tanggal_exp TEXT, " +
                    "id_barang INTEGER NOT NULL, " +
                    "id_detail_pembelian INTEGER, " +
                    "FOREIGN KEY (id_barang) REFERENCES barang(id), " +
                    "FOREIGN KEY (id_detail_pembelian) REFERENCES detail_pembelian(id_detail_pembelian))");

            // ---------------- other tables (voucher, transaksi, etc.) ----------------
            stmt.execute("CREATE TABLE IF NOT EXISTS kode_voucher (" +
                    "id_voucher INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "kode TEXT UNIQUE, " +
                    "id_guru INTEGER, " +
                    "bulan TEXT, " +
                    "current_balance TEXT DEFAULT '0', " +
                    "created_at TEXT DEFAULT (datetime('now')), " +
                    "FOREIGN KEY(id_guru) REFERENCES data_guru(id_guru))");

            stmt.execute("CREATE TABLE IF NOT EXISTS transaksi_penjualan (" +
                    "id_transaksi INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "kode_transaksi TEXT UNIQUE, " +
                    "tgl_transaksi TEXT DEFAULT (datetime('now')), " +
                    "total_harga TEXT NOT NULL, " +
                    "total_bayar TEXT NOT NULL DEFAULT '0', " +
                    "kembalian TEXT NOT NULL DEFAULT '0', " +
                    "payment_method TEXT NOT NULL DEFAULT 'CASH', " +
                    "id_voucher INTEGER, " +
                    "FOREIGN KEY (id_voucher) REFERENCES kode_voucher(id_voucher))");

            stmt.execute("CREATE TABLE IF NOT EXISTS detail_penjualan (" +
                    "id_detail_penjualan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_transaksi INTEGER NOT NULL, " +
                    "id_detail_barang INTEGER NOT NULL, " +
                    "jumlah_barang INTEGER NOT NULL, " +
                    "harga_unit TEXT NOT NULL, " +
                    "subtotal TEXT NOT NULL, " +
                    "FOREIGN KEY (id_transaksi) REFERENCES transaksi_penjualan(id_transaksi))");

            stmt.execute("CREATE TABLE IF NOT EXISTS voucher_usage (" +
                    "id_usage INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_voucher INTEGER NOT NULL, " +
                    "id_transaksi INTEGER NOT NULL, " +
                    "used_amount TEXT NOT NULL, " +
                    "used_at TEXT DEFAULT (datetime('now')))");

            stmt.execute("CREATE TABLE IF NOT EXISTS receivable (" +
                    "id_receivable INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_transaksi INTEGER NOT NULL UNIQUE, " +
                    "amount_total TEXT NOT NULL, " +
                    "amount_paid TEXT NOT NULL DEFAULT '0', " +
                    "amount_outstanding TEXT NOT NULL, " +
                    "created_at TEXT DEFAULT (datetime('now')), " +
                    "status TEXT DEFAULT 'OPEN')");

            // ---------------- indexes for performance ----------------
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_detail_barang_id_detail_pembelian ON detail_barang(id_detail_pembelian)"); } catch (Throwable t) {}
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_detail_pembelian_id_barang ON detail_pembelian(id_barang)"); } catch (Throwable t) {}
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_detail_pembelian_id_supplier ON detail_pembelian(id_supplier)"); } catch (Throwable t) {}

            // ---------------- triggers: sync detail_barang from detail_pembelian ----------------
            // Insert: when a purchase detail is created, create a corresponding detail_barang batch (if not exists)
            try {
                stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS trg_dp_after_insert_create_db " +
                    "AFTER INSERT ON detail_pembelian " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "  INSERT OR IGNORE INTO detail_barang (barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian) " +
                    "  VALUES (NULL, NEW.stok, CAST(NEW.harga_beli AS TEXT), NULL, NEW.id_barang, NEW.id_detail_pembelian);" +
                    "END;"
                );
            } catch (Throwable t) {
                System.err.println("warning creating trigger trg_dp_after_insert_create_db: " + t.getMessage());
            }

            // Update: sync stok & harga_jual when purchase detail updated
            try {
                stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS trg_dp_after_update_sync_db " +
                    "AFTER UPDATE ON detail_pembelian " +
                    "FOR EACH ROW " +
                    "WHEN NEW.id_detail_pembelian IS NOT NULL " +
                    "BEGIN " +
                    "  UPDATE detail_barang SET stok = NEW.stok, harga_jual = CAST(NEW.harga_beli AS TEXT) " +
                    "  WHERE id_detail_pembelian = NEW.id_detail_pembelian; " +
                    "END;"
                );
            } catch (Throwable t) {
                System.err.println("warning creating trigger trg_dp_after_update_sync_db: " + t.getMessage());
            }

            // Delete: if purchase detail removed, remove corresponding batch row (optional but kept)
            try {
                stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS trg_dp_after_delete_remove_db " +
                    "AFTER DELETE ON detail_pembelian " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "  DELETE FROM detail_barang WHERE id_detail_pembelian = OLD.id_detail_pembelian; " +
                    "END;"
                );
            } catch (Throwable t) {
                System.err.println("warning creating trigger trg_dp_after_delete_remove_db: " + t.getMessage());
            }

            // ---------------- sample data jika DB baru ----------------
            if (!existed) {
                // ensure default kategori
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO data_kategori (id_kategori, nama_kategori) VALUES (?, ?)")) {
                    ps.setString(1, "KTG01");
                    ps.setString(2, "Umum");
                    ps.executeUpdate();
                }

                // sample barang
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO barang (nama, id_kategori) VALUES (?, ?)")) {
                    ps.setString(1, "Kopi Bondowoso");
                    ps.setString(2, "KTG01");
                    ps.executeUpdate();

                    ps.setString(1, "Teh Hijau");
                    ps.setString(2, "KTG01");
                    ps.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample barang: " + t.getMessage());
                }

                // sample supplier(s)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO data_supplier (nama_supplier, alamat_supplier, notelp_supplier) VALUES (?, ?, ?)")) {
                    ps.setString(1, "Supplier A");
                    ps.setString(2, "Jl. Merdeka 1");
                    ps.setString(3, "081100111");
                    ps.executeUpdate();

                    ps.setString(1, "Supplier B");
                    ps.setString(2, "Jl. Sudirman 2");
                    ps.setString(3, "081200222");
                    ps.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample suppliers: " + t.getMessage());
                }

                // sample pembelian header + detail (this will auto-create detail_barang via trigger)
                try (PreparedStatement psHead = conn.prepareStatement(
                        "INSERT INTO data_pembelian (id_pembelian, tgl_pembelian, total_harga) VALUES (?, ?, ?)")) {
                    psHead.setString(1, "PB-20251019-001");
                    psHead.setString(2, "2025-10-19");
                    psHead.setInt(3, 170000);
                    psHead.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample pembelian header: " + t.getMessage());
                }

                try (PreparedStatement psDetail = conn.prepareStatement(
                        "INSERT INTO detail_pembelian (harga_beli, stok, subtotal, id_supplier, id_pembelian, id_detail_barang) VALUES (?, ?, ?, ?, ?, ?)")) {
                    psDetail.setInt(1, 50000);
                    psDetail.setInt(2, 2);
                    psDetail.setInt(3, 100000);
                    psDetail.setInt(4, 1); // supplier A
                    psDetail.setString(5, "PB-20251019-001");
                    psDetail.setInt(6, 1); // barang id 1
                    psDetail.executeUpdate();

                    psDetail.setInt(1, 70000);
                    psDetail.setInt(2, 1);
                    psDetail.setInt(3, 70000);
                    psDetail.setInt(4, 2); // supplier B
                    psDetail.setString(5, "PB-20251019-001");
                    psDetail.setInt(6, 2); // barang id 2
                    psDetail.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample detail_pembelian: " + t.getMessage());
                }

                // sample voucher
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO kode_voucher (kode,id_guru,current_balance) VALUES (?,?,?)")) {
                    ps.setString(1, "VCHR-001");
                    ps.setObject(2, null);
                    ps.setString(3, new BigDecimal("50000").toPlainString());
                    ps.executeUpdate();
                } catch (Throwable t) {
                    System.err.println("warning inserting sample voucher: " + t.getMessage());
                }

                System.out.println("Database baru dibuat dan sample data disisipkan.");
            } else {
                System.out.println("Database sudah ada: " + DB_PATH);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite tidak ditemukan. Pastikan sqlite-jdbc.jar ada di Libraries.");
        }
        return DriverManager.getConnection(URL);
    }

    // ------------------ CRUD Barang ------------------
    public static List<Barang> getAllBarang() throws SQLException {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT b.id, b.nama, b.id_kategori, k.nama_kategori " +
                "FROM barang b LEFT JOIN data_kategori k ON b.id_kategori = k.id_kategori ORDER BY b.nama";
        try (Connection conn = getConnection();
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

    public static void insertBarang(Barang b) throws SQLException {
        String sql = "INSERT INTO barang (nama, id_kategori) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getIdKategori());
            ps.executeUpdate();
        }
    }

    public static void updateBarang(Barang b) throws SQLException {
        String sql = "UPDATE barang SET nama=?, id_kategori=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getIdKategori());
            ps.setInt(3, b.getId());
            ps.executeUpdate();
        }
    }

    public static void deleteBarang(int id) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM barang WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------ CRUD Voucher ------------------
    public static List<Voucher> getAllVouchers() throws SQLException {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT id_voucher, kode, current_balance FROM kode_voucher ORDER BY id_voucher";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String bal = rs.getString("current_balance");
                BigDecimal b = (bal == null || bal.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(bal);
                Voucher v = new Voucher();
                v.setIdVoucher(rs.getInt("id_voucher"));
                v.setKode(rs.getString("kode"));
                v.setCurrentBalance(b);
                list.add(v);
            }
        }
        return list;
    }

    public static Voucher findVoucherById(int idVoucher) throws SQLException {
        String sql = "SELECT id_voucher, kode, current_balance FROM kode_voucher WHERE id_voucher = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVoucher);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String bal = rs.getString("current_balance");
                    BigDecimal b = (bal == null || bal.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(bal);
                    Voucher v = new Voucher();
                    v.setIdVoucher(rs.getInt("id_voucher"));
                    v.setKode(rs.getString("kode"));
                    v.setCurrentBalance(b);
                    return v;
                }
            }
        }
        return null;
    }

    public static int insertVoucher(Voucher v) throws SQLException {
        String sql = "INSERT INTO kode_voucher (kode, id_guru, current_balance) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getKode());
            ps.setObject(2, null);
            ps.setString(3, v.getCurrentBalance() == null ? BigDecimal.ZERO.toPlainString() : v.getCurrentBalance().toPlainString());
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getInt(1);
            }
        }
        return -1;
    }

    public static void updateVoucherBalance(int idVoucher, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE kode_voucher SET current_balance = ? WHERE id_voucher = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newBalance.toPlainString());
            ps.setInt(2, idVoucher);
            ps.executeUpdate();
        }
    }

    public static void deleteVoucher(int idVoucher) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM kode_voucher WHERE id_voucher = ?")) {
            ps.setInt(1, idVoucher);
            ps.executeUpdate();
        }
    }

    // ------------------ CRUD Detail Barang (revised: NO id_supplier) ------------------
    public static List<DetailBarang> getAllDetailBarang() throws SQLException {
        List<DetailBarang> list = new ArrayList<>();
        String sql = "SELECT id_detail_barang, barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian FROM detail_barang ORDER BY id_detail_barang";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DetailBarang d = new DetailBarang();
                d.setId(rs.getInt("id_detail_barang"));
                d.setBarcode(rs.getString("barcode"));
                d.setStok(rs.getInt("stok"));
                String hargaStr = rs.getString("harga_jual");
                d.setHargaJual((hargaStr == null || hargaStr.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(hargaStr));
                d.setTanggalExp(rs.getString("tanggal_exp"));
                d.setIdBarang(rs.getInt("id_barang"));
                // if your DetailBarang model has idDetailPembelian field, set it here (optional)
                try {
                    DetailBarang.class.getMethod("setIdDetailPembelian", Integer.class).invoke(d, (Integer) rs.getObject("id_detail_pembelian"));
                } catch (NoSuchMethodException nsme) {
                    // model doesn't have field, ignore
                } catch (Exception ignore) {}
                list.add(d);
            }
        }
        return list;
    }

    public static DetailBarang findDetailById(int idDetail) throws SQLException {
        String sql = "SELECT id_detail_barang, barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian FROM detail_barang WHERE id_detail_barang = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DetailBarang d = new DetailBarang();
                    d.setId(rs.getInt("id_detail_barang"));
                    d.setBarcode(rs.getString("barcode"));
                    d.setStok(rs.getInt("stok"));
                    String hargaStr = rs.getString("harga_jual");
                    d.setHargaJual((hargaStr == null || hargaStr.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(hargaStr));
                    d.setTanggalExp(rs.getString("tanggal_exp"));
                    d.setIdBarang(rs.getInt("id_barang"));
                    try {
                        DetailBarang.class.getMethod("setIdDetailPembelian", Integer.class).invoke(d, (Integer) rs.getObject("id_detail_pembelian"));
                    } catch (NoSuchMethodException nsme) {
                        // ignore
                    } catch (Exception ignore) {}
                    return d;
                }
            }
        }
        return null;
    }

    public static int insertDetailBarang(DetailBarang d) throws SQLException {
        String sql = "INSERT INTO detail_barang (barcode, stok, harga_jual, tanggal_exp, id_barang, id_detail_pembelian) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getBarcode());
            ps.setInt(2, d.getStok());
            ps.setString(3, d.getHargaJual() == null ? BigDecimal.ZERO.toPlainString() : d.getHargaJual().toPlainString());
            ps.setString(4, d.getTanggalExp());
            ps.setInt(5, d.getIdBarang());
            // set id_detail_pembelian if model exposes it, else null
            try {
                Integer idDet = (Integer) DetailBarang.class.getMethod("getIdDetailPembelian").invoke(d);
                if (idDet == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, idDet);
            } catch (NoSuchMethodException nsme) {
                ps.setNull(6, Types.INTEGER);
            } catch (Exception ex) {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getInt(1);
            }
        }
        return -1;
    }

    public static void updateDetailBarang(DetailBarang d) throws SQLException {
        String sql = "UPDATE detail_barang SET barcode=?, stok=?, harga_jual=?, tanggal_exp=?, id_barang=?, id_detail_pembelian=? WHERE id_detail_barang=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getBarcode());
            ps.setInt(2, d.getStok());
            ps.setString(3, d.getHargaJual() == null ? BigDecimal.ZERO.toPlainString() : d.getHargaJual().toPlainString());
            ps.setString(4, d.getTanggalExp());
            ps.setInt(5, d.getIdBarang());
            try {
                Integer idDet = (Integer) DetailBarang.class.getMethod("getIdDetailPembelian").invoke(d);
                if (idDet == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, idDet);
            } catch (NoSuchMethodException nsme) {
                ps.setNull(6, Types.INTEGER);
            } catch (Exception ex) {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setInt(7, d.getId());
            ps.executeUpdate();
        }
    }

    public static void deleteDetailBarang(int idDetail) throws SQLException {
        String sql = "DELETE FROM detail_barang WHERE id_detail_barang = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetail);
            ps.executeUpdate();
        }
    }

    public static void backupDatabase(String targetPath) throws IOException {
        Path src = Paths.get(DB_PATH);
        Path dst = Paths.get(targetPath);
        if (Files.notExists(src)) throw new IOException("Database file tidak ditemukan: " + src.toString());
        Files.copy(src, dst);
    }
}
