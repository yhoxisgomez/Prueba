/*
 * Práctica 3 - PiChatRa - Desarrollado por Yhoxis Gómez y Gustavo González
 * */

package practica3yhoxisgustavo;

import javax.swing.JOptionPane;

import org.jgroups.stack.AddressGenerator;
import org.jgroups.util.OneTimeAddressGenerator;

public class PiChatRa {

	public static void main(String[] args) {
		Draw draw = null;
		String props = null;
		boolean no_channel = false;
		boolean jmx = true;
		boolean use_state = false;
		String group_name = null;
		long state_timeout = 5000;
		boolean use_unicasts = false;
		String name = null;
		boolean send_own_state_on_merge = true;
		AddressGenerator generator = null;

		for (int i = 0; i < args.length; i++) {
			if ("-help".equals(args[i])) {
				return;
			}
			if ("-props".equals(args[i])) {
				props = args[++i];
				continue;
			}
			if ("-no_channel".equals(args[i])) {
				no_channel = true;
				continue;
			}
			if ("-jmx".equals(args[i])) {
				jmx = Boolean.parseBoolean(args[++i]);
				continue;
			}
			if ("-clustername".equals(args[i])) {
				group_name = args[++i];
				continue;
			}
			if ("-state".equals(args[i])) {
				use_state = true;
				continue;
			}
			if ("-timeout".equals(args[i])) {
				state_timeout = Long.parseLong(args[++i]);
				continue;
			}
			if ("-bind_addr".equals(args[i])) {
				System.setProperty("jgroups.bind_addr", args[++i]);
				continue;
			}
			if ("-use_unicasts".equals(args[i])) {
				use_unicasts = true;
				continue;
			}
			if ("-name".equals(args[i])) {
				name = args[++i];
				continue;
			}
			if ("-send_own_state_on_merge".equals(args[i])) {
				send_own_state_on_merge = Boolean.getBoolean(args[++i]);
				continue;
			}
			if ("-uuid".equals(args[i])) {
				generator = new OneTimeAddressGenerator(Long.valueOf(args[++i]));
				continue;
			}
			return;
		}

		try {
			draw = new Draw(props, no_channel, jmx, use_state, state_timeout, use_unicasts, name, send_own_state_on_merge,
					generator);
			if (group_name != null)
				draw.setClusterName(group_name);
			String usrnm = JOptionPane.showInputDialog("ESCRIBA SU NOMBRE DE USUARIO ÚNICO");
			draw.go();
			draw.setTitle(usrnm);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}
