package testsqlite;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog transaksi interaktif:
 * - pilih detail_barang dari list
 * - masukkan qty, tambah ke cart
 * - pilih voucher (opsional)
 * - masukkan cash, proses transaksi
 */
public class TransactionDialog extends JDialog {
    private JTable tStock;
    private DefaultTableModel stockModel;

    private JTable tCart;
    private DefaultTableModel cartModel;

    private JComboBox<String> cbVoucher;
    private Voucher[] vouchers = new Voucher[0];

    private JTextField txtCash;
    private JLabel lblTotal;

    private DetailBarangDAO detailDao = new DetailBarangDAO();
    private TransactionDAO txDao = new TransactionDAO();

    public TransactionDialog(Frame parent) {
        super(parent, true);
        setTitle("Transaksi - Interaktif");
        setSize(800, 500);
        setLayout(new BorderLayout(6,6));

        // top: stock list
        stockModel = new DefaultTableModel(new Object[]{"ID","Barcode","Stok","Harga Jual","Id Barang"}, 0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        tStock = new JTable(stockModel);
        JScrollPane spStock = new JScrollPane(tStock);
        spStock.setPreferredSize(new Dimension(380,250));

        // cart table
        cartModel = new DefaultTableModel(new Object[]{"ID Detail","Qty","Harga Unit","Subtotal"},0) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tCart = new JTable(cartModel);
        JScrollPane spCart = new JScrollPane(tCart);
        spCart.setPreferredSize(new Dimension(380,250));

        // center layout: stock left, cart right, buttons middle-bottom
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        center.add(spStock);
        center.add(spCart);

        add(center, BorderLayout.CENTER);

        // controls below
        JPanel ctrl = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,6,4,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton bAdd = new JButton("Tambah ke Cart");
        bAdd.addActionListener(e-> onAddToCart());
        JButton bRemove = new JButton("Hapus dari Cart");
        bRemove.addActionListener(e-> onRemoveFromCart());

        // voucher combobox
        cbVoucher = new JComboBox<>();
        loadVouchers(); // isi vouchers & combo

        txtCash = new JTextField("0",10);
        lblTotal = new JLabel("Total: 0");

        JButton bProcess = new JButton("Proses Transaksi");
        bProcess.addActionListener(e-> onProcess());

        // arrange controls
        gbc.gridx=0; gbc.gridy=0; ctrl.add(new JLabel("Voucher (opsional):"), gbc);
        gbc.gridx=1; gbc.gridy=0; ctrl.add(cbVoucher, gbc);
        gbc.gridx=0; gbc.gridy=1; ctrl.add(new JLabel("Tunai (cash):"), gbc);
        gbc.gridx=1; gbc.gridy=1; ctrl.add(txtCash, gbc);
        gbc.gridx=0; gbc.gridy=2; ctrl.add(bAdd, gbc);
        gbc.gridx=1; gbc.gridy=2; ctrl.add(bRemove, gbc);
        gbc.gridx=0; gbc.gridy=3; ctrl.add(lblTotal, gbc);
        gbc.gridx=1; gbc.gridy=3; ctrl.add(bProcess, gbc);

        add(ctrl, BorderLayout.SOUTH);

        // load stock initially
        loadStock();

        setLocationRelativeTo(parent);
    }

    private void loadStock() {
        try {
            List<DetailBarang> list = detailDao.findAll();
            stockModel.setRowCount(0);
            for (DetailBarang d : list) {
                String harga = d.getHargaJual() == null ? "0" : d.getHargaJual().toPlainString();
                stockModel.addRow(new Object[]{ d.getId(), d.getBarcode(), d.getStok(), harga, d.getIdBarang() });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load stock: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadVouchers() {
        try {
            List<Voucher> list = DatabaseHelper.getAllVouchers();
            cbVoucher.removeAllItems();
            vouchers = new Voucher[list.size()];
            int i=0;
            cbVoucher.addItem("- Tidak pakai voucher -");
            for (Voucher v: list) {
                vouchers[i] = v;
                String lbl = "[" + v.getIdVoucher() + "] " + v.getKode() + " (saldo: " + (v.getCurrentBalance()==null?"0":v.getCurrentBalance().toPlainString()) + ")";
                cbVoucher.addItem(lbl);
                i++;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load voucher: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAddToCart() {
        int r = tStock.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Pilih detail barang dari daftar stock terlebih dahulu."); return; }
        try {
            int idDetail = Integer.parseInt(stockModel.getValueAt(r,0).toString());
            int stokAvailable = Integer.parseInt(stockModel.getValueAt(r,2).toString());
            String hargaStr = stockModel.getValueAt(r,3).toString();
            BigDecimal harga = new BigDecimal(hargaStr);

            String qtyStr = JOptionPane.showInputDialog(this, "Jumlah (stok tersedia: " + stokAvailable + "):", "1");
            if (qtyStr == null) return;
            int qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) { JOptionPane.showMessageDialog(this,"Qty harus > 0"); return; }
            if (qty > stokAvailable) { JOptionPane.showMessageDialog(this,"Qty melebihi stok tersedia"); return; }

            // kalau item sudah ada di cart, tambahkan qty (cek total <= stok)
            boolean found = false;
            for (int i=0;i<cartModel.getRowCount();i++) {
                int idInCart = Integer.parseInt(cartModel.getValueAt(i,0).toString());
                if (idInCart == idDetail) {
                    int existingQty = Integer.parseInt(cartModel.getValueAt(i,1).toString());
                    int newQty = existingQty + qty;
                    if (newQty > stokAvailable) {
                        JOptionPane.showMessageDialog(this,"Jumlah total di cart melebihi stok tersedia"); return;
                    }
                    cartModel.setValueAt(newQty, i, 1);
                    BigDecimal subtotal = harga.multiply(BigDecimal.valueOf(newQty));
                    cartModel.setValueAt(subtotal.toPlainString(), i, 3);
                    found = true;
                    break;
                }
            }
            if (!found) {
                BigDecimal subtotal = harga.multiply(BigDecimal.valueOf(qty));
                cartModel.addRow(new Object[]{ idDetail, qty, harga.toPlainString(), subtotal.toPlainString() });
            }
            updateTotalLabel();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this,"Input angka tidak valid: " + nfe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Gagal tambah ke cart: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRemoveFromCart() {
        int r = tCart.getSelectedRow();
        if (r<0) { JOptionPane.showMessageDialog(this,"Pilih baris di cart untuk dihapus."); return; }
        cartModel.removeRow(r);
        updateTotalLabel();
    }

    private void updateTotalLabel() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i=0;i<cartModel.getRowCount();i++) {
            String sub = cartModel.getValueAt(i,3).toString();
            total = total.add(new BigDecimal(sub));
        }
        lblTotal.setText("Total: " + total.toPlainString());
    }

    private void onProcess() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,"Cart kosong. Tambahkan barang dulu.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // build items
        List<SaleItem> items = new ArrayList<>();
        for (int i=0;i<cartModel.getRowCount();i++) {
            int idDetail = Integer.parseInt(cartModel.getValueAt(i,0).toString());
            int qty = Integer.parseInt(cartModel.getValueAt(i,1).toString());
            BigDecimal price = new BigDecimal(cartModel.getValueAt(i,2).toString());
            items.add(new SaleItem(idDetail, qty, price));
        }

        // voucher selected?
        Integer voucherId = null;
        int idx = cbVoucher.getSelectedIndex();
        if (idx > 0) { // 0 = "Tidak pakai voucher"
            int arrIndex = idx - 1;
            if (arrIndex >= 0 && arrIndex < vouchers.length && vouchers[arrIndex] != null) {
                voucherId = vouchers[arrIndex].getIdVoucher();
            }
        }

        BigDecimal cashPaid = BigDecimal.ZERO;
        try {
            String c = txtCash.getText().trim();
            if (c.isEmpty()) c = "0";
            cashPaid = new BigDecimal(c.replace(",",""));
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this,"Format tunai tidak valid.", "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // create kode transaksi
        String kode = "TRX-" + System.currentTimeMillis();

        // proses transaksi (TransactionDAO sudah handle transaction/rollback)
        try {
            txDao.processSale(items, voucherId, cashPaid, kode);
            JOptionPane.showMessageDialog(this,"Transaksi berhasil. Kode: " + kode, "Sukses", JOptionPane.INFORMATION_MESSAGE);
            // close dialog so MainFrame can refresh lists
            setVisible(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal proses transaksi: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
