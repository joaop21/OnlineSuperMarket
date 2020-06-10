package benchmarking;

import client.ClientStub;

import java.io.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientBot extends Thread{

    private static boolean started, stopped;
    private static int n;
    private static long total;
    private static final long time = 30;

    private synchronized static void registerTime(long tr) {
        if (started && !stopped) {
            n++;
            total += tr;
        }
    }

    private synchronized static boolean terminated() {
        return stopped;
    }

    private synchronized static void startBench() {
        started = true;

        // System.out.println("Started!");
    }

    private synchronized static void stopBench(List<Double> times, List<Double> throughputs) {
        stopped = true;

        // Response time
        double response_time = total/(n*1e9d);
        times.add(response_time);
        // System.out.println("Response Time = "+(response_time));

        // Throughput
        double throughput = n/((double)time);
        throughputs.add(throughput);
        // System.out.println("Throughput = "+(throughput));
    }

    private int login(){
        Random rand = new Random();
        int num = rand.nextInt(3);
        String username = "";
        String pass = "";
        switch (num){
            case 0:
                username = "henrique";
                pass = "henrique";
                break;
            case 1:
                username = "joao";
                pass = "joao";
                break;
            case 2:
                username = "miguel";
                pass = "miguel";
                break;
        }

        return new ClientStub().login(username, pass);
    }

    @Override
    public void run() {

        int userID = login();

        System.out.println("userID: "+userID);

        while(!terminated()) {
            Random rand = new Random();
            int op = rand.nextInt(6);

            long before = System.nanoTime();

            int itemId = rand.nextInt(2);

            switch (op) {
                case 0:
                    // System.out.println("Show Catalog");
                    new ClientStub().getItems();
                    break;
                case 1:
                    // System.out.println("Search Item");
                    new ClientStub().getItem(itemId);
                    break;
                case 2:
                    // System.out.println("Add Item");
                    new ClientStub().addItemToCart(userID, itemId);
                    break;
                case 3:
                    // System.out.println("Remove Item");
                    new ClientStub().removeItemFromCart(userID, itemId);
                    break;
                case 4:
                    // System.out.println("Show Cart");
                    new ClientStub().getCartItems(userID);
                    break;
                case 5:
                    // System.out.println("Order");
                    new ClientStub().order(userID);
                    break;
            }

            long after = System.nanoTime();

            registerTime(after-before);

        }

    }

    private static void runBots(int clients, List<Double> times, List<Double> throughputs) throws Exception {
        ClientBot[] list = new ClientBot[clients];

        // create instances of this object
        for(int i=0; i < list.length; i++)
            list[i] = new ClientBot();

        // Start testing threads
        for (ClientBot bot : list)
            bot.start();

        System.out.println("Warming up...");
        Thread.sleep(5000); // warm up
        System.out.println("Timer started");
        startBench();
        System.out.println("Measuring performance...");
        Thread.sleep(time*1000);  // measures
        System.out.println("Stopped timer");
        stopBench(times, throughputs);

        // wait for threads to die
        System.out.println("Waiting for bots to die...");
        for (ClientBot bot : list)
            bot.join();
    }

    private static void drawTimeChart(List<Integer> labels, List<Double> times) throws IOException {
        DefaultCategoryDataset line_chart_dataset = new DefaultCategoryDataset();
        for (int i = 0; i < labels.size(); i++){
            line_chart_dataset.addValue(labels.get(i), "Time", times.get(i));
        }

        JFreeChart lineChartObject = ChartFactory.createLineChart(
                "Average Response Time by Number of Clients","Number of Clients",
                "Response Time (in seconds)",
                line_chart_dataset,PlotOrientation.VERTICAL,
                true,true,false);

        int width = 640;    /* Width of the image */
        int height = 480;   /* Height of the image */
        File lineChart = new File( "TimeChart.jpeg" );
        ChartUtilities.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
    }

    private static void drawThroughputChart(List<Integer> labels, List<Double> throughputs) throws IOException {
        DefaultCategoryDataset line_chart_dataset = new DefaultCategoryDataset();
        for (int i = 0; i < labels.size(); i++){
            line_chart_dataset.addValue(labels.get(i), "Throughput", throughputs.get(i));
        }

        JFreeChart lineChartObject = ChartFactory.createLineChart(
                "Average Throughput by Number of Clients","Number of Clients",
                "Number of Operations per Second",
                line_chart_dataset,PlotOrientation.VERTICAL,
                true,true,false);

        int width = 640;    /* Width of the image */
        int height = 480;   /* Height of the image */
        File lineChart = new File( "ThroughputChart.jpeg" );
        ChartUtilities.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
    }



    public static void main(String[] args) throws Exception {

        int[] numbers = new int[]{1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987};

        System.out.println("Benchmarking starting...");

        List<Integer> labels = new ArrayList<>();
        List<Double> times = new ArrayList<>();
        List<Double> throughputs = new ArrayList<>();

        for (int clients : numbers) {
            System.out.println(clients + " clients running...");
            labels.add(clients);
            runBots(clients, times, throughputs);
            System.out.println(clients + " finished !");
        }

        System.out.println("Benchmarking finished !");

        System.out.println("Drawing Time Chart...");
        drawTimeChart(labels, times);
        System.out.println("Drawing Throughput Chart...");
        drawThroughputChart(labels, throughputs);

        System.out.println("GreaTF Success!");
    }
}
