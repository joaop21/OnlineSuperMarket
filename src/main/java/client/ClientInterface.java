package client;

import application.Item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ClientInterface {

    private static final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private static String username = null;

    public static void start () {

        System.out.println("#####################################################");
        System.out.println("#               Welcome to InTFmarche               #");
        System.out.println("#####################################################");

        loginScreen();

    }

    public static void loginScreen () {

        try {

            System.out.println("####################### Login #######################");

            System.out.print("Username: ");
            String username = in.readLine();

            System.out.print("Password: ");
            String password = in.readLine();

            System.out.println("#####################################################");

            if (new ClientStub().login(username, password)) { ClientInterface.username = username; menuScreen(); }
            else { errorScreen("The credentials provided are invalid. Try again."); loginScreen(); }

        } catch (IOException e) {

            exceptionScreen("Oops, IO Error. Try again!");

            loginScreen();

        }

    }

    public static void menuScreen () {

        try {

            System.out.println("####################### Menu ########################");

            System.out.println("# 1 - Show Catalog");;
            System.out.println("# 2 - Search Item (by name)");;
            System.out.println("# 3 - Search Item (by id)");;
            System.out.println("# 4 - Add Item to Cart");;
            System.out.println("# 5 - Remove Item from Cart");;
            System.out.println("# 6 - Show Cart");;
            System.out.println("# 7 - Complete order");;

            System.out.println("#####################################################");

            System.out.print("Option: ");
            int option = Integer.parseInt(in.readLine());

            System.out.println("#####################################################");

            switch (option) {

                case 1:
                    catalogScreen();
                    return;
                case 2:
                    searchNameScreen();
                    return;
                case 3:
                    searchIdScreen();
                    return;
                case 4:
                    addScreen();
                    return;
                case 5:
                    remScreen();
                    return;
                case 6:
                    cartScreen();
                    return;
                case 7:
                    orderScreen();
                    return;
                default:
                    errorScreen("# Please choose one of the menu options.");
                    menuScreen();

            }

        } catch (IOException e) {

            exceptionScreen("# Oops, IO Error. Try again!");

            menuScreen();

        } catch (NumberFormatException e) {

            exceptionScreen("# Please provide a number.");

            menuScreen();

        }

    }

    private static void catalogScreen() {

        List<Item> items = new ClientStub().getItems();

        System.out.println("###################### Catalog ######################");

        for (Item it: items) System.out.println(it.toPrettyString());

        System.out.println("#####################################################");

        menuScreen();

    }

    private static void searchNameScreen() {

        try {

            System.out.println("###################### Search #######################");

            System.out.print("Name: ");
            String itemName = in.readLine();

            System.out.println("#####################################################");

            itemScreen(new ClientStub().getItem(itemName));

            menuScreen();

        } catch (IOException e) {

            exceptionScreen("# Oops, IO Error. Try again!");

            searchNameScreen();

        }
    }

    private static void searchIdScreen() {

        try {

            System.out.println("###################### Search #######################");

            System.out.print("Id: ");
            int itemId = Integer.parseInt(in.readLine());

            System.out.println("#####################################################");

            // MISSING: CHECK IF ITEM EXISTS
            Item item = new ClientStub().getItem(itemId);
            itemScreen(item);

            menuScreen();

        } catch (IOException e) {

            exceptionScreen("# Oops, IO Error. Try again!");

            searchIdScreen();

        } catch (NumberFormatException e) {

            exceptionScreen("# Please provide a number.");

        }
    }

    private static void itemScreen(Item item) {

        System.out.println("####################### Item ########################");

        System.out.println(item.toDescriptionString());

        System.out.println("#####################################################");

    }

    private static void addScreen() {

        try {

            System.out.println("################# Add Item to Cart ##################");

            System.out.print("Id: ");
            int itemId = Integer.parseInt(in.readLine());

            System.out.println("#####################################################");

            if (new ClientStub().addItemToCart(ClientInterface.username, itemId))

                System.out.println("# Item added to cart!");

            else

                System.out.println("# Item not added to cart!");

            System.out.println("#####################################################");

            menuScreen();

        } catch (IOException e) {

            exceptionScreen("# Oops, IO Error. Try again!");

            addScreen();

        } catch (NumberFormatException e) {

            exceptionScreen("# Please provide a number.");

        }

    }

    private static void remScreen() {

        try {

            System.out.println("############### Remove Item from Cart ###############");

            System.out.print("Id: ");
            int itemId = Integer.parseInt(in.readLine());

            System.out.println("#####################################################");

            new ClientStub().removeItemFromCart(ClientInterface.username, itemId);

            menuScreen();

        } catch (IOException e) {

            exceptionScreen("# Oops, IO Error. Try again!");

            remScreen();

        } catch (NumberFormatException e) {

            exceptionScreen("# Please provide a number.");

        }

    }

    private static void cartScreen() {

        List<Item> items = new ClientStub().getCartItems(ClientInterface.username);

        System.out.println("####################### Cart ########################");

        for (Item it: items) System.out.println(it.toPrettyString());

        System.out.println("#####################################################");

        menuScreen();

    }

    private static void orderScreen() {

        if (new ClientStub().order(ClientInterface.username))

            System.out.println("# All items ordered.");

        else

            System.out.println("# Couldn't order every cart item.");

        System.out.println("#####################################################");

    }

    public static void errorScreen (String message) {

        System.out.println("####################### Error #######################");
        System.out.println(message);
        System.out.println("#####################################################");

    }

    public static void exceptionScreen (String message) {

        System.out.println("#####################################################");
        System.out.println("##################### Exception #####################");
        System.out.println(message);
        System.out.println("#####################################################");

    }



}