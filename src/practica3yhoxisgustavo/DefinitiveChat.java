package practica3yhoxisgustavo;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JButton;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

public class DefinitiveChat extends ReceiverAdapter {
	
	JChannel channel;
	String user_name = System.getProperty("user.name", "n/a");
	final List<String> state = new LinkedList<>();
	private JPanel contentPane;
	private static JTextField chat_type_area;
	private static JTextArea chat_area;
	private JFrame chatFrame;
	
	public DefinitiveChat() throws Exception {
		channel = new JChannel();
		channel.setReceiver(this);
		channel.connect("ChatCluster");
		channel.getState(null, 10000);
		GUIChat();
	}

	@Override
	public void viewAccepted(View new_view) {
		System.out.println("** view: " + new_view);
	}

	@Override
	public void receive(Message msg) {
		String line = msg.getObject().toString();
		chat_area.append(line + "\n");
		synchronized (state) {
			state.add(line);
		}
	}

	@Override
	public void getState(OutputStream output) throws Exception {
		synchronized (state) {
			Util.objectToStream(state, new DataOutputStream(output));
		}
	}

	// INTERFAZ GRÁFICA
	public void GUIChat() {
		chat_area = new JTextArea();
		chat_type_area = new JTextField();
		chatFrame = new JFrame();
		chatFrame.setResizable(false);
		chatFrame.setBounds(100, 100, 300, 500);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		chatFrame.setContentPane(contentPane);
		contentPane.setLayout(null);

		chat_area.setBounds(10, 11, 264, 359);
		contentPane.add(chat_area);

		JButton send_button = new JButton("ENVIAR");
		send_button.setForeground(Color.BLUE);
		send_button.setFont(new Font("Tahoma", Font.PLAIN, 10));
		send_button.addActionListener((ActionEvent e) -> {
			if (chat_type_area.getText().equals(""))
				return;
			String line = user_name + ": " + chat_type_area.getText();
			Message msg = new Message(null, null, line);
			try {
				channel.send(msg);
			} catch (Exception e1) {
			}
			chat_type_area.setText("");
		});

		send_button.setBounds(197, 384, 77, 66);
		contentPane.add(send_button);

		chat_type_area.setBounds(10, 384, 177, 66);
		contentPane.add(chat_type_area);
		chat_type_area.setColumns(10);
		chatFrame.setResizable(false);
	}

	public void setVisible() {
		chatFrame.setVisible(true);
	}

	public void setUserName(String usrnm) {
		user_name = usrnm;
		chatFrame.setTitle("Chat de: " + usrnm);
	}
}