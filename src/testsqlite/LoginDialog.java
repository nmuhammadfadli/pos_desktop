package testsqlite;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author COMPUTER
 */

import Pengguna.Pengguna;
import Pengguna.PenggunaDAO;
import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private JTextField txtUsername = new JTextField(20);
    private JPasswordField txtPassword = new JPasswordField(20);
    private boolean succeeded = false;
    private Pengguna loggedUser = null;

    public LoginDialog(Frame parent) {
        super(parent, "Login", true);
        initUI(parent);
    }

    private void initUI(Frame parent) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; panel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; panel.add(txtPassword, gbc);

        // buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLogin = new JButton("Login");
        JButton btnCancel = new JButton("Batal");
        btns.add(btnLogin);
        btns.add(btnCancel);

        btnLogin.addActionListener(e -> doLogin());
       btnCancel.addActionListener(e -> {
            succeeded = false;
            loggedUser = null;
            txtPassword.setText("");
            dispose();
        });

        getRootPane().setDefaultButton(btnLogin);

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(btns, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan username", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            PenggunaDAO dao = new PenggunaDAO();
            Pengguna p = dao.findByUsername(username);
            if (p == null) {
                JOptionPane.showMessageDialog(this, "Username tidak ditemukan", "Login gagal", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // plain-text compare (sesuaikan kalau ingin hashing)
            String stored = p.getPassword() == null ? "" : p.getPassword();
            if (!stored.equals(password)) {
                JOptionPane.showMessageDialog(this, "Password salah", "Login gagal", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // sukses
        succeeded = true;
        loggedUser = p;
        dispose(); //
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal cek kredensial: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public boolean isSucceeded() { return succeeded; }
    public Pengguna getLoggedUser() { return loggedUser; }
}
