package testsqlite;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * MainFrame - UI utama: tabs untuk Barang, Kategori, Guru, Supplier, Detail Barang, Pembelian, Voucher, Transaksi, Piutang.
 */
public class MainFrame extends JFrame {
    // Barang
    private JTable tblBarang;
    private DefaultTableModel barangModel;
    private BarangDAO barangDAO = new BarangDAO();

    // Kategori
    private JTable tblKategori;
    private DefaultTableModel kategoriModel;
    private KategoriDAO kategoriDAO;

    // Guru
    private JTable tblGuru;
    private DefaultTableModel guruModel;
    private GuruDAO guruDAO;

    // Supplier
    private JTable tblSupplier;
    private DefaultTableModel supplierModel;
    private SupplierDAO supplierDAO;

    // Pembelian (header)
    private JTable tblPembelian;
    private DefaultTableModel pembelianModel;
    private PembelianDAO pembelianDAO;

    // Voucher (pakai DatabaseHelper.getAllVouchers())
    private JTable tblVoucher;
    private DefaultTableModel voucherModel;

    // Detail barang
    private JTable tblDetail;
    private DefaultTableModel detailModel;
    private DetailBarangDAO detailDao = new DetailBarangDAO();

    public MainFrame() {
        setTitle("POS Demo - SQLite");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // init DAOs (tangani kemungkinan exception)
        try {
            kategoriDAO = new KategoriDAO();
        } catch (Exception ex) {
            kategoriDAO = null;
            System.err.println("KategoriDAO init failed: " + ex.getMessage());
        }

        try {
            guruDAO = new GuruDAO();
        } catch (Exception ex) {
            guruDAO = null;
            System.err.println("GuruDAO init failed: " + ex.getMessage());
        }

        try {
            supplierDAO = new SupplierDAO();
        } catch (Exception ex) {
            supplierDAO = null;
            System.err.println("SupplierDAO init failed: " + ex.getMessage());
        }

        try {
            pembelianDAO = new PembelianDAO();
        } catch (Exception ex) {
            pembelianDAO = null;
            System.err.println("PembelianDAO init failed: " + ex.getMessage());
        }

        init();
        loadBarang();
        loadVouchers();
        loadDetailBarang();
        loadKategori();
        loadGuru();
        loadSupplier();
        loadPembelian();
        setLocationRelativeTo(null);
    }

