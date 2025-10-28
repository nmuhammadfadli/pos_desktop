package Pembelian;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import testsqlite.DatabaseHelper;

/**
 * Dialog tambah / edit Pembelian (header + daftar detail pembelian)
 * Disesuaikan dengan model Pembelian / DetailPembelian yang menggunakan Integer untuk harga/stok/subtotal.
 */
public class AddEditPembelianDialog extends JDialog {
    private final Frame parentFrame;

    private JTextField txtIdPembelian = new JTextField(20);
    private JTextField txtTglPembelian = new JTextField(12); // user can supply date string (yyyy-MM-dd)
    private JComboBox<String> cbPaymentMethod; // NEW
    private DefaultTableModel detailModel;
    private JTable tblDetails;

    private boolean saved = false;
    private Pembelian pembelian = new Pembelian();

    public AddEditPembelianDialog(Frame parent) {
        super(parent, true);
        this.parentFrame = parent;
        setTitle("Tambah / Edit Pembelian");
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        // --- header (ID + tanggal + payment method) ---
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0; gbc.gridy = 0;
        top.add(new JLabel("ID Pembelian:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        top.add(txtIdPembelian, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        top.add(new JLabel("Tanggal (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtTglPembelian.setText(java.time.LocalDate.now().toString());
        top.add(txtTglPembelian, gbc);

        // Payment method row (NEW)
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        top.add(new JLabel("Payment Method:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        String[] methods = new String[] {"CASH", "TRANSFER", "CREDIT"};
        cbPaymentMethod = new JComboBox<>(methods);
        cbPaymentMethod.setSelectedItem("CASH");
        top.add(cbPaymentMethod, gbc);

        add(top, BorderLayout.NORTH);

        // --- detail table (non-editable) ---
        detailModel = new DefaultTableModel(new Object[]{"ID Barang","Harga Beli","Stok","Subtotal","Supplier ID"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblDetails = new JTable(detailModel);
        tblDetails.setFillsViewportHeight(true);
        add(new JScrollPane(tblDetails), BorderLayout.CENTER);

        // --- left buttons for detail management ---
        JPanel leftBtns = new JPanel(new GridBagLayout());
        GridBagConstraints lb = new GridBagConstraints();
        lb.insets = new Insets(4,4,4,4);
        lb.gridx = 0; lb.gridy = 0; lb.fill = GridBagConstraints.HORIZONTAL;
        JButton bAdd = new JButton("Tambah Detail"); bAdd.addActionListener(e -> onAddDetail());
        JButton bEdit = new JButton("Edit Detail"); bEdit.addActionListener(e -> onEditDetail());
        JButton bDel = new JButton("Hapus Detail"); bDel.addActionListener(e -> onDeleteDetail());
        leftBtns.add(bAdd, lb);
        lb.gridy++; leftBtns.add(bEdit, lb);
        lb.gridy++; leftBtns.add(bDel, lb);
        add(leftBtns, BorderLayout.WEST);

        // --- bottom buttons ---
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan Pembelian");
        JButton bCancel = new JButton("Batal");
        bottom.add(bSave);
        bottom.add(bCancel);
        add(bottom, BorderLayout.SOUTH);

        // actions
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> onCancel());

        // register ESC to cancel
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // default button
        getRootPane().setDefaultButton(bSave);

        // ensure pembelian.details initialized
        if (pembelian.getDetails() == null) {
            pembelian.setDetails(new ArrayList<DetailPembelian>());
        }
        refreshDetailTable();
        txtIdPembelian.setText(generatePembelianId());
    }

    private void onAddDetail() {
        AddEditDetailPembelianDialog dlg = new AddEditDetailPembelianDialog(parentFrame, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            DetailPembelian d = dlg.getDetail();
            if (pembelian.getDetails() == null) pembelian.setDetails(new ArrayList<DetailPembelian>());
            pembelian.getDetails().add(d);
            refreshDetailTable();
        }
    }

    private void onEditDetail() {
        int r = tblDetails.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih baris detail terlebih dahulu", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DetailPembelian d = pembelian.getDetails().get(r);
        AddEditDetailPembelianDialog dlg = new AddEditDetailPembelianDialog(parentFrame, d);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            DetailPembelian nd = dlg.getDetail();
            pembelian.getDetails().set(r, nd);
            refreshDetailTable();
        }
    }

    private void onDeleteDetail() {
        int r = tblDetails.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih baris detail terlebih dahulu", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Hapus baris?", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            pembelian.getDetails().remove(r);
            refreshDetailTable();
        }
    }

    private void refreshDetailTable() {
        detailModel.setRowCount(0);
        if (pembelian.getDetails() == null) return;
        for (DetailPembelian d : pembelian.getDetails()) {
            Object idBarang = d.getIdBarang() == null ? "-" : d.getIdBarang();
            Integer harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli();
            Integer stok = d.getStok() == null ? 0 : d.getStok();
            Integer subtotal = d.getSubtotal() == null ? 0 : d.getSubtotal();
            // jika subtotal belum diisi, hitung dari harga * stok
            if ((subtotal == null || subtotal == 0) && harga > 0 && stok > 0) {
                long calc = (long) harga * (long) stok;
                // capping ke Integer jika overflow (sangat besar) — konversi aman
                subtotal = (int) Math.min(calc, Integer.MAX_VALUE);
            }
            Object idSupplier = d.getIdSupplier() == null ? "-" : d.getIdSupplier();
            detailModel.addRow(new Object[]{
                    idBarang,
                    harga.toString(),
                    stok,
                    subtotal.toString(),
                    idSupplier
            });
        }
    }

private void onSave() {
    JButton defaultSaveBtn = (JButton) getRootPane().getDefaultButton(); // get the default Save button
    if (defaultSaveBtn != null) defaultSaveBtn.setEnabled(false); // disable to avoid double submit

    try {
        String id = txtIdPembelian.getText().trim();
        String tgl = txtTglPembelian.getText().trim();
        if (tgl.isEmpty()) { JOptionPane.showMessageDialog(this, "Tanggal harus diisi"); return; }
        if (pembelian.getDetails() == null || pembelian.getDetails().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tambahkan minimal 1 detail"); return;
        }

        // validate date
        try { java.time.LocalDate.parse(tgl); } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format tanggal harus yyyy-MM-dd", "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // if id empty -> generate
        if (id.isEmpty()) {
            id = generatePembelianId();
            txtIdPembelian.setText(id);
        }

        // set fields to model
        pembelian.setIdPembelian(id);
        pembelian.setTglPembelian(tgl);
        String selMethod = (String) cbPaymentMethod.getSelectedItem();
        pembelian.setPaymentMethod(selMethod == null ? "CASH" : selMethod);

        // compute total
        long totalLong = 0L;
        for (DetailPembelian d : pembelian.getDetails()) {
            Integer harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli();
            Integer stok = d.getStok() == null ? 0 : d.getStok();
            Integer subtotal = d.getSubtotal() == null ? 0 : d.getSubtotal();
            if (subtotal == 0 && harga > 0 && stok > 0) {
                long calc = (long) harga * (long) stok;
                subtotal = (int) Math.min(calc, Integer.MAX_VALUE);
                d.setSubtotal(subtotal);
            }
            totalLong += subtotal;
            if (totalLong > Integer.MAX_VALUE) totalLong = Integer.MAX_VALUE;
        }
        pembelian.setTotalHarga((int) totalLong);

        // if reached here and inserted true:
        saved = true;
        setVisible(false);
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Gagal menyimpan pembelian: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    } finally {
        if (defaultSaveBtn != null) defaultSaveBtn.setEnabled(true); // re-enable
    }
}


private String generatePembelianId() {
    String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
    int nextNumber = 1;

    // ambil ID terakhir dari tabel pembelian yang sesuai tanggal hari ini
    try (java.sql.Connection conn = DatabaseHelper.getConnection();
         java.sql.PreparedStatement ps = conn.prepareStatement(
                 "SELECT id_pembelian FROM data_pembelian WHERE id_pembelian LIKE ? ORDER BY id_pembelian DESC LIMIT 1")) {
        ps.setString(1, today + "%");
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String lastId = rs.getString(1); // contoh: 221020250005
                String seqStr = lastId.substring(8); // ambil 0005
                try {
                    nextNumber = Integer.parseInt(seqStr) + 1;
                } catch (NumberFormatException ignored) {}
            }
        }
    } catch (Exception e) {
        System.err.println("Gagal cek ID terakhir: " + e.getMessage());
    }

    // format ke 4 digit (misal 0001)
    String newSeq = String.format("%04d", nextNumber);
    return today + newSeq;
}


    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Pembelian getPembelian() { return pembelian; }
}
