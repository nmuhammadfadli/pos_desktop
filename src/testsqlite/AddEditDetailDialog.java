package testsqlite;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dialog tambah / edit DetailBarang (UI untuk memasukkan barcode, stok, harga jual, tanggal exp,
 * memilih barang master, memilih supplier, dan optional id_detail_pembelian).
 */
public class AddEditDetailDialog extends JDialog {
    private JTextField txtBarcode = new JTextField(20);
    private JTextField txtStok = new JTextField(8);
    private JTextField txtHarga = new JTextField(12);
    private JTextField txtTanggalExp = new JTextField(12);
    private JComboBox<String> cbBarang = new JComboBox<>();
    private JComboBox<String> cbSupplier = new JComboBox<>();
    private JTextField txtIdDetailPembelian = new JTextField(8); // optional

    private DetailBarang detail;
    private boolean saved = false;

    // mapping index -> id_barang / id_supplier
    private int[] barangIds = new int[0];
    private int[] supplierIds = new int[0];

    public AddEditDetailDialog(Frame parent, DetailBarang d) {
        super(parent, true);
        this.detail = (d == null) ? new DetailBarang() : d;
        setTitle(d == null ? "Tambah Detail Barang" : "Edit Detail Barang");
        initComponents();
        loadBarangList(); // isi combo barang
        loadSupplierList(); // isi combo supplier
        if (d != null) fillForm(d);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        gbc.anchor = GridBagConstraints.EAST;

        // Row 0: Barang
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Barang (master):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        form.add(cbBarang, gbc);

        // Row 1: Supplier
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Supplier (optional):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(cbSupplier, gbc);

        // Row 2: Barcode
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Barcode:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtBarcode, gbc);

        // Row 3: Stok
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Stok:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtStok, gbc);

        // Row 4: Harga Jual
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Harga Jual:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtHarga, gbc);

        // Row 5: Tanggal Exp
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Tanggal Exp (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtTanggalExp, gbc);

        // Row 6: optional id_detail_pembelian
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("ID Detail Pembelian (optional):"), gbc);
        gbc.gridx = 1; gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtIdDetailPembelian, gbc);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> onCancel());
        btns.add(bSave);
        btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);

        // register ESC untuk batal
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().setDefaultButton(bSave);
    }

    private void loadBarangList() {
        try {
            List<Barang> list = new BarangDAO().findAll();
            cbBarang.removeAllItems();
            barangIds = new int[list.size()];
            int idx = 0;
            int selectedIndex = -1;
            for (Barang b : list) {
                cbBarang.addItem(b.getNama() + " (id=" + b.getId() + ")");
                barangIds[idx] = b.getId();
                if (detail != null && detail.getIdBarang() == b.getId()) selectedIndex = idx;
                idx++;
            }
            if (selectedIndex >= 0) cbBarang.setSelectedIndex(selectedIndex);
            else if (cbBarang.getItemCount() > 0) cbBarang.setSelectedIndex(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load daftar barang: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSupplierList() {
        try {
            cbSupplier.removeAllItems();
            // first option = (kosong)
            cbSupplier.addItem("(kosong)");
            java.util.List<Supplier> list = new SupplierDAO().findAll();
            supplierIds = new int[list.size() + 1];
            supplierIds[0] = -1; // index 0 -> null
            int idx = 1;
            int selectedIndex = 0;
            for (Supplier s : list) {
                cbSupplier.addItem(s.getNamaSupplier() + " (id=" + s.getIdSupplier() + ")");
                supplierIds[idx] = s.getIdSupplier();
                if (detail != null && detail.getIdSupplier() != null && detail.getIdSupplier().equals(s.getIdSupplier())) selectedIndex = idx;
                idx++;
            }
            cbSupplier.setSelectedIndex(selectedIndex);
        } catch (Exception ex) {
            // jika gagal load supplier, tetap biarkan combo berisi (kosong)
            cbSupplier.removeAllItems();
            cbSupplier.addItem("(kosong)");
            supplierIds = new int[]{ -1 };
        }
    }

    private void fillForm(DetailBarang d) {
        txtBarcode.setText(d.getBarcode() == null ? "" : d.getBarcode());
        txtStok.setText(String.valueOf(d.getStok()));
        txtHarga.setText(d.getHargaJual() == null ? "0" : d.getHargaJual().toPlainString());
        txtTanggalExp.setText(d.getTanggalExp() == null ? "" : d.getTanggalExp());
        txtIdDetailPembelian.setText(d.getIdDetailPembelian() == null ? "" : String.valueOf(d.getIdDetailPembelian()));

        // pilih item barang sesuai idBarang jika tersedia
        if (d.getIdBarang() > 0 && barangIds != null) {
            for (int i = 0; i < barangIds.length; i++) {
                if (barangIds[i] == d.getIdBarang()) {
                    cbBarang.setSelectedIndex(i);
                    break;
                }
            }
        }

        // pilih supplier jika ada
        if (d.getIdSupplier() != null && supplierIds != null) {
            for (int i = 0; i < supplierIds.length; i++) {
                if (supplierIds[i] == d.getIdSupplier()) {
                    cbSupplier.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void onSave() {
        try {
            if (cbBarang.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this, "Belum ada barang master. Tambah barang terlebih dahulu.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String barcode = txtBarcode.getText().trim();
            if (barcode.isEmpty()) barcode = null;

            String stokStr = txtStok.getText().trim();
            int stok = Integer.parseInt(stokStr);

            String hargaStr = txtHarga.getText().trim().replace(",", "");
            BigDecimal harga = new BigDecimal(hargaStr);

            String tExp = txtTanggalExp.getText().trim();
            int selected = cbBarang.getSelectedIndex();
            int idBarang = (barangIds.length > 0 && selected >= 0 && selected < barangIds.length) ? barangIds[selected] : 0;

            // supplier selection
            Integer idSupplier = null;
            int selSup = cbSupplier.getSelectedIndex();
            if (selSup >= 0 && selSup < supplierIds.length) {
                int sid = supplierIds[selSup];
                if (sid != -1) idSupplier = sid;
            }

            // optional id_detail_pembelian
            Integer idDetailPembelian = null;
            String idDetStr = txtIdDetailPembelian.getText().trim();
            if (!idDetStr.isEmpty()) {
                try {
                    idDetailPembelian = Integer.parseInt(idDetStr);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "ID Detail Pembelian harus angka atau dikosongkan.", "Input error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            detail.setBarcode(barcode);
            detail.setStok(stok);
            detail.setHargaJual(harga);
            detail.setTanggalExp(tExp.isEmpty() ? null : tExp);
            detail.setIdBarang(idBarang);
            detail.setIdSupplier(idSupplier);
            detail.setIdDetailPembelian(idDetailPembelian);

            saved = true;
            setVisible(false);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Stok dan Harga harus angka valid.", "Input error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal simpan: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() { saved = false; setVisible(false); }

    public boolean isSaved() { return saved; }
    public DetailBarang getDetailBarang() { return detail; }
}
