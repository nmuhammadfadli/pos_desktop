/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testsqlite;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseHelper.initDatabase();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null,"Gagal inisialisasi DB: "+ex.getMessage());
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> {
           
            new MainFrame().setVisible(true);
        });
    }
}

