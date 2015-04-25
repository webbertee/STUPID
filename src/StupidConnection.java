import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class StupidConnection {
	private PrintWriter out;
	private BufferedReader in;
	private Socket socket;
	private String hostname;

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		StupidConnection conn = new StupidConnection("141.82.57.23");
		conn.start(System.out);
	}

	public StupidConnection(String hostname) throws UnknownHostException,
			IOException {
		this.hostname = hostname;
	}

	public void start(PrintStream output) throws UnknownHostException,
			IOException {
		socket = new Socket(hostname, 12810);

		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			sendMessage("Name: Philipp Weber");

			messageLoop: while (true) {
				List<String> response = receiveMesssage();
				int status = checkHeader(response.get(0));
				switch (status) {
				case 100:
					out.println("Connection established");
					Integer number = getNumber(response);
					if(number != null) {
						sendMessage("Result: " + number);
						continue;
					} else {
						response.forEach(str -> out.println(str));
						throw new StupidException("No number in Message");
					}
				case 200:
					out.println("Test Passed, exiting");
					break messageLoop;
				case 300:
					throw new StupidException("Malformed packet");
				case 500:
					out.println("You already have passed, stopping");
					break messageLoop;
				default:
					throw new StupidException("Unexpected status code "
							+ status);
				}
			}

		} catch (IOException e) {
			out.print(e.getMessage());
		} catch (StupidException e) {
			out.println(e.getMessage());
		} finally {
		in.close();
		out.close();
		socket.close();
		}

	}

	private void sendMessage(String... args) {
		out.print("This is STUPID/1.0\r\n");
		for (String s : args) {
			out.print(s + "\r\n");
		}
		out.print("\r\n");
		out.flush();
	}

	private List<String> receiveMesssage() throws IOException {
		String newLine;
		List<String> message = new ArrayList<String>();
		while ((newLine = in.readLine()) != null) {
			message.add(newLine);
			if (newLine.equals(""))
				break;
		}
		return message;
	}

	private int checkHeader(String header) {
		if (!header.startsWith("STUPID/1.0"))
			throw new StupidException("Wrong protocoll");
		try {
			return Integer.parseInt(header.split(" ")[1]);
		} catch (NumberFormatException e) {
			throw new StupidException("Faulty staus code");
		}
	}

	private Integer getNumber(List<String> response) {
		Integer number;
		String str, key = "Number: ";
		for (int i = 1; i < response.size(); i++) {
			str = response.get(i);
			if (str.startsWith(key)) {
				str = str.substring(key.length());
				try {
					number = Integer.parseInt(str);
				} catch (NumberFormatException e) {
					throw new StupidException("strange Number " + str);
				}
				return number * 42;
			}
		}

		return null;
	}

}

@SuppressWarnings("serial")
class StupidException extends RuntimeException {
	public StupidException(String message) {
		super(message);
	}
}
