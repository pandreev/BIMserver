package org.bimserver;

/******************************************************************************
 * (c) Copyright bimserver.org 2009
 * Licensed under GNU GPLv3
 * http://www.gnu.org/licenses/gpl-3.0.txt
 * For more information mail to license@bimserver.org
 *
 * Bimserver.org is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bimserver.org is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License a 
 * long with Bimserver.org . If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Expander extends JFrame {
	private static final long serialVersionUID = 5356018168589837130L;
	private Process exec;

	public static void main(String[] args) {
		new Expander().start();
	}

	private void start() {
		final JTextArea logField = new JTextArea();
		PrintStream out = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				logField.append(new String(new char[] { (char) b }));
				logField.setCaretPosition(logField.getDocument().getLength());
			}
		});
		System.setOut(out);
		System.setErr(out);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("BIMserver Starter");
		try {
			setIconImage(ImageIO.read(getClass().getResource("logo_small.png")));
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		setSize(640, 500);
		getContentPane().setLayout(new BorderLayout());
		JPanel fields = new JPanel(new SpringLayout());

		JLabel jvmLabel = new JLabel("JVM");
		fields.add(jvmLabel);

		final JTextField jvmField = new JTextField("default");
		JButton browserJvm = new JButton("Browse...");
		browserJvm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File currentFile = new File(jvmField.getText());
				JFileChooser chooser = new JFileChooser(currentFile.exists() ? currentFile : new File("."));
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int showOpenDialog = chooser.showOpenDialog(Expander.this);
				if (showOpenDialog == JFileChooser.APPROVE_OPTION) {
					jvmField.setText(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		JPanel jvmPanel = new JPanel();
		jvmPanel.setLayout(new BorderLayout());
		jvmPanel.add(jvmField, BorderLayout.CENTER);
		jvmPanel.add(browserJvm, BorderLayout.EAST);
		fields.add(jvmPanel);

		JLabel addressLabel = new JLabel("Address");
		fields.add(addressLabel);

		final JTextField addressField = new JTextField("localhost");
		fields.add(addressField);

		JLabel portLabel = new JLabel("Port");
		fields.add(portLabel);

		final JTextField portField = new JTextField("8082");
		fields.add(portField);

		JLabel heapSizeLabel = new JLabel("Heap Size");
		fields.add(heapSizeLabel);

		final JTextField heapSizeField = new JTextField("1024m");
		fields.add(heapSizeField);

		JLabel stackSizeLabel = new JLabel("Stack Size");
		fields.add(stackSizeLabel);

		final JTextField stackSizeField = new JTextField("1024k");
		fields.add(stackSizeField);

		SpringUtilities.makeCompactGrid(fields, 5, 2, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		final JButton startStopButton = new JButton("Start");

		startStopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (startStopButton.getText().equals("Start")) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							if (jvmField.getText().equalsIgnoreCase("default") || new File(jvmField.getText()).exists()) {
								File file = expand();
								startStopButton.setText("Stop");
								start(file, addressField.getText(), portField.getText(), heapSizeField.getText(), stackSizeField.getText(), jvmField.getText());
							} else {
								JOptionPane.showMessageDialog(Expander.this, "JVM field should contain a valid JVM directory, or 'default' for the default JVM");
							}
						}
					}).start();
				} else if (startStopButton.getText().equals("Stop")) {
					if (exec != null) {
						exec.destroy();
						System.out.println("Server has been shut down");
						exec = null;
						startStopButton.setText("Start");
					}
				}
			}
		});

		buttons.add(startStopButton);

		JButton launchWebBrowser = new JButton("Launch Webbrowser");
		launchWebBrowser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("http://" + addressField.getText() + ":" + portField.getText()));
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		});
		buttons.add(launchWebBrowser);

		logField.setEditable(true);
		logField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					exec.getOutputStream().write(e.getKeyChar());
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						exec.getOutputStream().flush();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(logField);
		getContentPane().add(fields, BorderLayout.NORTH);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if (exec != null) {
					exec.destroy();
				}
			}
		}));
		setVisible(true);
	}

	private void start(File destDir, String address, String port, String heapsize, String stacksize, String jvmPath) {
		try {
			String command = "";
			if (jvmPath.equalsIgnoreCase("default")) {
				command = "java";
			} else {
				File jvm = new File(jvmPath);
				if (jvm.exists()) {
					File jre = new File(jvm, "jre");
					if (!jre.exists()) {
						jre = jvm;
					}
					command = new File(jre, "bin" + File.separator + "java").getAbsolutePath();
					File jreLib = new File(jre, "lib");
					command += " -Xbootclasspath:\"";
					for (File file : jreLib.listFiles()) {
						if (file.getName().endsWith(".jar")) {
							command += file.getAbsolutePath() + File.pathSeparator;
						}
					}
					if (jre != jvm) {
						command += new File(jvm, "lib" + File.separator + "tools.jar");
					}
					command += "\"";
				}
			}
			command += " -Xmx" + heapsize;
			command += " -Xss" + stacksize;
			command += " -classpath";
			command += " lib" + File.pathSeparator;
			File dir = new File(destDir + File.separator + "lib");
			for (File lib : dir.listFiles()) {
				if (lib.isFile()) {
					command += "lib" + File.separator + lib.getName() + File.pathSeparator;
				}
			}
			if (command.endsWith(File.pathSeparator)) {
				command = command.substring(0, command.length()-1);
			}
			command += " org.bimserver.Server";
			command += " address=" + address;
			command += " port=" + port;
			System.out.println("Running: " + command);
			exec = Runtime.getRuntime().exec(command, null, destDir);

			new Thread(new Runnable(){
				@Override
				public void run() {
					BufferedInputStream inputStream = new BufferedInputStream(exec.getInputStream());
					byte[] buffer = new byte[1024];
					int red;
					try {
						red = inputStream.read(buffer);
						while (red != -1) {
							String s = new String(buffer, 0, red);
							System.out.print(s);
							red = inputStream.read(buffer);
						}
					} catch (IOException e) {
					}
				}}).start();
			new Thread(new Runnable(){
				@Override
				public void run() {
					BufferedInputStream errorStream = new BufferedInputStream(exec.getErrorStream());
					byte[] buffer = new byte[1024];
					int red;
					try {
						red = errorStream.read(buffer);
						while (red != -1) {
							String s = new String(buffer, 0, red);
							System.out.print(s);
							red = errorStream.read(buffer);
						}
					} catch (IOException e) {
					}
				}}).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File expand() {
		JarFile jar;
		String jarFileName = getJarFileNameNew();
		File destDir = new File(jarFileName.substring(0, jarFileName.indexOf(".jar")));
		if (!destDir.isDirectory()) {
			System.out.println("Expanding " + jarFileName);
			try {
				jar = new java.util.jar.JarFile(jarFileName);
				Enumeration<JarEntry> enumr = jar.entries();
				while (enumr.hasMoreElements()) {
					JarEntry file = (JarEntry) enumr.nextElement();
					System.out.println(file.getName());
					File f = new File(destDir, file.getName());
					if (file.isDirectory()) {
						if (!f.getParentFile().exists()) {
							f.getParentFile().mkdir();
						}
						f.mkdir();
						continue;
					}
					InputStream is = jar.getInputStream(file);
					FileOutputStream fos = new FileOutputStream(f);
					byte[] buffer = new byte[1024];
					int red = is.read(buffer);
					while (red != -1) {
						fos.write(buffer, 0, red);
						red = is.read(buffer);
					}
					fos.close();
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("No expanding necessary");
		}
		return destDir;
	}

	private String getJarFileNameNew() {
		String name = this.getClass().getName().replace(".", "/") + ".class";
		URL urlJar = getClass().getClassLoader().getResource(name);
		String urlString = urlJar.getFile();
		urlString = urlString.substring(urlString.indexOf(":") + 1, urlString.indexOf("!"));
		try {
			return URLDecoder.decode(urlString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}