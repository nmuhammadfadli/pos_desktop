/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import javax.swing.*;
import java.awt.*;

public class AddEditSupplierDialog extends JDialog {
    private JTextField txtNama = new JTextField(30);
    private JTextField txtAlamat = new JTextField(40);
    private JTextField txtNotelp = new JTextField(20);

    private boolean saved = false;
    private Supplier supplier;
    private boolean editMode = false;

    public AddEditSupplierDialog(Frame parent, Supplier s) {
        super(parent, true);
        this.supplier = (s == null) ? new Supplier() : s;
        this.editMode = (s != null);
        setTitle(editMode ? "Edit Supplier" : "Tambah Supplier");
        initComponents();
        if (editMode) {
            txtNama.setText(this.supplier.getNamaSupplier());
            txtAlamat.setText(this.supplier.getAlamatSupplier());
            txtNotelp.setText(this.supplier.getNotelpSupplier());
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

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Nama Supplier:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(txtNama, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Alamat:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(txtAlamat, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("No. Telp:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(txtNotelp, gbc);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bSave = new JButton("Simpan");
        JButton bCancel = new JButton("Batal");
        bSave.addActionListener(e -> onSave());
        bCancel.addActionListener(e -> onCancel());
        btns.add(bSave);
        btns.add(bCancel);
        add(btns, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().setDefaultButton(bSave);
    }

    private void onSave() {
        String nama = txtNama.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama supplier harus diisi", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        supplier.setNamaSupplier(nama);
        supplier.setAlamatSupplier(txtAlamat.getText().trim());
        supplier.setNotelpSupplier(txtNotelp.getText().trim());
        saved = true;
        setVisible(false);
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public boolean isSaved() { return saved; }
    public Supplier getSupplier() { return supplier; }
}
