package Pembelian;

import Supplier.SupplierDAO;
import Supplier.Supplier;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import Barang.Barang;
import Barang.BarangDAO;

/**
 * Dialog detail pembelian: input id barang, harga, stok, pilih supplier, tampilkan subtotal.
 * Ditambah tombol "Pilih Barang" yang membuka dialog pemilih barang dari database.
 *
 * Note: BarangDAO di projectmu hanya punya findAll(); implementasi ini memakai findAll()
 * untuk mencari nama/objek Barang ketika diperlukan.
 */
public class AddEditDetailPembelianDialog extends JDialog {
    private JTextField txtIdBarang = new JTextField(8);
    private JTextField txtNamaBarang = new JTextField(20);
    private JTextField txtHargaBeli = new JTextField(10);
    private JTextField txtStok = new JTextField(6);
    private JComboBox<Supplier> cbSupplier;
    private JLabel lblSubtotal = new JLabel("0");

    private boolean saved = false;
    private DetailPembelian detail;

    public AddEditDetailPembelianDialog(Frame parent, DetailPembelian d) {
        super(parent, true);
        this.detail = (d == null) ? new DetailPembelian() : d;
        setTitle(d == null ? "Tambah Detail Pembelian" : "Edit Detail Pembelian");
        initComponents();

        // setelah komponen dibuat, load supplier (combo) dan isi field bila edit
        loadSuppliersIntoCombo();

        if (d != null) {
            populateFromModel(d);
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        gbc.anchor = GridBagConstraints.EAST;

        // ID Barang + Pilih Button + Nama Barang
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("ID Barang:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(txtIdBarang, gbc);

        JButton btnPilih = new JButton("Pilih Barang...");
        btnPilih.addActionListener(e -> openBarangPicker());
        gbc.gridx = 2; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; form.add(btnPilih, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Nama Barang:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtNamaBarang.setEditable(false);
        form.add(txtNamaBarang, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0; // reset

        // Harga Beli
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Harga Beli:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtHargaBeli, gbc);

        // Stok
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Stok:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtStok, gbc);

        // Supplier
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;

        // create combo (will be filled later)
        cbSupplier = new JComboBox<>();
        cbSupplier.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Supplier) setText(((Supplier) value).getNamaSupplier());
                else if (value instanceof String) setText((String) value);
                return this;
            }
        });
        form.add(cbSupplier, gbc);

        // Subtotal
        gbc.gridx = 0; gbc.gridy = 5; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Subtotal:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(lblSubtotal, gbc);

        add(form, BorderLayout.CENTER);

        // buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> onCancel());
        btns.add(bSave); btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);

        // update subtotal when harga/stok changed
        txtHargaBeli.getDocument().addDocumentListener(new SimpleDocListener(() -> recalcSubtotal()));
        txtStok.getDocument().addDocumentListener(new SimpleDocListener(() -> recalcSubtotal()));

        // ESC to cancel
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().setDefaultButton(bSave);
    }

    private void loadSuppliersIntoCombo() {
        try {
            DefaultComboBoxModel<Supplier> model = new DefaultComboBoxModel<>();
            List<Supplier> list = new SupplierDAO().findAll();
            if (list.isEmpty()) {
                // keep an empty selection entry
                model.addElement(null);
            } else {
                for (Supplier s : list) model.addElement(s);
            }
            cbSupplier.setModel(model);
            if (cbSupplier.getItemCount() > 0) cbSupplier.setSelectedIndex(0);
        } catch (Exception ex) {
            cbSupplier.removeAllItems();
            cbSupplier.addItem(null);
        }
    }

    private void populateFromModel(DetailPembelian d) {
        if (d == null) return;
        if (d.getIdBarang() != null) txtIdBarang.setText(String.valueOf(d.getIdBarang()));
        if (d.getHargaBeli() != null) txtHargaBeli.setText(String.valueOf(d.getHargaBeli()));
        if (d.getStok() != null) txtStok.setText(String.valueOf(d.getStok()));
        lblSubtotal.setText(String.valueOf(d.getSubtotal() == null ? 0 : d.getSubtotal()));

        // try to fill barang name by scanning findAll results (fallback because no findById)
        if (d.getIdBarang() != null) {
            try {
                List<Barang> all = new BarangDAO().findAll();
                for (Barang b : all) {
                    if (b.getId() == d.getIdBarang()) {
                        txtNamaBarang.setText(b.getNama());
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        // select supplier object in combo if present
        if (d.getIdSupplier() != null) {
            for (int i = 0; i < cbSupplier.getItemCount(); i++) {
                Object itm = cbSupplier.getItemAt(i);
                if (itm instanceof Supplier && Objects.equals(((Supplier) itm).getIdSupplier(), d.getIdSupplier())) {
                    cbSupplier.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void openBarangPicker() {
        BarangPickerDialog dlg = new BarangPickerDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isSelected()) {
            Barang b = dlg.getSelectedBarang();
            if (b != null) {
                txtIdBarang.setText(String.valueOf(b.getId()));
                txtNamaBarang.setText(b.getNama());
            }
        }
    }

    private void recalcSubtotal() {
        try {
            int h = Integer.parseInt(txtHargaBeli.getText().trim());
            int s = Integer.parseInt(txtStok.getText().trim());
            long calc = (long) h * (long) s;
            if (calc > Integer.MAX_VALUE) calc = Integer.MAX_VALUE;
            lblSubtotal.setText(String.valueOf(calc));
        } catch (Exception ex) {
            lblSubtotal.setText("0");
        }
    }

    private void onSave() {
        try {
            int idBarang = Integer.parseInt(txtIdBarang.getText().trim());
            int harga = Integer.parseInt(txtHargaBeli.getText().trim());
            int stok = Integer.parseInt(txtStok.getText().trim());
            Object sel = cbSupplier.getSelectedItem();
            Integer idSupplier = null;
            if (sel instanceof Supplier) idSupplier = ((Supplier) sel).getIdSupplier();

            detail.setIdBarang(idBarang);
            detail.setHargaBeli(harga);
            detail.setStok(stok);
            detail.setIdSupplier(idSupplier);
            long calc = (long) harga * (long) stok;
            detail.setSubtotal((int) Math.min(calc, Integer.MAX_VALUE));

            saved = true;
            setVisible(false);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "ID barang, harga, dan stok harus angka valid.", "Input error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public DetailPembelian getDetail() { return detail; }

    // -------------------- inner dialog: BarangPickerDialog --------------------
    private static class BarangPickerDialog extends JDialog {
        private JTable tbl;
        private DefaultTableModel model;
        private Barang selected = null;

        public BarangPickerDialog(Frame parent) {
            super(parent, true);
            setTitle("Pilih Barang");
            init();
            pack();
            setLocationRelativeTo(parent);
        }

        private void init() {
            setLayout(new BorderLayout(8,8));
            String[] cols = new String[]{"ID","Nama","Kategori"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };
            tbl = new JTable(model);
            loadBarangData();
            add(new JScrollPane(tbl), BorderLayout.CENTER);

            JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton choose = new JButton("Pilih");
            JButton cancel = new JButton("Batal");
            choose.addActionListener(e -> onChoose());
            cancel.addActionListener(e -> { selected = null; setVisible(false); });
            b.add(choose); b.add(cancel);
            add(b, BorderLayout.SOUTH);
        }

        private void loadBarangData() {
            try {
                List<Barang> list = new BarangDAO().findAll();
                model.setRowCount(0);
                for (Barang br : list) {
                    model.addRow(new Object[]{ br.getId(), br.getNama(), br.getNamaKategori() });
                }
            } catch (Exception ex) {
                model.setRowCount(0);
            }
        }

        private void onChoose() {
            int r = tbl.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih baris barang terlebih dahulu"); return; }
            Object idObj = tbl.getValueAt(r, 0);
            Object namaObj = tbl.getValueAt(r, 1);
            int id = Integer.parseInt(String.valueOf(idObj));
            String nama = namaObj == null ? "" : String.valueOf(namaObj);

            // try to get full Barang from DAO.findAll()
            try {
                List<Barang> all = new BarangDAO().findAll();
                for (Barang br : all) {
                    if (br.getId() == id) {
                        selected = br;
                        break;
                    }
                }
            } catch (Exception ignored) {}

            // fallback to minimal Barang object if not found
            if (selected == null) {
                selected = new Barang();
                selected.setId(id);
                selected.setNama(nama);
            }

            setVisible(false);
        }

        public boolean isSelected() { return selected != null; }
        public Barang getSelectedBarang() { return selected; }
    }
}
