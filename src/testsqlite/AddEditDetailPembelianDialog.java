/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AddEditDetailPembelianDialog extends JDialog {
    private JTextField txtIdBarang = new JTextField(8);
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
        pack();
        setLocationRelativeTo(parent);

        if (d != null) {
            if (d.getIdBarang() != null) txtIdBarang.setText(String.valueOf(d.getIdBarang()));
            if (d.getHargaBeli() != null) txtHargaBeli.setText(String.valueOf(d.getHargaBeli()));
            if (d.getStok() != null) txtStok.setText(String.valueOf(d.getStok()));
            lblSubtotal.setText(String.valueOf(d.getSubtotal() == null ? 0 : d.getSubtotal()));
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("ID Barang:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(txtIdBarang, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Harga Beli:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtHargaBeli, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Stok:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtStok, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;

        // load suppliers into combo
        DefaultComboBoxModel<Supplier> model = new DefaultComboBoxModel<>();
        try {
            List<Supplier> list = new SupplierDAO().findAll();
            for (Supplier s : list) model.addElement(s);
        } catch (Exception ex) {
            // ignore, combo empty
        }
        cbSupplier = new JComboBox<>(model);
        cbSupplier.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Supplier) setText(((Supplier)value).getNamaSupplier());
                return this;
            }
        });
        form.add(cbSupplier, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Subtotal:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
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
    }

    private void recalcSubtotal() {
        try {
            int h = Integer.parseInt(txtHargaBeli.getText().trim());
            int s = Integer.parseInt(txtStok.getText().trim());
            lblSubtotal.setText(String.valueOf(h * s));
        } catch (Exception ex) {
            lblSubtotal.setText("0");
        }
    }

    private void onSave() {
        try {
            int idBarang = Integer.parseInt(txtIdBarang.getText().trim());
            int harga = Integer.parseInt(txtHargaBeli.getText().trim());
            int stok = Integer.parseInt(txtStok.getText().trim());
            Supplier sel = (Supplier) cbSupplier.getSelectedItem();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Pilih supplier"); return; }

            detail.setIdBarang(idBarang);
            detail.setHargaBeli(harga);
            detail.setStok(stok);
            detail.setIdSupplier(sel.getIdSupplier());
            detail.setSubtotal(harga * stok);

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
}
