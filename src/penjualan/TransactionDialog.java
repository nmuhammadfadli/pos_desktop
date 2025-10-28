package penjualan;

import Barang.DetailBarang;
import Barang.DetailBarangDAO;
import testsqlite.DatabaseHelper;
import Voucher.Voucher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * TransactionDialog - improved layout and picker (consistent weights)
 *
 * - "Pilih Barang" opens a modal dialog with a JTable (searchable)
 * - Cart columns: [id_detail (hidden), Kode Barang, Nama Barang, Harga, Jumlah, Subtotal]
 * - If same id_detail is added again, qty is merged (and validated against stock)
 * - Total / Kembalian update automatically
 */
public class TransactionDialog extends JDialog {
    private final String currentUserId;
    private final String currentUserName;

    // Top
    private JTextField txtKodeTransaksi;
    private JLabel lblKasir;
    private JLabel lblTanggal;

    // Left form
    private JTextField txtBarcode = new JTextField();
    private JTextField txtNamaBarang = new JTextField();
    private JButton bPilihBarang = new JButton("Pilih...");
    private JTextField txtHarga = new JTextField();
    private JTextField txtJumlah = new JTextField();
    private JTextField txtSubtotal = new JTextField();
    private JButton bTambah = new JButton("Tambah");

    // Cart (right)
    private DefaultTableModel cartModel;
    private JTable tblCart;

    // Controls
    private JButton bReset = new JButton("Reset");
    private JButton bHapus = new JButton("Hapus");
    private JLabel lblTotalValue = new JLabel("0");
    private JTextField txtUangBayar = new JTextField("0", 12);
    private JLabel lblKembalian = new JLabel("0");
    private JButton bBayar = new JButton("Bayar");
    private JButton bCetak = new JButton("Cetak");

    // Voucher
    private JComboBox<String> cbVoucher;
    private Voucher[] vouchers = new Voucher[0];

    // DAOs / helpers
    private DetailBarangDAO detailDao = new DetailBarangDAO();
    private TransactionDAO txDao = new TransactionDAO();

    // Selected detail from picker
    private DetailBarang selectedDetail = null;

