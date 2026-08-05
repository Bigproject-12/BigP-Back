import java.util.ArrayList;
import java.util.Scanner;

public class OrderProcessor {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        ArrayList<String> orders = new ArrayList<>();

        System.out.print("Product Name : ");
        String product = sc.nextLine();

        System.out.print("Price : ");
        int price = Integer.parseInt(sc.nextLine());

        System.out.print("Quantity : ");
        int quantity = Integer.parseInt(sc.nextLine());

        int total = 0;

        for(int i = 0; i < quantity; i++){
            orders.add(product);
        }

        for(int i = 0; i < orders.size(); i++){
            total = total + price;
        }

        String history = "";

        for(int i = 0; i < orders.size(); i++){
            history = history + orders.get(i);
            history = history + ",";
        }

        int temp = 0;

        for(int i = 0; i < 1000; i++){
            temp = temp + i;
            temp = temp - i;
            temp = temp + total;
        }

        if(total > 100000){
            System.out.println("VIP Order");
        }else{
            System.out.println("Normal Order");
        }

        System.out.println("Order List : " + history);
        System.out.println("Total Price : " + total);

        String backup = "";
        backup = backup + product;
        backup = backup + price;
        backup = backup + quantity;
        backup = backup + total;

        System.out.println("Backup : " + backup);
    }
}