import java.io.BufferedReader;
import java.io.Closeable;
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

	public static void main(String[] args) {
		StupidConnection conn = new StupidConnection("141.82.57.23");
		conn.start(System.out);
	}

	public StupidConnection(String hostname) {
		this.hostname = hostname;
	}

	public void start(PrintStream output)  {
		try {
			//Open socket, get input and output Stream
			socket = new Socket(hostname, 12810);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			
			//Send initial message
			sendMessage("Name: Philipp Weber");
			
			//messageLoop receives Messages until transmission is finished 
			messageLoop: while (true) {
				List<String> response = receiveMesssage();
				
				//check Header and get status code
				int status = checkHeader(response.get(0));
				
				//react depending on status code:
				switch (status) {
				case 100:
					output.println("Connection established");
					//extract number from message
					Integer number = getNumber(response);
					if (number != null) {
						sendMessage("Result: " + number);
						continue;
					} else {
						response.forEach(str -> output.println(str));
						throw new StupidException("No number in Message");
					}
				case 200:
					output.println("Test Passed, exiting");
					break messageLoop;
				case 300:
					throw new StupidException("Malformed packet");
				case 400:
					throw new StupidException("Result wrong");
				case 500:
					output.println("You already have passed, stopping");
					break messageLoop;
				default:
					throw new StupidException("Unexpected status code "
							+ status);
				}
			}

		} catch (UnknownHostException e) {
			output.println("Unknown host: " + hostname);
		} catch (IOException e) {
			output.print(e.getStackTrace());
		} catch (StupidException e) {
			output.println(e.getMessage());
		} finally {
			//make sure ports and streams are closed
			saveClose(in, output);
			saveClose(out, output);
			saveClose(socket, output);
		}
	}
	
	private void saveClose(Closeable c, PrintStream output) {
			try {
				c.close();
			} catch (IOException e) {
				output.print(e.getStackTrace());
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

	private int checkHeader(String header) throws StupidException {
		if (!header.startsWith("STUPID/1.0"))
			throw new StupidException("Wrong protocoll");
		try {
			return Integer.parseInt(header.split(" ")[1]);
		} catch (NumberFormatException e) {
			throw new StupidException("Faulty staus code");
		}
	}
	
	private final String nameKey = "Number: ";
	private Integer getNumber(List<String> response) throws StupidException {
		String str;
		for (int i = 1; i < response.size(); i++) {
			str = response.get(i);
			if (str.startsWith(nameKey)) {
				str = str.substring(nameKey.length());
				try {
					return (Integer.parseInt(str) * 42);
				} catch (NumberFormatException e) {
					throw new StupidException("strange Number " + str);
				}
			}
		}
		return null;
	}

}

@SuppressWarnings("serial")
class StupidException extends Exception {
	public StupidException(String message) {
		super(message);
	}
}