    public TransactionDialog(Frame parent, String currentUserId, String currentUserName) {
        super(parent, true);
        this.currentUserId = currentUserId;
        this.currentUserName = currentUserName;

        setTitle("Transaksi");
        initUI();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));
        setPreferredSize(new Dimension(920, 560));

        // TOP: kode, kasir, tanggal
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints tgb = new GridBagConstraints();
        tgb.insets = new Insets(6, 10, 6, 10);
        tgb.anchor = GridBagConstraints.WEST;

        // Make top row expand nicely
        tgb.gridx = 0; tgb.gridy = 0;
        top.add(new JLabel("Kode Transaksi:"), tgb);
        tgb.gridx = 1; tgb.weightx = 0.6; tgb.fill = GridBagConstraints.HORIZONTAL;
        txtKodeTransaksi = new JTextField();
        txtKodeTransaksi.setEditable(false);
        txtKodeTransaksi.setText(generateTransactionCode());
        top.add(txtKodeTransaksi, tgb);

        tgb.gridx = 2; tgb.weightx = 0; tgb.fill = GridBagConstraints.NONE;
        top.add(new JLabel("Kasir:"), tgb);
        tgb.gridx = 3; tgb.weightx = 0.4; tgb.fill = GridBagConstraints.HORIZONTAL;
        lblKasir = new JLabel(currentUserName == null ? "-" : currentUserName);
        top.add(lblKasir, tgb);

        tgb.gridx = 0; tgb.gridy = 1; tgb.weightx = 0; tgb.fill = GridBagConstraints.NONE;
        top.add(new JLabel("Tanggal:"), tgb);
        tgb.gridx = 1; tgb.weightx = 1.0; tgb.fill = GridBagConstraints.HORIZONTAL;
        lblTanggal = new JLabel(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        top.add(lblTanggal, tgb);

        add(top, BorderLayout.NORTH);

        // CENTER: left form + right cart
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // LEFT: form panel with titled border
        JPanel left = new JPanel(new GridBagLayout());
        left.setBorder(BorderFactory.createTitledBorder("Input Item"));
        GridBagConstraints lf = new GridBagConstraints();
        lf.insets = new Insets(6,6,6,6);
        lf.anchor = GridBagConstraints.WEST;

        // Layout pattern: label at gridx=0 (weightx=0), field at gridx=1 (weightx=1)
        // Row: Barcode
        lf.gridx = 0; lf.gridy = 0; lf.weightx = 0; lf.fill = GridBagConstraints.NONE;
        left.add(new JLabel("Barcode"), lf);
        lf.gridx = 1; lf.weightx = 1.0; lf.fill = GridBagConstraints.HORIZONTAL;
        txtBarcode.setPreferredSize(new Dimension(300, txtBarcode.getPreferredSize().height));
        left.add(txtBarcode, lf);

        // Row: Nama Barang (field + pilih button)
        lf.gridx = 0; lf.gridy = 1; lf.weightx = 0; lf.fill = GridBagConstraints.NONE;
        left.add(new JLabel("Nama Barang"), lf);
        lf.gridx = 1; lf.weightx = 1.0; lf.fill = GridBagConstraints.HORIZONTAL;
        JPanel namaRow = new JPanel(new BorderLayout(6,0));
        txtNamaBarang.setEditable(false);
        txtNamaBarang.setPreferredSize(new Dimension(300, txtNamaBarang.getPreferredSize().height));
        bPilihBarang.setPreferredSize(new Dimension(90, txtNamaBarang.getPreferredSize().height));
        namaRow.add(txtNamaBarang, BorderLayout.CENTER);
        namaRow.add(bPilihBarang, BorderLayout.EAST);
        left.add(namaRow, lf);

        // Row: Harga/unit
        lf.gridx = 0; lf.gridy = 2; lf.weightx = 0; lf.fill = GridBagConstraints.NONE;
        left.add(new JLabel("Harga/unit"), lf);
        lf.gridx = 1; lf.weightx = 1.0; lf.fill = GridBagConstraints.HORIZONTAL;
        txtHarga.setPreferredSize(new Dimension(300, txtHarga.getPreferredSize().height));
        left.add(txtHarga, lf);

        // Row: Jumlah
        lf.gridx = 0; lf.gridy = 3; lf.weightx = 0; lf.fill = GridBagConstraints.NONE;
        left.add(new JLabel("Jumlah"), lf);
        lf.gridx = 1; lf.weightx = 1.0; lf.fill = GridBagConstraints.HORIZONTAL;
        txtJumlah.setPreferredSize(new Dimension(120, txtJumlah.getPreferredSize().height));
        left.add(txtJumlah, lf);

        // Row: Subtotal
        lf.gridx = 0; lf.gridy = 4; lf.weightx = 0; lf.fill = GridBagConstraints.NONE;
        left.add(new JLabel("Subtotal"), lf);
        lf.gridx = 1; lf.weightx = 1.0; lf.fill = GridBagConstraints.HORIZONTAL;
        txtSubtotal.setEditable(false);
        txtSubtotal.setPreferredSize(new Dimension(200, txtSubtotal.getPreferredSize().height));
        left.add(txtSubtotal, lf);

        // Row: button add (aligned left of field)
        lf.gridx = 1; lf.gridy = 5; lf.weightx = 0; lf.fill = GridBagConstraints.NONE;
        bTambah.setPreferredSize(new Dimension(120, 32));
        left.add(bTambah, lf);

        // RIGHT: cart panel
        JPanel right = new JPanel(new BorderLayout(6,6));
        right.setBorder(BorderFactory.createTitledBorder("Keranjang"));

        // cart model & table
        cartModel = new DefaultTableModel(new Object[]{"ID Detail","Kode Barang","Nama Barang","Harga","Jumlah","Subtotal"}, 0) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tblCart = new JTable(cartModel);
        tblCart.setFillsViewportHeight(true);

        // Configure table column widths and hide ID Detail
        TableColumnModel cm = tblCart.getColumnModel();
        if (cm.getColumnCount() > 0) {
            cm.getColumn(0).setMinWidth(0);
            cm.getColumn(0).setMaxWidth(0);
            cm.getColumn(0).setPreferredWidth(0);
            try {
                cm.getColumn(1).setPreferredWidth(70);
                cm.getColumn(2).setPreferredWidth(220);
                cm.getColumn(3).setPreferredWidth(90);
                cm.getColumn(4).setPreferredWidth(60);
                cm.getColumn(5).setPreferredWidth(100);
            } catch (Throwable ignore) {}
        }

        JScrollPane spCart = new JScrollPane(tblCart);
        spCart.setPreferredSize(new Dimension(480, 320));
        right.add(spCart, BorderLayout.CENTER);

        // controls panel under cart
        JPanel rightSouth = new JPanel(new GridBagLayout());
        GridBagConstraints rs = new GridBagConstraints();
        rs.insets = new Insets(6,6,6,6);
        rs.fill = GridBagConstraints.HORIZONTAL;

        rs.gridx = 0; rs.gridy = 0; rs.gridwidth = 1;
        rightSouth.add(bReset, rs);
        rs.gridx = 1; rs.gridy = 0;
        rightSouth.add(bHapus, rs);

        rs.gridx = 0; rs.gridy = 1; rs.gridwidth = 2;
        cbVoucher = new JComboBox<>();
        loadVouchers();
        rightSouth.add(cbVoucher, rs);

        rs.gridx = 0; rs.gridy = 2; rs.gridwidth = 1;
        rightSouth.add(new JLabel("Harga Total:"), rs);
        rs.gridx = 1; rs.gridy = 2;
        lblTotalValue.setOpaque(true);
        lblTotalValue.setBackground(Color.LIGHT_GRAY);
        lblTotalValue.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
        rightSouth.add(lblTotalValue, rs);

        rs.gridx = 0; rs.gridy = 3;
        rightSouth.add(new JLabel("Uang Dibayar:"), rs);
        rs.gridx = 1; rs.gridy = 3;
        JPanel bayarRow = new JPanel(new BorderLayout(6,0));
        txtUangBayar.setPreferredSize(new Dimension(140, txtUangBayar.getPreferredSize().height));
        bayarRow.add(txtUangBayar, BorderLayout.CENTER);
        bayarRow.add(bBayar, BorderLayout.EAST);
        rightSouth.add(bayarRow, rs);

        rs.gridx = 0; rs.gridy = 4;
        rightSouth.add(new JLabel("Kembalian:"), rs);
        rs.gridx = 1; rs.gridy = 4;
        lblKembalian.setOpaque(true);
        lblKembalian.setBackground(Color.LIGHT_GRAY);
        lblKembalian.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
        rightSouth.add(lblKembalian, rs);

        rs.gridx = 0; rs.gridy = 5; rs.gridwidth = 2;
        bCetak.setPreferredSize(new Dimension(120, 32));
        rightSouth.add(bCetak, rs);

        right.add(rightSouth, BorderLayout.SOUTH);

        // Add left/right to center with weights so they expand nicely
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.42; gbc.weighty = 1.0;
        center.add(left, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.58; gbc.weighty = 1.0;
        center.add(right, gbc);

        add(center, BorderLayout.CENTER);

        // ACTIONS
        bPilihBarang.addActionListener(e -> openPilihBarangDialog());
        bTambah.addActionListener(e -> onTambah());
        bReset.addActionListener(e -> onResetCart());
        bHapus.addActionListener(e -> onHapusSelected());
        bBayar.addActionListener(e -> onBayar());
        bCetak.addActionListener(e -> onCetak());

        // recalc subtotal when price or qty change
        DocumentListener recalc = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcSubtotal(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcSubtotal(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcSubtotal(); }
        };
        txtJumlah.getDocument().addDocumentListener(recalc);
        txtHarga.getDocument().addDocumentListener(recalc);

        // recalc kembalian when bayar changed
        txtUangBayar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcKembalian(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcKembalian(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcKembalian(); }
        });

        // double-click cart edits qty quickly
        tblCart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int r = tblCart.getSelectedRow();
                    if (r >= 0) {
                        int modelRow = tblCart.convertRowIndexToModel(r);
                        editCartRowQty(modelRow);
                    }
                }
            }
        });
    }

    // ---------------- Picker dialog (table-based) ----------------
    private void openPilihBarangDialog() {
        try {
            List<DetailBarang> list = detailDao.findAll();
            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tidak ada item di stock.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Dialog
            JDialog dlg = new JDialog(this, "Pilih Barang", true);
            dlg.setLayout(new BorderLayout(8,8));

            // search field
            JPanel top = new JPanel(new BorderLayout(6,6));
            JTextField txtSearch = new JTextField();
            top.add(new JLabel("Cari (nama/barcode):"), BorderLayout.WEST);
            top.add(txtSearch, BorderLayout.CENTER);
            dlg.add(top, BorderLayout.NORTH);

            // table model
            DefaultTableModel pickModel = new DefaultTableModel(new Object[]{"ID Detail","Barcode","Nama Barang","Stok","Harga"}, 0) {
                @Override public boolean isCellEditable(int r,int c){ return false; }
            };
            for (DetailBarang d : list) {
                String nama = d.getNamaBarang() == null ? "-" : d.getNamaBarang();
                String harga = d.getHargaJual() == null ? "0" : d.getHargaJual().toPlainString();
                pickModel.addRow(new Object[]{ d.getId(), d.getBarcode() == null ? "-" : d.getBarcode(), nama, d.getStok(), harga});
            }

            JTable tPick = new JTable(pickModel);
            tPick.setFillsViewportHeight(true);
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(pickModel);
            tPick.setRowSorter(sorter);

            JScrollPane sp = new JScrollPane(tPick);
            sp.setPreferredSize(new Dimension(640, 320));
            dlg.add(sp, BorderLayout.CENTER);

            // buttons
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Cancel");
            btns.add(ok); btns.add(cancel);
            dlg.add(btns, BorderLayout.SOUTH);

            // filter logic
            txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                void filter() {
                    String q = txtSearch.getText().trim();
                    if (q.isEmpty()) {
                        sorter.setRowFilter(null);
                    } else {
                        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
                    }
                }
                @Override public void insertUpdate(DocumentEvent e) { filter(); }
                @Override public void removeUpdate(DocumentEvent e) { filter(); }
                @Override public void changedUpdate(DocumentEvent e) { filter(); }
            });

            // actions
            ok.addActionListener(ae -> {
                int sel = tPick.getSelectedRow();
                if (sel < 0) {
                    JOptionPane.showMessageDialog(dlg, "Pilih baris dulu.");
                    return;
                }
                int modelRow = tPick.convertRowIndexToModel(sel);
                int idDetail = (Integer) pickModel.getValueAt(modelRow, 0);
                // find selected detail object
                selectedDetail = null;
                for (DetailBarang d : list) {
                    if (d.getId() == idDetail) { selectedDetail = d; break; }
                }
                if (selectedDetail != null) {
                    txtBarcode.setText(selectedDetail.getBarcode() == null ? "" : selectedDetail.getBarcode());
                    txtNamaBarang.setText(selectedDetail.getNamaBarang() == null ? "" : selectedDetail.getNamaBarang());
                    txtHarga.setText(selectedDetail.getHargaJual() == null ? "0" : selectedDetail.getHargaJual().toPlainString());
                    txtJumlah.setText("1");
                    recalcSubtotal();
                }
                dlg.dispose();
            });

            cancel.addActionListener(ae -> dlg.dispose());

            dlg.pack();
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal buka picker: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Cart operations ----------------
   private void onTambah() {
    try {
        if (selectedDetail == null) {
            JOptionPane.showMessageDialog(this, "Pilih barang dulu (tombol Pilih).");
            return;
        }

        int stokAvailable = selectedDetail.getStok();
        int qty;
        try {
            qty = Integer.parseInt(txtJumlah.getText().trim());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Jumlah harus berupa angka bulat.", "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (qty <= 0) { JOptionPane.showMessageDialog(this, "Qty harus > 0"); return; }

        // check existing qty present in cart for same id_detail
        int idDetail = selectedDetail.getId();
        int existingQty = 0;
        int rowFound = -1;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            int rId = Integer.parseInt(cartModel.getValueAt(i, 0).toString());
            if (rId == idDetail) {
                existingQty = Integer.parseInt(cartModel.getValueAt(i, 4).toString()); // jumlah column index 4
                rowFound = i;
                break;
            }
        }
        if ((long) existingQty + qty > stokAvailable) {
            JOptionPane.showMessageDialog(this, "Total qty melewati stok tersedia (" + stokAvailable + ")", "Stok", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal harga = selectedDetail.getHargaJual() == null ? BigDecimal.ZERO : selectedDetail.getHargaJual();
        BigDecimal sub = harga.multiply(BigDecimal.valueOf(qty));

        if (rowFound >= 0) {
            // merge: update jumlah & subtotal
            int newQty = existingQty + qty;
            BigDecimal newSub = harga.multiply(BigDecimal.valueOf(newQty));
            cartModel.setValueAt(newQty, rowFound, 4);
            cartModel.setValueAt(newSub.toPlainString(), rowFound, 5);
        } else {
            // add new row: columns [idDetail, kodeBarang, namaBarang, harga, jumlah, subtotal]
            String kodeBarang = String.valueOf(selectedDetail.getIdBarang());
            String nama = selectedDetail.getNamaBarang() == null ? "-" : selectedDetail.getNamaBarang();
            cartModel.addRow(new Object[]{
                    selectedDetail.getId(),
                    kodeBarang,
                    nama,
                    harga.toPlainString(),
                    qty,
                    sub.toPlainString()
            });
        }

        updateTotal();

        // === NEW: clear selection & input fields after adding to cart ===
        selectedDetail = null;
        txtNamaBarang.setText("");
        txtHarga.setText("");
        txtJumlah.setText("");
        txtSubtotal.setText("");
        txtBarcode.requestFocusInWindow(); // fokus kembali ke barcode (opsional)
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Gagal tambah item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}


    private void editCartRowQty(int row) {
        try {
            int idDetail = Integer.parseInt(cartModel.getValueAt(row, 0).toString());
            int stok = 0;
            // find stok from DB
            DetailBarang d = detailDao.findById(idDetail);
            if (d != null) stok = d.getStok();

            String cur = cartModel.getValueAt(row, 4).toString();
            String s = JOptionPane.showInputDialog(this, "Ubah jumlah (stok tersedia: " + stok + "):", cur);
            if (s == null) return;
            int newQty = Integer.parseInt(s.trim());
            if (newQty <= 0) { JOptionPane.showMessageDialog(this, "Qty harus > 0"); return; }
            if (newQty > stok) { JOptionPane.showMessageDialog(this, "Qty melebihi stok"); return; }
            BigDecimal harga = new BigDecimal(cartModel.getValueAt(row, 3).toString());
            BigDecimal newSub = harga.multiply(BigDecimal.valueOf(newQty));
            cartModel.setValueAt(newQty, row, 4);
            cartModel.setValueAt(newSub.toPlainString(), row, 5);
            updateTotal();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Input tidak valid.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal ubah qty: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHapusSelected() {
        int r = tblCart.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih baris keranjang untuk dihapus.");
            return;
        }
        int modelRow = tblCart.convertRowIndexToModel(r);
        cartModel.removeRow(modelRow);
        updateTotal();
    }

    private void onResetCart() {
        if (JOptionPane.showConfirmDialog(this, "Reset semua item di keranjang?", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cartModel.setRowCount(0);
            updateTotal();
        }
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String sub = cartModel.getValueAt(i, 5).toString();
            total = total.add(new BigDecimal(sub));
        }
        lblTotalValue.setText(total.toPlainString());
        recalcKembalian();
    }

    // ---------------- Payment / finalize ----------------
    private void recalcSubtotal() {
        try {
            String hargaStr = txtHarga.getText().trim();
            String qtyStr = txtJumlah.getText().trim();
            if (hargaStr.isEmpty() || qtyStr.isEmpty()) { txtSubtotal.setText("0"); return; }
            BigDecimal harga = new BigDecimal(hargaStr.replace(",",""));
            int qty = Integer.parseInt(qtyStr);
            BigDecimal sub = harga.multiply(BigDecimal.valueOf(qty));
            txtSubtotal.setText(sub.toPlainString());
        } catch (Exception ex) {
            txtSubtotal.setText("0");
        }
    }

    private void recalcKembalian() {
        try {
            BigDecimal total = new BigDecimal(lblTotalValue.getText());
            BigDecimal bayar = new BigDecimal(txtUangBayar.getText().trim().replace(",",""));
            BigDecimal kembalian = bayar.subtract(total);
            if (kembalian.signum() < 0) lblKembalian.setText("0");
            else lblKembalian.setText(kembalian.toPlainString());
        } catch (Exception ex) {
            lblKembalian.setText("0");
        }
    }

    private void onBayar() {
        if (cartModel.getRowCount() == 0) { JOptionPane.showMessageDialog(this, "Keranjang kosong."); return; }

        // build items (SaleItem expects detail id, qty, price)
        List<SaleItem> items = new ArrayList<>();
        try {
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int idDetail = Integer.parseInt(cartModel.getValueAt(i, 0).toString());
                int qty = Integer.parseInt(cartModel.getValueAt(i, 4).toString());
                BigDecimal price = new BigDecimal(cartModel.getValueAt(i, 3).toString());
                items.add(new SaleItem(idDetail, qty, price));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Data keranjang rusak: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // voucher
        Integer voucherId = null;
        int idx = cbVoucher.getSelectedIndex();
        if (idx > 0) {
            int arrIndex = idx - 1;
            if (arrIndex >= 0 && arrIndex < vouchers.length && vouchers[arrIndex] != null) voucherId = vouchers[arrIndex].getIdVoucher();
        }

        // cash
        BigDecimal cashPaid;
        try {
            cashPaid = new BigDecimal(txtUangBayar.getText().trim().replace(",",""));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format uang bayar tidak valid.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String kodeTrans = txtKodeTransaksi.getText();

        try {
            txDao.processSale(items, voucherId, cashPaid, kodeTrans, currentUserId);
            JOptionPane.showMessageDialog(this, "Transaksi berhasil. Kode: " + kodeTrans, "Sukses", JOptionPane.INFORMATION_MESSAGE);

            // reset UI
            cartModel.setRowCount(0);
            updateTotal();
            txtUangBayar.setText("0");
            lblKembalian.setText("0");
            txtKodeTransaksi.setText(generateTransactionCode());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal memproses transaksi: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCetak() {
        JOptionPane.showMessageDialog(this, "Fungsi cetak belum diimplementasikan.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------- Load vouchers ----------------
    private void loadVouchers() {
        try {
            List<Voucher> list = DatabaseHelper.getAllVouchers();
            cbVoucher.removeAllItems();
            vouchers = new Voucher[list.size()];
            cbVoucher.addItem("- Tidak pakai voucher -");
            int i = 0;
            for (Voucher v : list) {
                vouchers[i] = v;
                String lbl = v.getKode() + " (saldo: " + (v.getCurrentBalance() == null ? "0" : v.getCurrentBalance().toPlainString()) + ")";
                cbVoucher.addItem(lbl);
                i++;
            }
        } catch (Exception ex) {
            cbVoucher.removeAllItems();
            cbVoucher.addItem("- Tidak pakai voucher -");
        }
    }

    // ---------------- transaction code generator ----------------
    private String generateTransactionCode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TRX" + today;
        int next = 1;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT kode_transaksi FROM transaksi_penjualan WHERE kode_transaksi LIKE ? ORDER BY kode_transaksi DESC LIMIT 1")) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String last = rs.getString(1);
                    if (last != null && last.length() > prefix.length()) {
                        String seq = last.substring(prefix.length());
                        try { next = Integer.parseInt(seq) + 1; } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception ex) {
            // fallback next = 1
        }
        return prefix + String.format("%04d", next);
    }
}
