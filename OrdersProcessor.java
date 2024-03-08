package processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class OrdersProcessor {
	private Map<String, Double> itemsDatamMap;
	private Map<String, Integer> summaryMap;

	public OrdersProcessor(String dataFileName) {
		itemsDatamMap = new HashMap<String, Double>();
		summaryMap = new TreeMap<String, Integer>();

		try {
			Scanner scanner = new Scanner(new File(dataFileName));
			while (scanner.hasNext()) {
				itemsDatamMap.put(scanner.next(), scanner.nextDouble());
			}
		} catch (FileNotFoundException e) {

		}
	}
	public static void main(String[] args) {
		Scanner keyboard = new Scanner(System.in);

		System.out.print("Enter item's data file name: ");
		String dataFileName = keyboard.next().trim();

		System.out.print("Enter 'y' for multiple threads, any other character otherwise: ");
		String option = keyboard.next().trim();

		System.out.print("Enter number of orders to process: ");
		int numOrders = keyboard.nextInt();

		System.out.print("Enter order's base filename: ");
		String baseFileName = keyboard.next();

		System.out.print("Enter result's filename: ");
		String resultsFileName = keyboard.next().trim();

		keyboard.close();
		OrdersProcessor processor = new OrdersProcessor(dataFileName);
		if (option.equals("y")) {
			processor.processMultipleThread(numOrders, baseFileName, resultsFileName);
		} else {
			processor.processSingleThread(numOrders, baseFileName, resultsFileName);
		}
	}

	private void processMultipleThread(int numOrders, String baseFileName, String resultsFileName) {
		long startTime = System.currentTimeMillis();
		try {
			PrintWriter writer = new PrintWriter(new File(resultsFileName));
			List<Thread> threads = new ArrayList<Thread>();
			List<StringBuilder> builders = new ArrayList<StringBuilder>();
			for (int i = 1; i <= numOrders; i++) {
				String orderFileName = baseFileName + i + ".txt";
				StringBuilder builder = new StringBuilder();
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						synchronized (summaryMap) {
							processFile(orderFileName, builder, summaryMap);
						}
					}
				};
				Thread thread = new Thread(runnable);
				threads.add(thread);
				builders.add(builder);
				thread.start();
			}
			waitForThreads(threads);
			writeResultsToFile(writer, builders);
			writeSummaryToFile(writer);
			writer.close();
		} catch (FileNotFoundException e) {
		}
		long endTime = System.currentTimeMillis();
		println("Processing time (msec): " + (endTime - startTime));
		println("Results can be found in the file: " + resultsFileName);
	}


	private void waitForThreads(List<Thread> threads) {
		for (int i = 0; i < threads.size(); i++) {
			Thread thread = threads.get(i);
			try {
				thread.join();
			} catch (InterruptedException e) {

			}
		}
	}

	private void writeResultsToFile(PrintWriter writer, List<StringBuilder> builders) {
		for (int i = 0; i < builders.size(); i++) {
			writer.println(builders.get(i));
		}
	}

	private void writeSummaryToFile(PrintWriter writer) {
		StringBuilder builder = new StringBuilder();
		builder.append("***** Summary of all orders *****\n");
		getLines(summaryMap, builder, true);
		writer.println(builder);
	}

	private void processSingleThread(int numOrders, String baseFileName, String resultsFileName) {
		long startTime = System.currentTimeMillis();
		try {
			PrintWriter writer = new PrintWriter(new File(resultsFileName));
			for (int i = 1; i <= numOrders; i++) {
				String orderFileName = baseFileName + i + ".txt";
				StringBuilder builder = new StringBuilder();
				processFile(orderFileName, builder, summaryMap);
				writer.println(builder);
			}
			StringBuilder builder = new StringBuilder();
			builder.append("***** Summary of all orders *****\n");
			getLines(summaryMap, builder, true);
			writer.println(builder);
			writer.close();
		} catch (FileNotFoundException e) {
		}
		long endTime = System.currentTimeMillis();
		println("Processing time (msec): " + (endTime - startTime));
		println("Results can be found in the file: " + resultsFileName);
	}

	private synchronized void println(String line) {
		System.out.println(line);
	}

	private StringBuilder processFile(String orderFileName, StringBuilder builder, Map<String, Integer> summaryMap) {
		try {
			Map<String, Integer> map = new TreeMap<String, Integer>();
			Scanner scanner = new Scanner(new File(orderFileName));
			scanner.next();
			String clientId = scanner.next();
			builder.append("----- Order details for client with Id: " + clientId + " -----\n");

			String prompt = "Reading order for client with id: " + clientId;
			println(prompt);
			while (scanner.hasNext()) {
				String itemName = scanner.next();
				scanner.next();
				if (map.containsKey(itemName)) {
					map.put(itemName, map.get(itemName) + 1);
				} else {
					map.put(itemName, 1);
				}
			}
			synchronized (summaryMap) {
				for (String itemName : map.keySet()) {
					int quantity = map.get(itemName);
					summaryMap.put(itemName, summaryMap.getOrDefault(itemName, 0) + quantity);
				}
			}
			getLines(map, builder, false);
		} catch (FileNotFoundException e) {
		}
		return builder;
	}


	private void getLines(Map<String, Integer> map, StringBuilder builder, boolean summary) {
		double tot = 0;
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			String itemName = entry.getKey();
			int quantity = entry.getValue();
			double price = itemsDatamMap.get(itemName);
			String line = getItemsLine(itemName, quantity, price, summary);
			if (summary) {
				builder.append("Summary - " + line + "\n");
			} else {
				builder.append(line + "\n");
			}
			tot += quantity * price;
		}
		NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
		if (summary) {
			builder.append("Summary Grand Total: ");
		} else {
			builder.append("Order Total: ");
		}
		builder.append(format.format(tot));
	}

	private String getItemsLine(String itemName, int quantity, double price, boolean summary) {
	    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
	    String lineFormat;
	    if (summary) {
			lineFormat = "Item's name: %s, Cost per item: %s, Number sold: %d, " + "Item's Total: %s";
	    } else {
			lineFormat = "Item's name: %s, Cost per item: %s, Quantity: %d, " + "Cost: %s";
	    }
		String line = String.format(lineFormat, itemName, format.format(price), quantity,
				format.format(quantity * price));
	    return line;
	}

}
