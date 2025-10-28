/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Pembelian;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SimpleDocListener implements DocumentListener {
    private final Runnable r;
    public SimpleDocListener(Runnable r) { this.r = r; }
    public void insertUpdate(DocumentEvent e) { r.run(); }
    public void removeUpdate(DocumentEvent e) { r.run(); }
    public void changedUpdate(DocumentEvent e) { r.run(); }
}

