package com.intel.galileo.flash.tool;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import java.awt.Toolkit;

public class About {
	
	private JFrame frame;
	
	public static void showMe(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					About window = new About();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public About() {
		initialize();
	}
	
	private void initialize() {
		frame = new JFrame();
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(About.class.getResource("/icons/application.png")));
		frame.setResizable(false);
		frame.setBounds(100, 100, 527, 295);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JButton btnExitButton = new JButton("Close");
		btnExitButton.setSelectedIcon(null);
		
		btnExitButton.addActionListener(new java.awt.event.ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				frame.dispose();
			}
		});
		
		String HTMLlabelStr = "<html><div style='text-align:center'> "
			+ "<h1><strong>Intel&reg Galileo Firmware Update "
			+ Firmware
	}
}