package practica3yhoxisgustavo;

import org.jgroups.*;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.util.Util;

import javax.management.MBeanServer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Draw extends ReceiverAdapter implements ActionListener, ChannelListener {
	protected String cluster_name = "draw";
	private JChannel channel = null;
	private int member_size = 1;
	private JFrame mainFrame = null;
	private JPanel sub_panel = null;
	private DrawPanel panel = null;
	private JButton clear_button, leave_button, open_chat_button;
	public DefinitiveChat Definitivechat = new DefinitiveChat();
	private final Random random = new Random(System.currentTimeMillis());
	private final Font default_font = new Font("Helvetica", Font.PLAIN, 12);
	private final Color draw_color = selectColor();
	private static final Color background_color = Color.white;
	boolean no_channel = false;
	boolean jmx;
	private boolean use_state = false;
	private long state_timeout = 5000;
	private boolean use_unicasts = false;
	protected boolean send_own_state_on_merge = true;
	private final List<Address> members = new ArrayList<>();

	public Draw(String props, boolean no_channel, boolean jmx, boolean use_state, long state_timeout,
			boolean use_unicasts, String name, boolean send_own_state_on_merge, AddressGenerator gen) throws Exception {
		this.no_channel = no_channel;
		this.jmx = jmx;
		this.use_state = use_state;
		this.state_timeout = state_timeout;
		this.use_unicasts = use_unicasts;
		if (no_channel)
			return;

		channel = new JChannel(props);
		if (gen != null)
			channel.addAddressGenerator(gen);
		if (name != null)
			channel.setName(name);
		channel.setReceiver(this);
		channel.addChannelListener(this);
		this.send_own_state_on_merge = send_own_state_on_merge;
	}

	public String getClusterName() {
		return cluster_name;
	}

	public void setClusterName(String clustername) {
		if (clustername != null)
			this.cluster_name = clustername;
	}

	public String getChannel() {
		return channel.getAddressAsString();
	}

	private Color selectColor() {
		int red = Math.abs(random.nextInt()) % 255;
		int green = Math.abs(random.nextInt()) % 255;
		int blue = Math.abs(random.nextInt()) % 255;
		return new Color(red, green, blue);
	}

	private void sendToAll(byte[] buf) throws Exception {
		for (Address mbr : members)
			channel.send(new Message(mbr, buf));
	}

	public void go() throws Exception {
		if (!no_channel && !use_state)
			channel.connect(cluster_name);

		/*** DEFINIMOS LAS CARACTERÍSTICAS DE LOS BOTONES ***/
		clear_button = new JButton("Limpiar pizarra");
		clear_button.setFont(default_font);
		clear_button.addActionListener(this);
		clear_button.setForeground(Color.blue);
		leave_button = new JButton("Salir de clase");
		leave_button.setFont(default_font);
		leave_button.addActionListener(this);
		leave_button.setForeground(Color.blue);
		open_chat_button = new JButton("Abrir chat");
		open_chat_button.setFont(default_font);
		open_chat_button.addActionListener(this);
		open_chat_button.setForeground(Color.blue);

		/*** DEFINIMOS LAS CARACTERÍSTICAS DEL JFRAME PRINCIPAL ***/
		mainFrame = new JFrame();
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.pack();
		mainFrame.setLocation(15, 25);
		mainFrame.setBounds(new Rectangle(750, 500));
		mainFrame.setBackground(background_color);
		mainFrame.setResizable(false);

		/*** INSTANCIA DEL DRAWPANEL EN PANEL Y DEFINIMOS SU COLOR ***/
		panel = new DrawPanel(use_state);
		panel.setBackground(background_color);

		/*** INSTANCIA DEL SUB_PANEL Y AÑADIDO DE BOTONES ***/
		sub_panel = new JPanel();
		sub_panel.add("South", clear_button);
		sub_panel.add("South", leave_button);
		sub_panel.add("South", open_chat_button);

		/*** AÑADIDO DE PANELES ***/
		mainFrame.getContentPane().add("Center", panel);
		mainFrame.getContentPane().add("South", sub_panel);

		/*** ESTABLECIMIENTO DE TIMEOUT ***/
		if (!no_channel && use_state) {
			channel.connect(cluster_name, null, state_timeout);
		}

		mainFrame.setVisible(true);
		setTitle(null);
	}

	/*** DEFINE EL TÍTULO DEL JFRAME ***/
	public void setTitle(String title) {
		String tmp = "PiChatRa - ";
		if (no_channel) {
			mainFrame.setTitle("PiChatRa fuera de servicio");
			return;
		}
		if (title != null) {
			mainFrame.setTitle("PiChatRa - Pizarra de: " + title.toUpperCase());
			Definitivechat.setUserName(title);
		} else {
			if (channel.getAddress() != null)
				tmp += channel.getAddress();
			tmp += " (" + member_size + ")";
			mainFrame.setTitle(tmp);
		}
	}

	/*** MÉTODO DE RECIBIMIENTO ***/
	public void receive(Message msg) {
		byte[] buf = msg.getRawBuffer();
		if (buf == null) {
			System.err.println("[" + channel.getAddress() + "] received null buffer from " + msg.getSrc() + ", headers: "
					+ msg.printHeaders());
			return;
		}

		try {
			DrawCommand comm = (DrawCommand) Util.streamableFromByteBuffer(DrawCommand.class, buf, msg.getOffset(),
					msg.getLength());
			switch (comm.mode) {
			case DrawCommand.DRAW:
				if (panel != null)
					panel.drawPoint(comm);
				break;
			case DrawCommand.CLEAR:
				clearPanel();
				break;
			default:
				System.err.println("***** received invalid draw command " + comm.mode);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*** MÉTODO PARA MOSTRAR INGRESOS AL GRUPO ***/
	public void viewAccepted(View v) {
		member_size = v.size();
		if (mainFrame != null)
			setTitle(null);
		members.clear();
		members.addAll(v.getMembers());

		if (v instanceof MergeView) {
			System.out.println("** " + v);

			if (use_state && !members.isEmpty()) {
				Address coord = members.get(0);
				Address local_addr = channel.getAddress();
				if (local_addr != null && !local_addr.equals(coord)) {
					try {

						// GENERA UN COPIA DEL ESTADO INICIAL
						Map<Point, Color> copy = null;
						if (send_own_state_on_merge) {
							synchronized (panel.state) {
								copy = new LinkedHashMap<>(panel.state);
							}
						}
						System.out.println("fetching state from " + coord);
						channel.getState(coord, 5000);
						if (copy != null)
							sendOwnState(copy); // MULTICASTEO DEL ESTADO PARA TODOS
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} else
			System.out.println("** View=" + v);
	}

	/*** STATE SET Y GET ***/
	public void getState(OutputStream ostream) throws Exception {
		panel.writeState(ostream);
	}

	public void setState(InputStream istream) throws Exception {
		panel.readState(istream);
	}

	/*** CALLBACKS ***/

	public void clearPanel() {
		if (panel != null)
			panel.clear();
	}

	public void sendClearPanelMsg() {
		DrawCommand comm = new DrawCommand(DrawCommand.CLEAR);
		try {
			byte[] buf = Util.streamableToByteBuffer(comm);
			if (use_unicasts)
				sendToAll(buf);
			else
				channel.send(new Message(null, null, buf));
		} catch (Exception ex) {
			System.err.println(ex);
		}
	}

	/*** ACTION PERFORMED - MANEJO DE BOTONES ***/
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		switch (command) {
		case "Limpiar pizarra":
			if (no_channel) {
				clearPanel();
				return;
			}
			sendClearPanelMsg();
			break;
		case "Salir de clase":
			stop();
			break;
		case "Abrir chat":
			try {
				Definitivechat.setVisible();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
		default:
			System.out.println("Unknown action");
			break;
		}
	}

	/*** MÉTODO PARA PARAR TRANSMISIÓN ***/
	public void stop() {
		if (!no_channel) {
			try {
				channel.close();
			} catch (Exception ex) {
				System.err.println(ex);
			}
		}
		mainFrame.setVisible(false);
		mainFrame.dispose();
	}

	protected void sendOwnState(final Map<Point, Color> copy) {
		if (copy == null)
			return;
		for (Point point : copy.keySet()) {
			// we don't need the color: it is our draw_color anyway
			DrawCommand comm = new DrawCommand(DrawCommand.DRAW, point.x, point.y, draw_color.getRGB());
			try {
				byte[] buf = Util.streamableToByteBuffer(comm);
				if (use_unicasts)
					sendToAll(buf);
				else
					channel.send(new Message(null, buf));
			} catch (Exception ex) {
				System.err.println(ex);
			}
		}
	}

	/*** CHANNEL LISTENERS INTERFACES ***/

	public void channelConnected(Channel channel) {
		if (jmx) {
			Util.registerChannel((JChannel) channel, "jgroups");
		}
	}

	public void channelDisconnected(Channel channel) {
		if (jmx) {
			MBeanServer server = Util.getMBeanServer();
			if (server != null) {
				try {
					JmxConfigurator.unregisterChannel((JChannel) channel, server, cluster_name);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void channelClosed(Channel channel) {

	}

	@SuppressWarnings("serial")
	protected class DrawPanel extends JPanel implements MouseMotionListener {
		protected final Dimension preferred_size = new Dimension(235, 170);
		protected Image img; // for drawing pixels
		protected Dimension d, imgsize;
		protected Graphics gr;
		protected final Map<Point, Color> state;

		public DrawPanel(boolean use_state) {
			if (use_state)
				state = new LinkedHashMap<>();
			else
				state = null;
			createOffscreenImage(false);
			addMouseMotionListener(this);
			addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					if (getWidth() <= 0 || getHeight() <= 0)
						return;
					createOffscreenImage(false);
				}
			});
		}

		public void writeState(OutputStream outstream) throws IOException {
			if (state == null)
				return;
			synchronized (state) {
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(outstream));
				// DataOutputStream dos=new DataOutputStream(outstream);
				dos.writeInt(state.size());
				for (Map.Entry<Point, Color> entry : state.entrySet()) {
					Point point = entry.getKey();
					Color col = entry.getValue();
					dos.writeInt(point.x);
					dos.writeInt(point.y);
					dos.writeInt(col.getRGB());
				}
				dos.flush();
				System.out.println("wrote " + state.size() + " elements");
			}
		}

		public void readState(InputStream instream) throws IOException {
			DataInputStream in = new DataInputStream(new BufferedInputStream(instream));
			Map<Point, Color> new_state = new LinkedHashMap<>();
			int num = in.readInt();
			for (int i = 0; i < num; i++) {
				Point point = new Point(in.readInt(), in.readInt());
				Color col = new Color(in.readInt());
				new_state.put(point, col);
			}

			synchronized (state) {
				state.clear();
				state.putAll(new_state);
				System.out.println("read " + state.size() + " elements");
				createOffscreenImage(true);
			}
		}

		void createOffscreenImage(boolean discard_image) {
			d = getSize();
			if (discard_image) {
				img = null;
				imgsize = null;
			}
			if (img == null || imgsize == null || imgsize.width != d.width || imgsize.height != d.height) {
				img = createImage(d.width, d.height);
				if (img != null) {
					gr = img.getGraphics();
					if (gr != null && state != null) {
						// drawState();
					}
				}
				imgsize = d;
			}
			repaint();
		}

		/*** MOUSE MOTION INTERFACES ***/

		public void mouseMoved(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			int x = e.getX(), y = e.getY();
			DrawCommand comm = new DrawCommand(DrawCommand.DRAW, x, y, draw_color.getRGB());

			if (no_channel) {
				drawPoint(comm);
				return;
			}

			try {
				byte[] buf = Util.streamableToByteBuffer(comm);
				if (use_unicasts)
					sendToAll(buf);
				else
					channel.send(new Message(null, null, buf));
			} catch (Exception ex) {
				System.err.println(ex);
			}
		}

		public void drawPoint(DrawCommand c) {
			if (c == null || gr == null)
				return;
			Color col = new Color(c.rgb);
			gr.setColor(col);
			gr.fillOval(c.x, c.y, 10, 10);
			repaint();
			if (state != null) {
				synchronized (state) {
					state.put(new Point(c.x, c.y), col);
				}
			}
		}

		public void clear() {
			if (gr == null)
				return;
			gr.clearRect(0, 0, getSize().width, getSize().height);
			repaint();
			if (state != null) {
				synchronized (state) {
					state.clear();
				}
			}
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (img != null) {
				g.drawImage(img, 0, 0, null);
			}
		}

	}

}