    private void init() {
        JTabbedPane tabs = new JTabbedPane();

        // ---------------- Panel Barang ----------------
        JPanel pBarang = new JPanel(new BorderLayout());
        barangModel = new DefaultTableModel(new Object[]{"ID", "Nama", "Kategori"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBarang = new JTable(barangModel);
        pBarang.add(new JScrollPane(tblBarang), BorderLayout.CENTER);

        JPanel btns = new JPanel();
        JButton bAdd = new JButton("Tambah"); bAdd.addActionListener(e -> onTambahBarang());
        JButton bEdit = new JButton("Edit"); bEdit.addActionListener(e -> onEditBarang());
        JButton bDel = new JButton("Hapus"); bDel.addActionListener(e -> onHapusBarang());
        JButton bRef = new JButton("Refresh"); bRef.addActionListener(e -> loadBarang());
        btns.add(bAdd); btns.add(bEdit); btns.add(bDel); btns.add(bRef);
        pBarang.add(btns, BorderLayout.SOUTH);
        tabs.addTab("Barang", pBarang);

        // ---------------- Panel Kategori ----------------
        JPanel pKategori = new JPanel(new BorderLayout());
        kategoriModel = new DefaultTableModel(new Object[]{"ID Kategori", "Nama Kategori"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblKategori = new JTable(kategoriModel);
        pKategori.add(new JScrollPane(tblKategori), BorderLayout.CENTER);

        JPanel kBtns = new JPanel();
        JButton kAdd = new JButton("Tambah Kategori"); kAdd.addActionListener(e -> onTambahKategori());
        JButton kEdit = new JButton("Edit Kategori"); kEdit.addActionListener(e -> onEditKategori());
        JButton kDel = new JButton("Hapus Kategori"); kDel.addActionListener(e -> onHapusKategori());
        JButton kRef = new JButton("Refresh"); kRef.addActionListener(e -> loadKategori());
        kBtns.add(kAdd); kBtns.add(kEdit); kBtns.add(kDel); kBtns.add(kRef);
        pKategori.add(kBtns, BorderLayout.SOUTH);
        tabs.addTab("Kategori", pKategori);

        // ---------------- Panel Guru ----------------
        JPanel pGuru = new JPanel(new BorderLayout());
        guruModel = new DefaultTableModel(new Object[]{"ID Guru", "Nama", "No Telp", "Jabatan"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblGuru = new JTable(guruModel);
        pGuru.add(new JScrollPane(tblGuru), BorderLayout.CENTER);

        JPanel gBtns = new JPanel();
        JButton gAdd = new JButton("Tambah Guru"); gAdd.addActionListener(e -> onTambahGuru());
        JButton gEdit = new JButton("Edit Guru"); gEdit.addActionListener(e -> onEditGuru());
        JButton gDel = new JButton("Hapus Guru"); gDel.addActionListener(e -> onHapusGuru());
        JButton gRef = new JButton("Refresh"); gRef.addActionListener(e -> loadGuru());
        gBtns.add(gAdd); gBtns.add(gEdit); gBtns.add(gDel); gBtns.add(gRef);
        pGuru.add(gBtns, BorderLayout.SOUTH);
        tabs.addTab("Guru", pGuru);

        // ---------------- Panel Supplier ----------------
        JPanel pSupplier = new JPanel(new BorderLayout());
        supplierModel = new DefaultTableModel(new Object[]{"ID Supplier", "Nama", "Alamat", "No Telp"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSupplier = new JTable(supplierModel);
        pSupplier.add(new JScrollPane(tblSupplier), BorderLayout.CENTER);

        JPanel sBtns = new JPanel();
        JButton sAdd = new JButton("Tambah Supplier"); sAdd.addActionListener(e -> onTambahSupplier());
        JButton sEdit = new JButton("Edit Supplier"); sEdit.addActionListener(e -> onEditSupplier());
        JButton sDel = new JButton("Hapus Supplier"); sDel.addActionListener(e -> onHapusSupplier());
        JButton sRef = new JButton("Refresh"); sRef.addActionListener(e -> loadSupplier());
        sBtns.add(sAdd); sBtns.add(sEdit); sBtns.add(sDel); sBtns.add(sRef);
        pSupplier.add(sBtns, BorderLayout.SOUTH);
        tabs.addTab("Supplier", pSupplier);

        // ---------------- Panel Detail Barang ----------------
        JPanel pDetail = new JPanel(new BorderLayout());
        // sesuaikan kolom dengan model DetailBarang (tidak termasuk id_detail_pembelian)
        detailModel = new DefaultTableModel(
               new Object[]{"ID", "Barcode", "Stok", "Harga Jual", "Tanggal Exp", "Id Barang"}, 0) {
           @Override public boolean isCellEditable(int r, int c) { return false; }
       };

        tblDetail = new JTable(detailModel);
        pDetail.add(new JScrollPane(tblDetail), BorderLayout.CENTER);

        JPanel dbBtns = new JPanel();
        JButton dbAdd = new JButton("Tambah Detail"); dbAdd.addActionListener(e -> onTambahDetail());
        JButton dbEdit = new JButton("Edit Detail"); dbEdit.addActionListener(e -> onEditDetail());
        JButton dbDel = new JButton("Hapus Detail"); dbDel.addActionListener(e -> onHapusDetail());
        JButton dbRef = new JButton("Refresh"); dbRef.addActionListener(e -> loadDetailBarang());
        dbBtns.add(dbAdd); dbBtns.add(dbEdit); dbBtns.add(dbDel); dbBtns.add(dbRef);
        pDetail.add(dbBtns, BorderLayout.SOUTH);
        tabs.addTab("Detail Barang", pDetail);

        // ---------------- Panel Pembelian ----------------
        JPanel pPembelian = new JPanel(new BorderLayout());
        pembelianModel = new DefaultTableModel(new Object[]{"ID Pembelian", "Tanggal", "Total Harga"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPembelian = new JTable(pembelianModel);
        pPembelian.add(new JScrollPane(tblPembelian), BorderLayout.CENTER);

        JPanel pbBtns = new JPanel();
        JButton pbAdd = new JButton("Tambah Pembelian"); pbAdd.addActionListener(e -> onTambahPembelian());
        JButton pbView = new JButton("Lihat Pembelian"); pbView.addActionListener(e -> onViewPembelian());
        JButton pbRef = new JButton("Refresh"); pbRef.addActionListener(e -> loadPembelian());
        pbBtns.add(pbAdd); pbBtns.add(pbView); pbBtns.add(pbRef);
        pPembelian.add(pbBtns, BorderLayout.SOUTH);
        tabs.addTab("Pembelian", pPembelian);

        // ---------------- Panel Voucher ----------------
        JPanel pVoucher = new JPanel(new BorderLayout());
        voucherModel = new DefaultTableModel(new Object[]{"ID","Kode","Saldo"}, 0) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tblVoucher = new JTable(voucherModel);
        pVoucher.add(new JScrollPane(tblVoucher), BorderLayout.CENTER);

        JPanel vb = new JPanel();
        JButton addV = new JButton("Tambah Voucher"); addV.addActionListener(ev -> onTambahVoucher());
        JButton editV = new JButton("Edit Voucher"); editV.addActionListener(ev -> onEditVoucher());
        JButton delV = new JButton("Hapus Voucher"); delV.addActionListener(ev -> onHapusVoucher());
        JButton refV = new JButton("Refresh"); refV.addActionListener(ev -> loadVouchers());
        vb.add(addV); vb.add(editV); vb.add(delV); vb.add(refV);
        pVoucher.add(vb, BorderLayout.SOUTH);
        tabs.addTab("Voucher", pVoucher);

        // ---------------- Panel Transaksi ----------------
        JPanel pTrans = new JPanel(new FlowLayout());
        JButton bTrans = new JButton("Buat Transaksi (demo)");
        bTrans.addActionListener(e -> {
            TransactionDialog dlg = new TransactionDialog(this);
            dlg.setVisible(true);
            loadBarang();
            loadVouchers();
            loadDetailBarang();
            loadKategori();
            loadGuru();
            loadSupplier();
        });

        JButton bList = new JButton("Daftar Transaksi");
        bList.addActionListener(e -> {
            TransactionListDialog dlg = new TransactionListDialog(this);
            dlg.setVisible(true);
        });

        JButton bReceivable = new JButton("Piutang");
        bReceivable.addActionListener(e -> {
            ReceivableListDialog dlg = new ReceivableListDialog(this);
            dlg.setVisible(true);
        });

        pTrans.add(bTrans);
        pTrans.add(bList);
        pTrans.add(bReceivable);
        tabs.addTab("Transaksi", pTrans);

        // ---------------- Tab Piutang (opsional) ----------------
        JPanel pPiutang = new JPanel(new BorderLayout());
        JPanel piuCenter = new JPanel(new FlowLayout());
        JButton openPiutang = new JButton("Lihat / Kelola Piutang");
        openPiutang.addActionListener(e -> {
            ReceivableListDialog dlg = new ReceivableListDialog(this);
            dlg.setVisible(true);
        });
        piuCenter.add(openPiutang);
        pPiutang.add(piuCenter, BorderLayout.CENTER);
        tabs.addTab("Piutang", pPiutang);

        add(tabs);
    }

    // ----------------- Load Data -----------------
    private void loadBarang() {
        try {
            List<Barang> list = barangDAO.findAll();
            barangModel.setRowCount(0);
            for (Barang b : list) {
                barangModel.addRow(new Object[]{
                        b.getId(),
                        b.getNama(),
                        b.getNamaKategori() != null ? b.getNamaKategori() : "-"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load barang: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadKategori() {
        try {
            if (kategoriDAO == null) kategoriDAO = new KategoriDAO();
            List<Kategori> list = kategoriDAO.findAll();
            kategoriModel.setRowCount(0);
            for (Kategori k : list) {
                kategoriModel.addRow(new Object[]{ k.getIdKategori(), k.getNamaKategori() });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load kategori: " + ex.getMessage(),
                    "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadGuru() {
        try {
            if (guruDAO == null) guruDAO = new GuruDAO();
            List<Guru> list = guruDAO.findAll();
            guruModel.setRowCount(0);
            for (Guru g : list) {
                guruModel.addRow(new Object[]{
                        g.getIdGuru(),
                        g.getNamaGuru(),
                        g.getNotelpGuru() != null ? g.getNotelpGuru() : "-",
                        g.getJabatan() != null ? g.getJabatan() : "-"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load guru: " + ex.getMessage(),
                    "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadSupplier() {
        try {
            if (supplierDAO == null) supplierDAO = new SupplierDAO();
            List<Supplier> list = supplierDAO.findAll();
            supplierModel.setRowCount(0);
            for (Supplier s : list) {
                supplierModel.addRow(new Object[]{
                        s.getIdSupplier(),
                        s.getNamaSupplier(),
                        s.getAlamatSupplier() != null ? s.getAlamatSupplier() : "-",
                        s.getNotelpSupplier() != null ? s.getNotelpSupplier() : "-"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load supplier: " + ex.getMessage(),
                    "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadDetailBarang() {
        try {
            List<DetailBarang> list = detailDao.findAll();
            detailModel.setRowCount(0);
            for (DetailBarang d : list) {
               String harga = d.getHargaJual() == null ? "0" : d.getHargaJual().toPlainString();
               detailModel.addRow(new Object[]{
                       d.getId(),
                       d.getBarcode(),
                       d.getStok(),
                       harga,
                       d.getTanggalExp(),
                       d.getIdBarang()
               });
           }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load detail barang: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPembelian() {
        try {
            if (pembelianDAO == null) pembelianDAO = new PembelianDAO();
            List<Pembelian> list = pembelianDAO.findAllPembelian(); // sesuai PembelianDAO yang kita buat
            pembelianModel.setRowCount(0);
            for (Pembelian p : list) {
                pembelianModel.addRow(new Object[]{
                        p.getIdPembelian(),
                        p.getTglPembelian(),
                        p.getTotalHarga()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load pembelian: " + ex.getMessage(),
                    "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadVouchers() {
        try {
            List<Voucher> list = DatabaseHelper.getAllVouchers(); // gunakan DatabaseHelper supaya aman
            voucherModel.setRowCount(0);
            for (Voucher v : list) {
                String saldo = v.getCurrentBalance() == null ? "0" : v.getCurrentBalance().toPlainString();
                voucherModel.addRow(new Object[]{
                        v.getIdVoucher(),
                        v.getKode(),
                        v.getIdGuru(),
                        v.getNamaGuru(),
                        saldo
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load voucher: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------- CRUD Barang -----------------
    private void onTambahBarang() {
        AddEditBarangDialog dlg = new AddEditBarangDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            try {
                barangDAO.insert(dlg.getBarang());
                loadBarang();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal tambah: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditBarang() {
        int r = tblBarang.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }

        try {
            int id = Integer.parseInt(tblBarang.getValueAt(r, 0).toString());
            Barang b = new Barang();
            b.setId(id);

            // ambil full object via DAO agar lebih aman (mendapatkan idKategori)
            List<Barang> list = new BarangDAO().findAll();
            for (Barang bx : list) if (bx.getId() == id) { b = bx; break; }

            AddEditBarangDialog dlg = new AddEditBarangDialog(this, b);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                barangDAO.update(dlg.getBarang());
                loadBarang();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal edit: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHapusBarang() {
        int r = tblBarang.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        int id = Integer.parseInt(tblBarang.getValueAt(r, 0).toString());
        if (JOptionPane.showConfirmDialog(this, "Hapus ID " + id + "?") == JOptionPane.YES_OPTION) {
            try {
                barangDAO.delete(id);
                loadBarang();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal hapus: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------- CRUD Kategori -----------------
    private void onTambahKategori() {
        AddEditKategoriDialog dlg = new AddEditKategoriDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            try {
                if (kategoriDAO == null) kategoriDAO = new KategoriDAO();
                kategoriDAO.insert(dlg.getKategori());
                loadKategori();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal tambah kategori: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditKategori() {
        int r = tblKategori.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        String id = tblKategori.getValueAt(r,0).toString();
        try {
            if (kategoriDAO == null) kategoriDAO = new KategoriDAO();
            Kategori k = kategoriDAO.findById(id);
            if (k == null) { JOptionPane.showMessageDialog(this, "Kategori tidak ditemukan"); return; }
            AddEditKategoriDialog dlg = new AddEditKategoriDialog(this, k);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                kategoriDAO.update(dlg.getKategori());
                loadKategori();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal edit kategori: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHapusKategori() {
        int r = tblKategori.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        String id = tblKategori.getValueAt(r,0).toString();
        if (JOptionPane.showConfirmDialog(this, "Hapus kategori ID " + id + "?") == JOptionPane.YES_OPTION) {
            try {
                if (kategoriDAO == null) kategoriDAO = new KategoriDAO();
                kategoriDAO.delete(id);
                loadKategori();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal hapus kategori: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------- CRUD Guru -----------------
    private void onTambahGuru() {
        AddEditGuruDialog dlg = new AddEditGuruDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            try {
                if (guruDAO == null) guruDAO = new GuruDAO();
                int newId = guruDAO.insert(dlg.getGuru());
                if (newId > 0) {
                    JOptionPane.showMessageDialog(this, "Guru ditambahkan (id=" + newId + ")");
                    loadGuru();
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal menambahkan guru.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal tambah guru: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditGuru() {
        int r = tblGuru.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        try {
            int id = Integer.parseInt(tblGuru.getValueAt(r, 0).toString());
            if (guruDAO == null) guruDAO = new GuruDAO();
            Guru g = guruDAO.findById(id);
            if (g == null) { JOptionPane.showMessageDialog(this, "Guru tidak ditemukan"); return; }
            AddEditGuruDialog dlg = new AddEditGuruDialog(this, g);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                guruDAO.update(dlg.getGuru());
                loadGuru();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal edit guru: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHapusGuru() {
        int r = tblGuru.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        int id = Integer.parseInt(tblGuru.getValueAt(r, 0).toString());
        if (JOptionPane.showConfirmDialog(this, "Hapus guru ID " + id + "?") == JOptionPane.YES_OPTION) {
            try {
                if (guruDAO == null) guruDAO = new GuruDAO();
                guruDAO.delete(id);
                loadGuru();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal hapus guru: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------- CRUD Supplier -----------------
    private void onTambahSupplier() {
        AddEditSupplierDialog dlg = new AddEditSupplierDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            try {
                if (supplierDAO == null) supplierDAO = new SupplierDAO();
                int newId = supplierDAO.insert(dlg.getSupplier());
                if (newId > 0) {
                    JOptionPane.showMessageDialog(this, "Supplier ditambahkan (id=" + newId + ")");
                    loadSupplier();
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal menambahkan supplier.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal tambah supplier: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditSupplier() {
        int r = tblSupplier.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        try {
            int id = Integer.parseInt(tblSupplier.getValueAt(r, 0).toString());
            if (supplierDAO == null) supplierDAO = new SupplierDAO();
            Supplier s = supplierDAO.findById(id);
            if (s == null) { JOptionPane.showMessageDialog(this, "Supplier tidak ditemukan"); return; }
            AddEditSupplierDialog dlg = new AddEditSupplierDialog(this, s);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                supplierDAO.update(dlg.getSupplier());
                loadSupplier();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal edit supplier: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHapusSupplier() {
        int r = tblSupplier.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        int id = Integer.parseInt(tblSupplier.getValueAt(r, 0).toString());
        if (JOptionPane.showConfirmDialog(this, "Hapus supplier ID " + id + "?") == JOptionPane.YES_OPTION) {
            try {
                if (supplierDAO == null) supplierDAO = new SupplierDAO();
                supplierDAO.delete(id);
                loadSupplier();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal hapus supplier: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------- CRUD Detail Barang -----------------
    private void onTambahDetail() {
        AddEditDetailDialog dlg = new AddEditDetailDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            try {
                DetailBarang d = dlg.getDetailBarang();
                int newId = detailDao.insert(d);
                if (newId > 0) {
                    JOptionPane.showMessageDialog(this, "Detail barang ditambahkan (id=" + newId + ")");
                    loadDetailBarang();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal tambah detail: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditDetail() {
        int r = tblDetail.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        try {
            DetailBarang d = detailDao.findById(Integer.parseInt(tblDetail.getValueAt(r, 0).toString()));
            if (d == null) {
                JOptionPane.showMessageDialog(this, "Data tidak ditemukan", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            AddEditDetailDialog dlg = new AddEditDetailDialog(this, d);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                detailDao.update(dlg.getDetailBarang());
                loadDetailBarang();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal edit detail: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHapusDetail() {
        int r = tblDetail.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris"); return; }
        int id = Integer.parseInt(tblDetail.getValueAt(r, 0).toString());
        if (JOptionPane.showConfirmDialog(this, "Hapus detail ID " + id + "?") == JOptionPane.YES_OPTION) {
            try {
                detailDao.delete(id);
                loadDetailBarang();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal hapus detail: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------- CRUD Pembelian -----------------
    private void onTambahPembelian() {
        AddEditPembelianDialog dlg = new AddEditPembelianDialog(this);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            try {
                Pembelian p = dlg.getPembelian();
                if (pembelianDAO == null) pembelianDAO = new PembelianDAO();
                pembelianDAO.insertPembelianWithDetails(p);
                JOptionPane.showMessageDialog(this, "Pembelian tersimpan.");
                loadPembelian();
                loadDetailBarang();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal simpan pembelian: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onViewPembelian() {
        int r = tblPembelian.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih pembelian terlebih dahulu"); return; }
        String id = tblPembelian.getValueAt(r, 0).toString();
        try {
            if (pembelianDAO == null) pembelianDAO = new PembelianDAO();
            // ambil header via findAllPembelian() dan cari yang cocok (simple)
            Pembelian header = null;
            List<Pembelian> all = pembelianDAO.findAllPembelian();
            for (Pembelian p : all) if (id.equals(p.getIdPembelian())) { header = p; break; }

            List<DetailPembelian> details = pembelianDAO.findDetailsByPembelian(id);

            StringBuilder sb = new StringBuilder();
            if (header != null) {
                sb.append("ID: ").append(header.getIdPembelian()).append("\n");
                sb.append("Tgl: ").append(header.getTglPembelian()).append("\n");
                sb.append("Total: ").append(header.getTotalHarga()).append("\n\n");
            } else {
                sb.append("ID: ").append(id).append("\n\n");
            }

            sb.append("DETAIL:\n");
            for (DetailPembelian d : details) {
                sb.append("- Barang ID=").append( d.getIdBarang() == null ? "-" : d.getIdBarang() )
                  .append(" | Harga=").append(d.getHargaBeli())
                  .append(" | Stok=").append(d.getStok())
                  .append(" | Subtotal=").append(d.getSubtotal())
                  .append(" | Supplier=").append(d.getIdSupplier() == null ? "-" : d.getIdSupplier())
                  .append("\n");
            }

            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setColumns(50);
            ta.setRows(Math.min(20, details.size()+6));
            JScrollPane sp = new JScrollPane(ta);
            JOptionPane.showMessageDialog(this, sp, "Detail Pembelian", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal baca pembelian: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------- CRUD Voucher -----------------
    private void onTambahVoucher() {
        AddEditVoucherDialog dlg = new AddEditVoucherDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            try {
                Voucher v = dlg.getVoucher();
                DatabaseHelper.insertVoucher((v)); // wrapper helper if needed
                loadVouchers();
            } catch (Exception ex) {
                // Jika DatabaseHelper.insertVoucher tidak ada, fallback ke VoucherDAO jika tersedia
                try {
                    new VoucherDAO().insert(dlg.getVoucher());
                    loadVouchers();
                } catch (Exception e2) {
                    JOptionPane.showMessageDialog(this, "Gagal tambah voucher: " + e2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void onEditVoucher() {
        int r = tblVoucher.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih baris dulu.");
            return;
        }

        try {
            int id = Integer.parseInt(tblVoucher.getValueAt(r, 0).toString());
            Voucher v = null;
            // ambil voucher via DatabaseHelper
            for (Voucher vv : DatabaseHelper.getAllVouchers()) if (vv.getIdVoucher() == id) { v = vv; break; }

            if (v == null) {
                // fallback ke VoucherDAO jika tersedia
                v = new VoucherDAO().findById(id);
            }

            if (v == null) {
                JOptionPane.showMessageDialog(this, "Voucher tidak ditemukan.");
                return;
            }

            AddEditVoucherDialog dlg = new AddEditVoucherDialog(this, v);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                // update via DatabaseHelper atau VoucherDAO
                try {
                    DatabaseHelper.updateVoucherBalance(dlg.getVoucher().getIdVoucher(), dlg.getVoucher().getCurrentBalance());
                } catch (Exception ex) {
                    // fallback
                    new VoucherDAO().updateBalance(dlg.getVoucher().getIdVoucher(), dlg.getVoucher().getCurrentBalance());
                }
                JOptionPane.showMessageDialog(this, "Voucher diperbarui.");
                loadVouchers();
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "ID voucher tidak valid.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal edit voucher: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHapusVoucher() {
        int r = tblVoucher.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih baris dulu.");
            return;
        }

        int id = Integer.parseInt(tblVoucher.getValueAt(r, 0).toString());
        if (JOptionPane.showConfirmDialog(this, "Hapus voucher ID " + id + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {
            try {
                // coba DatabaseHelper.deleteVoucher() kalau ada, kalau tidak pakai VoucherDAO
                try {
                    DatabaseHelper.deleteVoucher(id);
                } catch (Throwable ignore) {
                    new VoucherDAO().delete(id);
                }
                loadVouchers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal hapus voucher: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
